package com.cobolbenchmark.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReturnCodeService - from RTNCDE00.
 * Tests return code classification logic:
 * 0=SUCCESS, 1-4=WARNING, 5-8=ERROR, other=SEVERE.
 */
class ReturnCodeServiceTest {

    private ReturnCodeService returnCodeService;

    @BeforeEach
    void setUp() {
        returnCodeService = new ReturnCodeService();
    }

    @Test
    void testInitialize() {
        returnCodeService.setCode(5, "TEST");
        returnCodeService.initialize();
        assertEquals(0, returnCodeService.getCode());
        assertEquals(0, returnCodeService.getHighestCode());
    }

    @Test
    void testSetAndGetCode() {
        returnCodeService.setCode(4, "OPERATION1");
        assertEquals(4, returnCodeService.getCode());
    }

    @Test
    void testHighestCodeTracking() {
        returnCodeService.setCode(4, "OP1");
        returnCodeService.setCode(2, "OP2");
        returnCodeService.setCode(8, "OP3");
        returnCodeService.setCode(1, "OP4");

        assertEquals(1, returnCodeService.getCode()); // Current
        assertEquals(8, returnCodeService.getHighestCode()); // Highest
    }

    @Test
    void testClassifySuccess() {
        assertEquals(BatchConstants.ReturnCode.SUCCESS, returnCodeService.classify(0));
    }

    @Test
    void testClassifyWarning() {
        assertEquals(BatchConstants.ReturnCode.WARNING, returnCodeService.classify(1));
        assertEquals(BatchConstants.ReturnCode.WARNING, returnCodeService.classify(2));
        assertEquals(BatchConstants.ReturnCode.WARNING, returnCodeService.classify(3));
        assertEquals(BatchConstants.ReturnCode.WARNING, returnCodeService.classify(4));
    }

    @Test
    void testClassifyError() {
        assertEquals(BatchConstants.ReturnCode.ERROR, returnCodeService.classify(5));
        assertEquals(BatchConstants.ReturnCode.ERROR, returnCodeService.classify(6));
        assertEquals(BatchConstants.ReturnCode.ERROR, returnCodeService.classify(7));
        assertEquals(BatchConstants.ReturnCode.ERROR, returnCodeService.classify(8));
    }

    @Test
    void testClassifySevere() {
        assertEquals(BatchConstants.ReturnCode.SEVERE, returnCodeService.classify(9));
        assertEquals(BatchConstants.ReturnCode.SEVERE, returnCodeService.classify(10));
        assertEquals(BatchConstants.ReturnCode.SEVERE, returnCodeService.classify(11));
        assertEquals(BatchConstants.ReturnCode.SEVERE, returnCodeService.classify(12));
    }

    @Test
    void testClassifyCritical() {
        assertEquals(BatchConstants.ReturnCode.CRITICAL, returnCodeService.classify(13));
        assertEquals(BatchConstants.ReturnCode.CRITICAL, returnCodeService.classify(16));
        assertEquals(BatchConstants.ReturnCode.CRITICAL, returnCodeService.classify(99));
    }

    @Test
    void testAnalyze() {
        returnCodeService.setCode(0, "OP1");
        returnCodeService.setCode(4, "OP2");
        returnCodeService.setCode(2, "OP3");

        BatchConstants.ReturnCode result = returnCodeService.analyze();
        assertEquals(BatchConstants.ReturnCode.WARNING, result); // Highest is 4
    }
}
