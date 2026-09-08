package com.clbs.testdata;

import com.clbs.common.Inputs;
import com.clbs.common.ProgramResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * TSTVAL00 — test result validation.
 *
 * <p>The COBOL source defines the TEST-CASE-RECORD layout, the
 * FUNCTIONAL/INTEGRATE/PERFORM/ERROR dispatch, the WS-TEST-DETAIL and WS-SUMMARY-LINE report lines
 * and the success-rate computation. The paragraphs that run a test (2200-2500) and
 * 2600-VALIDATE-RESULTS are PERFORMed but never defined, so validation here is the comparison of
 * the supplied EXPECTED-RECORD and ACTUAL-RECORD values that those files carry.
 */
@Service
public class TestValidationProgram {

    public static final String PROGRAM = "TSTVAL00";

    public static final String FUNCTIONAL = "FUNCTIONAL";
    public static final String INTEGRATION = "INTEGRATE";
    public static final String PERFORMANCE = "PERFORM";
    public static final String ERROR = "ERROR";
    public static final String ERR_INVALID_TYPE = "INVALID TEST TYPE";

    /** TEST-CASE-RECORD plus its EXPECTED-RECORD / ACTUAL-RECORD pair. */
    public record TestCase(String id, String type, String description, String expected,
            String actual) {
    }

    /** 2000-PROCESS. */
    public ProgramResult run(List<TestCase> cases) {
        ProgramResult result = new ProgramResult(PROGRAM);
        long total = 0;
        long passed = 0;
        long failed = 0;

        for (TestCase testCase : Inputs.records(cases)) {
            String type = testCase.type() == null ? "" : testCase.type().trim();
            if (!List.of(FUNCTIONAL, INTEGRATION, PERFORMANCE, ERROR).contains(type)) {
                result.display(ERR_INVALID_TYPE);
                result.setReturnCode(12);
                return result;
            }

            total++;
            boolean ok = String.valueOf(testCase.expected())
                    .equals(String.valueOf(testCase.actual()));
            if (ok) {
                passed++;
            } else {
                failed++;
            }
            result.display(String.format("%-10s  %-10s  %-50s  %-4s", testCase.id(), type,
                    testCase.description(), ok ? "PASS" : "FAIL"));
        }

        result.display(String.format("TOTAL TESTS:   %5d  PASSED:   %5d  FAILED:   %5d"
                + "  SUCCESS:   %6s", total, passed, failed, successRate(passed, total)));
        result.count("total", total).count("passed", passed).count("failed", failed);
        if (failed > 0) {
            result.setReturnCode(4);
        }
        return result;
    }

    /** 2900-WRITE-SUMMARY: (WS-TESTS-PASSED / WS-TOTAL-TESTS) * 100, guarded against no tests. */
    static BigDecimal successRate(long passed, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(passed)
                .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
