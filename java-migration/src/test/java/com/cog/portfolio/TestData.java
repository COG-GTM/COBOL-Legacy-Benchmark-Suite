package com.cog.portfolio;

import com.cog.portfolio.domain.ClientType;
import com.cog.portfolio.domain.Portfolio;
import com.cog.portfolio.domain.PortfolioId;
import com.cog.portfolio.domain.PortfolioStatus;
import com.cog.portfolio.domain.Position;
import com.cog.portfolio.domain.PositionId;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionId;
import com.cog.portfolio.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** TSTGEN00.cbl: test data factory. */
public final class TestData {

    public static final String PORTFOLIO_ID = "PORT0001";
    public static final String ACCOUNT_NO = "1234567890";
    public static final String INVESTMENT_ID = "FUND001";
    public static final LocalDate DATE = LocalDate.of(2024, 1, 15);

    private TestData() {
    }

    public static Portfolio portfolio() {
        Portfolio p = new Portfolio(new PortfolioId(PORTFOLIO_ID, ACCOUNT_NO), "Test Client", ClientType.INDIVIDUAL,
                PortfolioStatus.ACTIVE);
        p.setTotalValue(new BigDecimal("10000.00"));
        p.setCashBalance(new BigDecimal("500.00"));
        return p;
    }

    public static Position position(String quantity, String costBasis) {
        Position pos = new Position(new PositionId(PORTFOLIO_ID, DATE, INVESTMENT_ID));
        pos.setQuantity(new BigDecimal(quantity).setScale(4));
        pos.setCostBasis(new BigDecimal(costBasis).setScale(2));
        pos.setMarketValue(new BigDecimal(costBasis).setScale(2));
        pos.setDescription("Test Fund");
        return pos;
    }

    public static Transaction transaction(TransactionType type, String sequence, String quantity, String price,
                                          String amount) {
        Transaction trn = new Transaction(new TransactionId(DATE, LocalTime.of(10, 0, Integer.parseInt(sequence) % 60),
                PORTFOLIO_ID, sequence), INVESTMENT_ID, type);
        trn.setQuantity(new BigDecimal(quantity).setScale(4));
        trn.setPrice(new BigDecimal(price).setScale(4));
        trn.setAmount(new BigDecimal(amount).setScale(2));
        return trn;
    }
}
