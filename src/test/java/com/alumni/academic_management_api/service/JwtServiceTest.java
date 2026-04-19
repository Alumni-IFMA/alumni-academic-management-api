package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "test-secret-key-with-at-least-32-characters-long";
    private static final long EXPIRATION_MS = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Nested
    class GenerateToken {

        @Test
        void givenEmailAndAdminRole_whenGenerateToken_thenTokenContainsAdminRole() {
            String token = jwtService.generateToken("admin@test.com", Role.ADMIN);

            assertThat(token).isNotBlank();
            assertThat(jwtService.extractRole(token)).isEqualTo(Role.ADMIN);
        }

        @Test
        void givenEmailAndAlumniRole_whenGenerateToken_thenTokenContainsAlumniRole() {
            String token = jwtService.generateToken("alumni@test.com", Role.ALUMNI);

            assertThat(jwtService.extractRole(token)).isEqualTo(Role.ALUMNI);
        }

        @Test
        void givenEmailAndRole_whenGenerateToken_thenEmailIsPreserved() {
            String token = jwtService.generateToken("user@test.com", Role.ALUMNI);

            assertThat(jwtService.extractEmail(token)).isEqualTo("user@test.com");
        }
    }
}
