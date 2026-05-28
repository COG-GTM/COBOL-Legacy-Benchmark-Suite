package com.clbs.portfolio.service.validation;

import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.EntityStatus;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CrossReferenceValidator implements Validator {

    private final PositionRepository positionRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    public CrossReferenceValidator(PositionRepository positionRepository,
                                    TransactionRecordRepository transactionRecordRepository) {
        this.positionRepository = positionRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public String getType() {
        return "XREF";
    }

    @Override
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult("XREF");

        List<Position> activePositions = positionRepository.findByStatus(EntityStatus.ACTIVE);
        List<TransactionRecord> allTransactions = transactionRecordRepository.findAll();

        Set<String> positionKeys = activePositions.stream()
                .map(p -> p.getPortfolioId() + "|" + p.getInvestmentId())
                .collect(Collectors.toSet());

        Set<String> transactionKeys = allTransactions.stream()
                .map(t -> t.getPortfolioId() + "|" + t.getInvestmentId())
                .collect(Collectors.toSet());

        // Check transactions referencing non-existent positions
        for (TransactionRecord trn : allTransactions) {
            result.incrementRecordsRead();
            String key = trn.getPortfolioId() + "|" + trn.getInvestmentId();
            if (!positionKeys.contains(key)) {
                result.addError(
                        trn.getPortfolioId() + "/" + trn.getInvestmentId(),
                        "Transaction references non-existent position");
            } else {
                result.incrementRecordsValid();
            }
        }

        // Check active positions with no transactions
        for (Position pos : activePositions) {
            result.incrementRecordsRead();
            String key = pos.getPortfolioId() + "|" + pos.getInvestmentId();
            if (!transactionKeys.contains(key)) {
                result.addError(
                        pos.getPortfolioId() + "/" + pos.getInvestmentId(),
                        "Active position has no associated transactions");
            } else {
                result.incrementRecordsValid();
            }
        }

        return result;
    }
}
