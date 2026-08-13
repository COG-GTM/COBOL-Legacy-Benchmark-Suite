package com.ipms.domain;

import com.ipms.common.fixedwidth.FixedWidthReader;
import com.ipms.common.fixedwidth.FixedWidthWriter;

import java.math.BigDecimal;

/**
 * POSITION-RECORD from {@code src/copybook/common/POSREC.cpy}.
 *
 * <pre>
 * POS-KEY:   POS-PORTFOLIO-ID X(8), POS-DATE X(8), POS-INVESTMENT-ID X(10)
 * POS-DATA:  POS-QUANTITY S9(11)V9(4) COMP-3, POS-COST-BASIS S9(13)V9(2) COMP-3,
 *            POS-MARKET-VALUE S9(13)V9(2) COMP-3, POS-CURRENCY X(3), POS-STATUS X(1)
 * POS-AUDIT: POS-LAST-MAINT-DATE X(26), POS-LAST-MAINT-USER X(8)
 * POS-FILLER X(50)
 * </pre>
 */
public record PositionRecord(
        String posPortfolioId,
        String posDate,
        String posInvestmentId,
        BigDecimal posQuantity,
        BigDecimal posCostBasis,
        BigDecimal posMarketValue,
        String posCurrency,
        PositionStatus posStatus,
        String posLastMaintDate,
        String posLastMaintUser) {

    public static final int RECORD_LENGTH = 162;

    public static PositionRecord parse(String record) {
        FixedWidthReader r = new FixedWidthReader(record);
        return new PositionRecord(
                r.string(8),
                r.string(8),
                r.string(10),
                r.signedDecimal(11, 4),
                r.signedDecimal(13, 2),
                r.signedDecimal(13, 2),
                r.string(3),
                PositionStatus.fromCode(r.string(1)),
                r.string(26),
                r.string(8));
    }

    public String toRecord() {
        return new FixedWidthWriter(RECORD_LENGTH)
                .string(posPortfolioId, 8)
                .string(posDate, 8)
                .string(posInvestmentId, 10)
                .signedDecimal(posQuantity, 11, 4)
                .signedDecimal(posCostBasis, 13, 2)
                .signedDecimal(posMarketValue, 13, 2)
                .string(posCurrency, 3)
                .string(posStatus.code(), 1)
                .string(posLastMaintDate, 26)
                .string(posLastMaintUser, 8)
                .filler(50)
                .toRecord();
    }
}
