package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.connection.ConnectionResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.CampusCourse;
import com.alumni.academic_management_api.entity.Connection;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.ConnectionStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.ConnectionMapper;
import com.alumni.academic_management_api.mapper.UserMapper;
import com.alumni.academic_management_api.repository.ConnectionRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectionMapper connectionMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ConnectionService connectionService;

    @Nested
    class SendRequest {

        @Test
        void givenValidUsers_whenSendRequest_thenCreatePendingConnection() {
            Long requesterId = 1L;
            Long addresseeId = 2L;
            User requester = User.builder()
                    .id(requesterId)
                    .name("Requester")
                    .email("requester@test.com")
                    .role(Role.ALUMNI)
                    .build();
            User addressee = User.builder()
                    .id(addresseeId)
                    .name("Addressee")
                    .email("addressee@test.com")
                    .role(Role.ALUMNI)
                    .build();
            Connection savedConnection = Connection.builder()
                    .id(10L)
                    .requester(requester)
                    .addressee(addressee)
                    .userLowId(requesterId)
                    .userHighId(addresseeId)
                    .status(ConnectionStatus.PENDING)
                    .build();
            ConnectionResponseDTO expectedResponse = ConnectionResponseDTO.builder()
                    .id(10L)
                    .requester(new UserSimpleDTO(requesterId, "Requester", "requester@test.com", null,
                            List.of(), null, Role.ALUMNI))
                    .addressee(new UserSimpleDTO(addresseeId, "Addressee", "addressee@test.com", null,
                            List.of(), null, Role.ALUMNI))
                    .status(ConnectionStatus.PENDING)
                    .build();

            Mockito.when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
            Mockito.when(connectionRepository.findByUserLowIdAndUserHighId(requesterId, addresseeId))
                    .thenReturn(Optional.empty());
            Mockito.when(userRepository.findById(addresseeId)).thenReturn(Optional.of(addressee));
            Mockito.when(connectionRepository.save(Mockito.any(Connection.class))).thenReturn(savedConnection);
            Mockito.when(connectionMapper.toResponseDTO(savedConnection)).thenReturn(expectedResponse);

            ConnectionResponseDTO result = connectionService.sendRequest(requester.getEmail(), addresseeId);

            assertThat(result).isEqualTo(expectedResponse);
            ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);
            Mockito.verify(connectionRepository).save(captor.capture());
            Connection connection = captor.getValue();
            assertThat(connection.getRequester()).isEqualTo(requester);
            assertThat(connection.getAddressee()).isEqualTo(addressee);
            assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.PENDING);
            assertThat(connection.getUserLowId()).isEqualTo(requesterId);
            assertThat(connection.getUserHighId()).isEqualTo(addresseeId);
        }

        @Test
        void givenSameUser_whenSendRequest_thenThrowBusinessException() {
            User user = User.builder()
                    .id(1L)
                    .email("user@test.com")
                    .build();
            Mockito.when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> connectionService.sendRequest(user.getEmail(), 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cannot connect with themselves");

            Mockito.verify(connectionRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenExistingConnection_whenSendRequest_thenThrowBusinessException() {
            User requester = User.builder()
                    .id(2L)
                    .email("requester@test.com")
                    .build();
            Connection existingConnection = Connection.builder()
                    .id(1L)
                    .status(ConnectionStatus.PENDING)
                    .build();
            Mockito.when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
            Mockito.when(connectionRepository.findByUserLowIdAndUserHighId(1L, 2L))
                    .thenReturn(Optional.of(existingConnection));

            assertThatThrownBy(() -> connectionService.sendRequest(requester.getEmail(), 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Connection already exists");

            Mockito.verify(userRepository, Mockito.never()).findById(Mockito.anyLong());
            Mockito.verify(connectionRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenMissingAuthenticatedUser_whenSendRequest_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.sendRequest("ghost@test.com", 2L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            Mockito.verify(connectionRepository, Mockito.never())
                    .findByUserLowIdAndUserHighId(Mockito.anyLong(), Mockito.anyLong());
            Mockito.verify(connectionRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenMissingAddressee_whenSendRequest_thenThrowResourceNotFoundException() {
            User requester = User.builder()
                    .id(1L)
                    .email("requester@test.com")
                    .build();
            Mockito.when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
            Mockito.when(connectionRepository.findByUserLowIdAndUserHighId(1L, 2L))
                    .thenReturn(Optional.empty());
            Mockito.when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.sendRequest(requester.getEmail(), 2L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("2");

            Mockito.verify(connectionRepository, Mockito.never()).save(Mockito.any());
        }
    }

    @Nested
    class AcceptRequest {

        @Test
        void givenAddresseeAndPendingConnection_whenAcceptRequest_thenUpdateStatusToAccepted() {
            Long connectionId = 10L;
            Long requesterId = 1L;
            Long addresseeId = 2L;
            User requester = User.builder()
                    .id(requesterId)
                    .name("Requester")
                    .email("requester@test.com")
                    .role(Role.ALUMNI)
                    .build();
            User addressee = User.builder()
                    .id(addresseeId)
                    .name("Addressee")
                    .email("addressee@test.com")
                    .role(Role.ALUMNI)
                    .build();
            Connection connection = Connection.builder()
                    .id(connectionId)
                    .requester(requester)
                    .addressee(addressee)
                    .status(ConnectionStatus.PENDING)
                    .build();
            ConnectionResponseDTO expectedResponse = ConnectionResponseDTO.builder()
                    .id(connectionId)
                    .requester(new UserSimpleDTO(requesterId, "Requester", "requester@test.com", null,
                            List.of(), null, Role.ALUMNI))
                    .addressee(new UserSimpleDTO(addresseeId, "Addressee", "addressee@test.com", null,
                            List.of(), null, Role.ALUMNI))
                    .status(ConnectionStatus.ACCEPTED)
                    .build();

            Mockito.when(userRepository.findByEmail(addressee.getEmail())).thenReturn(Optional.of(addressee));
            Mockito.when(connectionRepository.findById(connectionId)).thenReturn(Optional.of(connection));
            Mockito.when(connectionRepository.save(connection)).thenReturn(connection);
            Mockito.when(connectionMapper.toResponseDTO(connection)).thenReturn(expectedResponse);

            ConnectionResponseDTO result = connectionService.acceptRequest(connectionId, addressee.getEmail());

            assertThat(result).isEqualTo(expectedResponse);
            assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.ACCEPTED);
            Mockito.verify(connectionRepository).save(connection);
        }

        @Test
        void givenMissingConnection_whenAcceptRequest_thenThrowResourceNotFoundException() {
            User addressee = User.builder()
                    .id(2L)
                    .email("addressee@test.com")
                    .build();
            Mockito.when(userRepository.findByEmail(addressee.getEmail())).thenReturn(Optional.of(addressee));
            Mockito.when(connectionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.acceptRequest(99L, addressee.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(connectionRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenRequesterTryingToAccept_whenAcceptRequest_thenThrowBusinessException() {
            User requester = User.builder()
                    .id(1L)
                    .email("requester@test.com")
                    .build();
            User addressee = User.builder()
                    .id(2L)
                    .build();
            Connection connection = Connection.builder()
                    .id(10L)
                    .requester(requester)
                    .addressee(addressee)
                    .status(ConnectionStatus.PENDING)
                    .build();

            Mockito.when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
            Mockito.when(connectionRepository.findById(10L)).thenReturn(Optional.of(connection));

            assertThatThrownBy(() -> connectionService.acceptRequest(10L, requester.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only the addressee");

            Mockito.verify(connectionRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenNonPendingConnection_whenAcceptRequest_thenThrowBusinessException() {
            User addressee = User.builder()
                    .id(2L)
                    .email("addressee@test.com")
                    .build();
            Connection connection = Connection.builder()
                    .id(10L)
                    .addressee(addressee)
                    .status(ConnectionStatus.ACCEPTED)
                    .build();

            Mockito.when(userRepository.findByEmail(addressee.getEmail())).thenReturn(Optional.of(addressee));
            Mockito.when(connectionRepository.findById(10L)).thenReturn(Optional.of(connection));

            assertThatThrownBy(() -> connectionService.acceptRequest(10L, addressee.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only pending");

            Mockito.verify(connectionRepository, Mockito.never()).save(Mockito.any());
        }
    }

    @Nested
    class DeleteConnection {

        @Test
        void givenRequesterParticipant_whenDeleteConnection_thenDeleteConnection() {
            User requester = User.builder()
                    .id(1L)
                    .email("requester@test.com")
                    .build();
            User addressee = User.builder()
                    .id(2L)
                    .build();
            Connection connection = Connection.builder()
                    .id(10L)
                    .requester(requester)
                    .addressee(addressee)
                    .status(ConnectionStatus.ACCEPTED)
                    .build();

            Mockito.when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
            Mockito.when(connectionRepository.findById(10L)).thenReturn(Optional.of(connection));

            connectionService.deleteConnection(10L, requester.getEmail());

            Mockito.verify(connectionRepository).delete(connection);
        }

        @Test
        void givenAddresseeParticipant_whenDeleteConnection_thenDeleteConnection() {
            User requester = User.builder()
                    .id(1L)
                    .build();
            User addressee = User.builder()
                    .id(2L)
                    .email("addressee@test.com")
                    .build();
            Connection connection = Connection.builder()
                    .id(10L)
                    .requester(requester)
                    .addressee(addressee)
                    .status(ConnectionStatus.PENDING)
                    .build();

            Mockito.when(userRepository.findByEmail(addressee.getEmail())).thenReturn(Optional.of(addressee));
            Mockito.when(connectionRepository.findById(10L)).thenReturn(Optional.of(connection));

            connectionService.deleteConnection(10L, addressee.getEmail());

            Mockito.verify(connectionRepository).delete(connection);
        }

        @Test
        void givenMissingConnection_whenDeleteConnection_thenThrowResourceNotFoundException() {
            User user = User.builder()
                    .id(1L)
                    .email("user@test.com")
                    .build();
            Mockito.when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Mockito.when(connectionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.deleteConnection(99L, user.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(connectionRepository, Mockito.never()).delete(Mockito.any());
        }

        @Test
        void givenNonParticipant_whenDeleteConnection_thenThrowBusinessException() {
            User requester = User.builder()
                    .id(1L)
                    .build();
            User addressee = User.builder()
                    .id(2L)
                    .build();
            User outsider = User.builder()
                    .id(3L)
                    .email("outsider@test.com")
                    .build();
            Connection connection = Connection.builder()
                    .id(10L)
                    .requester(requester)
                    .addressee(addressee)
                    .status(ConnectionStatus.ACCEPTED)
                    .build();

            Mockito.when(userRepository.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));
            Mockito.when(connectionRepository.findById(10L)).thenReturn(Optional.of(connection));

            assertThatThrownBy(() -> connectionService.deleteConnection(10L, outsider.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only connection participants");

            Mockito.verify(connectionRepository, Mockito.never()).delete(Mockito.any());
        }
    }

    @Nested
    class FindAcceptedConnections {

        @Test
        void givenAcceptedConnections_whenFindAcceptedConnections_thenReturnConnectionList() {
            User authenticatedUser = User.builder()
                    .id(1L)
                    .email("user@test.com")
                    .build();
            Connection connection = Connection.builder()
                    .id(10L)
                    .requester(authenticatedUser)
                    .addressee(User.builder().id(2L).build())
                    .status(ConnectionStatus.ACCEPTED)
                    .build();
            ConnectionResponseDTO response = ConnectionResponseDTO.builder()
                    .id(10L)
                    .status(ConnectionStatus.ACCEPTED)
                    .build();

            Mockito.when(userRepository.findByEmail(authenticatedUser.getEmail()))
                    .thenReturn(Optional.of(authenticatedUser));
            Mockito.when(connectionRepository.findByUserIdAndStatus(1L, ConnectionStatus.ACCEPTED))
                    .thenReturn(List.of(connection));
            Mockito.when(connectionMapper.toResponseDTO(connection)).thenReturn(response);

            List<ConnectionResponseDTO> result = connectionService.findAcceptedConnections(
                    authenticatedUser.getEmail());

            assertThat(result).containsExactly(response);
        }

        @Test
        void givenMissingUser_whenFindAcceptedConnections_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.findAcceptedConnections("ghost@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            Mockito.verify(connectionRepository, Mockito.never())
                    .findByUserIdAndStatus(Mockito.anyLong(), Mockito.any());
        }
    }

    @Nested
    class FindPendingReceivedRequests {

        @Test
        void givenPendingRequests_whenFindPendingReceivedRequests_thenReturnPendingList() {
            User authenticatedUser = User.builder().id(2L).email("addressee@test.com").build();
            User requester = User.builder().id(1L).build();
            Connection connection = Connection.builder()
                    .id(10L)
                    .requester(requester)
                    .addressee(authenticatedUser)
                    .status(ConnectionStatus.PENDING)
                    .build();
            ConnectionResponseDTO response = ConnectionResponseDTO.builder()
                    .id(10L)
                    .status(ConnectionStatus.PENDING)
                    .build();

            Mockito.when(userRepository.findByEmail(authenticatedUser.getEmail()))
                    .thenReturn(Optional.of(authenticatedUser));
            Mockito.when(connectionRepository.findByStatusAndAddresseeId(ConnectionStatus.PENDING, 2L))
                    .thenReturn(List.of(connection));
            Mockito.when(connectionMapper.toResponseDTO(connection)).thenReturn(response);

            List<ConnectionResponseDTO> result = connectionService.findPendingReceivedRequests(
                    authenticatedUser.getEmail());

            assertThat(result).containsExactly(response);
        }

        @Test
        void givenMissingUser_whenFindPendingReceivedRequests_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.findPendingReceivedRequests("ghost@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            Mockito.verify(connectionRepository, Mockito.never())
                    .findByStatusAndAddresseeId(Mockito.any(), Mockito.anyLong());
        }
    }

    @Nested
    class FindSentRequests {

        @Test
        void givenSentRequests_whenFindSentRequests_thenReturnSentList() {
            User authenticatedUser = User.builder().id(1L).email("requester@test.com").build();
            User addressee = User.builder().id(2L).build();
            Connection connection = Connection.builder()
                    .id(10L)
                    .requester(authenticatedUser)
                    .addressee(addressee)
                    .status(ConnectionStatus.PENDING)
                    .build();
            ConnectionResponseDTO response = ConnectionResponseDTO.builder()
                    .id(10L)
                    .status(ConnectionStatus.PENDING)
                    .build();

            Mockito.when(userRepository.findByEmail(authenticatedUser.getEmail()))
                    .thenReturn(Optional.of(authenticatedUser));
            Mockito.when(connectionRepository.findByStatusAndRequesterId(ConnectionStatus.PENDING, 1L))
                    .thenReturn(List.of(connection));
            Mockito.when(connectionMapper.toResponseDTO(connection)).thenReturn(response);

            List<ConnectionResponseDTO> result = connectionService.findSentRequests(authenticatedUser.getEmail());

            assertThat(result).containsExactly(response);
        }

        @Test
        void givenMissingUser_whenFindSentRequests_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.findSentRequests("ghost@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            Mockito.verify(connectionRepository, Mockito.never())
                    .findByStatusAndRequesterId(Mockito.any(), Mockito.anyLong());
        }
    }

    @Nested
    class FindSuggestions {

        @Test
        void givenUserWithoutCampusCourse_whenFindSuggestions_thenReturnEmptyList() {
            User authenticatedUser = User.builder()
                    .id(1L)
                    .email("user@test.com")
                    .academicProfiles(List.of())
                    .build();

            Mockito.when(userRepository.findByEmail(authenticatedUser.getEmail()))
                    .thenReturn(Optional.of(authenticatedUser));

            List<UserSimpleDTO> result = connectionService.findSuggestions(authenticatedUser.getEmail());

            assertThat(result).isEmpty();
            Mockito.verify(connectionRepository, Mockito.never())
                    .findByRequesterIdOrAddresseeId(Mockito.anyLong(), Mockito.anyLong());
            Mockito.verify(userRepository, Mockito.never())
                    .findDistinctByAcademicProfilesCampusCourseIdInAndIdNot(Mockito.any(), Mockito.anyLong());
        }

        @Test
        void givenSameCampusCourseUsers_whenFindSuggestions_thenExcludeExistingConnections() {
            Long authenticatedUserId = 1L;
            Long connectedUserId = 2L;
            Long suggestedUserId = 3L;
            CampusCourse campusCourse = CampusCourse.builder()
                    .id(100L)
                    .build();
            User authenticatedUser = User.builder()
                    .id(authenticatedUserId)
                    .email("user@test.com")
                    .academicProfiles(List.of(AcademicProfile.builder()
                            .campusCourse(campusCourse)
                            .build()))
                    .build();
            User connectedUser = User.builder()
                    .id(connectedUserId)
                    .build();
            User suggestedUser = User.builder()
                    .id(suggestedUserId)
                    .name("Suggested")
                    .email("suggested@test.com")
                    .role(Role.ALUMNI)
                    .build();
            Connection existingConnection = Connection.builder()
                    .id(10L)
                    .requester(authenticatedUser)
                    .addressee(connectedUser)
                    .status(ConnectionStatus.ACCEPTED)
                    .build();
            UserSimpleDTO suggestedDTO = new UserSimpleDTO(
                    suggestedUserId,
                    "Suggested",
                    "suggested@test.com",
                    List.of(),
                    null,
                    Role.ALUMNI
            );

            Mockito.when(userRepository.findByEmail(authenticatedUser.getEmail()))
                    .thenReturn(Optional.of(authenticatedUser));
            Mockito.when(connectionRepository.findByRequesterIdOrAddresseeId(authenticatedUserId, authenticatedUserId))
                    .thenReturn(List.of(existingConnection));
            Mockito.when(userRepository.findDistinctByAcademicProfilesCampusCourseIdInAndIdNot(
                    Mockito.eq(Set.of(100L)),
                    Mockito.eq(authenticatedUserId)
            )).thenReturn(List.of(connectedUser, suggestedUser));
            Mockito.when(userMapper.toSimpleDTO(suggestedUser)).thenReturn(suggestedDTO);

            List<UserSimpleDTO> result = connectionService.findSuggestions(authenticatedUser.getEmail());

            assertThat(result).containsExactly(suggestedDTO);
            Mockito.verify(userMapper, Mockito.never()).toSimpleDTO(connectedUser);
        }

        @Test
        void givenMissingUser_whenFindSuggestions_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.findSuggestions("ghost@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            Mockito.verify(connectionRepository, Mockito.never())
                    .findByRequesterIdOrAddresseeId(Mockito.anyLong(), Mockito.anyLong());
        }
    }
}
