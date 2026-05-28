package com.clbs.portfolio.service.maintenance;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceResult {

    private final String function;
    private long recordsProcessed;
    private long recordsAffected;
    private long errorsEncountered;
    private final List<String> details = new ArrayList<>();

    public MaintenanceResult(String function) {
        this.function = function;
    }

    public void incrementRecordsProcessed() {
        recordsProcessed++;
    }

    public void incrementRecordsAffected() {
        recordsAffected++;
    }

    public void incrementErrors() {
        errorsEncountered++;
    }

    public void addDetail(String detail) {
        details.add(detail);
    }

    public String getFunction() {
        return function;
    }

    public long getRecordsProcessed() {
        return recordsProcessed;
    }

    public long getRecordsAffected() {
        return recordsAffected;
    }

    public long getErrorsEncountered() {
        return errorsEncountered;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setRecordsProcessed(long recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public void setRecordsAffected(long recordsAffected) {
        this.recordsAffected = recordsAffected;
    }
}
