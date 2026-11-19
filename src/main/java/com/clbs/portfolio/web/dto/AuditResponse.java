package com.clbs.portfolio.web.dto;

import com.clbs.portfolio.domain.AuditRecord;

import java.time.LocalDateTime;

public record AuditResponse(Long id, LocalDateTime timestamp, String userId, String program, String type,
                            String action, String status, String portfolioId, String accountNo, String reasonCode,
                            String beforeImage, String afterImage, String message) {

    public static AuditResponse from(AuditRecord a) {
        return new AuditResponse(a.getId(), a.getTimestamp(), a.getUserId(), a.getProgram(), a.getType(),
                a.getAction(), a.getStatus(), a.getPortfolioId(), a.getAccountNo(), a.getReasonCode(),
                a.getBeforeImage(), a.getAfterImage(), a.getMessage());
    }
}
