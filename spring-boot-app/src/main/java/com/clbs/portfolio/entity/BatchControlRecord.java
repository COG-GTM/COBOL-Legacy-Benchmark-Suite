package com.clbs.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_control_records")
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

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "step_name", length = 8)
    private String stepName;

    @Column(name = "program_name", length = 8)
    private String programName;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "return_code")
    private Integer returnCode;

    @Column(name = "error_desc", length = 80)
    private String errorDesc;

    @Column(name = "restart_count")
    private Integer restartCount;

    @Column(name = "records_read")
    private Long recordsRead;

    @Column(name = "records_written")
    private Long recordsWritten;
}
