package com.cog.gtm.clbs.migration.golden;

import com.cog.gtm.clbs.migration.service.validation.ValidationResult;

public record GoldenMasterCase(
        String validationType,
        String inputValue,
        ValidationResult expectedResult) {
}
