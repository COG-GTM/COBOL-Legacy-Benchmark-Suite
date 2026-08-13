package com.ipms.domain;

import com.ipms.common.fixedwidth.FixedWidthReader;
import com.ipms.common.fixedwidth.FixedWidthWriter;

import java.math.BigDecimal;

/**
 * PORT-RECORD (Portfolio Master) from {@code src/copybook/common/PORTFLIO.cpy}.
 *
 * <pre>
 * PORT-KEY:            PORT-ID X(8), PORT-ACCOUNT-NO X(10)
 * PORT-CLIENT-INFO:    PORT-CLIENT-NAME X(30), PORT-CLIENT-TYPE X(1)
 * PORT-PORTFOLIO-INFO: PORT-CREATE-DATE 9(8), PORT-LAST-MAINT 9(8), PORT-STATUS X(1)
 * PORT-FINANCIAL-INFO: PORT-TOTAL-VALUE S9(13)V99 COMP-3, PORT-CASH-BALANCE S9(13)V99 COMP-3
 * PORT-AUDIT-INFO:     PORT-LAST-USER X(8), PORT-LAST-TRANS 9(8)
 * PORT-FILLER X(50)
 * </pre>
 */
public record PortfolioRecord(
        String portId,
        String portAccountNo,
        String portClientName,
        ClientType portClientType,
        String portCreateDate,
        String portLastMaint,
        PortfolioStatus portStatus,
        BigDecimal portTotalValue,
        BigDecimal portCashBalance,
        String portLastUser,
        String portLastTrans) {

    public static final int RECORD_LENGTH = 164;

    public static PortfolioRecord parse(String record) {
        FixedWidthReader r = new FixedWidthReader(record);
        return new PortfolioRecord(
                r.string(8),
                r.string(10),
                r.string(30),
                ClientType.fromCode(r.string(1)),
                r.rawString(8),
                r.rawString(8),
                PortfolioStatus.fromCode(r.string(1)),
                r.signedDecimal(13, 2),
                r.signedDecimal(13, 2),
                r.string(8),
                r.rawString(8));
    }

    public String toRecord() {
        return new FixedWidthWriter(RECORD_LENGTH)
                .string(portId, 8)
                .string(portAccountNo, 10)
                .string(portClientName, 30)
                .string(portClientType.code(), 1)
                .string(portCreateDate, 8)
                .string(portLastMaint, 8)
                .string(portStatus.code(), 1)
                .signedDecimal(portTotalValue, 13, 2)
                .signedDecimal(portCashBalance, 13, 2)
                .string(portLastUser, 8)
                .string(portLastTrans, 8)
                .filler(50)
                .toRecord();
    }
}
