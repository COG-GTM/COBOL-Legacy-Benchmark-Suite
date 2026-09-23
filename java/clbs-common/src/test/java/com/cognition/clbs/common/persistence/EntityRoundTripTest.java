package com.cognition.clbs.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.clbs.common.copybook.SampleRecords;
import com.cognition.clbs.common.copybook.common.AuditRecord;
import com.cognition.clbs.common.copybook.common.HistoryRecord;
import com.cognition.clbs.common.copybook.common.PortfolioRecord;
import com.cognition.clbs.common.copybook.common.PositionRecord;
import com.cognition.clbs.common.copybook.common.TransactionRecord;
import com.cognition.clbs.common.copybook.db2.ErrorLogRecord;
import com.cognition.clbs.common.copybook.db2.PositionHistoryRecord;
import com.cognition.clbs.common.persistence.entity.AuditLog;
import com.cognition.clbs.common.persistence.entity.ChangeHistory;
import com.cognition.clbs.common.persistence.entity.ChangeHistoryKey;
import com.cognition.clbs.common.persistence.entity.ErrorLog;
import com.cognition.clbs.common.persistence.entity.ErrorLogKey;
import com.cognition.clbs.common.persistence.entity.InvestmentPosition;
import com.cognition.clbs.common.persistence.entity.InvestmentPositionKey;
import com.cognition.clbs.common.persistence.entity.Portfolio;
import com.cognition.clbs.common.persistence.entity.PortfolioKey;
import com.cognition.clbs.common.persistence.entity.PortfolioMaster;
import com.cognition.clbs.common.persistence.entity.PortfolioTransaction;
import com.cognition.clbs.common.persistence.entity.PortfolioTransactionKey;
import com.cognition.clbs.common.persistence.entity.PositionHistory;
import com.cognition.clbs.common.persistence.entity.PositionHistoryKey;
import com.cognition.clbs.common.persistence.entity.PositionMaster;
import com.cognition.clbs.common.persistence.entity.PositionMasterKey;
import com.cognition.clbs.common.persistence.entity.ReturnCodeLog;
import com.cognition.clbs.common.persistence.entity.ReturnCodeLogKey;
import com.cognition.clbs.common.persistence.entity.TransactionHistory;
import com.cognition.clbs.common.persistence.mapper.AuditLogMapper;
import com.cognition.clbs.common.persistence.mapper.ChangeHistoryMapper;
import com.cognition.clbs.common.persistence.mapper.ErrorLogMapper;
import com.cognition.clbs.common.persistence.mapper.PortfolioMapper;
import com.cognition.clbs.common.persistence.mapper.PortfolioTransactionMapper;
import com.cognition.clbs.common.persistence.mapper.PositionHistoryMapper;
import com.cognition.clbs.common.persistence.mapper.PositionMasterMapper;
import com.cognition.clbs.common.persistence.repository.AuditLogRepository;
import com.cognition.clbs.common.persistence.repository.ChangeHistoryRepository;
import com.cognition.clbs.common.persistence.repository.ErrorLogRepository;
import com.cognition.clbs.common.persistence.repository.InvestmentPositionRepository;
import com.cognition.clbs.common.persistence.repository.PortfolioMasterRepository;
import com.cognition.clbs.common.persistence.repository.PortfolioRepository;
import com.cognition.clbs.common.persistence.repository.PortfolioTransactionRepository;
import com.cognition.clbs.common.persistence.repository.PositionHistoryRepository;
import com.cognition.clbs.common.persistence.repository.PositionMasterRepository;
import com.cognition.clbs.common.persistence.repository.ReturnCodeLogRepository;
import com.cognition.clbs.common.persistence.repository.TransactionHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Round-trips sample copybook records through the mappers, the entities and the Flyway-created
 * schema: record -> entity -> database -> entity -> record must reproduce the original bytes.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class EntityRoundTripTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2024, 3, 15, 14, 30, 45, 123456000);

  @Autowired private TestEntityManager entityManager;
  @Autowired private PortfolioRepository portfolios;
  @Autowired private PositionMasterRepository positions;
  @Autowired private PortfolioTransactionRepository transactions;
  @Autowired private ChangeHistoryRepository history;
  @Autowired private AuditLogRepository audits;
  @Autowired private PositionHistoryRepository positionHistory;
  @Autowired private ErrorLogRepository errorLogs;
  @Autowired private ReturnCodeLogRepository returnCodes;
  @Autowired private PortfolioMasterRepository portfolioMasters;
  @Autowired private InvestmentPositionRepository investmentPositions;
  @Autowired private TransactionHistoryRepository transactionHistory;

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void portfolioRecordRoundTripsThroughPortfolioTable() {
    PortfolioRecord record = SampleRecords.portfolio();
    portfolios.save(PortfolioMapper.toEntity(record));
    flushAndClear();

    Portfolio reloaded =
        portfolios.findById(new PortfolioKey("PORT0001", "ACC0000001")).orElseThrow();
    assertThat(reloaded.getCreateDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    assertThat(reloaded.getLastTransactionDate()).isNull();
    assertThat(reloaded.getCashBalance()).isEqualTo(new BigDecimal("-500.25"));
    assertThat(PortfolioMapper.toRecord(reloaded)).isEqualTo(record);
    assertThat(PortfolioMapper.toRecord(reloaded).toBytes()).isEqualTo(record.toBytes());
    assertThat(portfolios.findByIdPortfolioIdOrderByIdAccountNumber("PORT0001")).hasSize(1);
  }

  @Test
  void positionRecordRoundTripsThroughPositionMasterTable() {
    PositionRecord record = SampleRecords.position();
    positions.save(PositionMasterMapper.toEntity(record));
    flushAndClear();

    PositionMaster reloaded =
        positions
            .findById(new PositionMasterKey("PORT0001", LocalDate.of(2024, 3, 15), "AAPL"))
            .orElseThrow();
    assertThat(reloaded.getQuantity()).isEqualTo(new BigDecimal("1500.2500"));
    assertThat(reloaded.getLastMaintTimestamp()).isEqualTo(NOW);
    assertThat(PositionMasterMapper.toRecord(reloaded).toBytes()).isEqualTo(record.toBytes());
    assertThat(
            positions.findByIdPortfolioIdAndIdPositionDateOrderByIdInvestmentId(
                "PORT0001", LocalDate.of(2024, 3, 15)))
        .hasSize(1);
  }

  @Test
  void transactionRecordRoundTripsThroughPortfolioTransactionTable() {
    TransactionRecord record = SampleRecords.transaction();
    transactions.save(PortfolioTransactionMapper.toEntity(record));
    flushAndClear();

    PortfolioTransactionKey key =
        new PortfolioTransactionKey(
            LocalDate.of(2024, 3, 15), LocalTime.of(14, 30, 45), "PORT0001", "000042");
    PortfolioTransaction reloaded = transactions.findById(key).orElseThrow();
    assertThat(reloaded.getPrice()).isEqualTo(new BigDecimal("175.1234"));
    assertThat(reloaded.getProcessTimestamp()).isNull();
    assertThat(PortfolioTransactionMapper.toRecord(reloaded).toBytes()).isEqualTo(record.toBytes());
    assertThat(
            transactions
                .findByIdPortfolioIdOrderByIdTransactionDateAscIdTransactionTimeAscIdSequenceNumberAsc(
                    "PORT0001"))
        .hasSize(1);
  }

  @Test
  void historyRecordRoundTripsThroughChangeHistoryTable() {
    HistoryRecord record = SampleRecords.history();
    history.save(ChangeHistoryMapper.toEntity(record));
    flushAndClear();

    ChangeHistory reloaded =
        history
            .findById(
                new ChangeHistoryKey(
                    "PORT0001", LocalDate.of(2024, 3, 15), LocalTime.of(14, 30, 45), "0001"))
            .orElseThrow();
    assertThat(reloaded.getAfterImage()).hasSize(400);
    assertThat(ChangeHistoryMapper.toRecord(reloaded).toBytes()).isEqualTo(record.toBytes());
    assertThat(
            history.findByIdPortfolioIdOrderByIdHistoryDateAscIdHistoryTimeAscIdSequenceNumberAsc(
                "PORT0001"))
        .hasSize(1);
  }

  @Test
  void auditRecordRoundTripsThroughAuditLogTable() {
    AuditRecord record = SampleRecords.audit();
    AuditLog saved = audits.save(AuditLogMapper.toEntity(record));
    flushAndClear();

    assertThat(saved.getAuditId()).isNotNull();
    AuditLog reloaded = audits.findById(saved.getAuditId()).orElseThrow();
    assertThat(AuditLogMapper.toRecord(reloaded).toBytes()).isEqualTo(record.toBytes());
    assertThat(audits.findByPortfolioIdOrderByAuditTimestampAscAuditIdAsc("PORT0001")).hasSize(1);
  }

  @Test
  void positionHistoryHostVariablesRoundTripThroughPoshist() {
    PositionHistoryRecord record = SampleRecords.positionHistory();
    positionHistory.save(PositionHistoryMapper.toEntity(record));
    flushAndClear();

    PositionHistory reloaded =
        positionHistory
            .findById(
                new PositionHistoryKey(
                    "ACC00001", "PORT000001", LocalDate.of(2024, 3, 15), LocalTime.of(14, 30, 45)))
            .orElseThrow();
    assertThat(reloaded.getQuantity()).isEqualTo(new BigDecimal("250.500"));
    assertThat(reloaded.getGainLoss()).isEqualTo(new BigDecimal("6283.82"));
    assertThat(PositionHistoryMapper.toRecord(reloaded).toBytes()).isEqualTo(record.toBytes());
    assertThat(
            positionHistory.findBySecurityIdAndIdTransactionDate(
                "US0378331005", LocalDate.of(2024, 3, 15)))
        .hasSize(1);
  }

  @Test
  void errorLogHostVariablesRoundTripThroughErrlogAndCleanupDeletesOldRows() {
    ErrorLogRecord record = SampleRecords.errorLog();
    errorLogs.save(ErrorLogMapper.toEntity(record));
    flushAndClear();

    ErrorLog reloaded = errorLogs.findById(new ErrorLogKey(NOW, "TRNVAL00")).orElseThrow();
    assertThat(reloaded.getSeverity()).isEqualTo(3);
    assertThat(ErrorLogMapper.toRecord(reloaded).toBytes()).isEqualTo(record.toBytes());

    assertThat(errorLogs.deleteProcessedBefore(LocalDate.of(2024, 3, 15))).isZero();
    assertThat(errorLogs.deleteProcessedBefore(LocalDate.of(2024, 3, 16))).isEqualTo(1);
    flushAndClear();
    assertThat(errorLogs.count()).isZero();
  }

  @Test
  void returnCodeLogPersistsToRtncodes() {
    ReturnCodeLogKey key = new ReturnCodeLogKey(NOW, "PORTUPDT");
    returnCodes.save(new ReturnCodeLog(key, 4, 8, "W", "Warning issued"));
    flushAndClear();

    ReturnCodeLog reloaded = returnCodes.findById(key).orElseThrow();
    assertThat(reloaded.getReturnCode()).isEqualTo(4);
    assertThat(reloaded.getHighestCode()).isEqualTo(8);
    assertThat(reloaded.getStatusCode()).isEqualTo("W");
    assertThat(reloaded.getMessageText()).isEqualTo("Warning issued");
  }

  @Test
  void db2PortfolioSchemaHonoursForeignKeysAndPrecision() {
    PortfolioMaster master =
        new PortfolioMaster(
            "PORT0001",
            "IN",
            "01",
            "CLIENT0001",
            "Growth fund",
            "USD",
            "M",
            "A",
            LocalDate.of(2024, 1, 1),
            null,
            NOW,
            "SYSADMIN");
    portfolioMasters.save(master);
    investmentPositions.save(
        new InvestmentPosition(
            new InvestmentPositionKey("PORT0001", "AAPL", LocalDate.of(2024, 3, 15)),
            new BigDecimal("12345678901234.5678"),
            new BigDecimal("1000.00"),
            new BigDecimal("2000.00"),
            "USD",
            NOW,
            "POSUPDT"));
    transactionHistory.save(
        new TransactionHistory(
            "20240315143045PORT01",
            "PORT0001",
            LocalDate.of(2024, 3, 15),
            LocalTime.of(14, 30, 45),
            "AAPL",
            "BU",
            new BigDecimal("100.0000"),
            new BigDecimal("175.1234"),
            new BigDecimal("17512.34"),
            "USD",
            "P",
            NOW,
            "PORTTRAN"));
    flushAndClear();

    assertThat(portfolioMasters.findById("PORT0001")).isPresent();
    InvestmentPosition position =
        investmentPositions
            .findById(new InvestmentPositionKey("PORT0001", "AAPL", LocalDate.of(2024, 3, 15)))
            .orElseThrow();
    assertThat(position.getQuantity()).isEqualTo(new BigDecimal("12345678901234.5678"));
    assertThat(
            transactionHistory.findByPortfolioIdOrderByTransactionDateAscTransactionTimeAsc(
                "PORT0001"))
        .singleElement()
        .extracting(TransactionHistory::getTransactionId)
        .isEqualTo("20240315143045PORT01");
  }
}
