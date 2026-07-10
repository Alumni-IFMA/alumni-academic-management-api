package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.Connection;
import com.alumni.academic_management_api.enums.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    Optional<Connection> findByUserLowIdAndUserHighId(Long userLowId, Long userHighId);

    @Query("""
            SELECT c FROM Connection c
            WHERE c.status = :status
            AND (c.requester.id = :userId OR c.addressee.id = :userId)
            """)
    List<Connection> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") ConnectionStatus status
    );

    @Query("""
            SELECT c FROM Connection c
            WHERE c.status = 'PENDING'
            AND c.addressee.id = :userId
            """)
    List<Connection> findPendingReceivedByAddresseeId(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM Connection c
            WHERE c.status = 'PENDING'
            AND c.requester.id = :userId
            """)
    List<Connection> findPendingSentByRequesterId(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM Connection c
            WHERE c.requester.id = :userId OR c.addressee.id = :userId
            """)
    List<Connection> findByUserId(@Param("userId") Long userId);
}
