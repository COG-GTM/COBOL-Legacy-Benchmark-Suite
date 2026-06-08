package com.portfolio.domain.service;

import com.portfolio.domain.command.TransactionCommand;
import com.portfolio.domain.event.TransactionProcessedEvent;
import com.portfolio.domain.model.Portfolio;
import com.portfolio.domain.repository.PortfolioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Ports the 2200-UPDATE-POSITIONS dispatcher from PORTTRAN.cbl.
 * Dispatches to Portfolio aggregate methods and emits a domain event.
 */
@Service
public class TransactionProcessor {

    private final PortfolioRepository portfolioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionProcessor(PortfolioRepository portfolioRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.portfolioRepository = portfolioRepository;
        this.eventPublisher = eventPublisher;
    }

    public void process(TransactionCommand command) {
        Portfolio portfolio = portfolioRepository.findById(command.portfolioId())
                .orElseThrow(() -> new IllegalStateException(
                        "Portfolio not found for update: " + command.portfolioId()));

        switch (command.type()) {
            case BUY -> portfolio.applyBuy(command.quantity(), command.amount());
            case SELL -> portfolio.applySell(command.quantity(), command.amount());
            case FEE -> portfolio.applyFee(command.amount());
            case TRANSFER -> portfolio.applyTransfer();
        }

        portfolio.markMaintenance(command.processUser());
        portfolioRepository.save(portfolio);

        eventPublisher.publishEvent(new TransactionProcessedEvent(
                portfolio.getPortfolioId(),
                portfolio.getAccountNumber(),
                command.type(),
                command.amount(),
                command.processUser(),
                LocalDateTime.now()
        ));
    }
}
