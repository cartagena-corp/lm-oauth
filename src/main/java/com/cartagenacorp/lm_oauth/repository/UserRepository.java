package com.cartagenacorp.lm_oauth.repository;

import com.cartagenacorp.lm_oauth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsById(UUID id);

    boolean existsByEmail(String email);

    Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
        SELECT u FROM User u
        WHERE u.organizationId = :organizationId
           AND (LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<User> searchUsers(String search, UUID organizationId, Pageable pageable);
}
