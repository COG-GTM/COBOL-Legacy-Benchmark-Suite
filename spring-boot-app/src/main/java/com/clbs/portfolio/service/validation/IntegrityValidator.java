package com.clbs.portfolio.service.validation;

import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IntegrityValidator implements Validator {

    private final PositionRepository positionRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    public IntegrityValidator(PositionRepository positionRepository,
                               TransactionRecordRepository transactionRecordRepository) {
        this.positionRepository = positionRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public String getType() {
        return "INTEGRITY";
    }

    @Override
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult("INTEGRITY");

        // Check for orphan positions (positions without valid portfolios)
        List<Position> orphanedPositions = positionRepository.findOrphanedPositions();
        for (Position pos : orphanedPositions) {
            result.incrementRecordsRead();
            result.addError(
                    pos.getPortfolioId() + "/" + pos.getInvestmentId(),
                    "Position references non-existent portfolio: " + pos.getPortfolioId());
        }

        // Check for orphan transactions
        List<TransactionRecord> orphanedTransactions = transactionRecordRepository.findOrphanedTransactions();
        for (TransactionRecord trn : orphanedTransactions) {
            result.incrementRecordsRead();
            result.addError(
                    trn.getPortfolioId() + "/" + trn.getSequenceNo(),
                    "Transaction references non-existent portfolio: " + trn.getPortfolioId());
        }

        // Count valid records
        long totalPositions = positionRepository.count();
        long totalTransactions = transactionRecordRepository.count();
        long validCount = (totalPositions - orphanedPositions.size()) +
                          (totalTransactions - orphanedTransactions.size());
        for (long i = 0; i < validCount; i++) {
            result.incrementRecordsRead();
            result.incrementRecordsValid();
        }

        return result;
    }
}
