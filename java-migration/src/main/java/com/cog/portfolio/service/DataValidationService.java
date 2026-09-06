package com.cog.portfolio.service;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.domain.Portfolio;
import com.cog.portfolio.domain.Position;
import com.cog.portfolio.domain.PositionHistory;
import com.cog.portfolio.repository.PortfolioRepository;
import com.cog.portfolio.repository.PositionHistoryRepository;
import com.cog.portfolio.repository.PositionRepository;
import com.cog.portfolio.validation.PortfolioValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UTLVAL00.cbl. The COBOL program dispatches on VAL-TYPE (INTEGRITY / XREF /
 * FORMAT / BALANCE) and writes one 132-byte error line per finding
 * (type, key, description). The detail paragraphs 2210-2520 are only declared
 * as PERFORM targets in the source, so the concrete checks below are
 * reconstructed from their names.
 */
@Service
public class DataValidationService {

    /** UTLVAL00 WS-VALIDATION-TYPES. */
    public enum ValidationType implements CodedValue {
        INTEGRITY("INTEGRITY"), XREF("XREF"), FORMAT("FORMAT"), BALANCE("BALANCE");

        private final String code;

        ValidationType(String code) {
            this.code = code;
        }

        @Override
        public String code() {
            return code;
        }
    }

    /** WS-ERROR-LINE: type / key / description. */
    public record Finding(ValidationType type, String key, String description) {
    }

    /** WS-VALIDATION-TOTALS. */
    public record ValidationReport(long recordsRead, long recordsValid, List<Finding> findings) {
        public long recordsError() {
            return findings.size();
        }

        public boolean errorFound() {
            return !findings.isEmpty();
        }
    }

    private final PositionRepository positionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PositionHistoryRepository historyRepository;
    private final PortfolioValidator validator;

    public DataValidationService(PositionRepository positionRepository, PortfolioRepository portfolioRepository,
                                 PositionHistoryRepository historyRepository, PortfolioValidator validator) {
        this.positionRepository = positionRepository;
        this.portfolioRepository = portfolioRepository;
        this.historyRepository = historyRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public ValidationReport validate(Set<ValidationType> types) {
        List<Position> positions = positionRepository.findAll();
        List<PositionHistory> history = historyRepository.findAll();
        List<Portfolio> portfolios = portfolioRepository.findAll();
        List<Finding> findings = new ArrayList<>();
        for (ValidationType type : types) {
            switch (type) {
                case INTEGRITY -> checkIntegrity(positions, history, findings);
                case XREF -> checkCrossReference(positions, history, portfolios, findings);
                case FORMAT -> checkFormat(positions, portfolios, findings);
                case BALANCE -> checkBalance(positions, portfolios, findings);
            }
        }
        long read = positions.size() + history.size();
        return new ValidationReport(read, read - findings.size(), findings);
    }

    /** 2210/2220-CHECK-*-INTEGRITY: no negative quantities, amounts consistent. */
    private void checkIntegrity(List<Position> positions, List<PositionHistory> history, List<Finding> out) {
        for (Position p : positions) {
            if (p.getQuantity().signum() < 0) {
                out.add(new Finding(ValidationType.INTEGRITY, p.getId().toString(), "NEGATIVE QUANTITY"));
            }
            if (p.getCostBasis().signum() < 0) {
                out.add(new Finding(ValidationType.INTEGRITY, p.getId().toString(), "NEGATIVE COST BASIS"));
            }
        }
        for (PositionHistory h : history) {
            if (h.getTotalAmount().compareTo(h.getAmount().add(h.getFees())) != 0) {
                out.add(new Finding(ValidationType.INTEGRITY, h.getId().toString(), "TOTAL <> AMOUNT + FEES"));
            }
        }
    }

    /** 2310/2320-CHECK-*-XREF: every position/history row points at an existing portfolio. */
    private void checkCrossReference(List<Position> positions, List<PositionHistory> history,
                                     List<Portfolio> portfolios, List<Finding> out) {
        Set<String> portfolioIds = portfolios.stream().map(p -> p.getId().getPortfolioId()).collect(Collectors.toSet());
        Set<String> accounts = portfolios.stream().map(p -> p.getId().getAccountNo()).collect(Collectors.toSet());
        for (Position p : positions) {
            if (!portfolioIds.contains(p.getId().getPortfolioId())) {
                out.add(new Finding(ValidationType.XREF, p.getId().toString(), "PORTFOLIO NOT FOUND"));
            }
        }
        for (PositionHistory h : history) {
            if (!accounts.contains(h.getId().getAccountNo())) {
                out.add(new Finding(ValidationType.XREF, h.getId().toString(), "ACCOUNT NOT FOUND"));
            }
        }
    }

    /** 2410/2420-CHECK-*-FORMAT: PORTVALD rules on keys. */
    private void checkFormat(List<Position> positions, List<Portfolio> portfolios, List<Finding> out) {
        for (Portfolio f : portfolios) {
            if (!validator.validatePortfolioId(f.getId().getPortfolioId()).isValid()) {
                out.add(new Finding(ValidationType.FORMAT, f.getId().toString(), "INVALID PORTFOLIO ID FORMAT"));
            }
            if (!validator.validateAccountNo(f.getId().getAccountNo()).isValid()) {
                out.add(new Finding(ValidationType.FORMAT, f.getId().toString(), "INVALID ACCOUNT FORMAT"));
            }
        }
        for (Position p : positions) {
            if (p.getId().getInvestmentId() == null || p.getId().getInvestmentId().isBlank()) {
                out.add(new Finding(ValidationType.FORMAT, p.getId().toString(), "MISSING INVESTMENT ID"));
            }
        }
    }

    /** 2510-ACCUMULATE-POSITIONS + 2520-VERIFY-BALANCES: sum(market value) vs PORT-TOTAL-VALUE. */
    private void checkBalance(List<Position> positions, List<Portfolio> portfolios, List<Finding> out) {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (Position p : positions) {
            totals.merge(p.getId().getPortfolioId(), p.getMarketValue(), BigDecimal::add);
        }
        for (Portfolio f : portfolios) {
            BigDecimal accumulated = totals.getOrDefault(f.getId().getPortfolioId(), BigDecimal.ZERO);
            if (accumulated.compareTo(f.getTotalValue()) != 0) {
                out.add(new Finding(ValidationType.BALANCE, f.getId().toString(),
                        "POSITIONS " + accumulated + " <> CONTROL TOTAL " + f.getTotalValue()));
            }
        }
    }
}
