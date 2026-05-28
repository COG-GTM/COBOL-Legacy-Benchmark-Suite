package com.clbs.portfolio.batch.processor;

import com.clbs.portfolio.entity.ErrorLog;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.ReturnCode;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionValidationProcessor implements ItemProcessor<TransactionRecord, TransactionRecord> {

    private static final BigDecimal VAL_MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    private static final BigDecimal VAL_MAX_AMOUNT = new BigDecimal("9999999999999.99");
    private static final String VAL_ID_PREFIX = "PORT";

    private final ErrorLogRepository errorLogRepository;

    @Override
    public TransactionRecord process(TransactionRecord transaction) {
        ReturnCode result = validateTransaction(transaction);

        if (result == ReturnCode.VAL_SUCCESS) {
            transaction.setStatus("DONE");
            transaction.setProcessDate(LocalDateTime.now());
            log.debug("Transaction validated successfully: portfolioId={}, seq={}",
                    transaction.getPortfolioId(), transaction.getSequenceNo());
        } else {
            transaction.setStatus("FAILED");
            transaction.setErrorMessage(result.getMessage());
            transaction.setProcessDate(LocalDateTime.now());
            writeError(transaction, result);
            log.warn("Transaction validation failed: portfolioId={}, reason={}",
                    transaction.getPortfolioId(), result.getMessage());
        }

        return transaction;
    }

    private ReturnCode validateTransaction(TransactionRecord transaction) {
        ReturnCode keyResult = validateKey(transaction);
        if (keyResult != ReturnCode.VAL_SUCCESS) {
            return keyResult;
        }

        ReturnCode typeResult = validateType(transaction);
        if (typeResult != ReturnCode.VAL_SUCCESS) {
            return typeResult;
        }

        return validateAmount(transaction);
    }

    private ReturnCode validateKey(TransactionRecord transaction) {
        if (transaction.getTrnDate() == null || !isValidDateFormat(transaction.getTrnDate())) {
            return ReturnCode.VAL_INVALID_ID;
        }

        if (transaction.getTrnTime() == null || !isValidTimeFormat(transaction.getTrnTime())) {
            return ReturnCode.VAL_INVALID_ID;
        }

        if (transaction.getPortfolioId() == null || !transaction.getPortfolioId().startsWith(VAL_ID_PREFIX)) {
            return ReturnCode.VAL_INVALID_ID;
        }

        if (transaction.getSequenceNo() == null || !isNumericNonZero(transaction.getSequenceNo())) {
            return ReturnCode.VAL_INVALID_ACCT;
        }

        return ReturnCode.VAL_SUCCESS;
    }

    private ReturnCode validateType(TransactionRecord transaction) {
        if (transaction.getTrnType() == null) {
            return ReturnCode.VAL_INVALID_TYPE;
        }

        try {
            TransactionType.fromCode(transaction.getTrnType().name());
        } catch (IllegalArgumentException e) {
            return ReturnCode.VAL_INVALID_TYPE;
        }

        return ReturnCode.VAL_SUCCESS;
    }

    private ReturnCode validateAmount(TransactionRecord transaction) {
        BigDecimal amount = transaction.getAmount();

        if (amount == null || amount.compareTo(VAL_MIN_AMOUNT) < 0 || amount.compareTo(VAL_MAX_AMOUNT) > 0) {
            return ReturnCode.VAL_INVALID_AMT;
        }

        BigDecimal quantity = transaction.getQuantity();
        BigDecimal price = transaction.getPrice();

        if (quantity == null || price == null) {
            return ReturnCode.VAL_INVALID_AMT;
        }

        if (transaction.getTrnType() == TransactionType.BU && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return ReturnCode.VAL_INVALID_AMT;
        }
        if (transaction.getTrnType() == TransactionType.SL && quantity.compareTo(BigDecimal.ZERO) >= 0) {
            return ReturnCode.VAL_INVALID_AMT;
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return ReturnCode.VAL_INVALID_AMT;
        }

        BigDecimal expectedAmount = quantity.abs().multiply(price);
        if (expectedAmount.setScale(2, java.math.RoundingMode.HALF_UP)
                .compareTo(amount.abs().setScale(2, java.math.RoundingMode.HALF_UP)) != 0) {
            return ReturnCode.VAL_INVALID_AMT;
        }

        return ReturnCode.VAL_SUCCESS;
    }

    private boolean isValidDateFormat(String date) {
        if (date.length() != 8) return false;
        try {
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(4, 6));
            int day = Integer.parseInt(date.substring(6, 8));
            return year >= 1900 && year <= 2099 && month >= 1 && month <= 12 && day >= 1 && day <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidTimeFormat(String time) {
        if (time.length() != 6) return false;
        try {
            int hour = Integer.parseInt(time.substring(0, 2));
            int minute = Integer.parseInt(time.substring(2, 4));
            int second = Integer.parseInt(time.substring(4, 6));
            return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59 && second >= 0 && second <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isNumericNonZero(String value) {
        try {
            return Long.parseLong(value.trim()) != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void writeError(TransactionRecord transaction, ReturnCode returnCode) {
        ErrorLog errorLog = ErrorLog.builder()
                .errorTimestamp(LocalDateTime.now())
                .programId("TRNVAL00")
                .errorType("D")
                .errorSeverity(3)
                .errorCode(String.format("VAL%05d", returnCode.getCode()))
                .errorMessage(returnCode.getMessage())
                .processDate(LocalDateTime.now())
                .userId("BATCH")
                .additionalInfo("PortfolioId=" + transaction.getPortfolioId()
                        + ", SeqNo=" + transaction.getSequenceNo())
                .build();
        errorLogRepository.save(errorLog);
    }
}
