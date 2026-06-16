package com.clbs.portfolio.service;

import lombok.Builder;

/**
 * Audit trail entry, mapped from copybook {@code AUDITLOG} ({@code 01 AUDIT-RECORD})
 * and populated by PORTTRAN paragraph {@code 2300-UPDATE-AUDIT-TRAIL}.
 *
 * @param program     {@code AUD-PROGRAM}   — always {@code "PORTTRAN"}
 * @param type        {@code AUD-TYPE}      — always {@code "TRAN"}
 * @param action      {@code AUD-ACTION}    — CREATE (BU) / DELETE (SL) / UPDATE (TR,FE)
 * @param status      {@code AUD-STATUS}    — {@code "SUCC"} or {@code "FAIL"}
 * @param portfolioId {@code AUD-PORTFOLIO-ID}
 * @param accountNo   {@code AUD-ACCOUNT-NO}
 * @param message     {@code AUD-MESSAGE}   — free-text description
 */
@Builder
public record AuditRecord(
        String program,
        String type,
        String action,
        String status,
        String portfolioId,
        String accountNo,
        String message) {
}
