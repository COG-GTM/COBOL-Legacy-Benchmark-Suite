package com.portfolio.repository;

import com.portfolio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 * Replaces DB2 AUTHFILE table access operations.
 * 
 * @see src/programs/online/SECMGR.cbl - P200-CHECK-AUTH
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUserId(String userId);

    Optional<User> findByEmail(String email);

    List<User> findByRole(String role);

    List<User> findByIsActive(Boolean isActive);

    List<User> findByDepartment(String department);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.failedLoginAttempts < 5")
    List<User> findActiveUnlockedUsers();

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.username = :username")
    void incrementFailedLoginAttempts(@Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lastLoginAt = CURRENT_TIMESTAMP WHERE u.username = :username")
    void resetFailedLoginAttemptsAndUpdateLastLogin(@Param("username") String username);

    boolean existsByUsername(String username);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);
}
