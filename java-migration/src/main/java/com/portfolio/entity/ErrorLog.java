package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Error Log Entity
 * Migrated from: DB2 ERRLOG
 * COBOL Copybook: DBTBLS.cpy (ERRLOG-RECORD)
 * 
 * Primary Key: ERROR_TIMESTAMP, PROGRAM_ID
 */
@Entity
@Table(name = "error_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_error_log_key",
                        columnNames = {"error_timestamp", "program_id"})
        },
        indexes = {
                @Index(name = "idx_error_log_date", columnList = "process_date, error_severity DESC"),
                @Index(name = "idx_error_log_program", columnList = "program_id, error_timestamp")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Error Timestamp
     * COBOL: EL-ERROR-TIMESTAMP PIC X(26)
     */
    @NotNull(message = "Error timestamp is required")
    @Column(name = "error_timestamp", nullable = false)
    @Builder.Default
    private OffsetDateTime errorTimestamp = OffsetDateTime.now();

    /**
     * Program Identifier
     * COBOL: EL-PROGRAM-ID PIC X(8)
     */
    @NotBlank(message = "Program ID is required")
    @Size(max = 8, message = "Program ID must not exceed 8 characters")
    @Column(name = "program_id", nullable = false, length = 8)
    private String programId;

    /**
     * Error Type
     * COBOL: EL-ERROR-TYPE PIC X(1)
     * Values: S=System, A=Application, D=Data
     */
    @NotNull(message = "Error type is required")
    @Pattern(regexp = "[SAD]", message = "Error type must be S (System), A (Application), or D (Data)")
    @Column(name = "error_type", nullable = false, length = 1)
    private String errorType;

    /**
     * Error Severity
     * COBOL: EL-ERROR-SEVERITY PIC S9(4) COMP
     * Values: 1=Info, 2=Warning, 3=Error, 4=Severe
     */
    @NotNull(message = "Error severity is required")
    @Min(value = 1, message = "Error severity must be at least 1")
    @Max(value = 4, message = "Error severity must not exceed 4")
    @Column(name = "error_severity", nullable = false)
    private Integer errorSeverity;

    /**
     * Error Code
     * COBOL: EL-ERROR-CODE PIC X(8)
     */
    @NotBlank(message = "Error code is required")
    @Size(max = 8, message = "Error code must not exceed 8 characters")
    @Column(name = "error_code", nullable = false, length = 8)
    private String errorCode;

    /**
     * Error Message
     * COBOL: EL-ERROR-MESSAGE PIC X(200)
     */
    @NotBlank(message = "Error message is required")
    @Size(max = 200, message = "Error message must not exceed 200 characters")
    @Column(name = "error_message", nullable = false, length = 200)
    private String errorMessage;

    /**
     * Process Date
     * COBOL: EL-PROCESS-DATE PIC X(10)
     */
    @NotNull(message = "Process date is required")
    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    /**
     * Process Time
     * COBOL: EL-PROCESS-TIME PIC X(8)
     */
    @NotNull(message = "Process time is required")
    @Column(name = "process_time", nullable = false)
    private LocalTime processTime;

    /**
     * User Identifier
     * COBOL: EL-USER-ID PIC X(8)
     */
    @NotBlank(message = "User ID is required")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    @Column(name = "user_id", nullable = false, length = 8)
    private String userId;

    /**
     * Additional Information
     * COBOL: EL-ADDITIONAL-INFO PIC X(500)
     */
    @Size(max = 500, message = "Additional info must not exceed 500 characters")
    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    /**
     * Check if error is system type
     */
    public boolean isSystemError() {
        return "S".equals(this.errorType);
    }

    /**
     * Check if error is application type
     */
    public boolean isApplicationError() {
        return "A".equals(this.errorType);
    }

    /**
     * Check if error is data type
     */
    public boolean isDataError() {
        return "D".equals(this.errorType);
    }

    /**
     * Check if severity is info
     */
    public boolean isInfo() {
        return this.errorSeverity != null && this.errorSeverity == 1;
    }

    /**
     * Check if severity is warning
     */
    public boolean isWarning() {
        return this.errorSeverity != null && this.errorSeverity == 2;
    }

    /**
     * Check if severity is error
     */
    public boolean isError() {
        return this.errorSeverity != null && this.errorSeverity == 3;
    }

    /**
     * Check if severity is severe
     */
    public boolean isSevere() {
        return this.errorSeverity != null && this.errorSeverity == 4;
    }
}
