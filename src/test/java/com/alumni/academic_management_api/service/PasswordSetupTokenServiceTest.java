package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.entity.PasswordSetupToken;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.repository.PasswordSetupTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordSetupTokenServiceTest {

    @Mock
    private PasswordSetupTokenRepository tokenRepository;

    @InjectMocks
    private PasswordSetupTokenService passwordSetupTokenService;

    @Test
    void givenUser_whenGenerateSetupToken_thenStoredTokenIsHashNotRawValue() throws Exception {
        User user = User.builder().id(1L).email("joao@email.com").build();
        ArgumentCaptor<PasswordSetupToken> captor = ArgumentCaptor.forClass(PasswordSetupToken.class);
        when(tokenRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = passwordSetupTokenService.generateSetupToken(user);

        String expectedHash = sha256Hex(rawToken);
        assertThat(captor.getValue().getToken()).isNotEqualTo(rawToken);
        assertThat(captor.getValue().getToken()).isEqualTo(expectedHash);
    }

    @Test
    void givenUser_whenGenerateSetupToken_thenExpiresAt48HoursFromNow() {
        User user = User.builder().id(1L).email("joao@email.com").build();
        ArgumentCaptor<PasswordSetupToken> captor = ArgumentCaptor.forClass(PasswordSetupToken.class);
        when(tokenRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        passwordSetupTokenService.generateSetupToken(user);

        LocalDateTime expectedExpiry = LocalDateTime.now().plusHours(48);
        assertThat(captor.getValue().getExpiresAt()).isCloseTo(expectedExpiry, within(2, ChronoUnit.SECONDS));
    }

    @Test
    void givenUser_whenGenerateSetupToken_thenSavesTokenLinkedToUserWithNoUsedAt() {
        User user = User.builder().id(1L).email("joao@email.com").build();
        ArgumentCaptor<PasswordSetupToken> captor = ArgumentCaptor.forClass(PasswordSetupToken.class);
        when(tokenRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        passwordSetupTokenService.generateSetupToken(user);

        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getUsedAt()).isNull();
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        verify(tokenRepository).save(captor.getValue());
    }

    private String sha256Hex(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    }
}
