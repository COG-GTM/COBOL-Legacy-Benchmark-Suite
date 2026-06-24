package com.portfolio.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for Portfolio ID format.
 * Translated from PORTVALD.cbl paragraph 1000-VALIDATE-ID:
 * <pre>
 *   IF LS-INPUT-VALUE(1:4) NOT = VAL-ID-PREFIX   ('PORT')
 *   MOVE LS-INPUT-VALUE(5:4) TO VAL-NUMERIC-CHECK
 *   IF VAL-NUMERIC-CHECK IS NOT NUMERIC
 * </pre>
 * Portfolio ID must start with 'PORT' followed by exactly 4 numeric digits.
 */
@Documented
@Constraint(validatedBy = PortfolioIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPortfolioId {
    String message() default "Invalid Portfolio ID format — must start with 'PORT' followed by 4 digits";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
