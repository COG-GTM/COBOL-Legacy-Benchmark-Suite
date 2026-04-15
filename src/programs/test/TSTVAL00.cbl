       IDENTIFICATION DIVISION.
       PROGRAM-ID. TSTVAL00.
       AUTHOR. CLAUDE.
       DATE-WRITTEN. 2024-04-09.
      *****************************************************************
      * Test Validation Suite                                         *
      *                                                               *
      * Compares actual program output against expected results to     *
      * verify correctness of translated or modified COBOL programs.   *
      *                                                               *
      * Test Categories (via TEST-TYPE):                              *
      *   FUNCTIONAL - Unit-level output comparisons                  *
      *   INTEGRATE  - Cross-program data flow validation             *
      *   PERFORM    - Execution time within acceptable bounds        *
      *   ERROR      - Verify correct error handling behavior         *
      *                                                               *
      * Files:                                                        *
      *   TESTCASE - Input  : Test case definitions (ID, type, desc)  *
      *   EXPECTED - Input  : Expected result records                 *
      *   ACTUAL   - Input  : Actual result records to validate       *
      *   TESTRPT  - Output : Validation report with pass/fail status *
      *                                                               *
      * Copybooks: RTNCODE, ERRHAND                                  *
      *                                                               *
      * Report:    Produces a summary line with total, passed,        *
      *            failed counts, and success rate percentage.         *
      *                                                               *
      * Return Codes: 0 = All tests passed, 12 = Fatal error          *
      *****************************************************************
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SPECIAL-NAMES.
           CONSOLE IS CONS.
           
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT TEST-CASES ASSIGN TO TESTCASE
               ORGANIZATION IS SEQUENTIAL
               ACCESS MODE IS SEQUENTIAL
               FILE STATUS IS WS-TEST-STATUS.

           SELECT EXPECTED-RESULTS ASSIGN TO EXPECTED
               ORGANIZATION IS SEQUENTIAL
               ACCESS MODE IS SEQUENTIAL
               FILE STATUS IS WS-EXP-STATUS.

           SELECT ACTUAL-RESULTS ASSIGN TO ACTUAL
               ORGANIZATION IS SEQUENTIAL
               ACCESS MODE IS SEQUENTIAL
               FILE STATUS IS WS-ACT-STATUS.

           SELECT TEST-REPORT ASSIGN TO TESTRPT
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-RPT-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  TEST-CASES
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  TEST-CASE-RECORD.
           05  TEST-ID              PIC X(10).
           05  TEST-TYPE            PIC X(10).
           05  TEST-DESCRIPTION     PIC X(50).
           05  TEST-PARAMETERS      PIC X(100).

       FD  EXPECTED-RESULTS
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  EXPECTED-RECORD         PIC X(200).

       FD  ACTUAL-RESULTS
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  ACTUAL-RECORD          PIC X(200).

       FD  TEST-REPORT
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  REPORT-RECORD          PIC X(132).

       WORKING-STORAGE SECTION.
           COPY RTNCODE.
           COPY ERRHAND.

      *    File status indicators for all I/O operations
       01  WS-FILE-STATUS.
           05  WS-TEST-STATUS       PIC XX.
           05  WS-EXP-STATUS        PIC XX.
           05  WS-ACT-STATUS        PIC XX.
           05  WS-RPT-STATUS        PIC XX.

      *    Constants for test type dispatch
       01  WS-TEST-TYPES.
           05  WS-FUNCTIONAL        PIC X(10) VALUE 'FUNCTIONAL'.
           05  WS-INTEGRATION       PIC X(10) VALUE 'INTEGRATE'.
           05  WS-PERFORMANCE       PIC X(10) VALUE 'PERFORM'.
           05  WS-ERROR            PIC X(10) VALUE 'ERROR'.

       01  WS-PROCESSING-FLAGS.
           05  WS-END-OF-TESTS      PIC X VALUE 'N'.
               88  END-OF-TESTS     VALUE 'Y'.
           05  WS-TEST-PASSED       PIC X VALUE 'N'.
               88  TEST-PASSED      VALUE 'Y'.

      *    Running test execution metrics
       01  WS-TEST-METRICS.
           05  WS-TOTAL-TESTS       PIC 9(5) VALUE ZERO.
           05  WS-TESTS-PASSED      PIC 9(5) VALUE ZERO.
           05  WS-TESTS-FAILED      PIC 9(5) VALUE ZERO.
      *    Wall-clock timing for performance benchmarking
           05  WS-START-TIME        PIC 9(8) VALUE ZERO.
           05  WS-END-TIME          PIC 9(8) VALUE ZERO.
           05  WS-ELAPSED-TIME      PIC 9(8) VALUE ZERO.

       01  WS-REPORT-HEADERS.
           05  WS-HEADER1.
               10  FILLER           PIC X(132) VALUE ALL '*'.
           05  WS-HEADER2.
               10  FILLER           PIC X(30) VALUE SPACES.
               10  FILLER           PIC X(72) 
                   VALUE 'TEST VALIDATION REPORT'.
               10  FILLER           PIC X(30) VALUE SPACES.

       01  WS-TEST-DETAIL.
           05  WS-TEST-ID-OUT       PIC X(10).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-TEST-TYPE-OUT     PIC X(10).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-TEST-DESC-OUT     PIC X(50).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-TEST-STATUS-OUT   PIC X(4).
           05  FILLER               PIC X(52) VALUE SPACES.

       01  WS-SUMMARY-LINE.
           05  FILLER               PIC X(15) VALUE 'TOTAL TESTS:'.
           05  WS-TOTAL-OUT         PIC ZZ,ZZ9.
           05  FILLER               PIC X(15) VALUE '  PASSED:'.
           05  WS-PASSED-OUT        PIC ZZ,ZZ9.
           05  FILLER               PIC X(15) VALUE '  FAILED:'.
           05  WS-FAILED-OUT        PIC ZZ,ZZ9.
           05  FILLER               PIC X(15) VALUE '  SUCCESS:'.
           05  WS-SUCCESS-RATE      PIC ZZ9.99.
           05  FILLER               PIC X(1) VALUE '%'.
           05  FILLER               PIC X(40) VALUE SPACES.

       PROCEDURE DIVISION.
      *----------------------------------------------------------------*
      * Main control: initialize, run all tests, produce report.       *
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS
           PERFORM 3000-CLEANUP
           GOBACK.

      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Open files, write report headers, start timer.*
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           PERFORM 1100-OPEN-FILES
           PERFORM 1200-WRITE-HEADERS
           PERFORM 1300-INIT-METRICS.

      *----------------------------------------------------------------*
      * 1100-OPEN-FILES: Open test cases + expected/actual results     *
      * as input, and test report as output.                           *
      *----------------------------------------------------------------*
       1100-OPEN-FILES.
           OPEN INPUT TEST-CASES
           IF WS-TEST-STATUS NOT = '00'
               MOVE 'ERROR OPENING TEST CASES'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF

           OPEN INPUT EXPECTED-RESULTS
           IF WS-EXP-STATUS NOT = '00'
               MOVE 'ERROR OPENING EXPECTED RESULTS'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF

           OPEN INPUT ACTUAL-RESULTS
           IF WS-ACT-STATUS NOT = '00'
               MOVE 'ERROR OPENING ACTUAL RESULTS'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF

           OPEN OUTPUT TEST-REPORT
           IF WS-RPT-STATUS NOT = '00'
               MOVE 'ERROR OPENING TEST REPORT'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF.

      *----------------------------------------------------------------*
      * 1200-WRITE-HEADERS: Write report banner to output file.        *
      *----------------------------------------------------------------*
       1200-WRITE-HEADERS.
           WRITE REPORT-RECORD FROM WS-HEADER1
           WRITE REPORT-RECORD FROM WS-HEADER2.

       1300-INIT-METRICS.
           INITIALIZE WS-TEST-METRICS
           ACCEPT WS-START-TIME FROM TIME.

      *----------------------------------------------------------------*
      * 2000-PROCESS: Iterate through test cases, execute each,        *
      * validate results, then write the final summary line.           *
      *----------------------------------------------------------------*
       2000-PROCESS.
           PERFORM UNTIL END-OF-TESTS
               READ TEST-CASES
                   AT END
                       SET END-OF-TESTS TO TRUE
                   NOT AT END
                       PERFORM 2100-EXECUTE-TEST
               END-READ
           END-PERFORM
           PERFORM 2900-WRITE-SUMMARY.

      *----------------------------------------------------------------*
      * 2100-EXECUTE-TEST: Dispatch to the appropriate test runner,    *
      * then validate results, update metrics, and write detail line.  *
      *----------------------------------------------------------------*
       2100-EXECUTE-TEST.
           EVALUATE TEST-TYPE
               WHEN WS-FUNCTIONAL
                   PERFORM 2200-RUN-FUNCTIONAL-TEST
               WHEN WS-INTEGRATION
                   PERFORM 2300-RUN-INTEGRATION-TEST
               WHEN WS-PERFORMANCE
                   PERFORM 2400-RUN-PERFORMANCE-TEST
               WHEN WS-ERROR
                   PERFORM 2500-RUN-ERROR-TEST
               WHEN OTHER
                   MOVE 'INVALID TEST TYPE'
                     TO WS-ERROR-MESSAGE
                   PERFORM 9999-ERROR-HANDLER
           END-EVALUATE
           PERFORM 2600-VALIDATE-RESULTS
           PERFORM 2700-UPDATE-METRICS
           PERFORM 2800-WRITE-TEST-DETAIL.

      *----------------------------------------------------------------*
      * 2900-WRITE-SUMMARY: Calculate elapsed time and success rate,   *
      * then write the summary totals line to the report.              *
      *----------------------------------------------------------------*
       2900-WRITE-SUMMARY.
           ACCEPT WS-END-TIME FROM TIME
           COMPUTE WS-ELAPSED-TIME = WS-END-TIME - WS-START-TIME
           MOVE WS-TOTAL-TESTS TO WS-TOTAL-OUT
           MOVE WS-TESTS-PASSED TO WS-PASSED-OUT
           MOVE WS-TESTS-FAILED TO WS-FAILED-OUT
           COMPUTE WS-SUCCESS-RATE = 
               (WS-TESTS-PASSED / WS-TOTAL-TESTS) * 100
           WRITE REPORT-RECORD FROM WS-SUMMARY-LINE.

      *----------------------------------------------------------------*
      * 3000-CLEANUP: Close all files.                                 *
      *----------------------------------------------------------------*
       3000-CLEANUP.
           CLOSE TEST-CASES
                EXPECTED-RESULTS
                ACTUAL-RESULTS
                TEST-REPORT.

      *----------------------------------------------------------------*
      * 9999-ERROR-HANDLER: Display error and abort with RC=12.        *
      *----------------------------------------------------------------*
       9999-ERROR-HANDLER.
           DISPLAY WS-ERROR-MESSAGE UPON CONS
           MOVE 12 TO RETURN-CODE
           GOBACK.  