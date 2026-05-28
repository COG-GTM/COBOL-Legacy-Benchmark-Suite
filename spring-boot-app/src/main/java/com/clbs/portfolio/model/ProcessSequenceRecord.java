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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Process sequence definition for batch orchestration.
 * From COBOL copybook: src/copybook/batch/PRCSEQ.cpy (PROCESS-SEQUENCE-RECORD).
 */
@Entity
@Table(name = "process_sequence_record")
@IdClass(ProcessSequenceRecordId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessSequenceRecord {

    /** PSR-PROCESS-ID — PIC X(8) */
    @Id
    @Column(name = "process_id", length = 8, nullable = false)
    private String processId;

    /** PSR-VERSION — PIC 9(2) */
    @Id
    @Column(name = "version", nullable = false)
    private Integer version;

    /** PSR-DESCRIPTION — PIC X(30) */
    @Column(name = "description", length = 30)
    private String description;

    /** PSR-TYPE — PIC X(3): INI, PRC, RPT, TRM */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private SequenceType type;

    /** PSR-FREQ — PIC X(1): D=Daily, W=Weekly, M=Monthly */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", length = 10, nullable = false)
    private Frequency frequency;

    /** PSR-START-TIME — PIC 9(4) */
    @Column(name = "start_time")
    private Integer startTime;

    /** PSR-MAX-TIME — PIC 9(4) */
    @Column(name = "max_time")
    private Integer maxTime;

    /** PSR-DEP-ENTRY — OCCURS 10 TIMES */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "parent_process_id", referencedColumnName = "process_id"),
        @JoinColumn(name = "parent_version", referencedColumnName = "version")
    })
    @Builder.Default
    private List<ProcessDependency> dependencies = new ArrayList<>();

    /** PSR-PROGRAM — PIC X(8) */
    @Column(name = "program", length = 8)
    private String program;

    /** PSR-PARM — PIC X(50) */
    @Column(name = "parm", length = 50)
    private String parm;

    /** PSR-MAX-RC — PIC S9(4) COMP */
    @Column(name = "max_return_code")
    private Integer maxReturnCode;

    /** PSR-RESTART — PIC X(1): Y/N */
    @Column(name = "restartable")
    private Boolean restartable;

    /** PSR-ACTIVE-DAYS — PIC X(7) */
    @Column(name = "active_days", length = 7)
    private String activeDays;

    /** PSR-MONTH-END — PIC X(1) */
    @Column(name = "month_end", length = 1)
    private String monthEnd;

    /** PSR-HOLIDAY-RUN — PIC X(1) */
    @Column(name = "holiday_run", length = 1)
    private String holidayRun;

    /** PSR-RECOVERY-PGM — PIC X(8) */
    @Column(name = "recovery_program", length = 8)
    private String recoveryProgram;

    /** PSR-RECOVERY-PARM — PIC X(50) */
    @Column(name = "recovery_parm", length = 50)
    private String recoveryParm;

    /** PSR-ERROR-LIMIT — PIC 9(4) COMP */
    @Column(name = "error_limit")
    private Integer errorLimit;

    /** PSR-CREATE-DATE — PIC X(10) */
    @Column(name = "create_date")
    private LocalDate createDate;

    /** PSR-CREATE-USER — PIC X(8) */
    @Column(name = "create_user", length = 8)
    private String createUser;

    /** PSR-UPDATE-DATE — PIC X(10) */
    @Column(name = "update_date")
    private LocalDate updateDate;

    /** PSR-UPDATE-USER — PIC X(8) */
    @Column(name = "update_user", length = 8)
    private String updateUser;

    public enum SequenceType {
        INIT,
        PROCESS,
        REPORT,
        TERMINATE
    }

    public enum Frequency {
        DAILY,
        WEEKLY,
        MONTHLY
    }
}
