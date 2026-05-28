package com.clbs.portfolio.service.maintenance;

import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    private final TransactionRecordRepository transactionRecordRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${maintenance.archive.retention-days:365}")
    private int retentionDays;

    public ArchiveService(TransactionRecordRepository transactionRecordRepository) {
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Transactional
    public MaintenanceResult archive() {
        MaintenanceResult result = new MaintenanceResult("ARCHIVE");
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);

        log.info("Archiving transactions older than {}", cutoffDate);

        List<TransactionRecord> oldTransactions = transactionRecordRepository.findOlderThan(cutoffDate);
        result.setRecordsProcessed(oldTransactions.size());

        for (TransactionRecord trn : oldTransactions) {
            try {
                entityManager.createNativeQuery(
                        "INSERT INTO transaction_record_archive " +
                        "(id, transaction_date, transaction_time, portfolio_id, sequence_no, " +
                        "investment_id, transaction_type, quantity, price, amount, currency, " +
                        "status, process_date, process_user) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    .setParameter(1, trn.getId())
                    .setParameter(2, trn.getTransactionDate())
                    .setParameter(3, trn.getTransactionTime())
                    .setParameter(4, trn.getPortfolioId())
                    .setParameter(5, trn.getSequenceNo())
                    .setParameter(6, trn.getInvestmentId())
                    .setParameter(7, trn.getTransactionType().name())
                    .setParameter(8, trn.getQuantity())
                    .setParameter(9, trn.getPrice())
                    .setParameter(10, trn.getAmount())
                    .setParameter(11, trn.getCurrency())
                    .setParameter(12, trn.getStatus())
                    .setParameter(13, trn.getProcessDate())
                    .setParameter(14, trn.getProcessUser())
                    .executeUpdate();

                transactionRecordRepository.delete(trn);
                result.incrementRecordsAffected();
            } catch (Exception e) {
                log.error("Failed to archive transaction {}: {}", trn.getId(), e.getMessage());
                result.incrementErrors();
            }
        }

        result.addDetail(String.format("Archived %d transactions older than %s",
                result.getRecordsAffected(), cutoffDate));
        log.info("Archive complete: {} records archived", result.getRecordsAffected());
        return result;
    }
}
