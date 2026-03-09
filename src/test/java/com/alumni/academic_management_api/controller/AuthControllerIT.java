package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    private static final String URL = "/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    class Login {

        @Test
        void givenValidCredentials_whenLogin_thenReturnToken() throws Exception {
            User user = User.builder()
                    .name("Joao Silva")
                    .cpf("11111111111")
                    .email("joao@email.com")
                    .password(passwordEncoder.encode("12345678"))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();
            userRepository.save(user);

            LoginRequestDTO request = new LoginRequestDTO("joao@email.com", "12345678");

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        void givenInvalidPassword_whenLogin_thenReturnBadRequest() throws Exception {
            User user = User.builder()
                    .name("Joao Silva")
                    .cpf("22222222222")
                    .email("joao@email.com")
                    .password(passwordEncoder.encode("12345678"))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();
            userRepository.save(user);

            LoginRequestDTO request = new LoginRequestDTO("joao@email.com", "wrong-password");

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid email or password"));
        }

        @Test
        void givenEmailNotFound_whenLogin_thenReturnBadRequest() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("notfound@email.com", "12345678");

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid email or password"));
        }
    }
}
