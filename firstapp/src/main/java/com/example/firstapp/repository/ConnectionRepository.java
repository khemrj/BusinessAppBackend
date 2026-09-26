package com.example.firstapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.firstapp.entity.Connection;
import com.example.firstapp.enums.ConnectionStatus;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    // The "check the reverse pair before insert" lookup the schema note calls for —
    // use this in the service before creating a request so A->B can't exist alongside B->A.
    @Query("""
        SELECT c FROM Connection c
        WHERE (c.requester.id = :userA AND c.receiver.id = :userB)
           OR (c.requester.id = :userB AND c.receiver.id = :userA)
        """)
    Optional<Connection> findBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @EntityGraph(attributePaths = {"requester", "receiver"})
    @Query("""
        SELECT c FROM Connection c
        WHERE (c.requester.id = :userId OR c.receiver.id = :userId) AND c.status = :status
        ORDER BY c.createdAt DESC
        """)
    Slice<Connection> findByUserAndStatus(@Param("userId") Long userId,
                                           @Param("status") ConnectionStatus status,
                                           Pageable pageable);

    @Query("""
        SELECT CASE WHEN c.requester.id = :userId THEN c.receiver.id ELSE c.requester.id END
        FROM Connection c
        WHERE (c.requester.id = :userId OR c.receiver.id = :userId) AND c.status = 'ACCEPTED'
        """)
    List<Long> findAcceptedConnectionIds(@Param("userId") Long userId);

    boolean existsByRequesterIdAndReceiverIdAndStatus(Long requesterId, Long receiverId, ConnectionStatus status);
}
