package com.portfolio.domain;
import jakarta.persistence.*;
@Entity @Table(name="AUTHFILE",uniqueConstraints=@UniqueConstraint(columnNames={"USER_ID","RESOURCE_NAME","ACCESS_TYPE"}))
public class AuthEntry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long authId; @Column(name="USER_ID",length=8) private String userId; @Column(name="RESOURCE_NAME",length=8) private String resource; @Column(name="ACCESS_TYPE",length=8) private String accessType;
 public AuthEntry(){} public Long getAuthId(){return authId;} public void setAuthId(Long v){authId=v;} public String getUserId(){return userId;} public void setUserId(String v){userId=v;} public String getResource(){return resource;} public void setResource(String v){resource=v;} public String getAccessType(){return accessType;} public void setAccessType(String v){accessType=v;}
}
