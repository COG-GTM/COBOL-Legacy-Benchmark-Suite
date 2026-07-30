package com.cognition.portfolio.transaction;

import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionKey;
import com.cognition.portfolio.transaction.domain.TransactionStatus;
import com.cognition.portfolio.transaction.domain.TransactionType;
import java.math.BigDecimal;

/**
 * Representative records used across the tests.
 *
 * <p>The legacy repository has no ASCII extract of TRANHIST, so these values are derived from the
 * {@code TRNREC.cpy} layout and the conventions of the COBOL generators {@code PORTTEST.cbl}
 * ({@code 'PORT' + counter} ids) and {@code TSTGEN00.cbl}.
 */
public final class TestTransactions {

  private TestTransactions() {}

  /** A valid BU transaction: 150 units @ 187.45 = 28,117.50. */
  public static PortfolioTransaction buy() {
    return builder("20240320", "093015", "PORT0001", "000001")
        .trnType(TransactionType.BUY)
        .trnQuantity(new BigDecimal("150.0000"))
        .trnPrice(new BigDecimal("187.4500"))
        .trnAmount(new BigDecimal("28117.50"))
        .build();
  }

  /** A valid SL transaction: 50 units @ 191.20 = 9,560.00. */
  public static PortfolioTransaction sell() {
    return builder("20240320", "101122", "PORT0001", "000002")
        .trnType(TransactionType.SELL)
        .trnQuantity(new BigDecimal("50.0000"))
        .trnPrice(new BigDecimal("191.2000"))
        .trnAmount(new BigDecimal("9560.00"))
        .build();
  }

  /** A valid FE transaction: 1 unit @ 125.00 = 125.00. */
  public static PortfolioTransaction fee() {
    return builder("20240320", "104500", "PORT0001", "000003")
        .trnInvestmentId("MGMTFEE001")
        .trnType(TransactionType.FEE)
        .trnQuantity(new BigDecimal("1.0000"))
        .trnPrice(new BigDecimal("125.0000"))
        .trnAmount(new BigDecimal("125.00"))
        .build();
  }

  /** A TR transaction; price and amount are zero, which 2130-CHECK-AMOUNTS exempts for TR. */
  public static PortfolioTransaction transfer() {
    return builder("20240320", "113000", "PORT0002", "000001")
        .trnInvestmentId("MSFT000001")
        .trnType(TransactionType.TRANSFER)
        .trnQuantity(new BigDecimal("25.0000"))
        .trnPrice(new BigDecimal("0.0000"))
        .trnAmount(new BigDecimal("0.00"))
        .build();
  }

  /** Builder pre-filled with the fields that are the same across the fixtures. */
  public static PortfolioTransaction.PortfolioTransactionBuilder builder(
      String date, String time, String portfolioId, String sequenceNo) {
    return PortfolioTransaction.builder()
        .trnKey(new TransactionKey(date, time, portfolioId, sequenceNo))
        .trnInvestmentId("AAPL000001")
        .trnType(TransactionType.BUY)
        .trnQuantity(new BigDecimal("150.0000"))
        .trnPrice(new BigDecimal("187.4500"))
        .trnAmount(new BigDecimal("28117.50"))
        .trnCurrency("USD")
        .trnStatus(TransactionStatus.PENDING)
        .trnProcessDate("2024-03-20-09.30.15.123456")
        .trnProcessUser("BATCH001");
  }
}
