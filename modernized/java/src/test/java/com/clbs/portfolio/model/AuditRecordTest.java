package com.clbs.portfolio.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AUDITLOG.cpy: the audit buffer PORTTRAN rebuilds for every transaction. */
class AuditRecordTest {

    @Test
    @DisplayName("INITIALIZE returns every field to spaces")
    void initialize() {
        AuditRecord audit = new AuditRecord();
        audit.setAudMessage("Transaction: BU");
        audit.setAudStatus(AuditStatus.SUCCESS);
        audit.initialize();

        assertTrue(CobolText.isSpaces(audit.getAudMessage()));
        assertTrue(CobolText.isSpaces(audit.getAudStatus()));
        assertNull(audit.getAuditStatus());
    }

    @Test
    @DisplayName("AUD-HEADER is 58 bytes and AUD-KEY-INFO 18")
    void groupLayouts() {
        AuditRecord audit = new AuditRecord();
        assertEquals(58, audit.getAudHeader().length());
        assertEquals(18, audit.getAudKeyInfo().length());
    }

    @Test
    @DisplayName("actions are stored padded to the eight bytes the copybook spells out")
    void actionsArePadded() {
        AuditRecord audit = new AuditRecord();
        audit.setAudAction(AuditAction.CREATE);

        assertEquals("CREATE  ", audit.getAudAction());
        assertEquals(AuditAction.CREATE, audit.getAuditAction());
        assertEquals("SHUTDOWN", AuditAction.SHUTDOWN.code());
        assertEquals(AuditAction.UPDATE, AuditAction.fromCode("UPDATE  "));
    }

    @Test
    @DisplayName("type and status map the level-88 values PORTTRAN writes")
    void typeAndStatus() {
        AuditRecord audit = new AuditRecord();
        audit.setAudType(AuditType.TRANSACTION);
        audit.setAudStatus(AuditStatus.FAILURE);

        assertEquals("TRAN", audit.getAudType());
        assertEquals(AuditStatus.FAILURE, audit.getAuditStatus());
        assertNull(AuditStatus.fromCode("OKAY"));
    }

    @Test
    @DisplayName("images and message are fixed at 100 bytes")
    void fixedWidthImages() {
        AuditRecord audit = new AuditRecord();
        audit.setAudBeforeImage(CobolText.picX("PORT0001", 8));
        audit.setAudMessage("Transaction: BU Amount: +000000001250000 Units: +0001000000000");

        assertEquals(100, audit.getAudBeforeImage().length());
        assertEquals(100, audit.getAudMessage().length());
        assertEquals("Transaction: BU Amount: +000000001250000 Units: +0001000000000",
                audit.getAudMessageTrimmed());
    }

    @Test
    @DisplayName("copying preserves a record the shared area is about to overwrite")
    void copyIsIndependent() {
        AuditRecord audit = new AuditRecord();
        audit.setAudMessage("first");
        AuditRecord snapshot = new AuditRecord(audit);
        audit.setAudMessage("second");

        assertEquals("first", snapshot.getAudMessageTrimmed());
    }
}
