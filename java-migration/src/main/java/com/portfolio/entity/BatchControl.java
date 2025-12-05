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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Batch Control Entity
 * Migrated from: VSAM BCHCTL
 * COBOL Copybook: BCHCTL.cpy
 * 
 * Controls batch process execution and supports checkpoint/restart
 */
@Entity
@Table(name = "batch_control",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_batch_control_key",
                        columnNames = {"process_date", "process_id"})
        },
        indexes = {
                @Index(name = "idx_batch_control_status", columnList = "status, process_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchControl implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Process Date
     * COBOL: BCH-PROCESS-DATE PIC 9(08)
     */
    @NotNull(message = "Process date is required")
    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    /**
     * Process Identifier
     * COBOL: BCH-PROCESS-ID PIC X(08)
     */
    @NotBlank(message = "Process ID is required")
    @Size(max = 8, message = "Process ID must not exceed 8 characters")
    @Column(name = "process_id", nullable = false, length = 8)
    private String processId;

    /**
     * Process Status
     * COBOL: BCH-STATUS PIC X(01)
     * Values: W=Waiting, P=In Process, C=Complete, E=Error
     */
    @NotNull(message = "Status is required")
    @Pattern(regexp = "[WPCE]", message = "Status must be W (Waiting), P (In Process), C (Complete), or E (Error)")
    @Column(name = "status", nullable = false, length = 1)
    @Builder.Default
    private String status = "W";

    /**
     * Process Start Time
     * COBOL: BCH-START-TIME PIC 9(08)
     */
    @Column(name = "start_time")
    private OffsetDateTime startTime;

    /**
     * Process End Time
     * COBOL: BCH-END-TIME PIC 9(08)
     */
    @Column(name = "end_time")
    private OffsetDateTime endTime;

    /**
     * Records Processed Count
     * COBOL: BCH-RECORD-COUNT PIC 9(09)
     */
    @NotNull(message = "Record count is required")
    @Column(name = "record_count", nullable = false)
    @Builder.Default
    private Long recordCount = 0L;

    /**
     * Error Count
     * COBOL: BCH-ERROR-COUNT PIC 9(09)
     */
    @NotNull(message = "Error count is required")
    @Column(name = "error_count", nullable = false)
    @Builder.Default
    private Long errorCount = 0L;

    /**
     * Last Position Processed
     * COBOL: BCH-LAST-POS PIC 9(09)
     */
    @NotNull(message = "Last position is required")
    @Column(name = "last_position", nullable = false)
    @Builder.Default
    private Long lastPosition = 0L;

    /**
     * Return Code
     * COBOL: BCH-RETURN-CODE PIC 9(04)
     */
    @NotNull(message = "Return code is required")
    @Column(name = "return_code", nullable = false)
    @Builder.Default
    private Integer returnCode = 0;

    /**
     * Status Message
     * COBOL: BCH-MESSAGE PIC X(50)
     */
    @Size(max = 50, message = "Message must not exceed 50 characters")
    @Column(name = "message", length = 50)
    private String message;

    /**
     * Created Timestamp
     */
    @NotNull(message = "Created at is required")
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * Updated Timestamp
     */
    @NotNull(message = "Updated at is required")
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    /**
     * Check if process is waiting
     */
    public boolean isWaiting() {
        return "W".equals(this.status);
    }

    /**
     * Check if process is in progress
     */
    public boolean isInProgress() {
        return "P".equals(this.status);
    }

    /**
     * Check if process is complete
     */
    public boolean isComplete() {
        return "C".equals(this.status);
    }

    /**
     * Check if process has error
     */
    public boolean hasError() {
        return "E".equals(this.status);
    }

    /**
     * Mark process as started
     */
    public void markStarted() {
        this.status = "P";
        this.startTime = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Mark process as complete
     */
    public void markComplete(int returnCode) {
        this.status = "C";
        this.returnCode = returnCode;
        this.endTime = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Mark process as error
     */
    public void markError(int returnCode, String errorMessage) {
        this.status = "E";
        this.returnCode = returnCode;
        this.message = errorMessage;
        this.endTime = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Update progress
     */
    public void updateProgress(long recordCount, long errorCount, long lastPosition) {
        this.recordCount = recordCount;
        this.errorCount = errorCount;
        this.lastPosition = lastPosition;
        this.updatedAt = OffsetDateTime.now();
    }
}
