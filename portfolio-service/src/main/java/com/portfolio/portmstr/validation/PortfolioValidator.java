package com.portfolio.portmstr.validation;

import com.portfolio.portmstr.dto.PortfolioRequest;
import com.portfolio.portmstr.dto.TransactionRequest;
import com.portfolio.portmstr.exception.PortfolioValidationException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Portfolio validation logic.
 * Direct translation of COBOL PORTVALD.cbl validation subroutine.
 * Preserves all validation rules from paragraphs 1000-4000.
 *
 * Validation codes match COBOL PORTVAL.cpy:
 *   0 = VAL-SUCCESS
 *   1 = VAL-INVALID-ID
 *   2 = VAL-INVALID-ACCT
 *   3 = VAL-INVALID-TYPE
 *   4 = VAL-INVALID-AMT
 */
@Component
public class PortfolioValidator {

    private static final String ID_PREFIX = "PORT";
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");

    /**
     * Validate portfolio ID format.
     * From COBOL 1000-VALIDATE-ID: Must start with 'PORT' and have 4 numeric digits.
     */
    public void validatePortfolioId(String portfolioId) {
        if (portfolioId == null || portfolioId.length() < 8) {
            throw new PortfolioValidationException("Invalid Portfolio ID format", 1);
        }

        if (!portfolioId.startsWith(ID_PREFIX)) {
            throw new PortfolioValidationException("Invalid Portfolio ID format", 1);
        }

        String numericPart = portfolioId.substring(4, 8);
        if (!numericPart.matches("\\d{4}")) {
            throw new PortfolioValidationException("Invalid Portfolio ID format", 1);
        }
    }

    /**
     * Validate account number.
     * From COBOL 2000-VALIDATE-ACCOUNT: Must be 10 numeric digits, not all zeros.
     */
    public void validateAccountNumber(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return; // optional field
        }

        if (!accountNo.matches("\\d{10}")) {
            throw new PortfolioValidationException("Invalid Account Number format", 2);
        }

        if (accountNo.equals("0000000000")) {
            throw new PortfolioValidationException("Invalid Account Number format", 2);
        }
    }

    /**
     * Validate investment type.
     * From COBOL 3000-VALIDATE-TYPE: Must be STK, BND, MMF, or ETF.
     */
    public void validateInvestmentType(String type) {
        if (type == null) {
            throw new PortfolioValidationException("Invalid Investment Type", 3);
        }

        if (!type.equals("STK") && !type.equals("BND") &&
                !type.equals("MMF") && !type.equals("ETF")) {
            throw new PortfolioValidationException("Invalid Investment Type", 3);
        }
    }

    /**
     * Validate amount range.
     * From COBOL 4000-VALIDATE-AMOUNT: Amount must be within valid range.
     */
    public void validateAmount(BigDecimal amount) {
        if (amount == null) {
            return;
        }

        if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new PortfolioValidationException("Amount outside valid range", 4);
        }
    }

    /**
     * Validate a complete portfolio request.
     * From COBOL PORTMSTR.cbl 2100-VALIDATE-PORTFOLIO paragraph.
     */
    public void validatePortfolioRequest(PortfolioRequest request) {
        validatePortfolioId(request.portfolioId());

        if (request.clientName() == null || request.clientName().isBlank()) {
            throw new PortfolioValidationException("Portfolio Name is required", 1);
        }

        String status = request.status();
        if (status == null || (!status.equals("A") && !status.equals("C") && !status.equals("S"))) {
            throw new PortfolioValidationException("Invalid Portfolio Status", 3);
        }

        validateAmount(request.totalValue());
        validateAmount(request.cashBalance());
    }

    /**
     * Validate a transaction request.
     * From COBOL PORTTRAN.cbl 2100-VALIDATE-TRANSACTION paragraph.
     */
    public void validateTransactionRequest(TransactionRequest request) {
        if (request.portfolioId() == null || request.portfolioId().isBlank()) {
            throw new PortfolioValidationException("Portfolio ID is required", 1);
        }

        String type = request.transactionType();
        if (type == null || (!type.equals("BU") && !type.equals("SL") &&
                !type.equals("TR") && !type.equals("FE"))) {
            throw new PortfolioValidationException("Invalid Transaction Type: " + type, 3);
        }

        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PortfolioValidationException("Quantity must be greater than zero", 4);
        }

        if (!type.equals("TR")) {
            if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PortfolioValidationException("Price must be greater than zero", 4);
            }
            if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PortfolioValidationException("Amount must be greater than zero", 4);
            }
        }
    }
}
