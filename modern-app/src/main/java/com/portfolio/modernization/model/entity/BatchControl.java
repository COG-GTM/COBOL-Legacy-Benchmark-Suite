package com.portfolio.modernization.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "batch_control", indexes = {
    @Index(name = "idx_batch_status", columnList = "status, process_date")
})
@IdClass(BatchControlId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchControl {

    @Id
    @Column(name = "job_name", length = 8)
    private String jobName;

    @Id
    @Column(name = "process_date", length = 8)
    private String processDate;

    @Id
    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(name = "status", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
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

    @Column(name = "error_description", length = 80)
    private String errorDescription;

    @Column(name = "restart_count")
    private Integer restartCount;

    @Column(name = "attempt_timestamp")
    private LocalDateTime attemptTimestamp;

    @Column(name = "complete_timestamp")
    private LocalDateTime completeTimestamp;

    public enum BatchStatus {
        R, // Ready
        A, // Active
        W, // Waiting
        D, // Done
        E  // Error
    }
}
