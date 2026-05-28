package com.clbs.portfolio.batch.rules;

import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final TransactionRecordRepository transactionRecordRepository;

    public AdjudicationResult apply(TransactionRecord transaction) {
        List<TransactionRecord> duplicates = transactionRecordRepository
                .findByPortfolioIdAndInvestmentIdAndTrnDateAndTrnTypeAndAmount(
                        transaction.getPortfolioId(),
                        transaction.getInvestmentId(),
                        transaction.getTrnDate(),
                        transaction.getTrnType(),
                        transaction.getAmount());

        long otherMatches = duplicates.stream()
                .filter(t -> !t.getId().equals(transaction.getId()))
                .count();

        if (otherMatches > 0) {
            log.warn("Duplicate transaction detected for portfolio={}, investment={}, date={}, count={}",
                    transaction.getPortfolioId(), transaction.getInvestmentId(),
                    transaction.getTrnDate(), otherMatches);
            return AdjudicationResult.DENIED;
        }

        return AdjudicationResult.APPROVED;
    }
}
