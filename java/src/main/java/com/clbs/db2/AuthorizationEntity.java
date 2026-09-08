package com.clbs.db2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** AUTHFILE table queried by SECMGR P200-CHECK-AUTH. */
@Entity
@Table(name = "authfile")
public class AuthorizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 8)
    private String userId;

    @Column(name = "resource_name", length = 8)
    private String resource;

    @Column(name = "access_type", length = 8)
    private String accessType;

    public AuthorizationEntity() {
    }

    public AuthorizationEntity(String userId, String resource, String accessType) {
        this.userId = userId;
        this.resource = resource;
        this.accessType = accessType;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }
}
