package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class createUser {

        private static final String URL = "/auth/register";

        @Test
        void givenValidUserRequest_whenCreate_thenReturnCreated() throws Exception {
            RegisterRequestDTO requestDTO = RegisterRequestDTO.builder()
                    .name("João Silva")
                    .cpf("12345678900")
                    .email("joao@gmail.com")
                    .campusCourseId(1L)
                    .entryYear(2021)
                    .conclusionYear(2024)
                    .build();

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("João Silva"))
                    .andExpect(jsonPath("$.email").value("joao@gmail.com"))
                    .andExpect(jsonPath("$.role").value("ALUMNI"));

        }
    }

    @Nested
    class FindAll {

        private static final String URL = "/auth/users";

        @Test
        void givenUsersExist_whenFindAll_thenReturn200WithList() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

}