package com.portfolio.domain.model;

import com.portfolio.domain.enums.PortfolioStatus;
import com.portfolio.domain.enums.TransactionStatus;
import com.portfolio.domain.enums.TransactionType;
import com.portfolio.domain.repository.TransactionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testPortfolio = new Portfolio();
        testPortfolio.setPortfolioId("PORT0001");
        testPortfolio.setAccountType("IN");
        testPortfolio.setBranchId("01");
        testPortfolio.setClientId("CLIENT0001");
        testPortfolio.setPortfolioName("Test Portfolio");
        testPortfolio.setCurrencyCode("USD");
        testPortfolio.setRiskLevel("M");
        testPortfolio.setStatus(PortfolioStatus.ACTIVE);
        testPortfolio.setOpenDate(LocalDate.of(2024, 1, 15));
        testPortfolio.setLastMaintDate(LocalDateTime.now());
        testPortfolio.setLastMaintUser("ADMIN001");
        entityManager.persistAndFlush(testPortfolio);
    }

    @Test
    void shouldPersistAndReadTransaction() {
        TransactionRecord txn = createTransaction("TXN00000000000001", LocalDate.of(2024, 3, 15),
                TransactionType.BUY, TransactionStatus.DONE);
        entityManager.persistAndFlush(txn);
        entityManager.clear();

        Optional<TransactionRecord> found = transactionRecordRepository.findById("TXN00000000000001");
        assertThat(found).isPresent();
        TransactionRecord t = found.get();
        assertThat(t.getTransactionType()).isEqualTo(TransactionType.BUY);
        assertThat(t.getStatus()).isEqualTo(TransactionStatus.DONE);
        assertThat(t.getQuantity()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(t.getPrice()).isEqualByComparingTo(new BigDecimal("50.2500"));
    }

    @Test
    void shouldStoreTransactionTypeAsCobolCode() {
        TransactionRecord txn = createTransaction("TXN00000000000002", LocalDate.of(2024, 3, 15),
                TransactionType.SELL, TransactionStatus.PENDING);
        entityManager.persistAndFlush(txn);
        entityManager.clear();

        Object typeValue = entityManager.getEntityManager()
                .createNativeQuery("SELECT transaction_type FROM transaction_history WHERE transaction_id = 'TXN00000000000002'")
                .getSingleResult();
        assertThat(typeValue.toString()).isEqualTo("SL");

        Object statusValue = entityManager.getEntityManager()
                .createNativeQuery("SELECT status FROM transaction_history WHERE transaction_id = 'TXN00000000000002'")
                .getSingleResult();
        assertThat(statusValue.toString()).isEqualTo("P");
    }

    @Test
    void shouldFindByPortfolioIdAndDateRange() {
        entityManager.persistAndFlush(createTransaction("TXN00000000000010",
                LocalDate.of(2024, 3, 1), TransactionType.BUY, TransactionStatus.DONE));
        entityManager.persistAndFlush(createTransaction("TXN00000000000011",
                LocalDate.of(2024, 3, 15), TransactionType.SELL, TransactionStatus.DONE));
        entityManager.persistAndFlush(createTransaction("TXN00000000000012",
                LocalDate.of(2024, 4, 1), TransactionType.FEE, TransactionStatus.DONE));
        entityManager.clear();

        List<TransactionRecord> marchTxns = transactionRecordRepository
                .findByPortfolioIdAndTransactionDateBetween("PORT0001",
                        LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31));
        assertThat(marchTxns).hasSize(2);
    }

    private TransactionRecord createTransaction(String id, LocalDate date,
            TransactionType type, TransactionStatus status) {
        TransactionRecord txn = new TransactionRecord();
        txn.setTransactionId(id);
        txn.setPortfolioId("PORT0001");
        txn.setTransactionDate(date);
        txn.setTransactionTime(LocalTime.of(10, 30, 0));
        txn.setInvestmentId("INV0000001");
        txn.setTransactionType(type);
        txn.setQuantity(new BigDecimal("100.0000"));
        txn.setPrice(new BigDecimal("50.2500"));
        txn.setAmount(new BigDecimal("5025.00"));
        txn.setCurrencyCode("USD");
        txn.setStatus(status);
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("SYSTEM01");
        return txn;
    }
}
