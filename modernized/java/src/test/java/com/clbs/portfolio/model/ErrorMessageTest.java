package com.clbs.portfolio.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ERRHAND.cpy: the ERR-TEXT flag semantics PORTTRAN's validation is built on. */
class ErrorMessageTest {

    @Test
    @DisplayName("a fresh area is spaces, which is how validation reads 'no error yet'")
    void freshAreaIsSpaces() {
        ErrorMessage error = new ErrorMessage();
        assertTrue(error.isErrTextSpaces());
        assertEquals(ErrorMessage.TEXT_LENGTH, error.getErrText().length());
        assertEquals("", error.getErrTextTrimmed());
    }

    @Test
    @DisplayName("ERR-TEXT is padded to 80 bytes and truncated beyond it")
    void errTextIsFixedWidth() {
        ErrorMessage error = new ErrorMessage();
        error.setErrText("Portfolio ID is required");

        assertEquals(80, error.getErrText().length());
        assertEquals("Portfolio ID is required", error.getErrTextTrimmed());
        assertFalse(error.isErrTextSpaces());

        error.setErrText(new String(new char[100]).replace('\0', 'x'));
        assertEquals(80, error.getErrText().length());
    }

    @Test
    @DisplayName("MOVE SPACES TO ERR-TEXT clears the flag again")
    void clearErrText() {
        ErrorMessage error = new ErrorMessage();
        error.setErrText("Invalid Transaction Type: XX");
        error.clearErrText();
        assertTrue(error.isErrTextSpaces());
    }

    @Test
    @DisplayName("category, code and severity map to the copybook constants")
    void codedFields() {
        ErrorMessage error = new ErrorMessage();
        error.setErrCategory(ErrorCategory.PROCESSING);
        error.setErrSeverity(ErrorSeverity.ERROR);

        assertEquals("PR", error.getErrCategory());
        assertEquals(8, error.getErrSeverity());
        assertEquals(ErrorSeverity.ERROR, error.getErrorSeverity());

        // PORTTRAN never sets ERR-CODE, so it stays blank and maps to no catalogued code.
        assertTrue(CobolText.isSpaces(error.getErrCode()));
        assertNull(ErrorCode.fromCode(error.getErrCode()));

        error.setErrCode(ErrorCode.INVALID_TRANSACTION_TYPE);
        assertEquals("E003", error.getErrCode());
    }

    @Test
    @DisplayName("ERR-TIMESTAMP is the 18 bytes of date and time")
    void timestampLayout() {
        ErrorMessage error = new ErrorMessage();
        error.setErrDate("2024-03-20");
        error.setErrTime("15.30.45");
        assertEquals("2024-03-2015.30.45", error.getErrTimestamp());
    }

    @Test
    @DisplayName("copying preserves an error the shared area is about to overwrite")
    void copyIsIndependent() {
        ErrorMessage error = new ErrorMessage();
        error.setErrText("Insufficient units for sale");
        ErrorMessage snapshot = new ErrorMessage(error);

        error.setErrText("Error updating portfolio");

        assertEquals("Insufficient units for sale", snapshot.getErrTextTrimmed());
        assertEquals("Error updating portfolio", error.getErrTextTrimmed());
    }

    @Test
    @DisplayName("the copybook names four VSAM statuses and one fallback message")
    void vsamStatuses() {
        assertEquals("Record not found", VsamStatus.messageFor("23"));
        assertEquals("Duplicate record key", VsamStatus.messageFor("22"));
        assertEquals(VsamStatus.END_OF_FILE, VsamStatus.fromCode("10"));
        assertEquals(VsamStatus.OTHER_MESSAGE, VsamStatus.messageFor("37"));
        assertNull(VsamStatus.fromCode("37"));
    }
}
