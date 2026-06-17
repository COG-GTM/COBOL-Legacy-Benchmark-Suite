package com.clbs.position.web;

import com.clbs.position.service.PositionUpdateService;
import com.clbs.position.web.dto.PositionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for position queries &mdash; the modern, stateless replacement for the
 * CICS online inquiry program {@code INQONLN}/{@code INQPORT}
 * ({@code src/programs/online/}). The COMMAREA-based pseudo-conversational
 * lookup ({@code INQUERY-COMMAREA}, function 'P' = position) becomes an HTTP
 * {@code GET}.
 */
@RestController
@RequestMapping("/positions")
@Tag(name = "Positions", description = "Query portfolio positions (modernized POSUPDT/INQONLN)")
public class PositionController {

    private final PositionUpdateService service;

    public PositionController(PositionUpdateService service) {
        this.service = service;
    }

    /**
     * Lists positions, optionally filtered by portfolio or status &mdash;
     * sequential browse of the position master ({@code RPTPOS00.cbl}
     * {@code 2100-READ-POSITIONS}).
     */
    @GetMapping
    @Operation(summary = "List positions",
            description = "Sequential browse of the position master, optionally "
                    + "filtered by portfolioId or status (A/C/P).")
    public List<PositionResponse> list(
            @Parameter(description = "Portfolio identifier (POS-PORTFOLIO-ID)")
            @RequestParam(required = false) String portfolioId,
            @Parameter(description = "Position status: A=Active, C=Closed, P=Pending")
            @RequestParam(required = false) String status) {
        return resolve(portfolioId, status).stream().map(PositionResponse::from).toList();
    }

    /**
     * Keyed read of a single position by its surrogate id &mdash; the VSAM keyed
     * {@code READ} ({@code PORTTRAN.cbl 2110-CHECK-PORTFOLIO}).
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a position by id",
            description = "Keyed read of a single position; 404 if not found.")
    public ResponseEntity<PositionResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(PositionResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private List<com.clbs.position.entity.Position> resolve(String portfolioId, String status) {
        if (portfolioId != null && !portfolioId.isBlank()) {
            return service.findByPortfolio(portfolioId);
        }
        if (status != null && !status.isBlank()) {
            return service.findByStatus(status);
        }
        return service.findAll();
    }
}
