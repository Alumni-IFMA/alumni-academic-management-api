package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.support.SupportMessageRequestDTO;
import com.alumni.academic_management_api.dto.support.SupportMessageResponseDTO;
import com.alumni.academic_management_api.entity.SupportMessage;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.mapper.SupportMessageMapper;
import com.alumni.academic_management_api.repository.SupportMessageRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SupportMessageServiceTest {

    @Mock
    private SupportMessageRepository supportMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupportMessageMapper supportMessageMapper;

    @InjectMocks
    private SupportMessageService supportMessageService;

    @Nested
    class Send {

        @Test
        void givenValidUser_whenSend_thenSaveMessageWithUserNameAndEmail() {
            String email = "joao@email.com";
            User user = User.builder().id(1L).name("João Silva").email(email).build();
            SupportMessageRequestDTO request = SupportMessageRequestDTO.builder()
                    .subject("Dúvida sobre certificado")
                    .message("Como emito meu certificado?")
                    .build();

            ArgumentCaptor<SupportMessage> captor = ArgumentCaptor.forClass(SupportMessage.class);
            Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            Mockito.when(supportMessageRepository.save(captor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            SupportMessageResponseDTO expectedResponse = SupportMessageResponseDTO.builder().id(10L).build();
            Mockito.when(supportMessageMapper.toResponseDTO(any(SupportMessage.class)))
                    .thenReturn(expectedResponse);

            SupportMessageResponseDTO response = supportMessageService.send(email, request);

            assertThat(response).isEqualTo(expectedResponse);
            assertThat(captor.getValue().getUser()).isEqualTo(user);
            assertThat(captor.getValue().getName()).isEqualTo("João Silva");
            assertThat(captor.getValue().getEmail()).isEqualTo(email);
            assertThat(captor.getValue().getSubject()).isEqualTo("Dúvida sobre certificado");
            assertThat(captor.getValue().getMessage()).isEqualTo("Como emito meu certificado?");
        }

        @Test
        void givenUserNotFound_whenSend_thenThrowBusinessException() {
            String email = "notfound@email.com";
            SupportMessageRequestDTO request = SupportMessageRequestDTO.builder()
                    .subject("Assunto")
                    .message("Mensagem")
                    .build();

            Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supportMessageService.send(email, request))
                    .isInstanceOf(BusinessException.class);

            Mockito.verify(supportMessageRepository, Mockito.never()).save(any());
        }
    }
}
