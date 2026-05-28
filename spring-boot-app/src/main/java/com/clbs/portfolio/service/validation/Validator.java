package com.clbs.portfolio.service.validation;

public interface Validator {

    String getType();

    ValidationResult validate();
}
