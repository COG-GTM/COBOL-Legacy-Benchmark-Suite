package com.clbs.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.common.AuditProcessor;
import com.clbs.common.ErrorProcessor;
import com.clbs.common.ProgramResult;
import com.clbs.db2.ErrorLogRepository;
import com.clbs.domain.PortfolioRecord;
import com.clbs.domain.ReturnCode;
import com.clbs.domain.TransactionRecord;
import com.clbs.store.DatasetCatalog;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortfolioTransactionProgramTest {

    private DatasetCatalog datasets;
    private PortfolioTransactionProgram program;

    @BeforeEach
    void setUp() {
        datasets = new DatasetCatalog();
        datasets.portfolioFile().open();
        program = new PortfolioTransactionProgram(datasets, new AuditProcessor(datasets),
                new ErrorProcessor(org.mockito.Mockito.mock(ErrorLogRepository.class)));

        PortfolioRecord portfolio = new PortfolioRecord();
        portfolio.setPortId("PORT0001");
        portfolio.setAccountNo("12345678");
        portfolio.setClientName("GROWTH");
        portfolio.setStatus('A');
        portfolio.setTotalUnits(new BigDecimal("100.0000"));
        portfolio.setTotalCost(new BigDecimal("10000.00"));
        datasets.portfolioFile().write(portfolio);
    }

    private static TransactionRecord transaction(String type, String quantity, String price,
            String amount) {
        TransactionRecord record = new TransactionRecord();
        record.setPortfolioId("PORT0001");
        record.setAccountNo("12345678");
        record.setSequenceNo("00000001");
        record.setType(type);
        record.setInvestmentId("IBM0000001");
        record.setQuantity(new BigDecimal(quantity));
        record.setPrice(new BigDecimal(price));
        record.setAmount(new BigDecimal(amount));
        return record;
    }

    private PortfolioRecord reload() {
        return datasets.portfolioFile().all().get(0);
    }

    @Test
    void buyIncreasesUnitsAndCost() {
        assertThat(program.apply(transaction("BU", "10.0000", "50.00", "500.00"))).isNull();

        assertThat(reload().getTotalUnits()).isEqualByComparingTo("110.0000");
        assertThat(reload().getTotalCost()).isEqualByComparingTo("10500.00");
    }

    @Test
    void sellDecreasesUnitsAndCost() {
        assertThat(program.apply(transaction("SL", "10.0000", "50.00", "500.00"))).isNull();

        assertThat(reload().getTotalUnits()).isEqualByComparingTo("90.0000");
        assertThat(reload().getTotalCost()).isEqualByComparingTo("9500.00");
    }

    @Test
    void sellIsRejectedWhenUnitsAreInsufficient() {
        assertThat(program.apply(transaction("SL", "1000.0000", "50.00", "500.00")))
                .isEqualTo(PortfolioTransactionProgram.ERR_INSUFFICIENT_UNITS);
    }

    @Test
    void feeReducesCostOnly() {
        assertThat(program.apply(transaction("FE", "1.0000", "25.00", "25.00"))).isNull();

        assertThat(reload().getTotalUnits()).isEqualByComparingTo("100.0000");
        assertThat(reload().getTotalCost()).isEqualByComparingTo("9975.00");
    }

    @Test
    void transferRemainsUnimplementedAsInPorttran() {
        assertThat(program.apply(transaction("TR", "10.0000", "50.00", "500.00")))
                .isEqualTo(PortfolioTransactionProgram.ERR_TRANSFER_UNSUPPORTED);
        assertThat(reload().getTotalUnits()).isEqualByComparingTo("100.0000");
    }

    @Test
    void validationRejectsUnknownPortfolioTypeAndAmounts() {
        TransactionRecord unknownPortfolio = transaction("BU", "1.0000", "1.00", "1.00");
        unknownPortfolio.setPortfolioId("PORT9999");
        assertThat(program.validate(unknownPortfolio))
                .startsWith(PortfolioTransactionProgram.ERR_INVALID_PORTFOLIO);

        assertThat(program.validate(transaction("ZZ", "1.0000", "1.00", "1.00")))
                .startsWith(PortfolioTransactionProgram.ERR_INVALID_TYPE);
        assertThat(program.validate(transaction("BU", "0.0000", "1.00", "1.00")))
                .isEqualTo(PortfolioTransactionProgram.ERR_QUANTITY);
        assertThat(program.validate(transaction("BU", "1.0000", "0.00", "1.00")))
                .isEqualTo(PortfolioTransactionProgram.ERR_PRICE);
        assertThat(program.validate(transaction("BU", "1.0000", "1.00", "0.00")))
                .isEqualTo(PortfolioTransactionProgram.ERR_AMOUNT);
    }

    @Test
    void runCountsReadProcessedAndErrors() {
        ProgramResult result = program.run(List.of(transaction("BU", "1.0000", "1.00", "1.00"),
                transaction("ZZ", "1.0000", "1.00", "1.00")));

        assertThat(result.counter("read")).isEqualTo(2);
        assertThat(result.counter("processed")).isEqualTo(1);
        assertThat(result.counter("errors")).isEqualTo(1);
        assertThat(result.getReturnCode()).isEqualTo(ReturnCode.ERROR);
    }
}
