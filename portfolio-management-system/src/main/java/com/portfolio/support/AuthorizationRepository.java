package com.portfolio.support;

import com.portfolio.model.AuthorizationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for AUTHFILE table.
 * Replaces COBOL SECMGR P200-CHECK-AUTH DB2 SELECT FROM AUTHFILE.
 */
@Repository
public interface AuthorizationRepository extends JpaRepository<AuthorizationRecord, Long> {

    long countByUserIdAndResourceAndAccessType(String userId, String resource, String accessType);
}
