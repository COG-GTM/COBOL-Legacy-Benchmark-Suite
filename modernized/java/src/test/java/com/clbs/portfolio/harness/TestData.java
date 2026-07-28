package com.clbs.portfolio.harness;

import com.clbs.portfolio.model.ClientType;
import com.clbs.portfolio.model.PortfolioRecord;
import com.clbs.portfolio.model.PortfolioStatus;
import com.clbs.portfolio.model.PositionRecord;
import com.clbs.portfolio.model.PositionStatus;
import com.clbs.portfolio.model.TransactionRecord;
import com.clbs.portfolio.model.TransactionStatus;
import com.clbs.portfolio.model.TransactionType;

import java.math.BigDecimal;

/**
 * Seed data for the translated programs, taken from the two documents that stand in for a runnable
 * z/OS system: {@code documentation/operations/test-data-specs.md} (sample records and value ranges)
 * and {@code documentation/technical/data-dictionary.md} (validation rules and error catalogue).
 *
 * <p>Where the documents and the copybooks disagree, the copybooks win and the documented value is
 * kept alongside as a constant so a test can pin the difference. Two such disagreements matter here:
 *
 * <ul>
 *   <li>the documented portfolio ids are nine characters ({@code PORT} plus five digits) while
 *       {@code PORT-ID} is {@code PIC X(8)}, so a documented id loses its last digit on the way into
 *       the record - see {@link #DOCUMENTED_GROWTH_PORTFOLIO_ID};</li>
 *   <li>the documented transaction types are the single characters {@code B} and {@code S} while
 *       {@code TRN-TYPE} is a two-byte field holding {@code BU}, {@code SL}, {@code TR} or
 *       {@code FE} - the copybook codes are the ones the programs validate.</li>
 * </ul>
 */
public final class TestData {

    /** Portfolio id as written in the documented sample records: nine characters. */
    public static final String DOCUMENTED_GROWTH_PORTFOLIO_ID = "PORT00001";

    /** Portfolio ids adapted to the eight characters {@code PORT-ID} actually holds. */
    public static final String GROWTH_PORTFOLIO_ID = "PORT0001";
    public static final String INCOME_PORTFOLIO_ID = "PORT0002";
    public static final String BALANCED_PORTFOLIO_ID = "PORT0003";
    public static final String MISSING_PORTFOLIO_ID = "PORT9999";

    public static final String INVESTMENT_ID = "IBM0000001";
    public static final String CURRENCY = "USD";
    public static final String USER_ID = "PORTUSER";
    public static final String TRANSACTION_DATE = "20240320";
    public static final String TRANSACTION_TIME = "153045";

    /** {@code test-data-specs.md} 3.5: transaction amount range. */
    public static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");
    public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("99999999999.99");

    /** {@code test-data-specs.md} 3.5: portfolio total value range. */
    public static final BigDecimal MIN_PORTFOLIO_VALUE = new BigDecimal("0.00");
    public static final BigDecimal MAX_PORTFOLIO_VALUE = new BigDecimal("9999999999999.99");

    private TestData() {
    }

    /** {@code PORT00001 GROWTH PORTFOLIO ... A 12345678.99} from the documented sample records. */
    public static PortfolioRecord growthPortfolio() {
        PortfolioRecord portfolio = portfolio(GROWTH_PORTFOLIO_ID, "GROWTH PORTFOLIO", "0000000001");
        portfolio.setPortTotalValue("12345678.99");
        portfolio.setPortCashBalance("2500000.00");
        portfolio.setPortTotalUnits("1000.0000");
        portfolio.setPortTotalCost("10000000.00");
        return portfolio;
    }

    /** {@code PORT00002 INCOME PORTFOLIO ... A 98765432.10}. */
    public static PortfolioRecord incomePortfolio() {
        PortfolioRecord portfolio = portfolio(INCOME_PORTFOLIO_ID, "INCOME PORTFOLIO", "0000000002");
        portfolio.setPortTotalValue("98765432.10");
        portfolio.setPortCashBalance("5000000.00");
        portfolio.setPortTotalUnits("250.0000");
        portfolio.setPortTotalCost("50000000.00");
        return portfolio;
    }

    /**
     * {@code PORT00003 BALANCED PORTFOLIO ... 5555555.55}. The documented status byte is {@code I}
     * for "Inactive", which no level-88 in {@code PORTFLIO.cpy} covers; the record therefore carries
     * the copybook's {@code S} (Suspended) and the documented byte is asserted separately.
     */
    public static PortfolioRecord balancedPortfolio() {
        PortfolioRecord portfolio =
                portfolio(BALANCED_PORTFOLIO_ID, "BALANCED PORTFOLIO", "0000000003");
        portfolio.setPortStatus(PortfolioStatus.SUSPENDED);
        portfolio.setPortTotalValue("5555555.55");
        portfolio.setPortCashBalance("0.00");
        return portfolio;
    }

    private static PortfolioRecord portfolio(String id, String name, String accountNo) {
        PortfolioRecord portfolio = new PortfolioRecord();
        portfolio.setPortId(id);
        portfolio.setPortAccountNo(accountNo);
        portfolio.setPortClientName(name);
        portfolio.setPortClientType(ClientType.INDIVIDUAL);
        portfolio.setPortCreateDate(20240320);
        portfolio.setPortLastMaint(20240320);
        portfolio.setPortStatus(PortfolioStatus.ACTIVE);
        portfolio.setPortLastUser(USER_ID);
        portfolio.setPortLastTrans(20240320);
        return portfolio;
    }

    /** A valid buy: 100 units at 125.0000 for 12,500.00, the documented sample transaction. */
    public static TransactionRecord buyTransaction() {
        return transaction(TransactionType.BUY, "100.0000", "125.0000", "12500.00");
    }

    /** A valid sell: 50 units at 125.0000 for 6,250.00. */
    public static TransactionRecord sellTransaction() {
        return transaction(TransactionType.SELL, "50.0000", "125.0000", "6250.00");
    }

    /**
     * A transfer. {@code 2130-CHECK-AMOUNTS} exempts transfers from the price and amount checks, so
     * the seeded record leaves both at zero and carries only a quantity.
     */
    public static TransactionRecord transferTransaction() {
        return transaction(TransactionType.TRANSFER, "25.0000", "0.0000", "0.00");
    }

    /** A fee: quantity must still be positive, since no validation exempts fees from that check. */
    public static TransactionRecord feeTransaction() {
        return transaction(TransactionType.FEE, "1.0000", "45.5000", "45.50");
    }

    /** A transaction whose type byte matches no level-88, for the invalid-type path. */
    public static TransactionRecord transactionWithRawType(String rawType) {
        TransactionRecord transaction = buyTransaction();
        transaction.setTrnType(rawType);
        return transaction;
    }

    public static TransactionRecord transaction(
            TransactionType type, String quantity, String price, String amount) {
        TransactionRecord transaction = new TransactionRecord();
        transaction.setTrnDate(TRANSACTION_DATE);
        transaction.setTrnTime(TRANSACTION_TIME);
        transaction.setTrnPortfolioId(GROWTH_PORTFOLIO_ID);
        transaction.setTrnSequenceNo("000001");
        transaction.setTrnInvestmentId(INVESTMENT_ID);
        transaction.setTrnType(type);
        transaction.setTrnQuantity(quantity);
        transaction.setTrnPrice(price);
        transaction.setTrnAmount(amount);
        transaction.setTrnCurrency(CURRENCY);
        transaction.setTrnStatus(TransactionStatus.PENDING);
        transaction.setTrnProcessDate("2024-03-20-15.30.45.123456");
        transaction.setTrnProcessUser(USER_ID);
        return transaction;
    }

    /** An active position backing {@link #growthPortfolio()}. */
    public static PositionRecord growthPosition() {
        PositionRecord position = new PositionRecord();
        position.setPosPortfolioId(GROWTH_PORTFOLIO_ID);
        position.setPosDate(TRANSACTION_DATE);
        position.setPosInvestmentId(INVESTMENT_ID);
        position.setPosQuantity("1000.0000");
        position.setPosCostBasis("10000000.00");
        position.setPosMarketValue("12345678.99");
        position.setPosCurrency(CURRENCY);
        position.setPosStatus(PositionStatus.ACTIVE);
        position.setPosLastMaintDate("2024-03-20-15.30.45.123456");
        position.setPosLastMaintUser(USER_ID);
        return position;
    }
}
