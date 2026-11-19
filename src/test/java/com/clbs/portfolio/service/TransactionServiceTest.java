package com.clbs.portfolio.service;

import com.clbs.portfolio.DatabaseCleaner;
import com.clbs.portfolio.IntegrationTest;
import com.clbs.portfolio.domain.AuditRecord;
import com.clbs.portfolio.domain.PortfolioKey;
import com.clbs.portfolio.domain.PortfolioTransaction;
import com.clbs.portfolio.domain.Position;
import com.clbs.portfolio.exception.BusinessRuleException;
import com.clbs.portfolio.exception.InvalidRequestException;
import com.clbs.portfolio.repository.PositionHistoryRepository;
import com.clbs.portfolio.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class TransactionServiceTest {

    @Autowired
    private TransactionService service;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PositionRepository positions;

    @Autowired
    private PositionHistoryRepository history;

    @Autowired
    private AuditService auditService;

    @Autowired
    private DatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.clean();
        portfolioService.create(PortfolioServiceTest.portfolio(new PortfolioKey("PORT0001", "1234567890"), "GROWTH", "A"));
    }

    @Test
    void buyCreatesPositionAndHistory() {
        PortfolioTransaction trn = service.process(cmd("BU", "100", "125.00", "12500.00"));

        assertThat(trn.getStatus()).isEqualTo(PortfolioTransaction.STATUS_DONE);
        Position position = positions.findAll().getFirst();
        assertThat(position.getQuantity()).isEqualByComparingTo("100");
        assertThat(position.getCostBasis()).isEqualByComparingTo("12500.00");
        assertThat(history.findTop10ByAccountNoOrderByTransDateDescTransTimeDescIdDesc("1234567890")).hasSize(1);
        assertThat(auditService.find("PORT0001")).extracting(AuditRecord::getAction, AuditRecord::getStatus)
                .contains(org.assertj.core.groups.Tuple.tuple("CREATE", "SUCC"));
    }

    @Test
    void sellReducesUnitsAndCostAndClosesEmptyPosition() {
        service.process(cmd("BU", "100", "125.00", "12500.00"));
        service.process(cmd("SL", "40", "130.00", "5000.00"));
        Position position = positions.findAll().getFirst();
        assertThat(position.getQuantity()).isEqualByComparingTo("60");
        assertThat(position.getCostBasis()).isEqualByComparingTo("7500.00");

        service.process(cmd("SL", "60", "130.00", "7500.00"));
        assertThat(positions.findAll().getFirst().getStatus()).isEqualTo(Position.STATUS_CLOSED);
    }

    @Test
    void sellWithInsufficientUnitsIsStoredAsFailed() {
        service.process(cmd("BU", "10", "125.00", "1250.00"));
        assertThatThrownBy(() -> service.process(cmd("SL", "11", "125.00", "1375.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Insufficient units for sale");

        List<PortfolioTransaction> all = service.findByPortfolio("PORT0001");
        assertThat(all).extracting(PortfolioTransaction::getStatus).containsExactlyInAnyOrder("D", "F");
        assertThat(positions.findAll().getFirst().getQuantity()).isEqualByComparingTo("10");
        assertThat(auditService.find("PORT0001")).extracting(AuditRecord::getStatus).contains("FAIL");
    }

    @Test
    void transferIsNotImplemented() {
        assertThatThrownBy(() -> service.process(cmd("TR", "10", "0", "0")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Transfer processing not implemented");
    }

    @Test
    void feeReducesCostBasis() {
        service.process(cmd("BU", "10", "100.00", "1000.00"));
        service.process(cmd("FE", "1", "15.00", "15.00"));
        assertThat(positions.findAll().getFirst().getCostBasis()).isEqualByComparingTo("985.00");
    }

    @ParameterizedTest
    @CsvSource({
            "'',BU,1,1,1,Portfolio ID is required",
            "PORT9999,BU,1,1,1,Invalid Portfolio ID: PORT9999",
            "PORT0001,XX,1,1,1,Invalid Transaction Type: XX",
            "PORT0001,BU,0,1,1,Quantity must be greater than zero",
            "PORT0001,SL,1,0,1,Price must be greater than zero",
            "PORT0001,FE,1,1,-1,Amount must be greater than zero"
    })
    void validationMessagesFollowCobolOrder(String portfolioId, String type, String qty, String price,
                                            String amount, String message) {
        TransactionCommand cmd = new TransactionCommand(portfolioId, "IBM0000001", type, new BigDecimal(qty),
                new BigDecimal(price), new BigDecimal(amount), "USD");
        assertThatThrownBy(() -> service.process(cmd))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(message);
    }

    @Test
    void transferSkipsPriceAndAmountChecks() {
        TransactionCommand cmd = new TransactionCommand("PORT0001", "IBM0000001", "TR", BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO, "USD");
        assertThatThrownBy(() -> service.process(cmd)).isInstanceOf(BusinessRuleException.class);
    }

    private static TransactionCommand cmd(String type, String qty, String price, String amount) {
        return new TransactionCommand("PORT0001", "IBM0000001", type, new BigDecimal(qty), new BigDecimal(price),
                new BigDecimal(amount), "USD");
    }
}
