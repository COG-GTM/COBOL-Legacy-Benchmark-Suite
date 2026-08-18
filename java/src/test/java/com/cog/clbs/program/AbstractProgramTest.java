package com.cog.clbs.program;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractProgramTest {

    static class TestProgram extends AbstractProgram {
        boolean initialized;
        boolean terminated;
        int iterations;

        TestProgram() {
            super("TESTPROG");
        }

        @Override
        protected void initialize() {
            initialized = true;
        }

        @Override
        protected void execute() {
            iterations++;
            setEndOfFile();
        }

        @Override
        protected void terminate() {
            terminated = true;
        }
    }

    @Test
    void runsLifecycleAndReturnsSuccess() {
        TestProgram p = new TestProgram();
        int rc = p.run();
        assertTrue(p.initialized);
        assertTrue(p.terminated);
        assertEquals(1, p.iterations);
        assertEquals(ReturnCode.SUCCESS.getCode(), rc);
        assertEquals("TESTPROG", p.getProgramName());
    }

    @Test
    void unhandledExceptionSetsErrorReturnCode() {
        TestProgram p = new TestProgram() {
            @Override
            protected void execute() {
                throw new RuntimeException("boom");
            }
        };
        assertEquals(ReturnCode.ERROR.getCode(), p.run());
        assertTrue(p.terminated);
    }

    @Test
    void returnCodeNeverLowers() {
        TestProgram p = new TestProgram() {
            @Override
            protected void execute() {
                setReturnCode(ReturnCode.SEVERE.getCode());
                setReturnCode(ReturnCode.WARNING.getCode());
                setEndOfFile();
            }
        };
        assertEquals(ReturnCode.SEVERE.getCode(), p.run());
    }

    @Test
    void returnCodeEnumMapsRc0To16() {
        assertEquals(0, ReturnCode.SUCCESS.getCode());
        assertEquals(4, ReturnCode.WARNING.getCode());
        assertEquals(8, ReturnCode.ERROR.getCode());
        assertEquals(12, ReturnCode.SEVERE.getCode());
        assertEquals(16, ReturnCode.CRITICAL.getCode());
        assertEquals(ReturnCode.SEVERE, ReturnCode.WARNING.max(ReturnCode.SEVERE));
        assertEquals(ReturnCode.ERROR, ReturnCode.fromCode(8));
    }
}
