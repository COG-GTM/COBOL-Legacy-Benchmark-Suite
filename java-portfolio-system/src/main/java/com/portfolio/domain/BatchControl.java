package com.portfolio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Batch Control entity - migrated from COBOL copybook BCHCTL.cpy
 * Manages job-level control and process sequencing
 */
@Entity
@Table(name = "batch_controls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", length = 8, nullable = false)
    private String jobName;

    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    @Column(name = "sequence_no")
    private Integer sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 1)
    private BatchStatus status;

    @Column(name = "step_name", length = 8)
    private String stepName;

    @Column(name = "program_name", length = 8)
    private String programName;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "return_code")
    private Integer returnCode;

    @Column(name = "error_desc", length = 80)
    private String errorDesc;

    @Column(name = "restart_count")
    private Integer restartCount;

    @Column(name = "attempt_timestamp")
    private LocalDateTime attemptTimestamp;

    @Column(name = "complete_timestamp")
    private LocalDateTime completeTimestamp;

    @Column(name = "records_read")
    private Long recordsRead;

    @Column(name = "records_written")
    private Long recordsWritten;

    @Column(name = "records_error")
    private Long recordsError;

    @PrePersist
    protected void onCreate() {
        if (processDate == null) {
            processDate = LocalDate.now();
        }
        if (status == null) {
            status = BatchStatus.R;
        }
        if (restartCount == null) {
            restartCount = 0;
        }
    }

    public enum BatchStatus {
        R, // Ready
        A, // Active
        W, // Waiting
        D, // Done
        E  // Error
    }
}
