package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.connection.ConnectionRequestDTO;
import com.alumni.academic_management_api.entity.Connection;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.ConnectionStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.ConnectionRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConnectionControllerIT {

    private static final String BASE_URL = "/connections";
    private static final String PASSWORD = "senha12345";

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private FileStorageService fileStorageService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        connectionRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String name, String email, String cpf, Role role) {
        return userRepository.save(User.builder()
                .name(name)
                .cpf(cpf)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .accountStatus(AccountStatus.ACTIVE)
                .role(role)
                .build());
    }

    private String loginAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Nested
    class SendRequest {

        @Test
        void givenNoToken_whenSendRequest_thenReturn401() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ConnectionRequestDTO(2L))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenValidToken_whenSendRequestToOtherUser_thenReturn201() throws Exception {
            User requester = createUser("Requester", "requester@test.com", "11111111111", Role.ALUMNI);
            User addressee = createUser("Addressee", "addressee@test.com", "22222222222", Role.ALUMNI);
            String token = loginAndGetToken(requester.getEmail());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ConnectionRequestDTO(addressee.getId())))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        void givenValidToken_whenSendRequestToSelf_thenReturn400() throws Exception {
            User user = createUser("User", "user@test.com", "11111111111", Role.ALUMNI);
            String token = loginAndGetToken(user.getEmail());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ConnectionRequestDTO(user.getId())))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenValidToken_whenSendDuplicateRequest_thenReturn400() throws Exception {
            User requester = createUser("Requester", "requester@test.com", "11111111111", Role.ALUMNI);
            User addressee = createUser("Addressee", "addressee@test.com", "22222222222", Role.ALUMNI);
            connectionRepository.save(Connection.builder()
                    .requester(requester)
                    .addressee(addressee)
                    .userLowId(Math.min(requester.getId(), addressee.getId()))
                    .userHighId(Math.max(requester.getId(), addressee.getId()))
                    .status(ConnectionStatus.PENDING)
                    .build());

            String token = loginAndGetToken(requester.getEmail());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ConnectionRequestDTO(addressee.getId())))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class FindAcceptedConnections {

        @Test
        void givenNoToken_whenFindAcceptedConnections_thenReturn401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenValidToken_whenFindAcceptedConnections_thenReturn200() throws Exception {
            User user = createUser("User", "user@test.com", "11111111111", Role.ALUMNI);
            String token = loginAndGetToken(user.getEmail());

            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class FindPendingReceivedRequests {

        @Test
        void givenNoToken_whenFindPendingReceivedRequests_thenReturn401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/pending"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenValidToken_whenFindPendingReceivedRequests_thenReturn200WithPendingList() throws Exception {
            User requester = createUser("Requester", "requester@test.com", "11111111111", Role.ALUMNI);
            User addressee = createUser("Addressee", "addressee@test.com", "22222222222", Role.ALUMNI);
            connectionRepository.save(Connection.builder()
                    .requester(requester)
                    .addressee(addressee)
                    .userLowId(Math.min(requester.getId(), addressee.getId()))
                    .userHighId(Math.max(requester.getId(), addressee.getId()))
                    .status(ConnectionStatus.PENDING)
                    .build());

            String token = loginAndGetToken(addressee.getEmail());

            mockMvc.perform(get(BASE_URL + "/pending")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }
    }

    @Nested
    class FindSentRequests {

        @Test
        void givenNoToken_whenFindSentRequests_thenReturn401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/sent"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenValidToken_whenFindSentRequests_thenReturn200WithSentList() throws Exception {
            User requester = createUser("Requester", "requester@test.com", "11111111111", Role.ALUMNI);
            User addressee = createUser("Addressee", "addressee@test.com", "22222222222", Role.ALUMNI);
            connectionRepository.save(Connection.builder()
                    .requester(requester)
                    .addressee(addressee)
                    .userLowId(Math.min(requester.getId(), addressee.getId()))
                    .userHighId(Math.max(requester.getId(), addressee.getId()))
                    .status(ConnectionStatus.PENDING)
                    .build());

            String token = loginAndGetToken(requester.getEmail());

            mockMvc.perform(get(BASE_URL + "/sent")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }
    }

    @Nested
    class FindSuggestions {

        @Test
        void givenNoToken_whenFindSuggestions_thenReturn401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/suggestions"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenValidToken_whenFindSuggestions_thenReturn200() throws Exception {
            User user = createUser("User", "user@test.com", "11111111111", Role.ALUMNI);
            String token = loginAndGetToken(user.getEmail());

            mockMvc.perform(get(BASE_URL + "/suggestions")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class AcceptRequest {

        @Test
        void givenNoToken_whenAcceptRequest_thenReturn401() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/1/accept"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAddresseeToken_whenAcceptPendingRequest_thenReturn200() throws Exception {
            User requester = createUser("Requester", "requester@test.com", "11111111111", Role.ALUMNI);
            User addressee = createUser("Addressee", "addressee@test.com", "22222222222", Role.ALUMNI);
            Connection connection = connectionRepository.save(Connection.builder()
                    .requester(requester)
                    .addressee(addressee)
                    .userLowId(Math.min(requester.getId(), addressee.getId()))
                    .userHighId(Math.max(requester.getId(), addressee.getId()))
                    .status(ConnectionStatus.PENDING)
                    .build());

            String token = loginAndGetToken(addressee.getEmail());

            mockMvc.perform(patch(BASE_URL + "/" + connection.getId() + "/accept")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }

        @Test
        void givenRequesterToken_whenAcceptOwnRequest_thenReturn400() throws Exception {
            User requester = createUser("Requester", "requester@test.com", "11111111111", Role.ALUMNI);
            User addressee = createUser("Addressee", "addressee@test.com", "22222222222", Role.ALUMNI);
            Connection connection = connectionRepository.save(Connection.builder()
                    .requester(requester)
                    .addressee(addressee)
                    .userLowId(Math.min(requester.getId(), addressee.getId()))
                    .userHighId(Math.max(requester.getId(), addressee.getId()))
                    .status(ConnectionStatus.PENDING)
                    .build());

            String token = loginAndGetToken(requester.getEmail());

            mockMvc.perform(patch(BASE_URL + "/" + connection.getId() + "/accept")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenValidToken_whenAcceptNonExistentConnection_thenReturn404() throws Exception {
            User user = createUser("User", "user@test.com", "11111111111", Role.ALUMNI);
            String token = loginAndGetToken(user.getEmail());

            mockMvc.perform(patch(BASE_URL + "/9999/accept")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteConnection {

        @Test
        void givenNoToken_whenDeleteConnection_thenReturn401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenParticipantToken_whenDeleteConnection_thenReturn204() throws Exception {
            User requester = createUser("Requester", "requester@test.com", "11111111111", Role.ALUMNI);
            User addressee = createUser("Addressee", "addressee@test.com", "22222222222", Role.ALUMNI);
            Connection connection = connectionRepository.save(Connection.builder()
                    .requester(requester)
                    .addressee(addressee)
                    .userLowId(Math.min(requester.getId(), addressee.getId()))
                    .userHighId(Math.max(requester.getId(), addressee.getId()))
                    .status(ConnectionStatus.PENDING)
                    .build());

            String token = loginAndGetToken(requester.getEmail());

            mockMvc.perform(delete(BASE_URL + "/" + connection.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        void givenNonParticipantToken_whenDeleteConnection_thenReturn400() throws Exception {
            User requester = createUser("Requester", "requester@test.com", "11111111111", Role.ALUMNI);
            User addressee = createUser("Addressee", "addressee@test.com", "22222222222", Role.ALUMNI);
            User outsider = createUser("Outsider", "outsider@test.com", "33333333333", Role.ALUMNI);
            Connection connection = connectionRepository.save(Connection.builder()
                    .requester(requester)
                    .addressee(addressee)
                    .userLowId(Math.min(requester.getId(), addressee.getId()))
                    .userHighId(Math.max(requester.getId(), addressee.getId()))
                    .status(ConnectionStatus.ACCEPTED)
                    .build());

            String token = loginAndGetToken(outsider.getEmail());

            mockMvc.perform(delete(BASE_URL + "/" + connection.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }
}
