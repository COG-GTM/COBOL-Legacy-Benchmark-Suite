package com.clbs.portfolio.web;

import com.clbs.portfolio.service.AuditService;
import com.clbs.portfolio.web.dto.AuditResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Audit trail (AUDITLOG)")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(operationId = "listAudit", summary = "Audit records, optionally for one portfolio")
    public List<AuditResponse> list(@RequestParam(required = false) String portfolioId) {
        return auditService.find(portfolioId).stream().map(AuditResponse::from).toList();
    }
}
