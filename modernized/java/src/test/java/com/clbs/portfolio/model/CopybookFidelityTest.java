package com.clbs.portfolio.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Field-by-field guard over the translated copybooks. Every expected value below is read straight
 * out of {@code src/copybook/common/*.cpy} rather than from the Java, so a mistyped width, a wrong
 * scale or a missing level-88 value fails here.
 *
 * <p>This exists for the slices still to be translated: they extend these models, and a widened
 * field or a quietly relaxed scale would otherwise surface as a wrong number much later.
 */
class CopybookFidelityTest {

    private static final String LONG = repeat('Z', 400);

    private static String repeat(char c, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append(c);
        }
        return b.toString();
    }

    /** Stores an over-long marker through the String setter and returns the stored width. */
    private static int storedWidth(Object record, String setter, String getter) throws Exception {
        Method set = record.getClass().getMethod(setter, String.class);
        Method get = record.getClass().getMethod(getter);
        set.invoke(record, LONG);
        String stored = (String) get.invoke(record);
        // right-truncation: everything kept must be the marker character
        assertEquals(repeat('Z', stored.length()), stored, setter + " must truncate on the right");
        return stored.length();
    }

    /** Default (uninitialised) width, which must be the same n, all spaces. */
    private static int defaultWidth(Object record, String getter) throws Exception {
        String stored = (String) record.getClass().getMethod(getter).invoke(record);
        assertEquals(repeat(' ', stored.length()), stored, getter + " must default to spaces");
        return stored.length();
    }

    private static void picX(Object fresh, Object dirty, String setter, String getter, int n)
            throws Exception {
        assertEquals(n, defaultWidth(fresh, getter), getter + " default width");
        assertEquals(n, storedWidth(dirty, setter, getter), getter + " stored width");
    }

    // ------------------------------------------------------------------ TRNREC.cpy

    @Test
    @DisplayName("TRNREC.cpy: every field width and scale")
    void transactionRecord() throws Exception {
        TransactionRecord fresh = new TransactionRecord();
        TransactionRecord r = new TransactionRecord();

        picX(fresh, r, "setTrnDate", "getTrnDate", 8);
        picX(fresh, r, "setTrnTime", "getTrnTime", 6);
        picX(fresh, r, "setTrnPortfolioId", "getTrnPortfolioId", 8);
        picX(fresh, r, "setTrnSequenceNo", "getTrnSequenceNo", 6);
        picX(fresh, r, "setTrnInvestmentId", "getTrnInvestmentId", 10);
        picX(fresh, r, "setTrnType", "getTrnType", 2);
        picX(fresh, r, "setTrnCurrency", "getTrnCurrency", 3);
        picX(fresh, r, "setTrnStatus", "getTrnStatus", 1);
        picX(fresh, r, "setTrnProcessDate", "getTrnProcessDate", 26);
        picX(fresh, r, "setTrnProcessUser", "getTrnProcessUser", 8);
        picX(fresh, r, "setTrnFiller", "getTrnFiller", 50);

        // TRN-KEY = 8 + 6 + 8 + 6
        assertEquals(28, new TransactionRecord().getTrnKey().length(), "TRN-KEY group length");

        // TRN-QUANTITY / TRN-PRICE  PIC S9(11)V9(4)  -> scale 4, 11 integer digits
        r.setTrnQuantity("1.123456789");
        assertEquals(4, r.getTrnQuantity().scale(), "TRN-QUANTITY scale");
        assertEquals(new BigDecimal("1.1234"), r.getTrnQuantity());
        r.setTrnPrice("1.123456789");
        assertEquals(4, r.getTrnPrice().scale(), "TRN-PRICE scale");
        r.setTrnQuantity("99999999999.9999");
        assertEquals(new BigDecimal("99999999999.9999"), r.getTrnQuantity(), "11 integer digits fit");
        r.setTrnQuantity("999999999999.9999");
        assertEquals(new BigDecimal("99999999999.9999"), r.getTrnQuantity(), "12th digit dropped");
        r.setTrnPrice("999999999999.9999");
        assertEquals(new BigDecimal("99999999999.9999"), r.getTrnPrice(), "TRN-PRICE 11 digits");

        // TRN-AMOUNT  PIC S9(13)V9(2)  -> scale 2, 13 integer digits
        r.setTrnAmount("1.999");
        assertEquals(2, r.getTrnAmount().scale(), "TRN-AMOUNT scale");
        assertEquals(new BigDecimal("1.99"), r.getTrnAmount());
        r.setTrnAmount("9999999999999.99");
        assertEquals(new BigDecimal("9999999999999.99"), r.getTrnAmount(), "13 integer digits fit");
        r.setTrnAmount("99999999999999.99");
        assertEquals(new BigDecimal("9999999999999.99"), r.getTrnAmount(), "14th digit dropped");

        // defaults are zero at the picture scale
        assertEquals(4, fresh.getTrnQuantity().scale());
        assertEquals(4, fresh.getTrnPrice().scale());
        assertEquals(2, fresh.getTrnAmount().scale());
    }

    // ------------------------------------------------------------------ POSREC.cpy

    @Test
    @DisplayName("POSREC.cpy: every field width and scale")
    void positionRecord() throws Exception {
        PositionRecord fresh = new PositionRecord();
        PositionRecord r = new PositionRecord();

        picX(fresh, r, "setPosPortfolioId", "getPosPortfolioId", 8);
        picX(fresh, r, "setPosDate", "getPosDate", 8);
        picX(fresh, r, "setPosInvestmentId", "getPosInvestmentId", 10);
        picX(fresh, r, "setPosCurrency", "getPosCurrency", 3);
        picX(fresh, r, "setPosStatus", "getPosStatus", 1);
        picX(fresh, r, "setPosLastMaintDate", "getPosLastMaintDate", 26);
        picX(fresh, r, "setPosLastMaintUser", "getPosLastMaintUser", 8);
        picX(fresh, r, "setPosFiller", "getPosFiller", 50);

        // POS-KEY = 8 + 8 + 10
        assertEquals(26, new PositionRecord().getPosKey().length(), "POS-KEY group length");

        r.setPosQuantity("1.99999");
        assertEquals(4, r.getPosQuantity().scale(), "POS-QUANTITY scale");
        r.setPosQuantity("999999999999.9999");
        assertEquals(new BigDecimal("99999999999.9999"), r.getPosQuantity(), "POS-QUANTITY 11 digits");

        r.setPosCostBasis("1.999");
        assertEquals(2, r.getPosCostBasis().scale(), "POS-COST-BASIS scale");
        r.setPosCostBasis("99999999999999.99");
        assertEquals(new BigDecimal("9999999999999.99"), r.getPosCostBasis(), "POS-COST-BASIS 13 digits");

        r.setPosMarketValue("1.999");
        assertEquals(2, r.getPosMarketValue().scale(), "POS-MARKET-VALUE scale");
        r.setPosMarketValue("99999999999999.99");
        assertEquals(new BigDecimal("9999999999999.99"), r.getPosMarketValue(), "POS-MARKET-VALUE 13 digits");
    }

    // ------------------------------------------------------------------ PORTFLIO.cpy

    @Test
    @DisplayName("PORTFLIO.cpy: every field width and scale")
    void portfolioRecord() throws Exception {
        PortfolioRecord fresh = new PortfolioRecord();
        PortfolioRecord r = new PortfolioRecord();

        picX(fresh, r, "setPortId", "getPortId", 8);
        picX(fresh, r, "setPortAccountNo", "getPortAccountNo", 10);
        picX(fresh, r, "setPortClientName", "getPortClientName", 30);
        picX(fresh, r, "setPortClientType", "getPortClientType", 1);
        picX(fresh, r, "setPortStatus", "getPortStatus", 1);
        picX(fresh, r, "setPortLastUser", "getPortLastUser", 8);
        picX(fresh, r, "setPortFiller", "getPortFiller", 50);

        // PORT-KEY = 8 + 10
        assertEquals(18, new PortfolioRecord().getPortKey().length(), "PORT-KEY group length");

        // PIC 9(8) display fields: unsigned, 8 digits
        r.setPortCreateDate(123456789);
        assertEquals(23456789, r.getPortCreateDate(), "PORT-CREATE-DATE 8 digits");
        r.setPortLastMaint(-20240320);
        assertEquals(20240320, r.getPortLastMaint(), "PORT-LAST-MAINT unsigned");
        r.setPortLastTrans(123456789);
        assertEquals(23456789, r.getPortLastTrans(), "PORT-LAST-TRANS 8 digits");

        // PIC S9(13)V99 -> scale 2, 13 integer digits
        r.setPortTotalValue("1.999");
        assertEquals(2, r.getPortTotalValue().scale(), "PORT-TOTAL-VALUE scale");
        r.setPortTotalValue("99999999999999.99");
        assertEquals(new BigDecimal("9999999999999.99"), r.getPortTotalValue());
        r.setPortCashBalance("1.999");
        assertEquals(2, r.getPortCashBalance().scale(), "PORT-CASH-BALANCE scale");
        r.setPortCashBalance("99999999999999.99");
        assertEquals(new BigDecimal("9999999999999.99"), r.getPortCashBalance());

        // synthetic G1 fields typed from POSREC
        r.setPortTotalUnits("1.99999");
        assertEquals(4, r.getPortTotalUnits().scale(), "PORT-TOTAL-UNITS scale (synthetic)");
        r.setPortTotalCost("1.999");
        assertEquals(2, r.getPortTotalCost().scale(), "PORT-TOTAL-COST scale (synthetic)");

        // the synthetic fields must not leak into the record image
        PortfolioRecord img = new PortfolioRecord();
        String before = img.toRecordImage();
        img.setPortTotalUnits("777.7777");
        img.setPortTotalCost("888.88");
        assertEquals(before, img.toRecordImage(), "synthetic fields excluded from toRecordImage()");
        // The image is an approximation, not the 148-byte record: each packed field renders as 16
        // characters. See G1 and section 3.3 of TRANSLATION-NOTES.md.
        assertEquals(164, before.length(), "toRecordImage() is a rendering, not the record layout");
    }

    // ------------------------------------------------------------------ ERRHAND.cpy

    @Test
    @DisplayName("ERRHAND.cpy: ERR-MESSAGE widths and the constant tables")
    void errorMessage() throws Exception {
        ErrorMessage fresh = new ErrorMessage();
        ErrorMessage r = new ErrorMessage();

        picX(fresh, r, "setErrDate", "getErrDate", 10);
        picX(fresh, r, "setErrTime", "getErrTime", 8);
        picX(fresh, r, "setErrProgram", "getErrProgram", 8);
        picX(fresh, r, "setErrCategory", "getErrCategory", 2);
        picX(fresh, r, "setErrCode", "getErrCode", 4);
        picX(fresh, r, "setErrText", "getErrText", 80);
        picX(fresh, r, "setErrDetails", "getErrDetails", 256);

        // ERR-TIMESTAMP = 10 + 8
        assertEquals(18, new ErrorMessage().getErrTimestamp().length(), "ERR-TIMESTAMP group length");

        // ERR-CATEGORIES
        assertEquals(new LinkedHashSet<>(Arrays.asList("VS", "VL", "PR", "SY")),
                Arrays.stream(ErrorCategory.values()).map(ErrorCategory::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        // ERR-RETURN-CODES
        assertEquals(Arrays.asList(0, 4, 8, 12, 16),
                Arrays.stream(ErrorSeverity.values()).map(ErrorSeverity::value)
                        .collect(Collectors.toList()));
        // ERR-VSAM-STATUSES + ERR-VSAM-MSGS
        assertEquals(new LinkedHashSet<>(Arrays.asList("00", "10", "22", "23")),
                Arrays.stream(VsamStatus.values()).map(VsamStatus::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        assertEquals("Duplicate record key", VsamStatus.DUPLICATE_KEY.message());
        assertEquals("Record not found", VsamStatus.NOT_FOUND.message());
        assertEquals("Unexpected VSAM error", VsamStatus.messageFor("99"));
        assertEquals("Unexpected VSAM error", VsamStatus.OTHER_MESSAGE);
    }

    // ------------------------------------------------------------------ AUDITLOG.cpy

    @Test
    @DisplayName("AUDITLOG.cpy: every field width and the group lengths")
    void auditRecord() throws Exception {
        AuditRecord fresh = new AuditRecord();
        AuditRecord r = new AuditRecord();

        picX(fresh, r, "setAudTimestamp", "getAudTimestamp", 26);
        picX(fresh, r, "setAudSystemId", "getAudSystemId", 8);
        picX(fresh, r, "setAudUserId", "getAudUserId", 8);
        picX(fresh, r, "setAudProgram", "getAudProgram", 8);
        picX(fresh, r, "setAudTerminal", "getAudTerminal", 8);
        picX(fresh, r, "setAudType", "getAudType", 4);
        picX(fresh, r, "setAudAction", "getAudAction", 8);
        picX(fresh, r, "setAudStatus", "getAudStatus", 4);
        picX(fresh, r, "setAudPortfolioId", "getAudPortfolioId", 8);
        picX(fresh, r, "setAudAccountNo", "getAudAccountNo", 10);
        picX(fresh, r, "setAudBeforeImage", "getAudBeforeImage", 100);
        picX(fresh, r, "setAudAfterImage", "getAudAfterImage", 100);
        picX(fresh, r, "setAudMessage", "getAudMessage", 100);

        // AUD-HEADER = 26 + 8 + 8 + 8 + 8 ; AUD-KEY-INFO = 8 + 10
        assertEquals(58, new AuditRecord().getAudHeader().length(), "AUD-HEADER group length");
        assertEquals(18, new AuditRecord().getAudKeyInfo().length(), "AUD-KEY-INFO group length");

        // INITIALIZE puts every field back to spaces
        r.initialize();
        assertEquals(repeat(' ', 58), r.getAudHeader());
        assertEquals(repeat(' ', 100), r.getAudMessage());
    }

    // ------------------------------------------------------------------ level-88 sets

    @Test
    @DisplayName("every level-88 condition set holds exactly the copybook values")
    void levelEightyEightSets() {
        assertEquals(codes("BU", "SL", "TR", "FE"),
                Arrays.stream(TransactionType.values()).map(TransactionType::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        assertEquals(codes("P", "D", "F", "R"),
                Arrays.stream(TransactionStatus.values()).map(TransactionStatus::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        assertEquals(codes("A", "C", "P"),
                Arrays.stream(PositionStatus.values()).map(PositionStatus::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        assertEquals(codes("I", "C", "T"),
                Arrays.stream(ClientType.values()).map(ClientType::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        assertEquals(codes("A", "C", "S"),
                Arrays.stream(PortfolioStatus.values()).map(PortfolioStatus::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        assertEquals(codes("TRAN", "USER", "SYST"),
                Arrays.stream(AuditType.values()).map(AuditType::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        assertEquals(codes("SUCC", "FAIL", "WARN"),
                Arrays.stream(AuditStatus.values()).map(AuditStatus::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        // AUD-ACTION values are spelled to eight bytes in the copybook
        assertEquals(codes("CREATE  ", "UPDATE  ", "DELETE  ", "INQUIRE ", "LOGIN   ",
                        "LOGOUT  ", "STARTUP ", "SHUTDOWN"),
                Arrays.stream(AuditAction.values()).map(AuditAction::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));

        // values outside the level-88 sets interpret as absent, as the raw byte survives
        assertNull(TransactionType.fromCode("XX"));
        assertNull(TransactionStatus.fromCode("X"));
        assertNull(PositionStatus.fromCode("X"));
        assertNull(ClientType.fromCode("X"));
        assertNull(PortfolioStatus.fromCode("I"));
        assertNull(AuditType.fromCode("XXXX"));
        assertNull(AuditStatus.fromCode("XXXX"));
        assertNull(AuditAction.fromCode("XXXXXXXX"));
        // an unpadded 6-byte 'CREATE' pads to the copybook's 'CREATE  ' and therefore matches
        assertEquals(AuditAction.CREATE, AuditAction.fromCode("CREATE"));
    }

    private static Set<String> codes(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
