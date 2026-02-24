package com.investment.portfolio.test;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Test Validator (TSTVAL00) - Java equivalent of TSTVAL00.cbl
 *
 * Original COBOL: src/programs/test/TSTVAL00.cbl
 *
 * Responsibilities:
 * - Validates test results against expected outcomes
 * - Executes functional, integration, performance, and error test suites
 * - Compares actual results to expected results
 * - Generates test validation reports with pass/fail metrics
 *
 * Test types (from WS-TEST-TYPE):
 * - FUNCTIONAL: Individual function/unit tests
 * - INTEGRATE:  Integration tests across components
 * - PERFORM:    Performance benchmark tests
 * - ERROR:      Error handling/negative tests
 *
 * Files:
 * - TEST-CASES        (input):  Test case definitions
 * - EXPECTED-RESULTS  (input):  Expected test outcomes
 * - ACTUAL-RESULTS    (input):  Actual test outcomes
 * - TEST-REPORT       (output): Test validation report
 *
 * Metrics:
 * - Total tests, passed, failed
 * - Success rate percentage
 * - Elapsed time for test suite
 */
public class TestValidator {

    private static final Logger LOGGER = Logger.getLogger(TestValidator.class.getName());
    private static final String PROGRAM_ID = "TSTVAL00";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_WIDTH = 120;

    /** Test types matching WS-TEST-TYPE */
    public enum TestType {
        FUNCTIONAL, INTEGRATE, PERFORM, ERROR
    }

    private final Path testCasesPath;
    private final Path expectedResultsPath;
    private final Path actualResultsPath;
    private final Path testReportPath;

    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;

    /** Test metrics - maps to WS-TEST-METRICS in COBOL */
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private long elapsedTimeMs;
    private final List<TestResult> testResults;

    public TestValidator(Path testCasesPath, Path expectedResultsPath,
                         Path actualResultsPath, Path testReportPath) {
        this.testCasesPath = testCasesPath;
        this.expectedResultsPath = expectedResultsPath;
        this.actualResultsPath = actualResultsPath;
        this.testReportPath = testReportPath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
        this.testResults = new ArrayList<>();
    }

    /**
     * Main entry point - maps to COBOL 0000-MAIN.
     *
     * PERFORM 1000-INITIALIZE
     * EVALUATE WS-TEST-TYPE
     *   WHEN 'FUNCTIONAL' PERFORM 2000-RUN-FUNCTIONAL
     *   WHEN 'INTEGRATE'  PERFORM 3000-RUN-INTEGRATION
     *   WHEN 'PERFORM'    PERFORM 4000-RUN-PERFORMANCE
     *   WHEN 'ERROR'      PERFORM 5000-RUN-ERROR-TESTS
     * END-EVALUATE
     * PERFORM 6000-GENERATE-REPORT
     * PERFORM 9000-TERMINATE
     */
    public int execute(TestType testType) {
        LOGGER.info(PROGRAM_ID + " - Test Validation starting: " + testType);

        Instant startTime = Instant.now();

        try {
            initialize();

            switch (testType) {
                case FUNCTIONAL:
                    runFunctionalTests();
                    break;
                case INTEGRATE:
                    runIntegrationTests();
                    break;
                case PERFORM:
                    runPerformanceTests();
                    break;
                case ERROR:
                    runErrorTests();
                    break;
            }

            elapsedTimeMs = Duration.between(startTime, Instant.now()).toMillis();
            generateReport();
            terminate();
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE: Reset test metrics.
     */
    private void initialize() {
        totalTests = 0;
        passedTests = 0;
        failedTests = 0;
        elapsedTimeMs = 0;
        testResults.clear();

        LOGGER.info(PROGRAM_ID + " - Initialization complete");
    }

    /**
     * 2000-RUN-FUNCTIONAL: Execute functional/unit tests.
     *
     * Maps to:
     *   OPEN INPUT TEST-CASES
     *   OPEN INPUT EXPECTED-RESULTS
     *   OPEN INPUT ACTUAL-RESULTS
     *   PERFORM UNTIL WS-EOF-TESTS
     *     READ TEST-CASES
     *     READ EXPECTED-RESULTS
     *     READ ACTUAL-RESULTS
     *     PERFORM 2100-COMPARE-RESULTS
     *   END-PERFORM
     */
    private void runFunctionalTests() {
        LOGGER.info("Running functional tests");
        compareExpectedVsActual("FUNCTIONAL");
    }

    /**
     * 3000-RUN-INTEGRATION: Execute integration tests.
     */
    private void runIntegrationTests() {
        LOGGER.info("Running integration tests");
        compareExpectedVsActual("INTEGRATION");
    }

    /**
     * 4000-RUN-PERFORMANCE: Execute performance benchmark tests.
     *
     * Performance tests compare actual elapsed time / throughput
     * against expected thresholds rather than exact value matching.
     */
    private void runPerformanceTests() {
        LOGGER.info("Running performance tests");

        try (FileHandler testFile = new FileHandler(testCasesPath);
             FileHandler actualFile = new FileHandler(actualResultsPath)) {

            if (!FileHandler.STATUS_SUCCESS.equals(testFile.openInput())
                    || !FileHandler.STATUS_SUCCESS.equals(actualFile.openInput())) {
                LOGGER.warning("Test files not available for performance tests");
                return;
            }

            String testLine;
            while ((testLine = testFile.readLine()) != null) {
                String actualLine = actualFile.readLine();
                totalTests++;

                if (testLine.length() < 20 || actualLine == null) {
                    recordResult("PERF-" + totalTests, "PERFORMANCE",
                            false, "Invalid test data or missing actual result");
                    continue;
                }

                // Parse performance test case
                // Format: TESTID|METRIC|THRESHOLD
                String[] testParts = testLine.split("\\|");
                String[] actualParts = actualLine.split("\\|");

                if (testParts.length >= 3 && actualParts.length >= 2) {
                    String testId = testParts[0].trim();
                    String metric = testParts[1].trim();
                    long threshold = Long.parseLong(testParts[2].trim());
                    long actualValue = Long.parseLong(actualParts[1].trim());

                    boolean passed = actualValue <= threshold;
                    String detail = String.format("%s: actual=%d threshold=%d",
                            metric, actualValue, threshold);
                    recordResult(testId, "PERFORMANCE", passed, detail);
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E400", "Error running performance tests", e);
        }
    }

    /**
     * 5000-RUN-ERROR-TESTS: Execute error handling/negative tests.
     *
     * Error tests verify that the system correctly rejects invalid
     * input and produces appropriate error messages.
     */
    private void runErrorTests() {
        LOGGER.info("Running error handling tests");

        try (FileHandler testFile = new FileHandler(testCasesPath);
             FileHandler expectedFile = new FileHandler(expectedResultsPath);
             FileHandler actualFile = new FileHandler(actualResultsPath)) {

            if (!FileHandler.STATUS_SUCCESS.equals(testFile.openInput())
                    || !FileHandler.STATUS_SUCCESS.equals(expectedFile.openInput())
                    || !FileHandler.STATUS_SUCCESS.equals(actualFile.openInput())) {
                LOGGER.warning("Test files not available for error tests");
                return;
            }

            String testLine;
            while ((testLine = testFile.readLine()) != null) {
                String expectedLine = expectedFile.readLine();
                String actualLine = actualFile.readLine();
                totalTests++;

                if (expectedLine == null || actualLine == null) {
                    recordResult("ERR-" + totalTests, "ERROR",
                            false, "Missing expected or actual result");
                    continue;
                }

                // For error tests, we check that the error was properly detected
                // Expected format: TESTID|ERROR_CODE|ERROR_MESSAGE
                String[] testParts = testLine.split("\\|");
                String[] expectedParts = expectedLine.split("\\|");
                String[] actualParts = actualLine.split("\\|");

                if (testParts.length >= 1 && expectedParts.length >= 2 && actualParts.length >= 2) {
                    String testId = testParts[0].trim();
                    String expectedCode = expectedParts[1].trim();
                    String actualCode = actualParts[1].trim();

                    boolean passed = expectedCode.equals(actualCode);
                    String detail = String.format("Expected error: %s Actual: %s",
                            expectedCode, actualCode);
                    recordResult(testId, "ERROR", passed, detail);
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E500", "Error running error tests", e);
        }
    }

    /**
     * Core comparison logic - reads test cases, expected, and actual results
     * and compares them line by line.
     *
     * Maps to 2100-COMPARE-RESULTS in COBOL:
     *   IF WS-EXPECTED-DATA = WS-ACTUAL-DATA
     *     ADD 1 TO WS-PASSED
     *   ELSE
     *     ADD 1 TO WS-FAILED
     *     PERFORM 2200-LOG-FAILURE
     *   END-IF
     */
    private void compareExpectedVsActual(String testCategory) {
        try (FileHandler testFile = new FileHandler(testCasesPath);
             FileHandler expectedFile = new FileHandler(expectedResultsPath);
             FileHandler actualFile = new FileHandler(actualResultsPath)) {

            if (!FileHandler.STATUS_SUCCESS.equals(testFile.openInput())
                    || !FileHandler.STATUS_SUCCESS.equals(expectedFile.openInput())
                    || !FileHandler.STATUS_SUCCESS.equals(actualFile.openInput())) {
                LOGGER.warning("Test files not available for " + testCategory);
                return;
            }

            String testLine;
            while ((testLine = testFile.readLine()) != null) {
                String expectedLine = expectedFile.readLine();
                String actualLine = actualFile.readLine();
                totalTests++;

                // Extract test ID from the test case line
                String testId;
                int sepIdx = testLine.indexOf('|');
                if (sepIdx > 0) {
                    testId = testLine.substring(0, sepIdx).trim();
                } else {
                    testId = testCategory + "-" + totalTests;
                }

                if (expectedLine == null) {
                    recordResult(testId, testCategory, false,
                            "Missing expected result");
                    continue;
                }

                if (actualLine == null) {
                    recordResult(testId, testCategory, false,
                            "Missing actual result");
                    continue;
                }

                // Compare expected vs actual
                boolean passed = expectedLine.trim().equals(actualLine.trim());
                String detail;
                if (passed) {
                    detail = "Match";
                } else {
                    detail = String.format("Expected: [%s] Actual: [%s]",
                            truncate(expectedLine.trim(), 40),
                            truncate(actualLine.trim(), 40));
                }

                recordResult(testId, testCategory, passed, detail);
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E200", "Error comparing results", e);
        }
    }

    /**
     * Records a test result.
     */
    private void recordResult(String testId, String category, boolean passed, String detail) {
        if (passed) {
            passedTests++;
        } else {
            failedTests++;
        }

        testResults.add(new TestResult(testId, category, passed, detail));
    }

    /**
     * 6000-GENERATE-REPORT: Write test validation report.
     *
     * Maps to:
     *   OPEN OUTPUT TEST-REPORT
     *   PERFORM 6100-PRINT-HEADER
     *   PERFORM 6200-PRINT-DETAILS
     *   PERFORM 6300-PRINT-SUMMARY
     *   CLOSE TEST-REPORT
     */
    private void generateReport() {
        try (FileHandler reportFile = new FileHandler(testReportPath)) {
            reportFile.openOutput();

            // 6100-PRINT-HEADER
            reportFile.writeLine("=".repeat(PAGE_WIDTH));
            reportFile.writeLine("TEST VALIDATION REPORT");
            reportFile.writeLine("Generated: " + LocalDateTime.now().format(TIMESTAMP_FMT));
            reportFile.writeLine("Program: " + PROGRAM_ID);
            reportFile.writeLine("=".repeat(PAGE_WIDTH));
            reportFile.writeLine("");

            // 6200-PRINT-DETAILS
            reportFile.writeLine(String.format("%-20s %-15s %-8s %s",
                    "Test ID", "Category", "Result", "Details"));
            reportFile.writeLine("-".repeat(PAGE_WIDTH));

            for (TestResult result : testResults) {
                reportFile.writeLine(String.format("%-20s %-15s %-8s %s",
                        result.testId,
                        result.category,
                        result.passed ? "PASS" : "FAIL",
                        result.detail));
            }

            // 6300-PRINT-SUMMARY
            reportFile.writeLine("");
            reportFile.writeLine("=".repeat(PAGE_WIDTH));
            reportFile.writeLine("TEST SUMMARY");
            reportFile.writeLine("-".repeat(40));
            reportFile.writeLine(String.format("  Total Tests:     %d", totalTests));
            reportFile.writeLine(String.format("  Passed:          %d", passedTests));
            reportFile.writeLine(String.format("  Failed:          %d", failedTests));

            double successRate = totalTests > 0
                    ? (double) passedTests / totalTests * 100.0 : 0;
            reportFile.writeLine(String.format("  Success Rate:    %.2f%%", successRate));
            reportFile.writeLine(String.format("  Elapsed Time:    %d ms", elapsedTimeMs));
            reportFile.writeLine("=".repeat(PAGE_WIDTH));

            // Overall assessment
            reportFile.writeLine("");
            if (failedTests == 0) {
                reportFile.writeLine("OVERALL RESULT: ALL TESTS PASSED");
            } else {
                reportFile.writeLine(String.format(
                        "OVERALL RESULT: %d TEST(S) FAILED - REVIEW REQUIRED", failedTests));
            }

            reportFile.writeLine("");
            reportFile.writeLine("*** END OF TEST REPORT ***");

        } catch (Exception e) {
            errorHandler.handleSystemError("E600", "Error generating test report", e);
            returnCode.setCode(ReturnCode.ERROR);
        }
    }

    /**
     * 9000-TERMINATE: Set return code based on test results.
     */
    private void terminate() {
        if (failedTests > 0) {
            returnCode.setCode(ReturnCode.ERROR);
        }

        displayStatistics();
    }

    private void displayStatistics() {
        double successRate = totalTests > 0
                ? (double) passedTests / totalTests * 100.0 : 0;

        LOGGER.info(PROGRAM_ID + " Test Results:");
        LOGGER.info(String.format("  Total: %d  Passed: %d  Failed: %d  Rate: %.2f%%",
                totalTests, passedTests, failedTests, successRate));
        LOGGER.info("  Elapsed Time: " + elapsedTimeMs + " ms");
        LOGGER.info("  Return Code: " + returnCode.getCurrentCode());
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    /**
     * Individual test result record.
     */
    private static class TestResult {
        final String testId;
        final String category;
        final boolean passed;
        final String detail;

        TestResult(String testId, String category, boolean passed, String detail) {
            this.testId = testId;
            this.category = category;
            this.passed = passed;
            this.detail = detail;
        }
    }
}
