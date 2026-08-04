package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.entity.RefreshToken;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.InvalidTokenException;
import com.alumni.academic_management_api.repository.RefreshTokenRepository;
import com.alumni.academic_management_api.util.TokenHasher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long REFRESH_EXPIRATION_MS = 604800000L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    private RefreshTokenService newService() {
        return new RefreshTokenService(refreshTokenRepository, REFRESH_EXPIRATION_MS);
    }

    @Nested
    class Generate {

        @Test
        void whenGenerate_thenSavesHashedTokenAndReturnsRawToken() {
            refreshTokenService = newService();
            User user = User.builder().id(1L).email("user@email.com").build();

            String rawToken = refreshTokenService.generate(user);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            Mockito.verify(refreshTokenRepository).save(captor.capture());

            assertThat(rawToken).isNotBlank();
            assertThat(captor.getValue().getTokenHash()).isEqualTo(TokenHasher.sha256(rawToken));
            assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawToken);
            assertThat(captor.getValue().isRevoked()).isFalse();
        }
    }

    @Nested
    class Rotate {

        @Test
        void givenValidToken_whenRotate_thenRevokesOldAndReturnsNewToken() {
            refreshTokenService = newService();
            User user = User.builder().id(1L).email("user@email.com").build();
            String rawToken = "raw-refresh-token";
            RefreshToken stored = RefreshToken.builder()
                    .tokenHash(TokenHasher.sha256(rawToken))
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusDays(1))
                    .revoked(false)
                    .build();

            Mockito.when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken)))
                    .thenReturn(Optional.of(stored));

            RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawToken);

            assertThat(stored.isRevoked()).isTrue();
            assertThat(result.user()).isEqualTo(user);
            assertThat(result.rawToken()).isNotBlank();
            Mockito.verify(refreshTokenRepository).save(stored);
            Mockito.verify(refreshTokenRepository, Mockito.times(2)).save(Mockito.any(RefreshToken.class));
        }

        @Test
        void givenUnknownToken_whenRotate_thenThrowInvalidTokenException() {
            refreshTokenService = newService();
            Mockito.when(refreshTokenRepository.findByTokenHash(Mockito.anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.rotate("unknown-token"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        void givenExpiredToken_whenRotate_thenThrowInvalidTokenException() {
            refreshTokenService = newService();
            User user = User.builder().id(1L).email("user@email.com").build();
            String rawToken = "expired-refresh-token";
            RefreshToken stored = RefreshToken.builder()
                    .tokenHash(TokenHasher.sha256(rawToken))
                    .user(user)
                    .expiryDate(LocalDateTime.now().minusMinutes(1))
                    .revoked(false)
                    .build();

            Mockito.when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken)))
                    .thenReturn(Optional.of(stored));

            assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                    .isInstanceOf(InvalidTokenException.class);

            Mockito.verify(refreshTokenRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenRevokedToken_whenRotate_thenThrowInvalidTokenException() {
            refreshTokenService = newService();
            User user = User.builder().id(1L).email("user@email.com").build();
            String rawToken = "revoked-refresh-token";
            RefreshToken stored = RefreshToken.builder()
                    .tokenHash(TokenHasher.sha256(rawToken))
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusDays(1))
                    .revoked(true)
                    .build();

            Mockito.when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken)))
                    .thenReturn(Optional.of(stored));

            assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                    .isInstanceOf(InvalidTokenException.class);

            Mockito.verify(refreshTokenRepository, Mockito.never()).save(Mockito.any());
        }
    }

    @Nested
    class Revoke {

        @Test
        void givenExistingToken_whenRevoke_thenMarksAsRevoked() {
            refreshTokenService = newService();
            String rawToken = "raw-token";
            RefreshToken stored = RefreshToken.builder()
                    .tokenHash(TokenHasher.sha256(rawToken))
                    .revoked(false)
                    .build();

            Mockito.when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken)))
                    .thenReturn(Optional.of(stored));

            refreshTokenService.revoke(rawToken);

            assertThat(stored.isRevoked()).isTrue();
            Mockito.verify(refreshTokenRepository).save(stored);
        }

        @Test
        void givenUnknownToken_whenRevoke_thenDoesNothing() {
            refreshTokenService = newService();
            Mockito.when(refreshTokenRepository.findByTokenHash(Mockito.anyString()))
                    .thenReturn(Optional.empty());

            refreshTokenService.revoke("unknown-token");

            Mockito.verify(refreshTokenRepository, Mockito.never()).save(Mockito.any());
        }
    }

    @Nested
    class RevokeAllForUser {

        @Test
        void whenRevokeAllForUser_thenDelegatesToRepository() {
            refreshTokenService = newService();
            User user = User.builder().id(1L).build();

            refreshTokenService.revokeAllForUser(user);

            Mockito.verify(refreshTokenRepository).revokeAllByUser(user);
        }
    }
}