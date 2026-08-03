package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.connection.ConnectionResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.Connection;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.ConnectionStatus;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.ConnectionMapper;
import com.alumni.academic_management_api.mapper.UserMapper;
import com.alumni.academic_management_api.repository.ConnectionRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Service
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final ConnectionMapper connectionMapper;
    private final UserMapper userMapper;

    public ConnectionService(
            ConnectionRepository connectionRepository,
            UserRepository userRepository,
            ConnectionMapper connectionMapper,
            UserMapper userMapper
    ) {
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.connectionMapper = connectionMapper;
        this.userMapper = userMapper;
    }

    public ConnectionResponseDTO sendRequest(String requesterEmail, Long addresseeId) {
        User requester = findAuthenticatedUser(requesterEmail);
        Long requesterId = requester.getId();

        if (requesterId.equals(addresseeId)) {
            throw new BusinessException("User cannot connect with themselves");
        }

        Long userLowId = Math.min(requesterId, addresseeId);
        Long userHighId = Math.max(requesterId, addresseeId);
        connectionRepository.findByUserLowIdAndUserHighId(userLowId, userHighId)
                .ifPresent(connection -> {
                    throw new BusinessException("Connection already exists between these users");
                });

        User addressee = userRepository.findById(addresseeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + addresseeId));

        Connection connection = Connection.builder()
                .requester(requester)
                .addressee(addressee)
                .userLowId(userLowId)
                .userHighId(userHighId)
                .status(ConnectionStatus.PENDING)
                .build();

        Connection savedConnection = connectionRepository.save(connection);
        return connectionMapper.toResponseDTO(savedConnection);
    }

    public ConnectionResponseDTO acceptRequest(Long connectionId, String authenticatedEmail) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found with id: " + connectionId));

        if (!connection.getAddressee().getId().equals(authenticatedUser.getId())) {
            throw new BusinessException("Only the addressee can accept this connection request");
        }

        if (!ConnectionStatus.PENDING.equals(connection.getStatus())) {
            throw new BusinessException("Only pending connection requests can be accepted");
        }

        connection.setStatus(ConnectionStatus.ACCEPTED);
        Connection savedConnection = connectionRepository.save(connection);
        return connectionMapper.toResponseDTO(savedConnection);
    }

    public void deleteConnection(Long connectionId, String authenticatedEmail) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found with id: " + connectionId));

        Long requesterId = connection.getRequester().getId();
        Long addresseeId = connection.getAddressee().getId();
        Long authenticatedUserId = authenticatedUser.getId();
        if (!requesterId.equals(authenticatedUserId) && !addresseeId.equals(authenticatedUserId)) {
            throw new BusinessException("Only connection participants can delete this connection");
        }

        connectionRepository.delete(connection);
    }

    @Transactional(readOnly = true)
    public List<ConnectionResponseDTO> findAcceptedConnections(String authenticatedEmail) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        return connectionRepository.findByUserIdAndStatus(authenticatedUser.getId(), ConnectionStatus.ACCEPTED)
                .stream()
                .map(connectionMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionResponseDTO> findPendingReceivedRequests(String authenticatedEmail) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        return connectionRepository.findByStatusAndAddresseeId(ConnectionStatus.PENDING, authenticatedUser.getId())
                .stream()
                .map(connectionMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionResponseDTO> findSentRequests(String authenticatedEmail) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        return connectionRepository.findByStatusAndRequesterId(ConnectionStatus.PENDING, authenticatedUser.getId())
                .stream()
                .map(connectionMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSimpleDTO> findSuggestions(String authenticatedEmail) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Long authenticatedUserId = authenticatedUser.getId();
        Set<Long> campusCourseIds = authenticatedUser.getAcademicProfiles()
                .stream()
                .map(AcademicProfile::getCampusCourse)
                .filter(campusCourse -> campusCourse != null && campusCourse.getId() != null)
                .map(campusCourse -> campusCourse.getId())
                .collect(Collectors.toSet());

        if (campusCourseIds.isEmpty()) {
            return List.of();
        }

        Set<Long> relatedUserIds = connectionRepository
                .findByRequesterIdOrAddresseeId(authenticatedUserId, authenticatedUserId)
                .stream()
                .map(connection -> getOtherUserId(connection, authenticatedUserId))
                .collect(Collectors.toSet());

        return userRepository
                .findDistinctByAcademicProfilesCampusCourseIdInAndIdNot(campusCourseIds, authenticatedUserId)
                .stream()
                .filter(user -> !relatedUserIds.contains(user.getId()))
                .map(userMapper::toSimpleDTO)
                .toList();
    }

    private User findAuthenticatedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Long getOtherUserId(Connection connection, Long authenticatedUserId) {
        Long requesterId = connection.getRequester().getId();
        if (requesterId.equals(authenticatedUserId)) {
            return connection.getAddressee().getId();
        }
        return requesterId;
    }
}
