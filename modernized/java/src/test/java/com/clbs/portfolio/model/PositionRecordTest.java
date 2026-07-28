package com.clbs.portfolio.model;

import com.clbs.portfolio.harness.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** POSREC.cpy: the composite key, packed scales and the position status conditions. */
class PositionRecordTest {

    @Test
    @DisplayName("POS-KEY is portfolio id, position date and investment id")
    void keyLayout() {
        PositionRecord position = TestData.growthPosition();
        assertEquals("PORT000120240320IBM0000001", position.getPosKey());
        assertEquals(26, position.getPosKey().length());
    }

    @Test
    @DisplayName("quantity holds four decimals, cost basis and market value hold two")
    void packedScales() {
        PositionRecord position = TestData.growthPosition();
        assertEquals(4, position.getPosQuantity().scale());
        assertEquals(2, position.getPosCostBasis().scale());
        assertEquals(2, position.getPosMarketValue().scale());
        assertEquals(new BigDecimal("10000000.00"), position.getPosCostBasis());
    }

    @Test
    @DisplayName("the level-88 statuses are A, C and P")
    void statuses() {
        PositionRecord position = TestData.growthPosition();
        assertTrue(position.isPosStatusActive());
        assertFalse(position.isPosStatusClosed());

        position.setPosStatus(PositionStatus.CLOSED);
        assertEquals(PositionStatus.CLOSED, position.getPositionStatus());

        position.setPosStatus("X");
        assertNull(position.getPositionStatus());
    }
}
