package com.cobolbenchmark.security;

/**
 * Security Request DTO - migrated from SECREQ.cpy.
 * Represents the COMMAREA-based request/response for SECMGR.
 */
public class SecurityRequest {

    private String userId;
    private String password;
    private String operation;  // VALIDATE, AUTHORIZE, AUDIT
    private String resourceId;
    private String resourceType;
    private String accessLevel;
    private String auditAction;
    private String auditDetail;

    public SecurityRequest() {
    }

    // Getters and Setters

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public String getAuditAction() { return auditAction; }
    public void setAuditAction(String auditAction) { this.auditAction = auditAction; }

    public String getAuditDetail() { return auditDetail; }
    public void setAuditDetail(String auditDetail) { this.auditDetail = auditDetail; }
}
