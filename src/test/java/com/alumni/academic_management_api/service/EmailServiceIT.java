package com.alumni.academic_management_api.service;

import io.minio.MinioClient;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class EmailServiceIT {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    @SuppressWarnings("unused")
    private MinioClient minioClient;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Nested
    class SendApprovalEmail {

        @Test
        void givenValidRecipient_whenSendApprovalEmail_thenEmailIsSentAsync() {
            emailService.sendApprovalEmail("joao@email.com", "João");

            verify(mailSender, timeout(3000)).send(any(MimeMessage.class));
        }

        @Test
        void givenValidRecipient_whenSendApprovalEmail_thenSubjectIsCorrect() throws Exception {
            emailService.sendApprovalEmail("joao@email.com", "João");

            verify(mailSender, timeout(3000)).send(any(MimeMessage.class));
            assertThat(mimeMessage.getSubject()).isEqualTo("Seu cadastro foi aprovado!");
        }
    }

    @Nested
    class SendForgotPasswordEmail {

        @Test
        void givenValidRecipient_whenSendForgotPasswordEmail_thenEmailIsSentAsync() {
            emailService.sendForgotPasswordEmail("joao@email.com", "abc123");

            verify(mailSender, timeout(3000)).send(any(MimeMessage.class));
        }

        @Test
        void givenValidRecipient_whenSendForgotPasswordEmail_thenSubjectIsCorrect() throws Exception {
            emailService.sendForgotPasswordEmail("joao@email.com", "abc123");

            verify(mailSender, timeout(3000)).send(any(MimeMessage.class));
            assertThat(mimeMessage.getSubject()).isEqualTo("Redefinição de senha");
        }
    }
}
