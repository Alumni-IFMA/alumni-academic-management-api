package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.connection.ConnectionRequestDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.Campus;
import com.alumni.academic_management_api.entity.CampusCourse;
import com.alumni.academic_management_api.entity.Connection;
import com.alumni.academic_management_api.entity.Course;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.ConnectionStatus;
import com.alumni.academic_management_api.enums.Level;
import com.alumni.academic_management_api.enums.Modality;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.AcademicProfileRepository;
import com.alumni.academic_management_api.repository.CampusesCourseRepository;
import com.alumni.academic_management_api.repository.ConnectionRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

    @Autowired
    private AcademicProfileRepository academicProfileRepository;

    @Autowired
    private CampusesCourseRepository campusesCourseRepository;

    @Autowired
    private EntityManager entityManager;

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

    private User createUserWithProfilePicture(String name, String email, String cpf, Role role, String pictureUrl) {
        return userRepository.save(User.builder()
                .name(name)
                .cpf(cpf)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .accountStatus(AccountStatus.ACTIVE)
                .role(role)
                .profilePictureUrl(pictureUrl)
                .build());
    }

    private CampusCourse createCampusCourse() {
        Campus campus = entityManager.merge(Campus.builder()
                .name("Campus Central")
                .city("São Luís")
                .build());
        Course course = entityManager.merge(Course.builder()
                .name("Engenharia")
                .level(Level.GRADUACAO)
                .modality(Modality.BACHARELADO)
                .build());
        return campusesCourseRepository.save(CampusCourse.builder()
                .campus(campus)
                .course(course)
                .build());
    }

    private User createUserWithAcademicProfile(String name, String email, String cpf, CampusCourse campusCourse) {
        User user = createUser(name, email, cpf, Role.ALUMNI);
        academicProfileRepository.save(AcademicProfile.builder()
                .user(user)
                .campusCourse(campusCourse)
                .entryYear(2020)
                .conclusionYear(2024)
                .build());
        return user;
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

        @Test
        void givenAcceptedConnectionWithProfilePicture_whenFindAcceptedConnections_thenReturnProfilePictureUrl()
                throws Exception {
            String objectKey = "avatars/requester.jpg";
            String presignedUrl = "https://s3.us-east-005.backblazeb2.com/alumni-files/avatars/requester.jpg"
                    + "?X-Amz-Signature=xyz";
            Mockito.when(fileStorageService.generatePresignedUrl(objectKey, 15)).thenReturn(presignedUrl);
            User requester = createUserWithProfilePicture("Requester", "requester@test.com", "11111111111",
                    Role.ALUMNI, objectKey);
            User addressee = createUser("Addressee", "addressee@test.com", "22222222222", Role.ALUMNI);
            connectionRepository.save(Connection.builder()
                    .requester(requester)
                    .addressee(addressee)
                    .userLowId(Math.min(requester.getId(), addressee.getId()))
                    .userHighId(Math.max(requester.getId(), addressee.getId()))
                    .status(ConnectionStatus.ACCEPTED)
                    .build());

            String token = loginAndGetToken(addressee.getEmail());

            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].requester.profilePictureUrl").value(presignedUrl));
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

        @Test
        void givenRequesterWithProfilePicture_whenFindPendingReceivedRequests_thenReturnProfilePictureUrl()
                throws Exception {
            String objectKey = "avatars/requester.jpg";
            String presignedUrl = "https://s3.us-east-005.backblazeb2.com/alumni-files/avatars/requester.jpg"
                    + "?X-Amz-Signature=xyz";
            Mockito.when(fileStorageService.generatePresignedUrl(objectKey, 15)).thenReturn(presignedUrl);
            User requester = createUserWithProfilePicture("Requester", "requester@test.com", "11111111111",
                    Role.ALUMNI, objectKey);
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
                    .andExpect(jsonPath("$[0].requester.profilePictureUrl").value(presignedUrl));
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

        @Test
        void givenAddresseeWithProfilePicture_whenFindSentRequests_thenReturnProfilePictureUrl() throws Exception {
            String objectKey = "avatars/addressee.jpg";
            String presignedUrl = "https://s3.us-east-005.backblazeb2.com/alumni-files/avatars/addressee.jpg"
                    + "?X-Amz-Signature=xyz";
            Mockito.when(fileStorageService.generatePresignedUrl(objectKey, 15)).thenReturn(presignedUrl);
            User requester = createUser("Requester", "requester@test.com", "11111111111", Role.ALUMNI);
            User addressee = createUserWithProfilePicture("Addressee", "addressee@test.com", "22222222222",
                    Role.ALUMNI, objectKey);
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
                    .andExpect(jsonPath("$[0].addressee.profilePictureUrl").value(presignedUrl));
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

        @Test
        void givenMoreSuggestionsThanPageSize_whenFindSuggestionsWithPageParams_thenReturnRequestedPage()
                throws Exception {
            CampusCourse campusCourse = createCampusCourse();
            User user = createUserWithAcademicProfile("User", "user@test.com", "11111111111", campusCourse);
            createUserWithAcademicProfile("Suggested1", "suggested1@test.com", "22222222222", campusCourse);
            createUserWithAcademicProfile("Suggested2", "suggested2@test.com", "33333333333", campusCourse);
            entityManager.flush();
            entityManager.clear();

            String token = loginAndGetToken(user.getEmail());

            mockMvc.perform(get(BASE_URL + "/suggestions")
                            .param("page", "0")
                            .param("size", "1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(2));
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
