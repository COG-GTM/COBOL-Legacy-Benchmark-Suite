package com.portfolio.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "error_log")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_timestamp", nullable = false)
    private LocalDateTime errorTimestamp;

    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "error_type", length = 1, nullable = false)
    private String errorType;

    @Column(name = "error_severity", nullable = false)
    private int errorSeverity;

    @Column(name = "error_code", length = 8, nullable = false)
    private String errorCode;

    @Column(name = "error_message", length = 200, nullable = false)
    private String errorMessage;

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    @Column(name = "user_id", length = 8)
    private String userId;

    public ErrorLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getErrorTimestamp() { return errorTimestamp; }
    public void setErrorTimestamp(LocalDateTime errorTimestamp) { this.errorTimestamp = errorTimestamp; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }

    public int getErrorSeverity() { return errorSeverity; }
    public void setErrorSeverity(int errorSeverity) { this.errorSeverity = errorSeverity; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isSystemError() { return "S".equals(errorType); }
    public boolean isApplicationError() { return "A".equals(errorType); }
    public boolean isDataError() { return "D".equals(errorType); }
}
