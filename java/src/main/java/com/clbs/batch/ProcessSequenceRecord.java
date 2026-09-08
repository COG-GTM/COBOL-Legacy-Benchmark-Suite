package com.clbs.batch;

import java.util.ArrayList;
import java.util.List;

/** PRCSEQ.cpy PROCESS-SEQUENCE-RECORD. */
public class ProcessSequenceRecord {

    /** PRCSEQ.cpy standard daily sequence (INITDAY .. ENDDAY). */
    public static final List<String> STANDARD_SEQUENCE = List.of(
            "INITDAY", "CKPCLR", "DATEVAL", "TRNVAL00", "POSUPD00",
            "HISTLD00", "RPTGEN00", "BCKLOD00", "ENDDAY");

    private String processId = "";
    private String version = "01";
    private String description = "";
    private String type = "UPD";
    private int sequenceNumber;
    private String programName = "";
    private String parameters = "";
    private int maxReturnCode = 4;
    private boolean restartable = true;
    private char status = BatchControlRecord.READY;
    private int returnCode;
    private final List<Dependency> dependencies = new ArrayList<>();

    /** PSR-KEY = PSR-PROCESS-ID + PSR-VERSION. */
    public String key() {
        return pad(processId, 8) + pad(version, 2);
    }

    private static String pad(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() >= length ? v.substring(0, length) : v + " ".repeat(length - v.length());
    }

    /** PSR-DEP-* occurrence: dependency id, hard/soft flag and the highest tolerated return code. */
    public record Dependency(String processId, boolean hard, int maxReturnCode) {
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
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

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
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

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
