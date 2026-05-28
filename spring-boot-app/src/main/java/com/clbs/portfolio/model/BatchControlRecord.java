package com.clbs.portfolio.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch control record for job-level sequencing and dependency tracking.
 * From COBOL copybook: src/copybook/batch/BCHCTL.cpy (BATCH-CONTROL-RECORD).
 */
@Entity
@Table(name = "batch_control_record")
@IdClass(BatchControlRecordId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchControlRecord {

    /** BCT-JOB-NAME — PIC X(8) */
    @Id
    @Column(name = "job_name", length = 8, nullable = false)
    private String jobName;

    /** BCT-PROCESS-DATE — PIC X(8) */
    @Id
    @Column(name = "process_date", length = 8, nullable = false)
    private String processDate;

    /** BCT-SEQUENCE-NO — PIC 9(4) */
    @Id
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    /** BCT-STATUS — PIC X(1): R=Ready, A=Active, W=Waiting, D=Done, E=Error */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private BatchControlStatus status;

    /** BCT-STEP-NAME — PIC X(8) */
    @Column(name = "step_name", length = 8)
    private String stepName;

    /** BCT-PROGRAM-NAME — PIC X(8) */
    @Column(name = "program_name", length = 8)
    private String programName;

    /** BCT-START-TIME — PIC X(8) */
    @Column(name = "start_time", length = 8)
    private String startTime;

    /** BCT-END-TIME — PIC X(8) */
    @Column(name = "end_time", length = 8)
    private String endTime;

    /** BCT-PREREQ-COUNT — PIC 9(2) COMP */
    @Column(name = "prereq_count")
    private Integer prereqCount;

    /** BCT-PREREQ-JOBS — OCCURS 10 TIMES */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "parent_job_name", referencedColumnName = "job_name"),
        @JoinColumn(name = "parent_process_date", referencedColumnName = "process_date"),
        @JoinColumn(name = "parent_sequence_no", referencedColumnName = "sequence_no")
    })
    @Builder.Default
    private List<PrerequisiteJob> prereqJobs = new ArrayList<>();

    /** BCT-RETURN-CODE — PIC S9(4) COMP */
    @Column(name = "return_code")
    private Integer returnCode;

    /** BCT-ERROR-DESC — PIC X(80) */
    @Column(name = "error_desc", length = 80)
    private String errorDesc;

    /** BCT-RESTART-COUNT — PIC 9(2) COMP */
    @Column(name = "restart_count")
    private Integer restartCount;

    /** BCT-ATTEMPT-TS — PIC X(26) */
    @Column(name = "attempt_timestamp")
    private LocalDateTime attemptTimestamp;

    /** BCT-COMPLETE-TS — PIC X(26) */
    @Column(name = "complete_timestamp")
    private LocalDateTime completeTimestamp;

    public enum BatchControlStatus {
        READY,
        ACTIVE,
        WAITING,
        DONE,
        ERROR
    }
}
