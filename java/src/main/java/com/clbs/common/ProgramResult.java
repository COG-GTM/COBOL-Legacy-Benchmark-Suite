package com.clbs.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The observable result of running a migrated program: the RETURN-CODE it would pass back to JCL,
 * its DISPLAY output and its WS-*-COUNT counters.
 */
public class ProgramResult {

    private static final Logger LOG = LoggerFactory.getLogger(ProgramResult.class);

    private final String programName;
    private final List<String> display = new ArrayList<>();
    private final Map<String, Long> counters = new LinkedHashMap<>();
    private int returnCode;

    public ProgramResult(String programName) {
        this.programName = programName;
    }

    /** COBOL DISPLAY: recorded for the caller and echoed to the console log. */
    public ProgramResult display(String line) {
        display.add(line);
        LOG.info("{}: {}", programName, line);
        return this;
    }

    public ProgramResult count(String name, long value) {
        counters.put(name, value);
        return this;
    }

    public long counter(String name) {
        return counters.getOrDefault(name, 0L);
    }

    public String getProgramName() {
        return programName;
    }

    public List<String> getDisplay() {
        return List.copyOf(display);
    }

    public Map<String, Long> getCounters() {
        return Map.copyOf(counters);
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
