package com.clbs.portfolio;

import com.clbs.portfolio.repository.AuditRepository;
import com.clbs.portfolio.repository.PortfolioRepository;
import com.clbs.portfolio.repository.PositionHistoryRepository;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRepository;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    private final PortfolioRepository portfolios;
    private final PositionRepository positions;
    private final TransactionRepository transactions;
    private final PositionHistoryRepository history;
    private final AuditRepository audit;

    public DatabaseCleaner(PortfolioRepository portfolios, PositionRepository positions,
                           TransactionRepository transactions, PositionHistoryRepository history,
                           AuditRepository audit) {
        this.portfolios = portfolios;
        this.positions = positions;
        this.transactions = transactions;
        this.history = history;
        this.audit = audit;
    }

    public void clean() {
        history.deleteAll();
        transactions.deleteAll();
        positions.deleteAll();
        portfolios.deleteAll();
        audit.deleteAll();
    }
}
