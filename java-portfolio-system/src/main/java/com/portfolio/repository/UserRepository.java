package com.portfolio.repository;

import com.portfolio.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity
 * Provides data access operations for user management
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    List<User> findByStatus(User.UserStatus status);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") User.UserRole role);

    @Query("SELECT u FROM User u WHERE u.status = 'A' ORDER BY u.fullName")
    List<User> findActiveUsers();

    boolean existsByEmail(String email);
}
