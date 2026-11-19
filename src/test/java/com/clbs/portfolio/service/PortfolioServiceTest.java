package com.clbs.portfolio.service;

import com.clbs.portfolio.DatabaseCleaner;
import com.clbs.portfolio.IntegrationTest;
import com.clbs.portfolio.domain.AuditRecord;
import com.clbs.portfolio.domain.Portfolio;
import com.clbs.portfolio.domain.PortfolioKey;
import com.clbs.portfolio.exception.DuplicateException;
import com.clbs.portfolio.exception.InvalidRequestException;
import com.clbs.portfolio.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class PortfolioServiceTest {

    private static final PortfolioKey KEY = new PortfolioKey("PORT0001", "1234567890");

    @Autowired
    private PortfolioService service;

    @Autowired
    private AuditService auditService;

    @Autowired
    private DatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.clean();
    }

    @Test
    void createSetsCreateAndMaintenanceDatesToToday() {
        Portfolio saved = service.create(portfolio(KEY, "GROWTH FUND", "A"));
        assertThat(saved.getCreateDate()).isEqualTo(LocalDate.of(2024, 3, 20));
        assertThat(saved.getLastMaintDate()).isEqualTo(LocalDate.of(2024, 3, 20));
        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void createRejectsMissingIdNameOrInactiveStatus() {
        assertThatThrownBy(() -> service.create(portfolio(new PortfolioKey(" ", "1"), "X", "A")))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.create(portfolio(KEY, "  ", "A")))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.create(portfolio(KEY, "NAME", "C")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("New portfolios must have status 'A'");
    }

    @Test
    void createRejectsDuplicateKey() {
        service.create(portfolio(KEY, "FIRST", "A"));
        assertThatThrownBy(() -> service.create(portfolio(KEY, "SECOND", "A")))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Duplicate record: PORT0001");
    }

    @Test
    void updateAppliesStatusNameAndValueActions() {
        service.create(portfolio(KEY, "GROWTH FUND", "A"));

        assertThat(service.update(KEY, 'S', "SUSPENDED").getStatus()).isEqualTo("S");
        assertThat(service.update(KEY, 'N', "A NAME LONGER THAN THIRTY CHARACTERS IN TOTAL").getClientName())
                .isEqualTo("A NAME LONGER THAN THIRTY CHAR");
        assertThat(service.update(KEY, 'V', "1050.759").getTotalValue()).isEqualByComparingTo("1050.75");
        assertThat(service.update(KEY, 'X', "ignored").getClientName()).isEqualTo("A NAME LONGER THAN THIRTY CHAR");
    }

    @Test
    void updateOfMissingRecordFails() {
        assertThatThrownBy(() -> service.update(KEY, 'N', "X"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Record not found: PORT00011234567890");
    }

    @Test
    void updateWithNonNumericValueFails() {
        service.create(portfolio(KEY, "GROWTH FUND", "A"));
        assertThatThrownBy(() -> service.update(KEY, 'V', "ABC")).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteRemovesRecordAndWritesAudit() {
        service.create(portfolio(KEY, "GROWTH FUND", "A"));
        service.delete(KEY, DeleteReason.CLOSED.code());

        assertThat(service.findAll()).isEmpty();
        List<AuditRecord> audit = auditService.find("PORT0001");
        assertThat(audit).extracting(AuditRecord::getAction).containsExactly("CREATE", "DELETE");
        assertThat(audit.get(1).getReasonCode()).isEqualTo("01");
        assertThat(audit.get(1).getMessage()).isEqualTo("Portfolio deleted, status A");
    }

    @Test
    void deleteOfMissingRecordFails() {
        assertThatThrownBy(() -> service.delete(KEY, "03")).isInstanceOf(NotFoundException.class);
    }

    static Portfolio portfolio(PortfolioKey key, String name, String status) {
        Portfolio p = new Portfolio(key);
        p.setClientName(name);
        p.setClientType("I");
        p.setStatus(status);
        p.setTotalValue(new BigDecimal("100.00"));
        p.setCashBalance(new BigDecimal("10.00"));
        return p;
    }
}
