package com.portfolio.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Process sequence definitions - migrated from COBOL PRCSEQ.cpy.
 */
public class ProcessSequence {

    private String processId;
    private int version;
    private String description;
    private String type;
    private String frequency;
    private int startTime;
    private int maxTime;
    private List<DependencyEntry> dependencies = new ArrayList<>();
    private String program;
    private String parameter;
    private int maxReturnCode;
    private boolean restartable;
    private String activeDays;
    private boolean monthEnd;
    private boolean holidayRun;
    private String recoveryProgram;
    private String recoveryParam;
    private int errorLimit;

    public ProcessSequence() {
        this.version = 1;
        this.restartable = true;
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
    public List<DependencyEntry> getDependencies() { return dependencies; }
    public void setDependencies(List<DependencyEntry> dependencies) { this.dependencies = dependencies; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public String getParameter() { return parameter; }
    public void setParameter(String parameter) { this.parameter = parameter; }
    public int getMaxReturnCode() { return maxReturnCode; }
    public void setMaxReturnCode(int maxReturnCode) { this.maxReturnCode = maxReturnCode; }
    public boolean isRestartable() { return restartable; }
    public void setRestartable(boolean restartable) { this.restartable = restartable; }
    public String getActiveDays() { return activeDays; }
    public void setActiveDays(String activeDays) { this.activeDays = activeDays; }
    public boolean isMonthEnd() { return monthEnd; }
    public void setMonthEnd(boolean monthEnd) { this.monthEnd = monthEnd; }
    public boolean isHolidayRun() { return holidayRun; }
    public void setHolidayRun(boolean holidayRun) { this.holidayRun = holidayRun; }
    public String getRecoveryProgram() { return recoveryProgram; }
    public void setRecoveryProgram(String recoveryProgram) { this.recoveryProgram = recoveryProgram; }
    public String getRecoveryParam() { return recoveryParam; }
    public void setRecoveryParam(String recoveryParam) { this.recoveryParam = recoveryParam; }
    public int getErrorLimit() { return errorLimit; }
    public void setErrorLimit(int errorLimit) { this.errorLimit = errorLimit; }

    public static class DependencyEntry {
        private String dependencyId;
        private String dependencyType;
        private int requiredReturnCode;

        public DependencyEntry() {}

        public DependencyEntry(String dependencyId, String dependencyType, int requiredReturnCode) {
            this.dependencyId = dependencyId;
            this.dependencyType = dependencyType;
            this.requiredReturnCode = requiredReturnCode;
        }

        public String getDependencyId() { return dependencyId; }
        public void setDependencyId(String dependencyId) { this.dependencyId = dependencyId; }
        public String getDependencyType() { return dependencyType; }
        public void setDependencyType(String dependencyType) { this.dependencyType = dependencyType; }
        public int getRequiredReturnCode() { return requiredReturnCode; }
        public void setRequiredReturnCode(int requiredReturnCode) { this.requiredReturnCode = requiredReturnCode; }
    }
}
