package com.clbs.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkpoint_controls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckpointControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "run_date", length = 8)
    private String runDate;

    @Column(name = "run_time", length = 6)
    private String runTime;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "records_read")
    private Long recordsRead;

    @Column(name = "records_processed")
    private Long recordsProcessed;

    @Column(name = "records_error")
    private Long recordsError;

    @Column(name = "restart_count")
    private Integer restartCount;

    @Column(name = "last_key", length = 50)
    private String lastKey;

    @Column(name = "last_time")
    private LocalDateTime lastTime;

    @Column(name = "phase", length = 2)
    private String phase;

    @Column(name = "commit_frequency")
    private Integer commitFrequency;

    @Column(name = "max_errors")
    private Integer maxErrors;
}
