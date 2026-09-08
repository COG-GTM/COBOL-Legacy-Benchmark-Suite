package com.clbs.db2;

import org.springframework.data.jpa.repository.JpaRepository;

/** Replaces the AUTHFILE SELECT of SECMGR P200-CHECK-AUTH. */
public interface AuthorizationRepository extends JpaRepository<AuthorizationEntity, Long> {

    long countByUserIdAndResourceAndAccessType(String userId, String resource, String accessType);
}
