package com.cog.portfolio.web;

import com.cog.portfolio.domain.Portfolio;
import com.cog.portfolio.dto.PortfolioRequest;
import com.cog.portfolio.dto.PortfolioUpdateRequest;
import com.cog.portfolio.service.PortfolioService;
import com.cog.portfolio.service.PortfolioUpdateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * src/jcl/portfolio/*.jcl: PORTADD -> POST, PORTREAD -> GET, PORTUPDT -> PATCH,
 * PORTDEL -> DELETE, full PORTMSTR update -> PUT. PORTDEF.jcl (IDCAMS) is the
 * Flyway schema.
 */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioUpdateService portfolioUpdateService;

    public PortfolioController(PortfolioService portfolioService, PortfolioUpdateService portfolioUpdateService) {
        this.portfolioService = portfolioService;
        this.portfolioUpdateService = portfolioUpdateService;
    }

    /** PORTREAD. */
    @GetMapping
    public List<Portfolio> readAll() {
        return portfolioService.readAll();
    }

    @GetMapping("/{portfolioId}/{accountNo}")
    public Portfolio read(@PathVariable String portfolioId, @PathVariable String accountNo) {
        return portfolioService.read(portfolioId, accountNo);
    }

    /** PORTADD. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Portfolio create(@Valid @RequestBody PortfolioRequest request) {
        return portfolioService.create(request);
    }

    /** PORTMSTR update. */
    @PutMapping
    public Portfolio update(@Valid @RequestBody PortfolioRequest request) {
        return portfolioService.update(request);
    }

    /** PORTUPDT: field-level update file. */
    @PatchMapping
    public PortfolioUpdateService.UpdateSummary applyUpdates(@Valid @RequestBody List<PortfolioUpdateRequest> updates) {
        return portfolioUpdateService.applyAll(updates);
    }

    /** PORTDEL. */
    @DeleteMapping("/{portfolioId}/{accountNo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String portfolioId, @PathVariable String accountNo) {
        portfolioService.delete(portfolioId, accountNo);
    }
}
