package com.clbs.posval.service;

import com.clbs.posval.cobol.CobolString;
import com.clbs.posval.domain.TransactionRecord;
import com.clbs.posval.domain.TransactionType;
import com.clbs.posval.repository.PortfolioPositionStore;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Port of the validation half of {@code src/programs/portfolio/PORTTRAN.cbl}
 * ({@code 2100-VALIDATE-TRANSACTION} and its three subordinate paragraphs).
 *
 * <table border="1">
 *   <caption>PORTTRAN validation to Java</caption>
 *   <tr><th>COBOL paragraph</th><th>Java method</th></tr>
 *   <tr><td>{@code 2100-VALIDATE-TRANSACTION}</td><td>{@link #validate(TransactionRecord)}</td></tr>
 *   <tr><td>{@code 2110-CHECK-PORTFOLIO}</td><td>{@link #checkPortfolio(TransactionRecord)}</td></tr>
 *   <tr><td>{@code 2120-CHECK-TRANSACTION-TYPE}</td><td>{@link #checkTransactionType(TransactionRecord)}</td></tr>
 *   <tr><td>{@code 2130-CHECK-AMOUNTS}</td><td>{@link #checkAmounts(TransactionRecord)}</td></tr>
 * </table>
 *
 * <p>The three checks are strictly sequential and short-circuit: {@code 2100} runs the next check
 * only while {@code ERR-TEXT} is still spaces, so at most one error is reported per transaction
 * and the first failure hides any later one. An empty result means the transaction is valid.
 */
@Service
public class TransactionValidationService {

    public static final String ERR_PORTFOLIO_REQUIRED = "Portfolio ID is required";
    public static final String ERR_INVALID_PORTFOLIO_PREFIX = "Invalid Portfolio ID: ";
    public static final String ERR_INVALID_TYPE_PREFIX = "Invalid Transaction Type: ";
    public static final String ERR_QUANTITY = "Quantity must be greater than zero";
    public static final String ERR_PRICE = "Price must be greater than zero";
    public static final String ERR_AMOUNT = "Amount must be greater than zero";

    /** {@code ERR-TEXT PIC X(80)}: messages built by {@code STRING … DELIMITED BY SIZE} are truncated here. */
    public static final int ERR_TEXT_WIDTH = 80;

    private final PortfolioPositionStore store;

    public TransactionValidationService(PortfolioPositionStore store) {
        this.store = store;
    }

    /** {@code 2100-VALIDATE-TRANSACTION}. Empty when the transaction passes every check. */
    public Optional<String> validate(TransactionRecord transaction) {
        Optional<String> error = checkPortfolio(transaction);
        if (error.isEmpty()) {
            error = checkTransactionType(transaction);
        }
        if (error.isEmpty()) {
            error = checkAmounts(transaction);
        }
        return error;
    }

    /**
     * {@code 2110-CHECK-PORTFOLIO}: the portfolio id must be present and must exist on
     * {@code PORTFILE}.
     *
     * <p>The failure message is built with {@code STRING 'Invalid Portfolio ID: '
     * TRN-PORTFOLIO-ID DELIMITED BY SIZE}, so the id is appended at its full {@code PIC X(8)}
     * width, trailing spaces included.
     */
    public Optional<String> checkPortfolio(TransactionRecord transaction) {
        String portfolioId = CobolString.move(transaction.portfolioId(), 8);

        if (CobolString.isSpaces(portfolioId)) {
            return Optional.of(errText(ERR_PORTFOLIO_REQUIRED));
        }
        if (store.read(portfolioId).isEmpty()) {
            return Optional.of(errText(ERR_INVALID_PORTFOLIO_PREFIX + portfolioId));
        }
        return Optional.empty();
    }

    /** {@code 2120-CHECK-TRANSACTION-TYPE}: the type must be {@code BU}, {@code SL}, {@code TR} or {@code FE}. */
    public Optional<String> checkTransactionType(TransactionRecord transaction) {
        String type = CobolString.move(transaction.type(), 2);
        if (TransactionType.fromCode(type).isEmpty()) {
            return Optional.of(errText(ERR_INVALID_TYPE_PREFIX + type));
        }
        return Optional.empty();
    }

    /**
     * {@code 2130-CHECK-AMOUNTS}: quantity must be strictly positive for every transaction type;
     * price and amount must be strictly positive for every type except {@code TR}.
     *
     * <p>The quantity test has no {@code TR} exemption, so a transfer with a zero quantity is
     * rejected while a transfer with a zero price and a zero amount is accepted — an asymmetry
     * that is almost certainly unintended (spec open question OQ-7). Comparisons are against zero,
     * so a negative quantity, price or amount is rejected by the same test.
     */
    public Optional<String> checkAmounts(TransactionRecord transaction) {
        boolean transfer = TransactionType.TRANSFER.code().equals(CobolString.move(transaction.type(), 2));

        if (isNotPositive(transaction.quantity())) {
            return Optional.of(errText(ERR_QUANTITY));
        }
        if (isNotPositive(transaction.price()) && !transfer) {
            return Optional.of(errText(ERR_PRICE));
        }
        if (isNotPositive(transaction.amount()) && !transfer) {
            return Optional.of(errText(ERR_AMOUNT));
        }
        return Optional.empty();
    }

    private static boolean isNotPositive(BigDecimal value) {
        return value.signum() <= 0;
    }

    private static String errText(String text) {
        return CobolString.move(text, ERR_TEXT_WIDTH).stripTrailing();
    }
}
