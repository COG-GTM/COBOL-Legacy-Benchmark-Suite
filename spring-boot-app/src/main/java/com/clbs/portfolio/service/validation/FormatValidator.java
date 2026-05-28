package com.clbs.portfolio.service.validation;

import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
public class FormatValidator implements Validator {

    private static final Set<String> VALID_CURRENCIES = Set.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD");
    private static final Set<String> VALID_TRN_STATUSES = Set.of("P", "D", "F", "R");

    private final PositionRepository positionRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    public FormatValidator(PositionRepository positionRepository,
                            TransactionRecordRepository transactionRecordRepository) {
        this.positionRepository = positionRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public String getType() {
        return "FORMAT";
    }

    @Override
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult("FORMAT");

        List<Position> allPositions = positionRepository.findAll();
        for (Position pos : allPositions) {
            result.incrementRecordsRead();
            boolean valid = true;

            if (pos.getPortfolioId() == null || pos.getPortfolioId().trim().isEmpty()) {
                result.addError(String.valueOf(pos.getId()), "Position has empty portfolio ID");
                valid = false;
            }

            if (pos.getPositionDate() == null) {
                result.addError(pos.getPortfolioId() + "/" + pos.getInvestmentId(),
                        "Position has null position date");
                valid = false;
            }

            if (pos.getQuantity() != null && pos.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                result.addError(pos.getPortfolioId() + "/" + pos.getInvestmentId(),
                        "Position has negative quantity: " + pos.getQuantity());
                valid = false;
            }

            if (pos.getCurrency() != null && !VALID_CURRENCIES.contains(pos.getCurrency())) {
                result.addError(pos.getPortfolioId() + "/" + pos.getInvestmentId(),
                        "Position has invalid currency: " + pos.getCurrency());
                valid = false;
            }

            if (valid) {
                result.incrementRecordsValid();
            }
        }

        List<TransactionRecord> allTransactions = transactionRecordRepository.findAll();
        for (TransactionRecord trn : allTransactions) {
            result.incrementRecordsRead();
            boolean valid = true;

            if (trn.getTransactionDate() == null) {
                result.addError(trn.getPortfolioId() + "/" + trn.getSequenceNo(),
                        "Transaction has null date");
                valid = false;
            }

            if (trn.getAmount() != null && trn.getAmount().abs().compareTo(new BigDecimal("999999999999999")) > 0) {
                result.addError(trn.getPortfolioId() + "/" + trn.getSequenceNo(),
                        "Transaction amount exceeds maximum: " + trn.getAmount());
                valid = false;
            }

            if (trn.getStatus() != null && !VALID_TRN_STATUSES.contains(trn.getStatus())) {
                result.addError(trn.getPortfolioId() + "/" + trn.getSequenceNo(),
                        "Transaction has invalid status: " + trn.getStatus());
                valid = false;
            }

            if (trn.getCurrency() != null && !VALID_CURRENCIES.contains(trn.getCurrency())) {
                result.addError(trn.getPortfolioId() + "/" + trn.getSequenceNo(),
                        "Transaction has invalid currency: " + trn.getCurrency());
                valid = false;
            }

            if (valid) {
                result.incrementRecordsValid();
            }
        }

        return result;
    }
}
