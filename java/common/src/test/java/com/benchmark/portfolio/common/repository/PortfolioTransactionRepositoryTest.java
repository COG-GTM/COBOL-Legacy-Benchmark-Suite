package com.benchmark.portfolio.common.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.benchmark.portfolio.common.entity.PortfolioMaster;
import com.benchmark.portfolio.common.entity.PortfolioMasterId;
import com.benchmark.portfolio.common.entity.PortfolioTransaction;
import com.benchmark.portfolio.common.entity.PortfolioTransactionId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

class PortfolioTransactionRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private PortfolioTransactionRepository repository;

    @Autowired
    private PortfolioMasterRepository masterRepository;

    private void seedMaster(String portfolioId, String accountNo) {
        PortfolioMaster master = new PortfolioMaster();
        master.setId(new PortfolioMasterId(portfolioId, accountNo));
        master.setClientName("CLIENT " + portfolioId);
        master.setClientType("I");
        master.setCreateDate(LocalDate.of(2024, 1, 15));
        master.setStatus("A");
        master.setTotalValue(BigDecimal.ZERO);
        master.setCashBalance(BigDecimal.ZERO);
        masterRepository.save(master);
    }

    private PortfolioTransaction transaction(
            LocalDate date, LocalTime time, String portfolioId, String seqNo) {
        PortfolioTransaction transaction = new PortfolioTransaction();
        transaction.setId(new PortfolioTransactionId(date, time, portfolioId, seqNo));
        transaction.setInvestmentId("INVEST0001");
        transaction.setTransType("BU");
        transaction.setQuantity(new BigDecimal("100.0000"));
        transaction.setPrice(new BigDecimal("10.5000"));
        transaction.setAmount(new BigDecimal("1050.00"));
        transaction.setCurrencyCode("USD");
        transaction.setStatus("D");
        return transaction;
    }

    @BeforeEach
    void seed() {
        seedMaster("PORT0001", "ACCT000001");
        seedMaster("PORT0002", "ACCT000002");
        seedMaster("PORT0009", "ACCT000009");
        masterRepository.flush();
        repository.saveAll(List.of(
                transaction(LocalDate.of(2024, 6, 1), LocalTime.of(9, 0), "PORT0001", "000001"),
                transaction(LocalDate.of(2024, 6, 1), LocalTime.of(10, 0), "PORT0001", "000002"),
                transaction(LocalDate.of(2024, 6, 2), LocalTime.of(9, 30), "PORT0001", "000003"),
                transaction(LocalDate.of(2024, 6, 2), LocalTime.of(9, 30), "PORT0002", "000004"),
                transaction(LocalDate.of(2024, 6, 3), LocalTime.of(14, 0), "PORT0002", "000005")));
        repository.flush();
    }

    @Test
    void crudRoundTrip() {
        PortfolioTransactionId id = new PortfolioTransactionId(
                LocalDate.of(2024, 7, 1), LocalTime.of(11, 0), "PORT0009", "000009");
        repository.saveAndFlush(
                transaction(LocalDate.of(2024, 7, 1), LocalTime.of(11, 0), "PORT0009", "000009"));

        PortfolioTransaction loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getAmount()).isEqualByComparingTo("1050.00");

        loaded.setStatus("R");
        repository.saveAndFlush(loaded);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo("R");

        repository.deleteById(id);
        repository.flush();
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void historyByPortfolioNewestFirstWithPaging() {
        Page<PortfolioTransaction> firstPage = repository
                .findByIdPortfolioIdOrderByIdTransDateDescIdTransTimeDescIdSequenceNoDesc(
                        "PORT0001", PageRequest.of(0, 2));
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(t -> t.getId().getSequenceNo())
                .containsExactly("000003", "000002");

        Page<PortfolioTransaction> secondPage = repository
                .findByIdPortfolioIdOrderByIdTransDateDescIdTransTimeDescIdSequenceNoDesc(
                        "PORT0001", PageRequest.of(1, 2));
        assertThat(secondPage.getContent()).extracting(t -> t.getId().getSequenceNo())
                .containsExactly("000001");
    }

    @Test
    void sequentialReadInKeyOrder() {
        List<PortfolioTransaction> all = repository
                .findAllByOrderByIdTransDateAscIdTransTimeAscIdPortfolioIdAscIdSequenceNoAsc();
        assertThat(all).extracting(t -> t.getId().getSequenceNo())
                .containsExactly("000001", "000002", "000003", "000004", "000005");
    }

    @Test
    void rangeScanFromCheckpointDate() {
        List<PortfolioTransaction> result = repository
                .findByIdTransDateGreaterThanEqualOrderByIdTransDateAscIdTransTimeAscIdPortfolioIdAscIdSequenceNoAsc(
                        LocalDate.of(2024, 6, 2));
        assertThat(result).extracting(t -> t.getId().getSequenceNo())
                .containsExactly("000003", "000004", "000005");
    }
}
