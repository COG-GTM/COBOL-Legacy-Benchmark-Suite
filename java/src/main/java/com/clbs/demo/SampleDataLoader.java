package com.clbs.demo;

import com.clbs.db2.AuthorizationEntity;
import com.clbs.db2.AuthorizationRepository;
import com.clbs.db2.PositionHistoryEntity;
import com.clbs.db2.PositionHistoryRepository;
import com.clbs.domain.PortfolioRecord;
import com.clbs.domain.PositionRecord;
import com.clbs.store.DatasetCatalog;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Loads the sample records from documentation/operations/test-data-specs.md into the in-memory
 * datasets and DB2 stand-in so the running application has data to serve.
 */
@Component
@ConditionalOnProperty(name = "clbs.sample-data", havingValue = "true", matchIfMissing = true)
public class SampleDataLoader implements ApplicationRunner {

    private final DatasetCatalog datasets;
    private final PositionHistoryRepository history;
    private final AuthorizationRepository authorizations;

    public SampleDataLoader(DatasetCatalog datasets, PositionHistoryRepository history,
            AuthorizationRepository authorizations) {
        this.datasets = datasets;
        this.history = history;
        this.authorizations = authorizations;
    }

    @Override
    public void run(ApplicationArguments args) {
        datasets.portfolioFile().open();
        datasets.positionFile().open();

        portfolio("PORT00001", "ACCT000001", "GROWTH PORTFOLIO", 'A', "12345678.99");
        portfolio("PORT00002", "ACCT000002", "INCOME PORTFOLIO", 'A', "98765432.10");
        portfolio("PORT00003", "ACCT000003", "BALANCED PORTFOLIO", 'I', "5555555.55");

        position("PORT00001", "IBM0000001", "100.0000", "12500.00", "13750.00", "12500.00");
        position("PORT00002", "MSFT000001", "50.0000", "5000.00", "5400.00", "5200.00");
        position("PORT00003", "AAPL000001", "75.0000", "7500.00", "7125.00", "7500.00");

        historyRow("ACCT000001", "PORT00001", "2024-03-20", "15.30.45", "BU", "IBM0000001",
                "100.0000", "125.00", "12500.00");
        historyRow("ACCT000002", "PORT00002", "2024-03-20", "15.31.12", "SL", "MSFT000001",
                "50.0000", "100.00", "5000.00");

        authorizations.save(new AuthorizationEntity("USER0001", "INQONLN", "READ"));
        authorizations.save(new AuthorizationEntity("USER0001", "PORTMSTR", "UPDATE"));
    }

    private void portfolio(String id, String accountNo, String name, char status, String value) {
        PortfolioRecord record = new PortfolioRecord();
        record.setPortId(id);
        record.setAccountNo(accountNo);
        record.setClientName(name);
        record.setCreateDate(20240320);
        record.setStatus(status);
        record.setTotalValue(new BigDecimal(value));
        datasets.portfolioFile().write(record);
    }

    private void position(String portfolioId, String investmentId, String quantity,
            String costBasis, String marketValue, String previousValue) {
        PositionRecord record = new PositionRecord();
        record.setPortfolioId(portfolioId);
        record.setDate("20240320");
        record.setInvestmentId(investmentId);
        record.setQuantity(new BigDecimal(quantity));
        record.setCostBasis(new BigDecimal(costBasis));
        record.setMarketValue(new BigDecimal(marketValue));
        record.setPreviousValue(new BigDecimal(previousValue));
        datasets.positionFile().write(record);
    }

    private void historyRow(String accountNo, String portfolioId, String date, String time,
            String type, String investmentId, String units, String price, String amount) {
        PositionHistoryEntity entity = new PositionHistoryEntity();
        entity.setAccountNo(accountNo);
        entity.setPortfolioId(portfolioId);
        entity.setTransDate(date);
        entity.setTransTime(time);
        entity.setTransType(type);
        entity.setInvestmentId(investmentId);
        entity.setTransUnits(new BigDecimal(units));
        entity.setTransPrice(new BigDecimal(price));
        entity.setTransAmount(new BigDecimal(amount));
        entity.setTotalAmount(new BigDecimal(amount));
        history.save(entity);
    }
}
