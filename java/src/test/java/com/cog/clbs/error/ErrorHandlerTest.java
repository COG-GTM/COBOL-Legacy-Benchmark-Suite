package com.cog.clbs.error;

import com.cog.clbs.program.ReturnCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorHandlerTest {

    @Test
    void cleanRunReturnsSuccess() {
        ErrorHandler handler = new ErrorHandler();
        assertEquals(ReturnCode.SUCCESS, handler.finalReturnCode());
        assertFalse(handler.isAbendRequested());
        assertTrue(handler.getErrorLog().isEmpty());
    }

    @Test
    void warningYieldsRc4AndContinueAction() {
        ErrorHandler handler = new ErrorHandler();
        ErrorRecord record = new ErrorRecord("TESTPGM", "2000-PROCESS", 0,
                ErrorSeverity.WARNING, "REQUIRED FIELD IS MISSING");
        ErrorAction action = handler.handle(record);
        assertEquals(ErrorAction.CONTINUE, action);
        assertEquals(ReturnCode.WARNING, handler.finalReturnCode());
        assertEquals(1, handler.getErrorLog().size());
    }

    @Test
    void fatalYieldsSevereRcAndAbendAction() {
        ErrorHandler handler = new ErrorHandler();
        ErrorRecord record = new ErrorRecord("TESTPGM", "2100-READ-VSAM", -803,
                ErrorSeverity.FATAL, "FILE OPERATION FAILED");
        assertEquals(ErrorAction.ABEND, handler.handle(record));
        assertEquals(ReturnCode.SEVERE, handler.finalReturnCode());
        assertTrue(handler.isAbendRequested());
    }

    @Test
    void recordedErrorsYieldRc8() {
        ErrorHandler handler = new ErrorHandler();
        handler.recordError();
        assertEquals(ReturnCode.ERROR, handler.finalReturnCode());
    }

    @Test
    void errorRecordMirrorsCopybookFields() {
        ErrorRecord record = new ErrorRecord("PGMNAME", "P200-LOG-ERROR", 100,
                ErrorSeverity.INFO, "row not found");
        assertEquals("PGMNAME", record.getProgram());
        assertEquals("P200-LOG-ERROR", record.getParagraph());
        assertEquals(100, record.getSqlCode());
        assertEquals(ErrorSeverity.INFO, record.getSeverity());
        assertNotNull(record.getTraceId());
        assertEquals(16, record.getTraceId().length());
        assertNotNull(record.getTimestamp());
    }

    @Test
    void formatsMessageWithProgramAndTraceId() {
        ErrorHandler handler = new ErrorHandler();
        ErrorRecord record = new ErrorRecord("PGM1", "P300", 0,
                ErrorSeverity.INFO, "something happened");
        handler.handle(record);
        assertTrue(record.getMessage().startsWith("Error in PGM1 - something happened ("));
        assertTrue(record.getMessage().endsWith(")"));
    }

    @Test
    void severityAndActionCodesRoundTrip() {
        assertEquals(ErrorSeverity.FATAL, ErrorSeverity.fromCode('F'));
        assertEquals('W', ErrorSeverity.WARNING.getCode());
        assertEquals(ErrorAction.ABEND, ErrorAction.fromCode('A'));
        assertEquals('R', ErrorAction.RETURN.getCode());
    }
}
