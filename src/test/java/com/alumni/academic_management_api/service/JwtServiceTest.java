package com.alumni.academic_management_api.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "test-secret-key-with-at-least-32-characters-long";
    private static final long EXPIRATION_MS = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Nested
    class Constructor {

        @Test
        void givenSecretShorterThan32Bytes_whenConstructing_thenThrowIllegalStateException() {
            assertThatThrownBy(() -> new JwtService("too-short-secret", EXPIRATION_MS))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("32");
        }
    }

    @Nested
    class GenerateToken {

        @Test
        void givenEmail_whenGenerateToken_thenEmailIsPreserved() {
            String token = jwtService.generateToken("user@test.com");

            assertThat(token).isNotBlank();
            assertThat(jwtService.parseClaims(token).getSubject()).isEqualTo("user@test.com");
        }
    }

    @Nested
    class ParseClaims {

        @Test
        void givenExpiredToken_whenParseClaims_thenThrowExpiredJwtException() {
            JwtService shortLivedJwtService = new JwtService(SECRET, -1000L);
            String token = shortLivedJwtService.generateToken("user@test.com");

            assertThatThrownBy(() -> shortLivedJwtService.parseClaims(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        void givenTokenSignedWithDifferentSecret_whenParseClaims_thenThrowSignatureException() {
            JwtService otherJwtService = new JwtService("another-test-secret-key-with-32-plus-chars", EXPIRATION_MS);
            String token = otherJwtService.generateToken("user@test.com");

            assertThatThrownBy(() -> jwtService.parseClaims(token))
                    .isInstanceOf(SignatureException.class);
        }
    }
}