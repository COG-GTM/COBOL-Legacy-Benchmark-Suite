package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
 * Return Code Entity
 * Migrated from: DB2 RTNCODES
 * COBOL Copybook: RTNCODE.cpy
 * 
 * Tracks program return codes for batch processing
 */
@Entity
@Table(name = "return_codes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_return_codes_key",
                        columnNames = {"log_timestamp", "program_id"})
        },
        indexes = {
                @Index(name = "idx_return_codes_program", columnList = "program_id, log_timestamp"),
                @Index(name = "idx_return_codes_status", columnList = "status_code, log_timestamp")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Log Timestamp
     * COBOL: RC-TIMESTAMP
     */
    @NotNull(message = "Log timestamp is required")
    @Column(name = "log_timestamp", nullable = false)
    @Builder.Default
    private OffsetDateTime logTimestamp = OffsetDateTime.now();

    /**
     * Program Identifier
     * COBOL: RC-PROGRAM-ID PIC X(8)
     */
    @NotBlank(message = "Program ID is required")
    @Size(max = 8, message = "Program ID must not exceed 8 characters")
    @Column(name = "program_id", nullable = false, length = 8)
    private String programId;

    /**
     * Current Return Code
     * COBOL: RC-CURRENT-CODE PIC S9(4) COMP
     */
    @NotNull(message = "Return code is required")
    @Column(name = "return_code", nullable = false)
    private Integer returnCode;

    /**
     * Highest Return Code
     * COBOL: RC-HIGHEST-CODE PIC S9(4) COMP
     */
    @NotNull(message = "Highest code is required")
    @Column(name = "highest_code", nullable = false)
    private Integer highestCode;

    /**
     * Status Code
     * COBOL: RC-STATUS PIC X
     * Values: S=Success, W=Warning, E=Error, F=Severe
     */
    @NotNull(message = "Status code is required")
    @Pattern(regexp = "[SWEF]", message = "Status code must be S (Success), W (Warning), E (Error), or F (Severe)")
    @Column(name = "status_code", nullable = false, length = 1)
    private String statusCode;

    /**
     * Message Text
     * COBOL: RC-MESSAGE PIC X(80)
     */
    @Size(max = 80, message = "Message text must not exceed 80 characters")
    @Column(name = "message_text", length = 80)
    private String messageText;

    /**
     * Check if status is success
     */
    public boolean isSuccess() {
        return "S".equals(this.statusCode);
    }

    /**
     * Check if status is warning
     */
    public boolean isWarning() {
        return "W".equals(this.statusCode);
    }

    /**
     * Check if status is error
     */
    public boolean isError() {
        return "E".equals(this.statusCode);
    }

    /**
     * Check if status is severe
     */
    public boolean isSevere() {
        return "F".equals(this.statusCode);
    }

    /**
     * Check if return code indicates success (0)
     */
    public boolean isReturnCodeSuccess() {
        return this.returnCode != null && this.returnCode == 0;
    }

    /**
     * Check if return code indicates warning (4)
     */
    public boolean isReturnCodeWarning() {
        return this.returnCode != null && this.returnCode == 4;
    }

    /**
     * Check if return code indicates error (8)
     */
    public boolean isReturnCodeError() {
        return this.returnCode != null && this.returnCode == 8;
    }

    /**
     * Check if return code indicates severe error (12 or 16)
     */
    public boolean isReturnCodeSevere() {
        return this.returnCode != null && this.returnCode >= 12;
    }
}
