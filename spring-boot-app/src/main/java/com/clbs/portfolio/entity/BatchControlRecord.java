package com.clbs.portfolio.entity;

import com.clbs.portfolio.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_control_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchControlRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", length = 8, nullable = false)
    private String jobName;

    @Column(name = "process_date", length = 8, nullable = false)
    private String processDate;

    @Column(name = "sequence_no")
    private Integer sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private BatchStatus status;

    @Column(name = "step_name", length = 8)
    private String stepName;

    @Column(name = "program_name", length = 8)
    private String programName;

    @Column(name = "start_time", length = 8)
    private String startTime;

    @Column(name = "end_time", length = 8)
    private String endTime;

    @Column(name = "return_code")
    private Integer returnCode;

    @Column(name = "error_desc", length = 200)
    private String errorDesc;

    @Column(name = "restart_count")
    private Integer restartCount;

    @Column(name = "attempt_timestamp")
    private LocalDateTime attemptTimestamp;

    @Column(name = "complete_timestamp")
    private LocalDateTime completeTimestamp;
}
