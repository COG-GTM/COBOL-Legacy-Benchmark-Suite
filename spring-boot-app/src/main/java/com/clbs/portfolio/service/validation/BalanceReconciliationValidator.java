package com.clbs.portfolio.service.validation;

import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.EntityStatus;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BalanceReconciliationValidator implements Validator {

    private final PositionRepository positionRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    public BalanceReconciliationValidator(PositionRepository positionRepository,
                                          TransactionRecordRepository transactionRecordRepository) {
        this.positionRepository = positionRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public String getType() {
        return "BALANCE";
    }

    @Override
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult("BALANCE");

        List<Position> activePositions = positionRepository.findByStatus(EntityStatus.ACTIVE);
        List<TransactionRecord> allTransactions = transactionRecordRepository.findAll();

        // Group transactions by portfolio+investment
        Map<String, List<TransactionRecord>> transactionsByKey = allTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPortfolioId() + "|" + t.getInvestmentId()));

        for (Position pos : activePositions) {
            result.incrementRecordsRead();
            String key = pos.getPortfolioId() + "|" + pos.getInvestmentId();
            List<TransactionRecord> posTransactions = transactionsByKey.getOrDefault(key, List.of());

            // Calculate expected amount from transactions
            BigDecimal transactionTotal = BigDecimal.ZERO;
            for (TransactionRecord trn : posTransactions) {
                if (trn.getAmount() == null) continue;

                if (trn.getTransactionType() == TransactionType.BUY) {
                    transactionTotal = transactionTotal.add(trn.getAmount());
                } else if (trn.getTransactionType() == TransactionType.SELL) {
                    transactionTotal = transactionTotal.subtract(trn.getAmount());
                } else if (trn.getTransactionType() == TransactionType.FEE) {
                    transactionTotal = transactionTotal.subtract(trn.getAmount());
                }
            }

            // Compare position cost basis with transaction total
            if (pos.getCostBasis() != null && !posTransactions.isEmpty()) {
                BigDecimal difference = pos.getCostBasis().subtract(transactionTotal).abs();
                BigDecimal threshold = pos.getCostBasis().abs()
                        .multiply(new BigDecimal("0.01")); // 1% tolerance

                if (difference.compareTo(threshold) > 0) {
                    result.addError(
                            pos.getPortfolioId() + "/" + pos.getInvestmentId(),
                            String.format("Balance mismatch: position cost basis=%s, " +
                                    "transaction total=%s, difference=%s",
                                    pos.getCostBasis(), transactionTotal, difference));
                } else {
                    result.incrementRecordsValid();
                }
            } else {
                result.incrementRecordsValid();
            }
        }

        return result;
    }
}
