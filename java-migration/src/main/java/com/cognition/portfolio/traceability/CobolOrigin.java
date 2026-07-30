package com.cognition.portfolio.traceability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Machine-readable link from migrated Java code back to the COBOL source it came from.
 *
 * <p>Every public method that carries business logic in this module is annotated so an engineer
 * (or a static check) can point at any Java method and find the originating COBOL paragraph.
 * {@code TraceabilityTest} enforces that the service layer is fully annotated.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface CobolOrigin {

  /** COBOL program or copybook name, e.g. {@code PORTTRAN} or {@code TRNREC}. */
  String program();

  /** COBOL paragraph or record element, e.g. {@code 2130-CHECK-AMOUNTS}. Empty for whole-program. */
  String paragraph() default "";

  /** Business rule identifiers from MIGRATION-NOTES.md, e.g. {@code BR-04}. */
  String[] rules() default {};

  /** Set when the Java behaviour is derived rather than a literal translation. */
  boolean derived() default false;
}
