package com.clbs.portfolio.harness;

import com.clbs.portfolio.model.AuditRecord;
import com.clbs.portfolio.model.AuditStatus;
import com.clbs.portfolio.model.ErrorMessage;
import com.clbs.portfolio.model.ErrorSeverity;
import com.clbs.portfolio.service.AuditProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The subroutine doubles the translated programs are tested against. */
class RecordingProcessorsTest {

    @Test
    @DisplayName("the audit double snapshots each record, so a reused buffer is not aliased")
    void auditDoubleSnapshots() {
        RecordingAuditProcessor audit = new RecordingAuditProcessor();
        AuditRecord buffer = new AuditRecord();

        buffer.setAudMessage("Transaction: BU");
        buffer.setAudStatus(AuditStatus.SUCCESS);
        assertEquals(AuditProcessor.RETURN_SUCCESS, audit.process(buffer));

        buffer.initialize();
        buffer.setAudMessage("Transaction: SL");
        buffer.setAudStatus(AuditStatus.FAILURE);
        audit.process(buffer);

        assertEquals(2, audit.count());
        assertEquals("Transaction: BU", audit.records().get(0).getAudMessageTrimmed());
        assertEquals(AuditStatus.SUCCESS, audit.records().get(0).getAuditStatus());
        assertEquals("Transaction: SL", audit.last().getAudMessageTrimmed());
    }

    @Test
    @DisplayName("the audit double can report the write failure branch")
    void auditDoubleCanFail() {
        RecordingAuditProcessor audit = new RecordingAuditProcessor().failing();
        assertEquals(AuditProcessor.RETURN_ERROR, audit.process(new AuditRecord()));
    }

    @Test
    @DisplayName("the error double snapshots each message and returns its severity")
    void errorDoubleSnapshots() {
        RecordingErrorProcessor errors = new RecordingErrorProcessor();
        ErrorMessage buffer = new ErrorMessage();

        buffer.setErrText("Portfolio ID is required");
        buffer.setErrSeverity(ErrorSeverity.ERROR);
        assertEquals(8, errors.process(buffer));

        buffer.setErrText("Invalid Transaction Type: XX");
        errors.process(buffer);

        assertEquals(2, errors.count());
        assertEquals(Arrays.asList("Portfolio ID is required", "Invalid Transaction Type: XX"),
                errors.messages());
        assertEquals("Invalid Transaction Type: XX", errors.lastMessage());
    }
}
