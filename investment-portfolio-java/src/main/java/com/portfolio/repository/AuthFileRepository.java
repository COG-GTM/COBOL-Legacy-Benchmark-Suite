package com.portfolio.repository;

import com.portfolio.entity.AuthFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuthFileRepository extends JpaRepository<AuthFile, Long> {

    long countByUserIdAndResourceAndAccessType(String userId, String resource, String accessType);

    List<AuthFile> findByUserId(String userId);

    List<AuthFile> findByUserIdAndResource(String userId, String resource);
}
