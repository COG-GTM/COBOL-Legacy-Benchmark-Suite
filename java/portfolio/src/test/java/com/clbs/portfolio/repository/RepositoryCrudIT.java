package com.clbs.portfolio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.portfolio.domain.HistoryKey;
import com.clbs.portfolio.domain.HistoryRecord;
import com.clbs.portfolio.domain.PortfolioKey;
import com.clbs.portfolio.domain.PortfolioMaster;
import com.clbs.portfolio.domain.PositionKey;
import com.clbs.portfolio.domain.PositionRecord;
import com.clbs.portfolio.domain.TransactionKey;
import com.clbs.portfolio.domain.TransactionRecord;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * CRUD + VSAM-style key-access integration tests against H2 + Flyway
 * (Phase 0, task 0.5 AC).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryCrudIT {

    @Autowired
    private PortfolioMasterRepository portfolioRepository;
    @Autowired
    private TransactionRecordRepository transactionRepository;
    @Autowired
    private PositionRecordRepository positionRepository;
    @Autowired
    private HistoryRecordRepository historyRepository;

    @Test
    void portfolioMasterCrudAndKeyAccess() {
        PortfolioMaster pm = new PortfolioMaster();
        pm.setKey(new PortfolioKey("PORT0001", "ACCT000001"));
        pm.setClientName("GROWTH PORTFOLIO");
        pm.setClientType("I");
        pm.setCreateDate(20240320);
        pm.setLastMaint(20240321);
        pm.setStatus("A");
        pm.setTotalValue(new BigDecimal("12345678.99"));
        pm.setCashBalance(new BigDecimal("1000000.00"));
        pm.setLastUser("TSTGEN00");
        pm.setLastTrans(20240321);
        portfolioRepository.save(pm);

        PortfolioMaster found = portfolioRepository
                .findById(new PortfolioKey("PORT0001", "ACCT000001")).orElseThrow();
        assertThat(found.getClientName()).isEqualTo("GROWTH PORTFOLIO");
        assertThat(found.getTotalValue()).isEqualByComparingTo("12345678.99");

        assertThat(portfolioRepository.findByKeyPortId("PORT0001")).hasSize(1);
        assertThat(portfolioRepository.findByClientTypeAndStatus("I", "A")).hasSize(1);
        assertThat(portfolioRepository
                .findByKeyPortIdGreaterThanEqualOrderByKeyPortIdAscKeyAccountNoAsc("PORT0000"))
                .hasSize(1);
    }

    @Test
    void transactionBrowseByPortfolioAndDateRange() {
        transactionRepository.save(transaction("20240320", "153001", "PORT0002", "000001"));
        transactionRepository.save(transaction("20240321", "153002", "PORT0002", "000002"));

        List<TransactionRecord> all = transactionRepository
                .findByKeyPortfolioIdOrderByKeyTrnDateAscKeyTrnTimeAscKeySequenceNoAsc("PORT0002");
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getKey().getTrnDate()).isEqualTo("20240320");

        List<TransactionRecord> ranged = transactionRepository
                .findByKeyPortfolioIdAndKeyTrnDateBetweenOrderByKeyTrnDateAscKeyTrnTimeAsc(
                        "PORT0002", "20240320", "20240320");
        assertThat(ranged).hasSize(1);
    }

    @Test
    void positionAndHistoryCrud() {
        PositionRecord pos = new PositionRecord();
        pos.setKey(new PositionKey("PORT0003", "20240320", "AAPL000001"));
        pos.setQuantity(new BigDecimal("100.0000"));
        pos.setCostBasis(new BigDecimal("15000.00"));
        pos.setMarketValue(new BigDecimal("17500.00"));
        pos.setCurrency("USD");
        pos.setStatus("A");
        pos.setLastMaintDate("2024-03-20-15.30.45.123456");
        pos.setLastMaintUser("TSTGEN00");
        positionRepository.save(pos);

        assertThat(positionRepository
                .findByKeyPortfolioIdAndKeyPosDateOrderByKeyInvestmentIdAsc("PORT0003", "20240320"))
                .hasSize(1);

        HistoryRecord hist = new HistoryRecord();
        hist.setKey(new HistoryKey("PORT0003", "20240320", "153045", "0001"));
        hist.setRecordType("PS");
        hist.setActionCode("A");
        hist.setReasonCode("NEW0");
        hist.setProcessDate("2024-03-20-15.30.45.123456");
        hist.setProcessUser("TSTGEN00");
        historyRepository.save(hist);

        assertThat(historyRepository.findByRecordType("PS")).hasSize(1);
        assertThat(historyRepository
                .findByKeyPortfolioIdOrderByKeyHistDateAscKeyHistTimeAscKeySeqNoAsc("PORT0003"))
                .hasSize(1);
    }

    private static TransactionRecord transaction(String date, String time, String portfolioId, String seq) {
        TransactionRecord t = new TransactionRecord();
        t.setKey(new TransactionKey(date, time, portfolioId, seq));
        t.setInvestmentId("AAPL000001");
        t.setType("BU");
        t.setQuantity(new BigDecimal("100.0000"));
        t.setPrice(new BigDecimal("10.5000"));
        t.setAmount(new BigDecimal("1050.00"));
        t.setCurrency("USD");
        t.setStatus("D");
        t.setProcessDate("2024-03-20-15.30.45.123456");
        t.setProcessUser("TSTGEN00");
        return t;
    }
}
