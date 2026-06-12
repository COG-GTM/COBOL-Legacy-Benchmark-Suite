package com.benchmark.portfolio.common.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.benchmark.portfolio.common.entity.HistoryRecord;
import com.benchmark.portfolio.common.entity.HistoryRecordId;
import com.benchmark.portfolio.common.entity.PortfolioMaster;
import com.benchmark.portfolio.common.entity.PortfolioMasterId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

class HistoryRecordRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private HistoryRecordRepository repository;

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

    private HistoryRecord history(
            String portfolioId, LocalDate date, LocalTime time, String seqNo) {
        HistoryRecord history = new HistoryRecord();
        history.setId(new HistoryRecordId(portfolioId, date, time, seqNo));
        history.setRecordType("TR");
        history.setActionCode("A");
        history.setAfterImage("AFTER-IMAGE");
        return history;
    }

    @BeforeEach
    void seed() {
        seedMaster("PORT0001", "ACCT000001");
        seedMaster("PORT0002", "ACCT000002");
        seedMaster("PORT0009", "ACCT000009");
        masterRepository.flush();
        repository.saveAll(List.of(
                history("PORT0001", LocalDate.of(2024, 6, 1), LocalTime.of(9, 0), "0001"),
                history("PORT0001", LocalDate.of(2024, 6, 1), LocalTime.of(10, 0), "0002"),
                history("PORT0001", LocalDate.of(2024, 6, 2), LocalTime.of(9, 0), "0003"),
                history("PORT0002", LocalDate.of(2024, 6, 1), LocalTime.of(9, 0), "0004")));
        repository.flush();
    }

    @Test
    void crudRoundTrip() {
        HistoryRecordId id = new HistoryRecordId(
                "PORT0009", LocalDate.of(2024, 7, 1), LocalTime.of(8, 0), "0009");
        repository.saveAndFlush(
                history("PORT0009", LocalDate.of(2024, 7, 1), LocalTime.of(8, 0), "0009"));

        HistoryRecord loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getRecordType()).isEqualTo("TR");

        loaded.setReasonCode("RC01");
        repository.saveAndFlush(loaded);
        assertThat(repository.findById(id).orElseThrow().getReasonCode()).isEqualTo("RC01");

        repository.deleteById(id);
        repository.flush();
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void sequentialReadInKeyOrder() {
        List<HistoryRecord> all = repository
                .findAllByOrderByIdPortfolioIdAscIdHistDateAscIdHistTimeAscIdSeqNoAsc();
        assertThat(all).extracting(h -> h.getId().getSeqNo())
                .containsExactly("0001", "0002", "0003", "0004");
    }

    @Test
    void historyByPortfolioNewestFirstWithPaging() {
        Page<HistoryRecord> page = repository
                .findByIdPortfolioIdOrderByIdHistDateDescIdHistTimeDescIdSeqNoDesc(
                        "PORT0001", PageRequest.of(0, 2));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(h -> h.getId().getSeqNo())
                .containsExactly("0003", "0002");
    }

    @Test
    void rangeScanWithinPortfolioFromDate() {
        List<HistoryRecord> result = repository
                .findByIdPortfolioIdAndIdHistDateGreaterThanEqualOrderByIdHistDateAscIdHistTimeAscIdSeqNoAsc(
                        "PORT0001", LocalDate.of(2024, 6, 2));
        assertThat(result).extracting(h -> h.getId().getSeqNo()).containsExactly("0003");
    }
}
