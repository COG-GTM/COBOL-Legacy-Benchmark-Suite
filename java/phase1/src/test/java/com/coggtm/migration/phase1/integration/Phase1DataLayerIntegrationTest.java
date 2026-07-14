package com.coggtm.migration.phase1.integration;

import com.coggtm.migration.phase1.entity.*;
import com.coggtm.migration.phase1.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class Phase1DataLayerIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PortfolioMasterRepository portfolioMasterRepository;
    @Autowired
    private InvestmentPositionRepository investmentPositionRepository;
    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;
    @Autowired
    private PoshistRepository poshistRepository;
    @Autowired
    private ErrlogRepository errlogRepository;
    @Autowired
    private RtncodesRepository rtncodesRepository;
    @Autowired
    private VsamPortmstrRepository vsamPortmstrRepository;
    @Autowired
    private VsamPoshistRepository vsamPoshistRepository;
    @Autowired
    private VsamTranhistRepository vsamTranhistRepository;
    @Autowired
    private HistoryRecordRepository historyRecordRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final LocalDate TEST_DATE = LocalDate.of(2024, 3, 20);
    private static final LocalTime TEST_TIME = LocalTime.of(12, 0, 0);
    private static final LocalDateTime TEST_TIMESTAMP = LocalDateTime.of(2024, 3, 20, 12, 0, 0, 123456000);
    private static final String FILLER_50 = String.format("%-50s", "").substring(0, 50);
    private static final String IMAGE_400 = String.format("%-400s", "before").substring(0, 400);
    private static final String IMAGE_100 = String.format("%-100s", "before").substring(0, 100);

    @Test
    void roundTripShouldPersistAndRetrieveAllRecords() {
        // 1. portfolio_master (DB2)
        PortfolioMaster portfolio = new PortfolioMaster();
        portfolio.setPortfolioId("PORT0001");
        portfolio.setAccountType("IN");
        portfolio.setBranchId("NY");
        portfolio.setClientId("1234567890");
        portfolio.setPortfolioName("Growth Portfolio");
        portfolio.setCurrencyCode("USD");
        portfolio.setRiskLevel("M");
        portfolio.setStatus("A");
        portfolio.setOpenDate(TEST_DATE);
        portfolio.setCloseDate(null);
        portfolio.setLastMaintDate(TEST_TIMESTAMP);
        portfolio.setLastMaintUser("USER0000");
        PortfolioMaster savedPortfolio = portfolioMasterRepository.saveAndFlush(portfolio);
        entityManager.clear();
        PortfolioMaster foundPortfolio = portfolioMasterRepository.findById("PORT0001").orElseThrow();
        assertThat(foundPortfolio.getPortfolioId()).isEqualTo("PORT0001");
        assertThat(foundPortfolio.getPortfolioName()).isEqualTo("Growth Portfolio");
        assertThat(foundPortfolio.getOpenDate()).isEqualTo(TEST_DATE);
        assertThat(foundPortfolio.getCloseDate()).isNull();

        // 2. investment_positions (DB2)
        InvestmentPosition position = new InvestmentPosition();
        position.setPortfolioId("PORT0001");
        position.setInvestmentId("INV0000001");
        position.setPositionDate(TEST_DATE);
        position.setQuantity(new BigDecimal("100.0000"));
        position.setCostBasis(new BigDecimal("10000.00"));
        position.setMarketValue(new BigDecimal("11000.00"));
        position.setCurrencyCode("USD");
        position.setLastMaintDate(TEST_TIMESTAMP);
        position.setLastMaintUser("USER0000");
        investmentPositionRepository.saveAndFlush(position);
        entityManager.clear();
        InvestmentPosition foundPosition = investmentPositionRepository.findById(
                new InvestmentPosition.InvestmentPositionId("PORT0001", "INV0000001", TEST_DATE)).orElseThrow();
        assertThat(foundPosition.getQuantity()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(foundPosition.getCostBasis()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(foundPosition.getMarketValue()).isEqualByComparingTo(new BigDecimal("11000.00"));

        // 3. transaction_history (DB2)
        TransactionHistory tx = new TransactionHistory();
        tx.setTransactionId("20240320120012000001");
        tx.setPortfolioId("PORT0001");
        tx.setTransactionDate(TEST_DATE);
        tx.setTransactionTime(TEST_TIME);
        tx.setInvestmentId("INV0000001");
        tx.setTransactionType("BU");
        tx.setQuantity(new BigDecimal("10.0000"));
        tx.setPrice(new BigDecimal("100.0000"));
        tx.setAmount(new BigDecimal("1000.00"));
        tx.setCurrencyCode("USD");
        tx.setStatus("P");
        tx.setProcessDate(TEST_TIMESTAMP);
        tx.setProcessUser("USER0000");
        transactionHistoryRepository.saveAndFlush(tx);
        entityManager.clear();
        TransactionHistory foundTx = transactionHistoryRepository.findById("20240320120012000001").orElseThrow();
        assertThat(foundTx.getQuantity()).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(foundTx.getPrice()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(foundTx.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));

        // 4. poshist (DB2)
        Poshist poshist = new Poshist();
        poshist.setAccountNo("ACC00001");
        poshist.setPortfolioId("PORT000001");
        poshist.setTransDate(TEST_DATE);
        poshist.setTransTime(TEST_TIME);
        poshist.setTransType("BU");
        poshist.setSecurityId("SEC000000001");
        poshist.setQuantity(new BigDecimal("12345678.123"));
        poshist.setPrice(new BigDecimal("12345678.123"));
        poshist.setAmount(new BigDecimal("12345678.12"));
        poshist.setFees(new BigDecimal("0.00"));
        poshist.setTotalAmount(new BigDecimal("12345678.12"));
        poshist.setCostBasis(new BigDecimal("12345678.12"));
        poshist.setGainLoss(new BigDecimal("12345678.12"));
        poshist.setProcessDate(TEST_DATE);
        poshist.setProcessTime(TEST_TIME);
        poshist.setProgramId("HISTLD00");
        poshist.setUserId("USER0000");
        poshist.setAuditTimestamp(TEST_TIMESTAMP);
        poshistRepository.saveAndFlush(poshist);
        entityManager.clear();
        Poshist foundPoshist = poshistRepository.findById(
                new Poshist.PoshistId("ACC00001", "PORT000001", TEST_DATE, TEST_TIME)).orElseThrow();
        assertThat(foundPoshist.getQuantity()).isEqualByComparingTo(new BigDecimal("12345678.123"));
        assertThat(foundPoshist.getPrice()).isEqualByComparingTo(new BigDecimal("12345678.123"));
        assertThat(foundPoshist.getAmount()).isEqualByComparingTo(new BigDecimal("12345678.12"));
        assertThat(foundPoshist.getFees()).isEqualByComparingTo(new BigDecimal("0.00"));

        // 5. errlog (DB2)
        Errlog errlog = new Errlog();
        errlog.setErrorTimestamp(TEST_TIMESTAMP);
        errlog.setProgramId("PROG0000");
        errlog.setErrorType("A");
        errlog.setErrorSeverity(2);
        errlog.setErrorCode("E0000001");
        errlog.setErrorMessage("Error message");
        errlog.setProcessDate(TEST_DATE);
        errlog.setProcessTime(TEST_TIME);
        errlog.setUserId("USER0000");
        errlog.setAdditionalInfo("Additional info");
        errlogRepository.saveAndFlush(errlog);
        entityManager.clear();
        Errlog foundErrlog = errlogRepository.findById(new Errlog.ErrlogId(TEST_TIMESTAMP, "PROG0000")).orElseThrow();
        assertThat(foundErrlog.getErrorSeverity()).isEqualTo(2);
        assertThat(foundErrlog.getErrorCode()).isEqualTo("E0000001");

        // 6. rtncodes (DB2)
        Rtncodes rtncodes = new Rtncodes();
        rtncodes.setLogTimestamp(TEST_TIMESTAMP);
        rtncodes.setProgramId("PROG0000");
        rtncodes.setReturnCode(0);
        rtncodes.setHighestCode(0);
        rtncodes.setStatusCode("S");
        rtncodes.setMessageText("OK");
        rtncodesRepository.saveAndFlush(rtncodes);
        entityManager.clear();
        Rtncodes foundRtncodes = rtncodesRepository.findById(new Rtncodes.RtncodesId(TEST_TIMESTAMP, "PROG0000")).orElseThrow();
        assertThat(foundRtncodes.getReturnCode()).isEqualTo(0);
        assertThat(foundRtncodes.getStatusCode()).isEqualTo("S");

        // 7. vsam_portmstr (VSAM PORTMSTR / PORTFLIO copybook)
        VsamPortmstr vsamPortmstr = new VsamPortmstr();
        vsamPortmstr.setPortfolioId("PORT0001");
        vsamPortmstr.setAccountNo("1234567890");
        vsamPortmstr.setClientName("John Doe");
        vsamPortmstr.setClientType("I");
        vsamPortmstr.setCreateDate(TEST_DATE);
        vsamPortmstr.setLastMaintDate(TEST_DATE);
        vsamPortmstr.setStatus("A");
        vsamPortmstr.setTotalValue(new BigDecimal("1000000.00"));
        vsamPortmstr.setCashBalance(new BigDecimal("100000.00"));
        vsamPortmstr.setLastUser("USER0000");
        vsamPortmstr.setLastTransDate(TEST_DATE);
        vsamPortmstr.setFiller(FILLER_50);
        vsamPortmstrRepository.saveAndFlush(vsamPortmstr);
        entityManager.clear();
        VsamPortmstr foundVsamPortmstr = vsamPortmstrRepository.findById(
                new VsamPortmstr.VsamPortmstrId("PORT0001", "1234567890")).orElseThrow();
        assertThat(foundVsamPortmstr.getTotalValue()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(foundVsamPortmstr.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(foundVsamPortmstr.getClientType()).isEqualTo("I");

        // 8. vsam_poshist (VSAM POSHIST / POSREC copybook)
        VsamPoshist vsamPoshist = new VsamPoshist();
        vsamPoshist.setPortfolioId("PORT0001");
        vsamPoshist.setPositionDate(TEST_DATE);
        vsamPoshist.setInvestmentId("INV0000001");
        vsamPoshist.setQuantity(new BigDecimal("100.0000"));
        vsamPoshist.setCostBasis(new BigDecimal("10000.00"));
        vsamPoshist.setMarketValue(new BigDecimal("11000.00"));
        vsamPoshist.setCurrencyCode("USD");
        vsamPoshist.setStatus("A");
        vsamPoshist.setLastMaintDate(TEST_TIMESTAMP);
        vsamPoshist.setLastMaintUser("USER0000");
        vsamPoshist.setFiller(FILLER_50);
        vsamPoshistRepository.saveAndFlush(vsamPoshist);
        entityManager.clear();
        VsamPoshist foundVsamPoshist = vsamPoshistRepository.findById(
                new VsamPoshist.VsamPoshistId("PORT0001", TEST_DATE, "INV0000001")).orElseThrow();
        assertThat(foundVsamPoshist.getQuantity()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(foundVsamPoshist.getCostBasis()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(foundVsamPoshist.getMarketValue()).isEqualByComparingTo(new BigDecimal("11000.00"));

        // 9. vsam_tranhist (VSAM TRANHIST / TRNREC copybook)
        VsamTranhist vsamTranhist = new VsamTranhist();
        vsamTranhist.setTransactionId("20240320120012000001");
        vsamTranhist.setPortfolioId("PORT0001");
        vsamTranhist.setTransactionDate(TEST_DATE);
        vsamTranhist.setTransactionTime(TEST_TIME);
        vsamTranhist.setSequenceNumber("000001");
        vsamTranhist.setInvestmentId("INV0000001");
        vsamTranhist.setTransactionType("BU");
        vsamTranhist.setQuantity(new BigDecimal("10.0000"));
        vsamTranhist.setPrice(new BigDecimal("100.0000"));
        vsamTranhist.setAmount(new BigDecimal("1000.00"));
        vsamTranhist.setCurrencyCode("USD");
        vsamTranhist.setStatus("P");
        vsamTranhist.setProcessDate(TEST_TIMESTAMP);
        vsamTranhist.setProcessUser("USER0000");
        vsamTranhist.setFiller(FILLER_50);
        vsamTranhistRepository.saveAndFlush(vsamTranhist);
        entityManager.clear();
        VsamTranhist foundVsamTranhist = vsamTranhistRepository.findById("20240320120012000001").orElseThrow();
        assertThat(foundVsamTranhist.getQuantity()).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(foundVsamTranhist.getPrice()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(foundVsamTranhist.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));

        // 10. history_record (HISTREC copybook)
        HistoryRecord historyRecord = new HistoryRecord();
        historyRecord.setPortfolioId("PORT0001");
        historyRecord.setHistoryDate(TEST_DATE);
        historyRecord.setHistoryTime(TEST_TIME);
        historyRecord.setSequenceNo("0001");
        historyRecord.setRecordType("PT");
        historyRecord.setActionCode("A");
        historyRecord.setBeforeImage(IMAGE_400);
        historyRecord.setAfterImage(IMAGE_400);
        historyRecord.setReasonCode("R001");
        historyRecord.setProcessTimestamp(TEST_TIMESTAMP);
        historyRecord.setProcessUser("USER0000");
        historyRecord.setFiller(FILLER_50);
        historyRecordRepository.saveAndFlush(historyRecord);
        entityManager.clear();
        HistoryRecord foundHistoryRecord = historyRecordRepository.findById(
                new HistoryRecord.HistoryRecordId("PORT0001", TEST_DATE, TEST_TIME, "0001")).orElseThrow();
        assertThat(foundHistoryRecord.getRecordType()).isEqualTo("PT");
        assertThat(foundHistoryRecord.getActionCode()).isEqualTo("A");
        assertThat(foundHistoryRecord.getBeforeImage()).isEqualTo(IMAGE_400);

        // 11. audit_log (AUDITLOG copybook)
        AuditLog auditLog = new AuditLog();
        auditLog.setLogTimestamp(TEST_TIMESTAMP);
        auditLog.setSystemId("SYSTEM01");
        auditLog.setUserId("USER0001");
        auditLog.setProgram("PROG0000");
        auditLog.setTerminal("TERM0001");
        auditLog.setType("TRAN");
        auditLog.setAction("CREATE");
        auditLog.setStatus("SUCC");
        auditLog.setPortfolioId("PORT0001");
        auditLog.setAccountNo("1234567890");
        auditLog.setBeforeImage(IMAGE_100);
        auditLog.setAfterImage(IMAGE_100);
        auditLog.setMessage(IMAGE_100);
        AuditLog savedAuditLog = auditLogRepository.saveAndFlush(auditLog);
        entityManager.clear();
        AuditLog foundAuditLog = auditLogRepository.findById(savedAuditLog.getId()).orElseThrow();
        assertThat(foundAuditLog.getSystemId()).isEqualTo("SYSTEM01");
        assertThat(foundAuditLog.getUserId()).isEqualTo("USER0001");
        assertThat(foundAuditLog.getType()).isEqualTo("TRAN");
    }
}
