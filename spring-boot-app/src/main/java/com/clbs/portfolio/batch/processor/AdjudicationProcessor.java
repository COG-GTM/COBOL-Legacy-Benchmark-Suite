package com.clbs.portfolio.batch.processor;

import com.clbs.portfolio.batch.rules.*;
import com.clbs.portfolio.entity.ErrorLog;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import com.clbs.portfolio.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdjudicationProcessor implements ItemProcessor<TransactionRecord, TransactionRecord> {

    private final ValidationRuleService validationRuleService;
    private final EligibilityRuleService eligibilityRuleService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final FeeScheduleService feeScheduleService;
    private final CostSharingService costSharingService;
    private final CoordinationOfBenefitsService coordinationOfBenefitsService;
    private final PaymentDeterminationService paymentDeterminationService;
    private final ErrorLogRepository errorLogRepository;

    @Override
    public TransactionRecord process(TransactionRecord transaction) {
        Map<String, Function<TransactionRecord, AdjudicationResult>> rules = new LinkedHashMap<>();
        rules.put("Validation", validationRuleService::apply);
        rules.put("Eligibility", eligibilityRuleService::apply);
        rules.put("DuplicateDetection", duplicateDetectionService::apply);
        rules.put("FeeSchedule", feeScheduleService::apply);
        rules.put("CostSharing", costSharingService::apply);
        rules.put("CoordinationOfBenefits", coordinationOfBenefitsService::apply);
        rules.put("PaymentDetermination", paymentDeterminationService::apply);

        for (Map.Entry<String, Function<TransactionRecord, AdjudicationResult>> entry : rules.entrySet()) {
            String ruleName = entry.getKey();
            AdjudicationResult result = entry.getValue().apply(transaction);

            log.debug("Rule '{}' result for transaction {}: {}", ruleName, transaction.getId(), result);

            if (result == AdjudicationResult.DENIED) {
                transaction.setAdjudicationStatus("DENIED");
                transaction.setStatus("FAILED");
                transaction.setProcessDate(LocalDateTime.now());
                transaction.setErrorMessage("Adjudication denied at rule: " + ruleName);
                writeError(transaction, ruleName);
                return transaction;
            }

            if (result == AdjudicationResult.NEEDS_REVIEW) {
                transaction.setAdjudicationStatus("NEEDS_REVIEW");
                transaction.setProcessDate(LocalDateTime.now());
                return transaction;
            }
        }

        transaction.setAdjudicationStatus("APPROVED");
        transaction.setProcessDate(LocalDateTime.now());
        log.info("Transaction {} adjudicated as APPROVED", transaction.getId());
        return transaction;
    }

    private void writeError(TransactionRecord transaction, String failedRule) {
        ErrorLog errorLog = ErrorLog.builder()
                .errorTimestamp(LocalDateTime.now())
                .programId("CLMADJ00")
                .errorType("A")
                .errorSeverity(3)
                .errorCode("ADJ" + failedRule.substring(0, Math.min(5, failedRule.length())))
                .errorMessage("Adjudication denied at rule: " + failedRule)
                .processDate(LocalDateTime.now())
                .userId("BATCH")
                .additionalInfo("PortfolioId=" + transaction.getPortfolioId()
                        + ", TxnId=" + transaction.getId())
                .build();
        errorLogRepository.save(errorLog);
    }
}
