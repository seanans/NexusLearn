package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    @Query("SELECT u FROM User u WHERE (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND u.id != :currentUserId")
    List<User> searchUsersExcludingSelf(@Param("query") String query, @Param("currentUserId") UUID currentUserId, Pageable pageable);
}
