package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Audit Log Entity
 * Migrated from: COBOL AUDITLOG copybook
 * COBOL Copybook: AUDITLOG.cpy
 * 
 * Tracks all system activities including transactions, user actions, and system events
 */
@Entity
@Table(name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_log_timestamp", columnList = "audit_timestamp"),
                @Index(name = "idx_audit_log_user", columnList = "user_id, audit_timestamp"),
                @Index(name = "idx_audit_log_portfolio", columnList = "portfolio_id, audit_timestamp"),
                @Index(name = "idx_audit_log_type", columnList = "audit_type, action, audit_timestamp")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Audit Timestamp
     * COBOL: AUD-TIMESTAMP PIC X(26)
     */
    @NotNull(message = "Audit timestamp is required")
    @Column(name = "audit_timestamp", nullable = false)
    @Builder.Default
    private OffsetDateTime auditTimestamp = OffsetDateTime.now();

    /**
     * System Identifier
     * COBOL: AUD-SYSTEM-ID PIC X(8)
     */
    @NotBlank(message = "System ID is required")
    @Size(max = 8, message = "System ID must not exceed 8 characters")
    @Column(name = "system_id", nullable = false, length = 8)
    private String systemId;

    /**
     * User Identifier
     * COBOL: AUD-USER-ID PIC X(8)
     */
    @NotBlank(message = "User ID is required")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    @Column(name = "user_id", nullable = false, length = 8)
    private String userId;

    /**
     * Program Identifier
     * COBOL: AUD-PROGRAM PIC X(8)
     */
    @NotBlank(message = "Program ID is required")
    @Size(max = 8, message = "Program ID must not exceed 8 characters")
    @Column(name = "program_id", nullable = false, length = 8)
    private String programId;

    /**
     * Terminal Identifier
     * COBOL: AUD-TERMINAL PIC X(8)
     */
    @Size(max = 8, message = "Terminal ID must not exceed 8 characters")
    @Column(name = "terminal_id", length = 8)
    private String terminalId;

    /**
     * Audit Type
     * COBOL: AUD-TYPE PIC X(4)
     * Values: TRAN=Transaction, USER=User Action, SYST=System Event
     */
    @NotNull(message = "Audit type is required")
    @Pattern(regexp = "TRAN|USER|SYST", message = "Audit type must be TRAN, USER, or SYST")
    @Column(name = "audit_type", nullable = false, length = 4)
    private String auditType;

    /**
     * Action Performed
     * COBOL: AUD-ACTION PIC X(8)
     * Values: CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN
     */
    @NotBlank(message = "Action is required")
    @Size(max = 8, message = "Action must not exceed 8 characters")
    @Column(name = "action", nullable = false, length = 8)
    private String action;

    /**
     * Status
     * COBOL: AUD-STATUS PIC X(4)
     * Values: SUCC=Success, FAIL=Failure, WARN=Warning
     */
    @NotNull(message = "Status is required")
    @Pattern(regexp = "SUCC|FAIL|WARN", message = "Status must be SUCC, FAIL, or WARN")
    @Column(name = "status", nullable = false, length = 4)
    private String status;

    /**
     * Portfolio Identifier
     * COBOL: AUD-PORTFOLIO-ID PIC X(8)
     */
    @Size(max = 8, message = "Portfolio ID must not exceed 8 characters")
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    /**
     * Account Number
     * COBOL: AUD-ACCOUNT-NO PIC X(10)
     */
    @Size(max = 10, message = "Account number must not exceed 10 characters")
    @Column(name = "account_no", length = 10)
    private String accountNo;

    /**
     * Before Image (record state before change)
     * COBOL: AUD-BEFORE-IMAGE PIC X(100)
     */
    @Column(name = "before_image", columnDefinition = "TEXT")
    private String beforeImage;

    /**
     * After Image (record state after change)
     * COBOL: AUD-AFTER-IMAGE PIC X(100)
     */
    @Column(name = "after_image", columnDefinition = "TEXT")
    private String afterImage;

    /**
     * Audit Message
     * COBOL: AUD-MESSAGE PIC X(100)
     */
    @Size(max = 100, message = "Message must not exceed 100 characters")
    @Column(name = "message", length = 100)
    private String message;

    /**
     * Check if audit type is transaction
     */
    public boolean isTransaction() {
        return "TRAN".equals(this.auditType);
    }

    /**
     * Check if audit type is user action
     */
    public boolean isUserAction() {
        return "USER".equals(this.auditType);
    }

    /**
     * Check if audit type is system event
     */
    public boolean isSystemEvent() {
        return "SYST".equals(this.auditType);
    }

    /**
     * Check if status is success
     */
    public boolean isSuccess() {
        return "SUCC".equals(this.status);
    }

    /**
     * Check if status is failure
     */
    public boolean isFailure() {
        return "FAIL".equals(this.status);
    }

    /**
     * Check if status is warning
     */
    public boolean isWarning() {
        return "WARN".equals(this.status);
    }

    /**
     * Check if action is create
     */
    public boolean isCreate() {
        return "CREATE".equals(this.action);
    }

    /**
     * Check if action is update
     */
    public boolean isUpdate() {
        return "UPDATE".equals(this.action);
    }

    /**
     * Check if action is delete
     */
    public boolean isDelete() {
        return "DELETE".equals(this.action);
    }

    /**
     * Check if action is inquire
     */
    public boolean isInquire() {
        return "INQUIRE".equals(this.action);
    }
}
