package com.clbs.portfolio.service.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DataValidationService {

    private static final Logger log = LoggerFactory.getLogger(DataValidationService.class);

    private final Map<String, Validator> validators;

    public DataValidationService(List<Validator> validatorList) {
        this.validators = validatorList.stream()
                .collect(Collectors.toMap(Validator::getType, Function.identity()));
    }

    public Map<String, ValidationResult> validate(List<String> types) {
        Map<String, ValidationResult> results = new LinkedHashMap<>();

        for (String type : types) {
            String upperType = type.toUpperCase();
            Validator validator = validators.get(upperType);
            if (validator == null) {
                log.warn("Unknown validation type: {}", upperType);
                ValidationResult unknownResult = new ValidationResult(upperType);
                unknownResult.addError("N/A", "Unknown validation type: " + upperType);
                results.put(upperType, unknownResult);
                continue;
            }

            log.info("Running {} validation", upperType);
            ValidationResult result = validator.validate();
            results.put(upperType, result);
            log.info("{} validation complete: read={}, valid={}, errors={}",
                    upperType, result.getRecordsRead(), result.getRecordsValid(),
                    result.getRecordsError());
        }

        return results;
    }
}
