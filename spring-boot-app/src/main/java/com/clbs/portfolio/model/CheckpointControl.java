package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Checkpoint/restart control structure for batch processing.
 * From COBOL copybook: src/copybook/batch/CKPRST.cpy (CHECKPOINT-CONTROL).
 */
@Entity
@Table(name = "checkpoint_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckpointControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** CK-PROGRAM-ID — PIC X(8) */
    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    /** CK-RUN-DATE — PIC X(8) */
    @Column(name = "run_date", length = 8, nullable = false)
    private String runDate;

    /** CK-RUN-TIME — PIC X(6) */
    @Column(name = "run_time", length = 6)
    private String runTime;

    /** CK-STATUS — PIC X(1): I=Initial, A=Active, C=Complete, F=Failed, R=Restarted */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 12, nullable = false)
    private CheckpointStatus status;

    /** CK-RECORDS-READ — PIC 9(9) COMP */
    @Column(name = "records_read")
    private Long recordsRead;

    /** CK-RECORDS-PROC — PIC 9(9) COMP */
    @Column(name = "records_processed")
    private Long recordsProcessed;

    /** CK-RECORDS-ERROR — PIC 9(9) COMP */
    @Column(name = "records_error")
    private Long recordsError;

    /** CK-RESTART-COUNT — PIC 9(2) COMP */
    @Column(name = "restart_count")
    private Integer restartCount;

    /** CK-LAST-KEY — PIC X(50) */
    @Column(name = "last_key", length = 50)
    private String lastKey;

    /** CK-LAST-TIME — PIC X(26) */
    @Column(name = "last_time")
    private LocalDateTime lastTime;

    /** CK-PHASE — PIC X(2): 00=Init, 10=Read, 20=Process, 30=Update, 40=Terminate */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 12)
    private CheckpointPhase phase;

    /** CK-FILE-STATUS — OCCURS 5 TIMES, stored as embedded JSON-like structure */
    @Embedded
    private FileStatuses fileStatuses;

    /** CK-COMMIT-FREQ — PIC 9(5) COMP VALUE 1000 */
    @Column(name = "commit_frequency")
    @Builder.Default
    private Integer commitFrequency = 1000;

    /** CK-MAX-ERRORS — PIC 9(3) COMP VALUE 100 */
    @Column(name = "max_errors")
    @Builder.Default
    private Integer maxErrors = 100;

    /** CK-MAX-RESTARTS — PIC 9(2) COMP VALUE 3 */
    @Column(name = "max_restarts")
    @Builder.Default
    private Integer maxRestarts = 3;

    /** CK-RESTART-MODE — PIC X(1): N=Normal, R=Restart, C=Recover */
    @Column(name = "restart_mode", length = 1)
    private String restartMode;

    public enum CheckpointStatus {
        INITIAL,
        ACTIVE,
        COMPLETE,
        FAILED,
        RESTARTED
    }

    public enum CheckpointPhase {
        INIT,
        READ,
        PROCESS,
        UPDATE,
        TERMINATE
    }
}
