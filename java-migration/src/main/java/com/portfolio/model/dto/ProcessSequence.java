package com.portfolio.model.dto;

import java.util.List;

public class ProcessSequence {

    private String processId;
    private int version;
    private String description;
    private String type;
    private String frequency;
    private int startTime;
    private int maxTime;
    private List<DependencyEntry> dependencies;
    private String program;
    private String parameter;
    private int maxReturnCode;
    private boolean restartable;

    public ProcessSequence() {
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(int maxTime) {
        this.maxTime = maxTime;
    }

    public List<DependencyEntry> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<DependencyEntry> dependencies) {
        this.dependencies = dependencies;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public int getMaxReturnCode() {
        return maxReturnCode;
    }

    public void setMaxReturnCode(int maxReturnCode) {
        this.maxReturnCode = maxReturnCode;
    }

    public boolean isRestartable() {
        return restartable;
    }

    public void setRestartable(boolean restartable) {
        this.restartable = restartable;
    }

    public static class DependencyEntry {
        private String dependencyId;
        private String dependencyType;
        private int requiredReturnCode;

        public DependencyEntry() {
        }

        public String getDependencyId() {
            return dependencyId;
        }

        public void setDependencyId(String dependencyId) {
            this.dependencyId = dependencyId;
        }

        public String getDependencyType() {
            return dependencyType;
        }

        public void setDependencyType(String dependencyType) {
            this.dependencyType = dependencyType;
        }

        public int getRequiredReturnCode() {
            return requiredReturnCode;
        }

        public void setRequiredReturnCode(int requiredReturnCode) {
            this.requiredReturnCode = requiredReturnCode;
        }
    }
}
