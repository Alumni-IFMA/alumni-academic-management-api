package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.CampusCourse;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.AcademicProfileRepository;
import com.alumni.academic_management_api.repository.CampusesCourseRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CampusesCourseRepository campusesCourseRepository;

    @MockitoBean
    private AcademicProfileRepository academicProfileRepository;

    @Nested
    class createUser {

        private static final String URL = "/auth/register";

        @Test
        void givenValidUserRequest_whenCreate_thenReturnCreated() throws Exception {
            CampusCourse mockCourse = new CampusCourse();

            RegisterRequestDTO requestDTO = RegisterRequestDTO.builder()
                    .name("João Silva")
                    .cpf("12345678900")
                    .email("joao@gmail.com")
                    .campusCourseId(1L)
                    .entryYear(2021)
                    .conclusionYear(2024)
                    .build();
            mockCourse.setId(1L);

            Mockito.when(campusesCourseRepository.findById(1L))
                    .thenReturn(Optional.of(mockCourse));

            Mockito.when(academicProfileRepository.save(Mockito.any()))
                    .thenReturn(new AcademicProfile());


            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("João Silva"))
                    .andExpect(jsonPath("$.email").value("joao@gmail.com"))
                    .andExpect(jsonPath("$.role").value("ALUMNI"));

        }

        @Test
        void givenInvalidCampusCourse_whenCreate_thenReturnNotFound() throws Exception {
            RegisterRequestDTO requestDTO = RegisterRequestDTO.builder()
                    .name("Erro Silva")
                    .cpf("12345678900")
                    .email("erro@gmail.com")
                    .campusCourseId(99L)
                    .entryYear(2021)
                    .conclusionYear(2024)
                    .build();
            Mockito.when(campusesCourseRepository.findById(99L))
                    .thenReturn(Optional.empty());


            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class FindAll {

        private static final String URL = "/auth/users";

        @Test
        @WithMockUser(roles = "ADMIN")
        void givenUsersExist_whenFindAll_thenReturn200WithList() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    class GetUserProfile {

        private static final String URL = "/auth/users/{id}/profile";

        @Test
        @WithMockUser
        void givenExistingUser_whenGetProfile_thenReturn200WithProfileData() throws Exception {
            CampusCourse mockCourse = new CampusCourse();

            RegisterRequestDTO requestDTO = RegisterRequestDTO.builder()
                    .name("Maria Silva")
                    .cpf("11122233344")
                    .email("maria@gmail.com")
                    .campusCourseId(1L)
                    .entryYear(2020)
                    .conclusionYear(2023)
                    .build();

            mockCourse.setId(1L);

            Mockito.when(campusesCourseRepository.findById(1L))
                    .thenReturn(Optional.of(mockCourse));

            Mockito.when(academicProfileRepository.save(Mockito.any()))
                    .thenReturn(new AcademicProfile());

            String responseBody = mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long userId = objectMapper.readTree(responseBody).get("id").asLong();

            mockMvc.perform(get(URL, userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.name").value("Maria Silva"))
                    .andExpect(jsonPath("$.email").value("maria@gmail.com"));
        }

        @Test
        @WithMockUser
        void givenNonExistingUser_whenGetProfile_thenReturn404() throws Exception {
            mockMvc.perform(get(URL, 999999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class FindUserById {

        private static final String URL = "/auth/users";


        @Test
        @WithMockUser
        void givenValidUserRequest_whenFindById_thenReturnUser() throws Exception {
            User user = User.builder()
                    .name("João Silva")
                    .email("joao@gmail.com")
                    .cpf("12345678900")
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .role(Role.ALUMNI)
                    .build();

            User savedUser = userRepository.save(user);

            mockMvc.perform(get(URL + "/" + savedUser.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("João Silva"))
                    .andExpect(jsonPath("$.email").value("joao@gmail.com"));
        }

        @Test
        @WithMockUser
        void givenInvalidUserRequest_whenFindById_thenReturnNotFound() throws Exception {
            mockMvc.perform(get("/auth/users/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("User not found")));
        }
    }
}