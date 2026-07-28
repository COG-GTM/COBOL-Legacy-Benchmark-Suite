package com.clbs.portfolio.model;

import com.clbs.portfolio.harness.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TRNREC.cpy: field widths, packed scales and the level-88 conditions on TRN-TYPE / TRN-STATUS. */
class TransactionRecordTest {

    @Test
    @DisplayName("TRN-KEY is the 28 bytes of date, time, portfolio id and sequence number")
    void keyLayout() {
        TransactionRecord transaction = TestData.buyTransaction();
        assertEquals("20240320153045PORT0001000001", transaction.getTrnKey());
        assertEquals(28, transaction.getTrnKey().length());
    }

    @Test
    @DisplayName("quantity and price hold four decimals, amount holds two")
    void packedScales() {
        TransactionRecord transaction = new TransactionRecord();
        transaction.setTrnQuantity(new BigDecimal("100.123456"));
        transaction.setTrnPrice(new BigDecimal("125.5"));
        transaction.setTrnAmount(new BigDecimal("12500.999"));

        assertEquals(new BigDecimal("100.1234"), transaction.getTrnQuantity());
        assertEquals(new BigDecimal("125.5000"), transaction.getTrnPrice());
        assertEquals(new BigDecimal("12500.99"), transaction.getTrnAmount());
    }

    @Test
    @DisplayName("an unset record reads as zeros and spaces, never null")
    void initialState() {
        TransactionRecord transaction = new TransactionRecord();
        assertEquals(CobolDecimal.ZERO_QUANTITY, transaction.getTrnQuantity());
        assertEquals(CobolDecimal.ZERO_AMOUNT, transaction.getTrnAmount());
        assertTrue(CobolText.isSpaces(transaction.getTrnType()));
        assertNull(transaction.getTransactionType());
    }

    @Test
    @DisplayName("the four level-88 transaction types map to their codes")
    void transactionTypes() {
        assertEquals("BU", TransactionType.BUY.code());
        assertEquals("SL", TransactionType.SELL.code());
        assertEquals("TR", TransactionType.TRANSFER.code());
        assertEquals("FE", TransactionType.FEE.code());

        TransactionRecord transaction = TestData.buyTransaction();
        assertTrue(transaction.isTrnTypeBuy());
        assertFalse(transaction.isTrnTypeSell());
        assertEquals(TransactionType.BUY, transaction.getTransactionType());
    }

    @Test
    @DisplayName("an unrecognised type byte is kept verbatim for the validation message")
    void unrecognisedTypeIsKept() {
        TransactionRecord transaction = TestData.transactionWithRawType("XX");
        assertEquals("XX", transaction.getTrnType());
        assertNull(transaction.getTransactionType());
        assertFalse(TransactionType.isValidCode("XX"));
        assertFalse(transaction.isTrnTypeBuy());

        // The documented single-character codes are not valid in the two-byte field.
        assertFalse(TransactionType.isValidCode("B"));
        assertFalse(TransactionType.isValidCode("S"));
    }

    @Test
    @DisplayName("the level-88 statuses map to their codes")
    void transactionStatuses() {
        TransactionRecord transaction = TestData.buyTransaction();
        assertEquals(TransactionStatus.PENDING, transaction.getTransactionStatus());

        transaction.setTrnStatus(TransactionStatus.FAILED);
        assertEquals("F", transaction.getTrnStatus());
        assertEquals(TransactionStatus.FAILED, transaction.getTransactionStatus());

        transaction.setTrnStatus("Z");
        assertNull(transaction.getTransactionStatus());
    }

    @Test
    @DisplayName("copying detaches a record from the shared read area")
    void copyIsIndependent() {
        TransactionRecord original = TestData.buyTransaction();
        TransactionRecord copy = new TransactionRecord(original);
        original.setTrnAmount("1.00");

        assertNotSame(original, copy);
        assertEquals(new BigDecimal("12500.00"), copy.getTrnAmount());
    }
}
