package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * Checkpoint Record Entity
 * Migrated from: COBOL Checkpoint/Restart pattern
 * COBOL Copybook: CKPRST.cpy
 * 
 * Supports batch restart capability by tracking last processed position
 */
@Entity
@Table(name = "checkpoint_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_checkpoint_key",
                        columnNames = {"process_date", "process_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckpointRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Process Date
     * COBOL: CHK-PROCESS-DATE PIC 9(08)
     */
    @NotNull(message = "Process date is required")
    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    /**
     * Process Identifier
     * COBOL: CHK-PROCESS-ID PIC X(08)
     */
    @NotBlank(message = "Process ID is required")
    @Size(max = 8, message = "Process ID must not exceed 8 characters")
    @Column(name = "process_id", nullable = false, length = 8)
    private String processId;

    /**
     * Last Transaction ID Processed
     * COBOL: CHK-LAST-TRANS-ID PIC X(12)
     */
    @Size(max = 12, message = "Last transaction ID must not exceed 12 characters")
    @Column(name = "last_trans_id", length = 12)
    private String lastTransId;

    /**
     * Last Account Processed
     * COBOL: CHK-LAST-ACCOUNT PIC 9(09)
     */
    @Size(max = 9, message = "Last account must not exceed 9 characters")
    @Column(name = "last_account", length = 9)
    private String lastAccount;

    /**
     * Last Fund Processed
     * COBOL: CHK-LAST-FUND PIC X(06)
     */
    @Size(max = 6, message = "Last fund must not exceed 6 characters")
    @Column(name = "last_fund", length = 6)
    private String lastFund;

    /**
     * Records Processed Count
     * COBOL: CHK-RECORDS-PROC PIC 9(09)
     */
    @NotNull(message = "Records processed is required")
    @Column(name = "records_processed", nullable = false)
    @Builder.Default
    private Long recordsProcessed = 0L;

    /**
     * Checkpoint Timestamp
     * COBOL: CHK-TIMESTAMP PIC X(26)
     */
    @NotNull(message = "Checkpoint timestamp is required")
    @Column(name = "checkpoint_timestamp", nullable = false)
    @Builder.Default
    private OffsetDateTime checkpointTimestamp = OffsetDateTime.now();

    /**
     * Update checkpoint with current progress
     */
    public void updateCheckpoint(String lastTransId, String lastAccount, String lastFund, long recordsProcessed) {
        this.lastTransId = lastTransId;
        this.lastAccount = lastAccount;
        this.lastFund = lastFund;
        this.recordsProcessed = recordsProcessed;
        this.checkpointTimestamp = OffsetDateTime.now();
    }

    /**
     * Reset checkpoint for new run
     */
    public void reset() {
        this.lastTransId = null;
        this.lastAccount = null;
        this.lastFund = null;
        this.recordsProcessed = 0L;
        this.checkpointTimestamp = OffsetDateTime.now();
    }
}
