package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Authorization Record entity.
 * Migrated from COBOL SECMGR.cbl (P200-CHECK-AUTH) AUTHFILE table.
 */
@Entity
@Table(name = "AUTHFILE")
public class AuthorizationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUTH_ID")
    private Long authId;

    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    @Column(name = "RESOURCE", length = 8, nullable = false)
    private String resource;

    @Column(name = "ACCESS_TYPE", length = 8, nullable = false)
    private String accessType;

    @Column(name = "GRANTED_DATE", nullable = false)
    private LocalDateTime grantedDate;

    @Column(name = "GRANTED_BY", length = 8, nullable = false)
    private String grantedBy;

    public AuthorizationRecord() {}

    public Long getAuthId() { return authId; }
    public void setAuthId(Long authId) { this.authId = authId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }

    public LocalDateTime getGrantedDate() { return grantedDate; }
    public void setGrantedDate(LocalDateTime grantedDate) { this.grantedDate = grantedDate; }

    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }
}
