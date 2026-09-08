package com.clbs.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.common.AuditProcessor;
import com.clbs.domain.PortfolioRecord;
import com.clbs.domain.ReturnCode;
import com.clbs.store.DatasetCatalog;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortfolioMasterServiceTest {

    private DatasetCatalog datasets;
    private PortfolioMasterService service;

    @BeforeEach
    void setUp() {
        datasets = new DatasetCatalog();
        datasets.portfolioFile().open();
        service = new PortfolioMasterService(datasets, new AuditProcessor(datasets));
    }

    private static PortfolioRecord record(String id, String account, String name, char status) {
        PortfolioRecord record = new PortfolioRecord();
        record.setPortId(id);
        record.setAccountNo(account);
        record.setClientName(name);
        record.setStatus(status);
        record.setTotalValue(new BigDecimal("1000.00"));
        return record;
    }

    @Test
    void createsThenReadsPortfolio() {
        assertThat(service.create(record("PORT00010", "12345678", "GROWTH", 'A')).ok()).isTrue();

        PortfolioMasterService.Response read = service.read("PORT00010", "12345678");
        assertThat(read.ok()).isTrue();
        assertThat(read.record().getClientName()).isEqualTo("GROWTH");
    }

    @Test
    void rejectsDuplicateCreate() {
        service.create(record("PORT00010", "12345678", "GROWTH", 'A'));

        PortfolioMasterService.Response second =
                service.create(record("PORT00010", "12345678", "GROWTH", 'A'));
        assertThat(second.returnCode()).isEqualTo(ReturnCode.ERROR);
        assertThat(second.errorText()).isEqualTo(PortfolioMasterService.ERR_DUPLICATE);
    }

    @Test
    void rejectsMissingNameAndBadStatus() {
        assertThat(service.create(record("PORT00010", "12345678", "  ", 'A')).errorText())
                .isEqualTo(PortfolioMasterService.ERR_NAME_REQUIRED);
        assertThat(service.create(record("PORT00020", "12345678", "GROWTH", 'S')).errorText())
                .isEqualTo(PortfolioMasterService.ERR_INVALID_STATUS);
    }

    @Test
    void updateRequiresExistingRecord() {
        PortfolioMasterService.Response response =
                service.update(record("PORT00090", "12345678", "GROWTH", 'A'));
        assertThat(response.errorText()).isEqualTo(PortfolioMasterService.ERR_NOT_FOUND_UPDATE);
    }

    @Test
    void updatesAndDeletesExistingRecord() {
        service.create(record("PORT00010", "12345678", "GROWTH", 'A'));

        PortfolioRecord updated = record("PORT00010", "12345678", "INCOME", 'I');
        assertThat(service.update(updated).ok()).isTrue();
        assertThat(service.read("PORT00010", "12345678").record().getClientName())
                .isEqualTo("INCOME");

        assertThat(service.delete("PORT00010", "12345678").ok()).isTrue();
        assertThat(service.read("PORT00010", "12345678").errorText())
                .isEqualTo(PortfolioMasterService.ERR_NOT_FOUND);
    }

    @Test
    void unknownCommandIsRejected() {
        PortfolioMasterService.Response response =
                service.execute('X', record("PORT00010", "12345678", "GROWTH", 'A'));
        assertThat(response.errorText()).isEqualTo(PortfolioMasterService.ERR_INVALID_COMMAND);
    }

    @Test
    void totalValueKeepsTwoDecimalScale() {
        PortfolioRecord record = record("PORT00010", "12345678", "GROWTH", 'A');
        record.setTotalValue(new BigDecimal("1234.5"));
        service.create(record);

        assertThat(service.read("PORT00010", "12345678").record().getTotalValue())
                .isEqualByComparingTo("1234.50")
                .satisfies(value -> assertThat(value.scale()).isEqualTo(2));
    }
}
