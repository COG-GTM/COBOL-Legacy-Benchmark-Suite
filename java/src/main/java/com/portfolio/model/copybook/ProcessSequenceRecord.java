package com.portfolio.model.copybook;

import java.util.ArrayList;
import java.util.List;

/**
 * Migrated from copybook {@code src/copybook/batch/PRCSEQ.cpy}
 * (01 PROCESS-SEQUENCE-RECORD).
 *
 * <p>Batch process scheduling/sequencing definition. Key = PSR-KEY
 * (process id + version).
 */
public class ProcessSequenceRecord {

    /** PSR-PROCESS-ID PIC X(8). */
    private String processId;

    /** PSR-VERSION PIC 9(2). */
    private int version;

    /** PSR-DESCRIPTION PIC X(30). */
    private String description;

    /** PSR-TYPE PIC X(3) — INI/PRC/RPT/TRM (level-88s). */
    private String type;

    /** PSR-FREQ PIC X(1) — D=Daily, W=Weekly, M=Monthly (level-88s). */
    private String frequency;

    /** PSR-START-TIME PIC 9(4) — HHMM. */
    private int startTime;

    /** PSR-MAX-TIME PIC 9(4) — minutes. */
    private int maxTime;

    /** PSR-DEP-ENTRY OCCURS 10 TIMES (PSR-DEP-COUNT PIC 9(2) COMP tracks count). */
    private List<Dependency> dependencies = new ArrayList<>();

    /** PSR-PROGRAM PIC X(8). */
    private String program;

    /** PSR-PARM PIC X(50). */
    private String parameter;

    /** PSR-MAX-RC PIC S9(4) COMP. */
    private int maxReturnCode;

    /** PSR-RESTART PIC X(1) — Y=Restartable, N=No restart (level-88s). */
    private String restartable;

    /** PSR-ACTIVE-DAYS PIC X(7) — one Y/N flag per weekday (level-88s WEEKDAY/WEEKEND/ALL-DAYS). */
    private String activeDays;

    /** PSR-MONTH-END PIC X(1) — Y=run on last day of month. */
    private String monthEnd;

    /** PSR-HOLIDAY-RUN PIC X(1) — Y=run on holidays, N=skip. */
    private String holidayRun;

    /** PSR-RECOVERY-PGM PIC X(8). */
    private String recoveryProgram;

    /** PSR-RECOVERY-PARM PIC X(50). */
    private String recoveryParameter;

    /** PSR-ERROR-LIMIT PIC 9(4) COMP. */
    private int errorLimit;

    /** PSR-CREATE-DATE PIC X(10). */
    private String createDate;

    /** PSR-CREATE-USER PIC X(8). */
    private String createUser;

    /** PSR-UPDATE-DATE PIC X(10). */
    private String updateDate;

    /** PSR-UPDATE-USER PIC X(8). */
    private String updateUser;

    /** One entry of PSR-DEP-ENTRY OCCURS 10 TIMES. */
    public static class Dependency {
        /** PSR-DEP-ID PIC X(8). */
        private String id;
        /** PSR-DEP-TYPE PIC X(1) — H=Hard, S=Soft (level-88s). */
        private String type;
        /** PSR-DEP-RC PIC S9(4) COMP. */
        private int returnCode;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getReturnCode() { return returnCode; }
        public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    }

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }
    public int getMaxTime() { return maxTime; }
    public void setMaxTime(int maxTime) { this.maxTime = maxTime; }
    public List<Dependency> getDependencies() { return dependencies; }
    public void setDependencies(List<Dependency> dependencies) { this.dependencies = dependencies; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public String getParameter() { return parameter; }
    public void setParameter(String parameter) { this.parameter = parameter; }
    public int getMaxReturnCode() { return maxReturnCode; }
    public void setMaxReturnCode(int maxReturnCode) { this.maxReturnCode = maxReturnCode; }
    public String getRestartable() { return restartable; }
    public void setRestartable(String restartable) { this.restartable = restartable; }
    public String getActiveDays() { return activeDays; }
    public void setActiveDays(String activeDays) { this.activeDays = activeDays; }
    public String getMonthEnd() { return monthEnd; }
    public void setMonthEnd(String monthEnd) { this.monthEnd = monthEnd; }
    public String getHolidayRun() { return holidayRun; }
    public void setHolidayRun(String holidayRun) { this.holidayRun = holidayRun; }
    public String getRecoveryProgram() { return recoveryProgram; }
    public void setRecoveryProgram(String recoveryProgram) { this.recoveryProgram = recoveryProgram; }
    public String getRecoveryParameter() { return recoveryParameter; }
    public void setRecoveryParameter(String recoveryParameter) { this.recoveryParameter = recoveryParameter; }
    public int getErrorLimit() { return errorLimit; }
    public void setErrorLimit(int errorLimit) { this.errorLimit = errorLimit; }
    public String getCreateDate() { return createDate; }
    public void setCreateDate(String createDate) { this.createDate = createDate; }
    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }
    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }
    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }
}
