package com.portfolio.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BatchExceptions hierarchy.
 * Verifies RC 0/4/8/12 mapping from COBOL ERRHAND copybook.
 */
class BatchExceptionsTest {

    @Test
    void testReturnCodeSuccess() {
        RuntimeException ex = BatchExceptions.fromReturnCode(BatchExceptions.RC_SUCCESS, "OK");
        assertThat(ex).isNull();
    }

    @Test
    void testReturnCodeWarning() {
        RuntimeException ex = BatchExceptions.fromReturnCode(BatchExceptions.RC_WARNING, "Warning");
        assertThat(ex).isInstanceOf(BatchExceptions.BatchWarningException.class);
        assertThat(ex.getMessage()).isEqualTo("Warning");
    }

    @Test
    void testReturnCodeError() {
        RuntimeException ex = BatchExceptions.fromReturnCode(BatchExceptions.RC_ERROR, "Error");
        assertThat(ex).isInstanceOf(BatchExceptions.BatchErrorException.class);
        assertThat(ex.getMessage()).isEqualTo("Error");
    }

    @Test
    void testReturnCodeSevere() {
        RuntimeException ex = BatchExceptions.fromReturnCode(BatchExceptions.RC_SEVERE, "Severe");
        assertThat(ex).isInstanceOf(BatchExceptions.BatchSevereException.class);
        assertThat(ex.getMessage()).isEqualTo("Severe");
    }

    @Test
    void testReturnCodeTerminal() {
        RuntimeException ex = BatchExceptions.fromReturnCode(BatchExceptions.RC_TERMINAL, "Terminal");
        assertThat(ex).isInstanceOf(BatchExceptions.BatchTerminalException.class);
    }

    @Test
    void testToReturnCodeMapping() {
        assertThat(BatchExceptions.toReturnCode(new BatchExceptions.BatchWarningException("w")))
                .isEqualTo(BatchExceptions.RC_WARNING);
        assertThat(BatchExceptions.toReturnCode(new BatchExceptions.BatchErrorException("e")))
                .isEqualTo(BatchExceptions.RC_ERROR);
        assertThat(BatchExceptions.toReturnCode(new BatchExceptions.BatchSevereException("s")))
                .isEqualTo(BatchExceptions.RC_SEVERE);
        assertThat(BatchExceptions.toReturnCode(new BatchExceptions.BatchTerminalException("t")))
                .isEqualTo(BatchExceptions.RC_TERMINAL);
    }

    @Test
    void testWarningExceptionReturnCode() {
        BatchExceptions.BatchWarningException ex = new BatchExceptions.BatchWarningException("test");
        assertThat(ex.getReturnCode()).isEqualTo(4);
    }

    @Test
    void testErrorExceptionReturnCode() {
        BatchExceptions.BatchErrorException ex = new BatchExceptions.BatchErrorException("test");
        assertThat(ex.getReturnCode()).isEqualTo(8);
    }

    @Test
    void testSevereExceptionReturnCode() {
        BatchExceptions.BatchSevereException ex = new BatchExceptions.BatchSevereException("test");
        assertThat(ex.getReturnCode()).isEqualTo(12);
    }

    @Test
    void testReturnCodeConstants() {
        assertThat(BatchExceptions.RC_SUCCESS).isEqualTo(0);
        assertThat(BatchExceptions.RC_WARNING).isEqualTo(4);
        assertThat(BatchExceptions.RC_ERROR).isEqualTo(8);
        assertThat(BatchExceptions.RC_SEVERE).isEqualTo(12);
        assertThat(BatchExceptions.RC_TERMINAL).isEqualTo(16);
    }
}
