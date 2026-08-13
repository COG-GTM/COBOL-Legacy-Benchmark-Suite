package com.ipms.domain;

import com.ipms.common.fixedwidth.FixedWidthReader;
import com.ipms.common.fixedwidth.FixedWidthWriter;

import java.math.BigDecimal;

/**
 * TRANSACTION-RECORD from {@code src/copybook/common/TRNREC.cpy}.
 *
 * <pre>
 * TRN-KEY:   TRN-DATE X(8), TRN-TIME X(6), TRN-PORTFOLIO-ID X(8), TRN-SEQUENCE-NO X(6)
 * TRN-DATA:  TRN-INVESTMENT-ID X(10), TRN-TYPE X(2),
 *            TRN-QUANTITY S9(11)V9(4) COMP-3, TRN-PRICE S9(11)V9(4) COMP-3,
 *            TRN-AMOUNT S9(13)V9(2) COMP-3, TRN-CURRENCY X(3), TRN-STATUS X(1)
 * TRN-AUDIT: TRN-PROCESS-DATE X(26), TRN-PROCESS-USER X(8)
 * TRN-FILLER X(50)
 * </pre>
 */
public record TransactionRecord(
        String trnDate,
        String trnTime,
        String trnPortfolioId,
        String trnSequenceNo,
        String trnInvestmentId,
        TransactionType trnType,
        BigDecimal trnQuantity,
        BigDecimal trnPrice,
        BigDecimal trnAmount,
        String trnCurrency,
        TransactionStatus trnStatus,
        String trnProcessDate,
        String trnProcessUser) {

    public static final int RECORD_LENGTH = 176;

    public static TransactionRecord parse(String record) {
        FixedWidthReader r = new FixedWidthReader(record);
        return new TransactionRecord(
                r.string(8),
                r.string(6),
                r.string(8),
                r.string(6),
                r.string(10),
                TransactionType.fromCode(r.string(2)),
                r.signedDecimal(11, 4),
                r.signedDecimal(11, 4),
                r.signedDecimal(13, 2),
                r.string(3),
                TransactionStatus.fromCode(r.string(1)),
                r.string(26),
                r.string(8));
    }

    public String toRecord() {
        return new FixedWidthWriter(RECORD_LENGTH)
                .string(trnDate, 8)
                .string(trnTime, 6)
                .string(trnPortfolioId, 8)
                .string(trnSequenceNo, 6)
                .string(trnInvestmentId, 10)
                .string(trnType.code(), 2)
                .signedDecimal(trnQuantity, 11, 4)
                .signedDecimal(trnPrice, 11, 4)
                .signedDecimal(trnAmount, 13, 2)
                .string(trnCurrency, 3)
                .string(trnStatus.code(), 1)
                .string(trnProcessDate, 26)
                .string(trnProcessUser, 8)
                .filler(50)
                .toRecord();
    }
}
