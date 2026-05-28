package com.clbs.portfolio.batch.processor;

import com.clbs.portfolio.entity.Portfolio;
import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.PortfolioRepository;
import com.clbs.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PositionUpdateProcessor implements ItemProcessor<TransactionRecord, TransactionRecord> {

    private final PositionRepository positionRepository;
    private final PortfolioRepository portfolioRepository;

    @Override
    public TransactionRecord process(TransactionRecord transaction) {
        if (transaction.getProcessDate() != null) {
            Optional<Position> existingPos = positionRepository
                    .findByPortfolioIdAndInvestmentId(transaction.getPortfolioId(), transaction.getInvestmentId());
            if (existingPos.isPresent()) {
                Position pos = existingPos.get();
                if (pos.getLastMaintDate() != null && !pos.getLastMaintDate().isBefore(transaction.getProcessDate())) {
                    log.warn("Duplicate payment prevention: transaction {} already applied to position",
                            transaction.getId());
                    return transaction;
                }
            }
        }

        TransactionType type = transaction.getTrnType();

        switch (type) {
            case BU -> processBuy(transaction);
            case SL -> processSell(transaction);
            case TR -> processTransfer(transaction);
            case FE -> processFee(transaction);
        }

        transaction.setStatus("APPLIED");
        transaction.setProcessDate(LocalDateTime.now());
        return transaction;
    }

    private void processBuy(TransactionRecord transaction) {
        Position position = getOrCreatePosition(transaction);
        position.setQuantity(position.getQuantity().add(transaction.getQuantity()));
        position.setCostBasis(position.getCostBasis().add(transaction.getAmount()));
        position.setMarketValue(position.getQuantity().multiply(transaction.getPrice())
                .setScale(2, RoundingMode.HALF_UP));
        position.setStatus("A");
        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("POSUPD00");
        positionRepository.save(position);
        log.debug("BUY processed: portfolio={}, investment={}, newQty={}",
                transaction.getPortfolioId(), transaction.getInvestmentId(), position.getQuantity());
    }

    private void processSell(TransactionRecord transaction) {
        Optional<Position> positionOpt = positionRepository
                .findByPortfolioIdAndInvestmentId(transaction.getPortfolioId(), transaction.getInvestmentId());

        if (positionOpt.isEmpty()) {
            log.error("No position found for SELL: portfolio={}, investment={}",
                    transaction.getPortfolioId(), transaction.getInvestmentId());
            return;
        }

        Position position = positionOpt.get();
        BigDecimal sellQuantity = transaction.getQuantity().abs();
        BigDecimal currentQuantity = position.getQuantity();

        BigDecimal proportion = sellQuantity.divide(currentQuantity, 6, RoundingMode.HALF_UP);
        BigDecimal costBasisReduction = position.getCostBasis().multiply(proportion)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal realizedGainLoss = transaction.getAmount().abs().subtract(costBasisReduction);

        position.setQuantity(currentQuantity.subtract(sellQuantity));
        position.setCostBasis(position.getCostBasis().subtract(costBasisReduction));

        if (position.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            position.setQuantity(BigDecimal.ZERO);
            position.setMarketValue(BigDecimal.ZERO);
            position.setStatus("C");
        } else {
            position.setMarketValue(position.getQuantity().multiply(transaction.getPrice())
                    .setScale(2, RoundingMode.HALF_UP));
            position.setStatus("A");
        }

        BigDecimal totalRealizedGL = position.getRealizedGainLoss() != null
                ? position.getRealizedGainLoss() : BigDecimal.ZERO;
        position.setRealizedGainLoss(totalRealizedGL.add(realizedGainLoss));
        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("POSUPD00");
        positionRepository.save(position);

        log.debug("SELL processed: portfolio={}, investment={}, soldQty={}, realizedGL={}",
                transaction.getPortfolioId(), transaction.getInvestmentId(), sellQuantity, realizedGainLoss);
    }

    private void processTransfer(TransactionRecord transaction) {
        Optional<Position> sourcePositionOpt = positionRepository
                .findByPortfolioIdAndInvestmentId(transaction.getPortfolioId(), transaction.getInvestmentId());

        if (sourcePositionOpt.isEmpty()) {
            log.error("No source position found for TRANSFER: portfolio={}, investment={}",
                    transaction.getPortfolioId(), transaction.getInvestmentId());
            return;
        }

        Position sourcePosition = sourcePositionOpt.get();
        BigDecimal transferQuantity = transaction.getQuantity().abs();
        BigDecimal currentQuantity = sourcePosition.getQuantity();

        BigDecimal proportion = transferQuantity.divide(currentQuantity, 6, RoundingMode.HALF_UP);
        BigDecimal costBasisTransfer = sourcePosition.getCostBasis().multiply(proportion)
                .setScale(2, RoundingMode.HALF_UP);

        sourcePosition.setQuantity(currentQuantity.subtract(transferQuantity));
        sourcePosition.setCostBasis(sourcePosition.getCostBasis().subtract(costBasisTransfer));

        if (sourcePosition.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            sourcePosition.setQuantity(BigDecimal.ZERO);
            sourcePosition.setMarketValue(BigDecimal.ZERO);
            sourcePosition.setStatus("C");
        } else {
            sourcePosition.setMarketValue(sourcePosition.getQuantity()
                    .multiply(transaction.getPrice()).setScale(2, RoundingMode.HALF_UP));
        }

        sourcePosition.setLastMaintDate(LocalDateTime.now());
        sourcePosition.setLastMaintUser("POSUPD00");
        positionRepository.save(sourcePosition);

        String destPortfolioId = extractDestinationPortfolioId(transaction);
        if (destPortfolioId != null) {
            Position destPosition = getOrCreatePositionForTransfer(destPortfolioId, transaction);
            destPosition.setQuantity(destPosition.getQuantity().add(transferQuantity));
            destPosition.setCostBasis(destPosition.getCostBasis().add(costBasisTransfer));
            destPosition.setMarketValue(destPosition.getQuantity()
                    .multiply(transaction.getPrice()).setScale(2, RoundingMode.HALF_UP));
            destPosition.setStatus("A");
            destPosition.setLastMaintDate(LocalDateTime.now());
            destPosition.setLastMaintUser("POSUPD00");
            positionRepository.save(destPosition);
        }

        log.debug("TRANSFER processed: from portfolio={}, qty={}", transaction.getPortfolioId(), transferQuantity);
    }

    private void processFee(TransactionRecord transaction) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(transaction.getPortfolioId());
        if (portfolioOpt.isEmpty()) {
            log.error("Portfolio not found for FEE: {}", transaction.getPortfolioId());
            return;
        }

        Portfolio portfolio = portfolioOpt.get();
        BigDecimal currentCash = portfolio.getCashBalance() != null ? portfolio.getCashBalance() : BigDecimal.ZERO;
        portfolio.setCashBalance(currentCash.subtract(transaction.getAmount().abs()));
        portfolioRepository.save(portfolio);

        log.debug("FEE processed: portfolio={}, feeAmount={}", transaction.getPortfolioId(), transaction.getAmount());
    }

    private Position getOrCreatePosition(TransactionRecord transaction) {
        return positionRepository
                .findByPortfolioIdAndInvestmentId(transaction.getPortfolioId(), transaction.getInvestmentId())
                .orElseGet(() -> Position.builder()
                        .portfolioId(transaction.getPortfolioId())
                        .investmentId(transaction.getInvestmentId())
                        .quantity(BigDecimal.ZERO)
                        .costBasis(BigDecimal.ZERO)
                        .marketValue(BigDecimal.ZERO)
                        .currency(transaction.getCurrency())
                        .status("A")
                        .realizedGainLoss(BigDecimal.ZERO)
                        .build());
    }

    private Position getOrCreatePositionForTransfer(String destPortfolioId, TransactionRecord transaction) {
        String investmentId = transaction.getInvestmentId();
        return positionRepository
                .findByPortfolioIdAndInvestmentId(destPortfolioId, investmentId)
                .orElseGet(() -> Position.builder()
                        .portfolioId(destPortfolioId)
                        .investmentId(investmentId)
                        .quantity(BigDecimal.ZERO)
                        .costBasis(BigDecimal.ZERO)
                        .marketValue(BigDecimal.ZERO)
                        .currency(transaction.getCurrency())
                        .status("A")
                        .realizedGainLoss(BigDecimal.ZERO)
                        .build());
    }

    private String extractDestinationPortfolioId(TransactionRecord transaction) {
        String investmentId = transaction.getInvestmentId();
        if (investmentId != null && investmentId.startsWith("PORT")) {
            return investmentId.length() > 8 ? investmentId.substring(0, 8) : investmentId;
        }
        return null;
    }
}
