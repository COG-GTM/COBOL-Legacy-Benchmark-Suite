package com.portfolio.domain;
import jakarta.persistence.*;
@Entity @Table(name="APP_USER")
public class AppUser {
 @Id @Column(name="USER_ID",length=8) private String userId; @Column(name="PASSWORD_HASH",length=100) private String passwordHash; @Column(length=16) private String role; private boolean enabled;
 public AppUser(){} public String getUserId(){return userId;} public void setUserId(String v){userId=v;} public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;} public String getRole(){return role;} public void setRole(String v){role=v;} public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
}
