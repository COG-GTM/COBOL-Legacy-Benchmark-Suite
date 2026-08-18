package com.cog.clbs.db;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Db2HandlerTest {

    @Test
    void instantiatesDisconnected() {
        try (Db2Handler handler = new Db2Handler()) {
            assertFalse(handler.isConnected());
            assertThrows(IllegalStateException.class, handler::commit);
            assertThrows(IllegalStateException.class, handler::rollback);
            assertThrows(IllegalStateException.class, () -> handler.prepare("SELECT 1"));
        }
    }

    @Test
    void sqlCodeExceptionCarriesSqlCode() {
        SQLException cause = new SQLException("duplicate key", "23505", -803);
        SqlCodeException e = SqlCodeException.from(cause);
        assertEquals(-803, e.getSqlCode());
        assertTrue(e.getMessage().contains("SQLCODE: -803"));
        assertEquals(cause, e.getCause());
    }

    @Test
    void sqlCodeConstantsMatchDb2Conventions() {
        assertEquals(100, SqlCodeException.SQLCODE_NOT_FOUND);
        assertEquals(-803, SqlCodeException.SQLCODE_DUPLICATE_KEY);
    }
}
