package com.cog.gtm.clbs.migration.service.validation;

import java.util.Objects;

public record ValidationResult(int returnCode, String errorMessage) {

    public ValidationResult {
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public ValidationResult(int returnCode) {
        this(returnCode, "");
    }

    public boolean matches(ValidationResult other) {
        return returnCode == other.returnCode
                && Objects.equals(errorMessage.trim(), other.errorMessage.trim());
    }
}
