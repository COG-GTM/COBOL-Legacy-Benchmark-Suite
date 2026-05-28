package com.clbs.portfolio.service.validation;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private final String validationType;
    private final List<ValidationError> errors = new ArrayList<>();
    private long recordsRead;
    private long recordsValid;
    private long recordsError;

    public ValidationResult(String validationType) {
        this.validationType = validationType;
    }

    public void addError(String key, String description) {
        errors.add(new ValidationError(validationType, key, description));
        recordsError++;
    }

    public void incrementRecordsRead() {
        recordsRead++;
    }

    public void incrementRecordsValid() {
        recordsValid++;
    }

    public String getValidationType() {
        return validationType;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public long getRecordsRead() {
        return recordsRead;
    }

    public long getRecordsValid() {
        return recordsValid;
    }

    public long getRecordsError() {
        return recordsError;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public record ValidationError(String errorType, String key, String description) {}
}
