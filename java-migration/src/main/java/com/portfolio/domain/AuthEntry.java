package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "AUTHFILE",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"USER_ID", "RESOURCE_NAME", "ACCESS_TYPE"}))
public class AuthEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long authId;

  @Column(name = "USER_ID", length = 8)
  private String userId;

  @Column(name = "RESOURCE_NAME", length = 8)
  private String resource;

  @Column(name = "ACCESS_TYPE", length = 8)
  private String accessType;

  public AuthEntry() {}

  public Long getAuthId() {
    return authId;
  }

  public void setAuthId(Long authId) {
    this.authId = authId;
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
