package com.ipms.common.fixedwidth;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixedWidthTest {

    @Test
    void roundTripsStringsAndDecimals() {
        String record = new FixedWidthWriter(8 + 16 + 10)
                .string("PORT0001", 8)
                .signedDecimal(new BigDecimal("-12345.6789"), 11, 4)
                .unsignedDecimal(new BigDecimal("20240320"), 8, 0)
                .filler(2)
                .toRecord();

        FixedWidthReader r = new FixedWidthReader(record);
        assertEquals("PORT0001", r.string(8));
        assertEquals(new BigDecimal("-12345.6789"), r.signedDecimal(11, 4));
        assertEquals(new BigDecimal("20240320"), r.unsignedDecimal(8, 0));
    }

    @Test
    void padsAndTruncatesStrings() {
        String record = new FixedWidthWriter(5).string("AB", 5).toRecord();
        assertEquals("AB   ", record);
        assertEquals("AB", new FixedWidthReader(record).string(5));

        assertEquals("ABCDE", new FixedWidthWriter(5).string("ABCDEFG", 5).toRecord());
    }

    @Test
    void zeroFillsDecimals() {
        String record = new FixedWidthWriter(16)
                .signedDecimal(new BigDecimal("1.5"), 11, 4)
                .toRecord();
        assertEquals("+000000000015000", record);
        assertEquals(new BigDecimal("1.5000"), new FixedWidthReader(record).signedDecimal(11, 4));
    }

    @Test
    void rejectsOverflowingValues() {
        assertThrows(FixedWidthException.class,
                () -> new FixedWidthWriter(4).unsignedDecimal(new BigDecimal("12345"), 4, 0));
        assertThrows(FixedWidthException.class,
                () -> new FixedWidthWriter(4).unsignedDecimal(new BigDecimal("-1"), 4, 0));
    }

    @Test
    void rejectsShortRecords() {
        assertThrows(FixedWidthException.class, () -> new FixedWidthReader("ABC").string(5));
    }

    @Test
    void rejectsLengthMismatchOnWrite() {
        assertThrows(FixedWidthException.class,
                () -> new FixedWidthWriter(10).string("AB", 5).toRecord());
    }
}
