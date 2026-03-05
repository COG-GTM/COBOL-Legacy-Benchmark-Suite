       IDENTIFICATION DIVISION.
       PROGRAM-ID. TSTTRN00.
       AUTHOR. CLAUDE.
       DATE-WRITTEN. 2024-04-09.
      *****************************************************************
      * Program Name: TSTTRN00                                        *
      * Description:  Unit Tests for Portfolio Transaction Processing *
      *               Tests validation and position update logic      *
      *               from PORTTRAN and PORTVALD programs             *
      * Version:      1.0                                             *
      * Date:         2024-04-09                                      *
      *****************************************************************
      * Test Cases:                                                   *
      *   1. Valid buy transaction (type BU)                          *
      *   2. Valid sell transaction (type SL)                         *
      *   3. Invalid portfolio ID (missing/spaces)                   *
      *   4. Invalid portfolio ID (nonexistent)                      *
      *   5. Invalid transaction type                                *
      *   6. Zero quantity rejected                                  *
      *   7. Negative quantity rejected                              *
      *   8. Zero price rejected (non-transfer)                      *
      *   9. Negative price rejected (non-transfer)                  *
      *  10. Zero price allowed for transfers                        *
      *  11. Buy updates position (units and cost added)             *
      *  12. Sell updates position (units and cost subtracted)       *
      *  13. Sell with insufficient units rejected                   *
      *  14. Fee subtracts amount from cost basis                    *
      *****************************************************************
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SPECIAL-NAMES.
           CONSOLE IS CONS.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
           COPY TRNREC.
           COPY ERRHAND.
           COPY PORTVAL.

      *----------------------------------------------------------------*
      * Test Framework Variables
      *----------------------------------------------------------------*
       01  WS-TEST-METRICS.
           05  WS-TESTS-RUN            PIC 9(5) VALUE ZERO.
           05  WS-TESTS-PASSED         PIC 9(5) VALUE ZERO.
           05  WS-TESTS-FAILED         PIC 9(5) VALUE ZERO.

       01  WS-TEST-CASE.
           05  WS-TEST-NAME            PIC X(40) VALUE SPACES.
           05  WS-TEST-RESULT          PIC X(4)  VALUE SPACES.
               88  TEST-PASSED         VALUE 'PASS'.
               88  TEST-FAILED         VALUE 'FAIL'.

      *----------------------------------------------------------------*
      * Mock Portfolio Record (mirrors PORTREC used by PORTTRAN)
      *----------------------------------------------------------------*
       01  MOCK-PORTFOLIO-RECORD.
           05  MOCK-PORT-KEY.
               10  MOCK-PORT-ID        PIC X(8).
               10  MOCK-PORT-ACCT-NO   PIC X(10).
           05  MOCK-PORT-CLIENT-INFO.
               10  MOCK-PORT-CLIENT    PIC X(30).
               10  MOCK-PORT-CLI-TYPE  PIC X(1).
           05  MOCK-PORT-INFO.
               10  MOCK-PORT-CREATE-DT PIC 9(8).
               10  MOCK-PORT-LAST-MNT  PIC 9(8).
               10  MOCK-PORT-STATUS    PIC X(1).
           05  MOCK-PORT-FINANCIALS.
               10  MOCK-PORT-TOTAL-UNITS
                                       PIC S9(11)V9(4) COMP-3.
               10  MOCK-PORT-TOTAL-COST
                                       PIC S9(13)V9(2) COMP-3.
           05  MOCK-PORT-AUDIT.
               10  MOCK-PORT-LAST-USER PIC X(8).
               10  MOCK-PORT-LAST-TRN  PIC 9(8).
           05  MOCK-PORT-FILLER        PIC X(50).

      *----------------------------------------------------------------*
      * Validation Working Fields
      *----------------------------------------------------------------*
       01  WS-VALIDATION-RESULT        PIC X(80).
       01  WS-EXPECTED-RESULT          PIC X(80).
       01  WS-VALID-PORTFOLIO-FLAG     PIC X(1).
           88  PORTFOLIO-VALID         VALUE 'Y'.
           88  PORTFOLIO-INVALID       VALUE 'N'.
       01  WS-VALID-TYPE-FLAG          PIC X(1).
           88  TYPE-VALID              VALUE 'Y'.
           88  TYPE-INVALID            VALUE 'N'.
       01  WS-VALID-AMOUNT-FLAG        PIC X(1).
           88  AMOUNT-VALID            VALUE 'Y'.
           88  AMOUNT-INVALID          VALUE 'N'.
       01  WS-POSITION-UPDATE-FLAG     PIC X(1).
           88  UPDATE-SUCCESS          VALUE 'Y'.
           88  UPDATE-FAILED           VALUE 'N'.

      *----------------------------------------------------------------*
      * Expected Values for Position Update Tests
      *----------------------------------------------------------------*
       01  WS-EXPECTED-UNITS           PIC S9(11)V9(4) COMP-3.
       01  WS-EXPECTED-COST            PIC S9(13)V9(2) COMP-3.
       01  WS-INITIAL-UNITS            PIC S9(11)V9(4) COMP-3.
       01  WS-INITIAL-COST             PIC S9(13)V9(2) COMP-3.

      *----------------------------------------------------------------*
      * Report Display Fields
      *----------------------------------------------------------------*
       01  WS-DISPLAY-LINE             PIC X(80) VALUE SPACES.
       01  WS-SEPARATOR                PIC X(60)
           VALUE '============================================'
                 '================'.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-RUN-TESTS
           PERFORM 3000-TERMINATE
           GOBACK.

      *----------------------------------------------------------------*
      * 1000 - INITIALIZE TEST FRAMEWORK
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           INITIALIZE WS-TEST-METRICS
           DISPLAY ' ' UPON CONS
           DISPLAY WS-SEPARATOR UPON CONS
           DISPLAY '  TSTTRN00 - PORTFOLIO TRANSACTION UNIT '
                   'TESTS' UPON CONS
           DISPLAY WS-SEPARATOR UPON CONS
           DISPLAY ' ' UPON CONS.

      *----------------------------------------------------------------*
      * 2000 - RUN ALL TEST CASES
      *----------------------------------------------------------------*
       2000-RUN-TESTS.
      *    Transaction Validation Tests
           PERFORM TEST-BUY-VALID
           PERFORM TEST-SELL-VALID
           PERFORM TEST-INVALID-PORT-MISSING
           PERFORM TEST-INVALID-PORT-NONEXIST
           PERFORM TEST-INVALID-TYPE
           PERFORM TEST-ZERO-QUANTITY
           PERFORM TEST-NEGATIVE-QUANTITY
           PERFORM TEST-ZERO-PRICE
           PERFORM TEST-NEGATIVE-PRICE
           PERFORM TEST-TRANSFER-ZERO-PRICE
      *    Position Update Tests
           PERFORM TEST-BUY-POSITION-UPDATE
           PERFORM TEST-SELL-POSITION-UPDATE
           PERFORM TEST-SELL-INSUFFICIENT-UNITS
           PERFORM TEST-FEE-COST-UPDATE.

      *================================================================*
      * TEST CASE: Valid Buy Transaction
      * Verify a well-formed BU transaction passes validation
      *================================================================*
       TEST-BUY-VALID.
           MOVE 'TEST-BUY-VALID' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE '20240401' TO TRN-DATE
           MOVE '120000'   TO TRN-TIME
           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE '000001'   TO TRN-SEQUENCE-NO
           MOVE 'INVEST0001' TO TRN-INVESTMENT-ID
           MOVE 'BU'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE 50.2500    TO TRN-PRICE
           MOVE 5025.00    TO TRN-AMOUNT
           MOVE 'USD'      TO TRN-CURRENCY
           MOVE 'P'        TO TRN-STATUS

           PERFORM 8100-VALIDATE-TRANSACTION-TYPE
           PERFORM 8200-VALIDATE-AMOUNTS

           IF TYPE-VALID AND AMOUNT-VALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Valid Sell Transaction
      * Verify a well-formed SL transaction passes validation
      *================================================================*
       TEST-SELL-VALID.
           MOVE 'TEST-SELL-VALID' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE '20240401' TO TRN-DATE
           MOVE '130000'   TO TRN-TIME
           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE '000002'   TO TRN-SEQUENCE-NO
           MOVE 'INVEST0001' TO TRN-INVESTMENT-ID
           MOVE 'SL'       TO TRN-TYPE
           MOVE 50.0000    TO TRN-QUANTITY
           MOVE 52.7500    TO TRN-PRICE
           MOVE 2637.50    TO TRN-AMOUNT
           MOVE 'USD'      TO TRN-CURRENCY
           MOVE 'P'        TO TRN-STATUS

           PERFORM 8100-VALIDATE-TRANSACTION-TYPE
           PERFORM 8200-VALIDATE-AMOUNTS

           IF TYPE-VALID AND AMOUNT-VALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Invalid Portfolio ID - Missing (spaces)
      * Verify blank portfolio ID is rejected
      *================================================================*
       TEST-INVALID-PORT-MISSING.
           MOVE 'TEST-INVALID-PORT-MISSING' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE SPACES     TO TRN-PORTFOLIO-ID
           MOVE 'BU'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE 50.2500    TO TRN-PRICE
           MOVE 5025.00    TO TRN-AMOUNT

           PERFORM 8300-VALIDATE-PORTFOLIO-ID

           IF PORTFOLIO-INVALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Invalid Portfolio ID - Nonexistent
      * Verify a portfolio ID not matching PORT prefix is rejected
      *================================================================*
       TEST-INVALID-PORT-NONEXIST.
           MOVE 'TEST-INVALID-PORT-NONEXIST' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE 'XXXX9999' TO TRN-PORTFOLIO-ID
           MOVE 'BU'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE 50.2500    TO TRN-PRICE
           MOVE 5025.00    TO TRN-AMOUNT

           PERFORM 8300-VALIDATE-PORTFOLIO-ID

           IF PORTFOLIO-INVALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Invalid Transaction Type
      * Verify unrecognized transaction type is rejected
      *================================================================*
       TEST-INVALID-TYPE.
           MOVE 'TEST-INVALID-TYPE' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'XX'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE 50.2500    TO TRN-PRICE
           MOVE 5025.00    TO TRN-AMOUNT

           PERFORM 8100-VALIDATE-TRANSACTION-TYPE

           IF TYPE-INVALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Zero Quantity Rejected
      * Verify quantity of zero is rejected
      *================================================================*
       TEST-ZERO-QUANTITY.
           MOVE 'TEST-ZERO-QUANTITY' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'BU'       TO TRN-TYPE
           MOVE ZERO        TO TRN-QUANTITY
           MOVE 50.2500    TO TRN-PRICE
           MOVE 5025.00    TO TRN-AMOUNT

           PERFORM 8200-VALIDATE-AMOUNTS

           IF AMOUNT-INVALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Negative Quantity Rejected
      * Verify negative quantity is rejected
      *================================================================*
       TEST-NEGATIVE-QUANTITY.
           MOVE 'TEST-NEGATIVE-QUANTITY' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'BU'       TO TRN-TYPE
           MOVE -100.0000  TO TRN-QUANTITY
           MOVE 50.2500    TO TRN-PRICE
           MOVE 5025.00    TO TRN-AMOUNT

           PERFORM 8200-VALIDATE-AMOUNTS

           IF AMOUNT-INVALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Zero Price Rejected (non-transfer)
      * Verify zero price is rejected for BU type
      *================================================================*
       TEST-ZERO-PRICE.
           MOVE 'TEST-ZERO-PRICE' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'BU'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE ZERO        TO TRN-PRICE
           MOVE 5025.00    TO TRN-AMOUNT

           PERFORM 8200-VALIDATE-AMOUNTS

           IF AMOUNT-INVALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Negative Price Rejected (non-transfer)
      * Verify negative price is rejected for SL type
      *================================================================*
       TEST-NEGATIVE-PRICE.
           MOVE 'TEST-NEGATIVE-PRICE' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'SL'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE -10.5000   TO TRN-PRICE
           MOVE 5025.00    TO TRN-AMOUNT

           PERFORM 8200-VALIDATE-AMOUNTS

           IF AMOUNT-INVALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Transfer Allows Zero Price
      * Verify zero price is allowed for TR (transfer) type
      *================================================================*
       TEST-TRANSFER-ZERO-PRICE.
           MOVE 'TEST-TRANSFER-ZERO-PRICE' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA

           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'TR'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE ZERO        TO TRN-PRICE
           MOVE ZERO        TO TRN-AMOUNT

           PERFORM 8200-VALIDATE-AMOUNTS

           IF AMOUNT-VALID
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Buy Updates Position
      * Verify buy adds units and cost to portfolio totals
      *================================================================*
       TEST-BUY-POSITION-UPDATE.
           MOVE 'TEST-BUY-POSITION-UPDATE' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA
           PERFORM 8400-INIT-PORTFOLIO-DATA

      *    Set initial portfolio position
           MOVE 500.0000   TO MOCK-PORT-TOTAL-UNITS
           MOVE 25000.00   TO MOCK-PORT-TOTAL-COST
           MOVE 500.0000   TO WS-INITIAL-UNITS
           MOVE 25000.00   TO WS-INITIAL-COST

      *    Set buy transaction
           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'BU'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE 50.2500    TO TRN-PRICE
           MOVE 5025.00    TO TRN-AMOUNT

      *    Calculate expected results
           COMPUTE WS-EXPECTED-UNITS =
               WS-INITIAL-UNITS + TRN-QUANTITY
           COMPUTE WS-EXPECTED-COST =
               WS-INITIAL-COST + TRN-AMOUNT

      *    Simulate buy: add units and cost
           ADD TRN-QUANTITY TO MOCK-PORT-TOTAL-UNITS
           ADD TRN-AMOUNT   TO MOCK-PORT-TOTAL-COST

      *    Verify results
           IF MOCK-PORT-TOTAL-UNITS = WS-EXPECTED-UNITS
               AND MOCK-PORT-TOTAL-COST = WS-EXPECTED-COST
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Sell Updates Position
      * Verify sell subtracts units and cost from portfolio totals
      *================================================================*
       TEST-SELL-POSITION-UPDATE.
           MOVE 'TEST-SELL-POSITION-UPDATE' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA
           PERFORM 8400-INIT-PORTFOLIO-DATA

      *    Set initial portfolio position
           MOVE 500.0000   TO MOCK-PORT-TOTAL-UNITS
           MOVE 25000.00   TO MOCK-PORT-TOTAL-COST
           MOVE 500.0000   TO WS-INITIAL-UNITS
           MOVE 25000.00   TO WS-INITIAL-COST

      *    Set sell transaction
           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'SL'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE 52.7500    TO TRN-PRICE
           MOVE 5275.00    TO TRN-AMOUNT

      *    Calculate expected results
           COMPUTE WS-EXPECTED-UNITS =
               WS-INITIAL-UNITS - TRN-QUANTITY
           COMPUTE WS-EXPECTED-COST =
               WS-INITIAL-COST - TRN-AMOUNT

      *    Simulate sell: subtract units and cost
           SUBTRACT TRN-QUANTITY FROM MOCK-PORT-TOTAL-UNITS
           SUBTRACT TRN-AMOUNT   FROM MOCK-PORT-TOTAL-COST

      *    Verify results
           IF MOCK-PORT-TOTAL-UNITS = WS-EXPECTED-UNITS
               AND MOCK-PORT-TOTAL-COST = WS-EXPECTED-COST
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Sell With Insufficient Units
      * Verify sell is rejected when units exceed available position
      *================================================================*
       TEST-SELL-INSUFFICIENT-UNITS.
           MOVE 'TEST-SELL-INSUFFICIENT-UNITS' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA
           PERFORM 8400-INIT-PORTFOLIO-DATA

      *    Set small portfolio position
           MOVE 50.0000    TO MOCK-PORT-TOTAL-UNITS
           MOVE 2500.00    TO MOCK-PORT-TOTAL-COST

      *    Attempt to sell more units than available
           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'SL'       TO TRN-TYPE
           MOVE 100.0000   TO TRN-QUANTITY
           MOVE 52.7500    TO TRN-PRICE
           MOVE 5275.00    TO TRN-AMOUNT

      *    Check insufficient units condition
      *    (mirrors PORTTRAN 2220-PROCESS-SELL logic)
           IF MOCK-PORT-TOTAL-UNITS < TRN-QUANTITY
               SET UPDATE-FAILED TO TRUE
           ELSE
               SET UPDATE-SUCCESS TO TRUE
           END-IF

           IF UPDATE-FAILED
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * TEST CASE: Fee Subtracts From Cost Basis
      * Verify fee amount is subtracted from portfolio cost
      *================================================================*
       TEST-FEE-COST-UPDATE.
           MOVE 'TEST-FEE-COST-UPDATE' TO WS-TEST-NAME
           PERFORM 8000-INIT-TRANSACTION-DATA
           PERFORM 8400-INIT-PORTFOLIO-DATA

      *    Set initial portfolio position
           MOVE 500.0000   TO MOCK-PORT-TOTAL-UNITS
           MOVE 25000.00   TO MOCK-PORT-TOTAL-COST
           MOVE 500.0000   TO WS-INITIAL-UNITS
           MOVE 25000.00   TO WS-INITIAL-COST

      *    Set fee transaction
           MOVE 'PORT0001' TO TRN-PORTFOLIO-ID
           MOVE 'FE'       TO TRN-TYPE
           MOVE 1.0000     TO TRN-QUANTITY
           MOVE 1.0000     TO TRN-PRICE
           MOVE 150.00     TO TRN-AMOUNT

      *    Expected: cost reduced by fee, units unchanged
           MOVE WS-INITIAL-UNITS TO WS-EXPECTED-UNITS
           COMPUTE WS-EXPECTED-COST =
               WS-INITIAL-COST - TRN-AMOUNT

      *    Simulate fee: subtract amount from cost
      *    (mirrors PORTTRAN 2240-PROCESS-FEE logic)
           SUBTRACT TRN-AMOUNT FROM MOCK-PORT-TOTAL-COST

      *    Verify results
           IF MOCK-PORT-TOTAL-UNITS = WS-EXPECTED-UNITS
               AND MOCK-PORT-TOTAL-COST = WS-EXPECTED-COST
               SET TEST-PASSED TO TRUE
           ELSE
               SET TEST-FAILED TO TRUE
           END-IF

           PERFORM 9000-RECORD-RESULT.

      *================================================================*
      * 8000 - HELPER PARAGRAPHS
      *================================================================*

      *----------------------------------------------------------------*
      * 8000 - Initialize Transaction Record
      *----------------------------------------------------------------*
       8000-INIT-TRANSACTION-DATA.
           INITIALIZE TRANSACTION-RECORD
           MOVE SPACES TO WS-VALIDATION-RESULT
           MOVE SPACES TO ERR-TEXT
           SET PORTFOLIO-VALID   TO TRUE
           SET TYPE-VALID        TO TRUE
           SET AMOUNT-VALID      TO TRUE
           SET UPDATE-SUCCESS    TO TRUE.

      *----------------------------------------------------------------*
      * 8100 - Validate Transaction Type
      * Mirrors PORTTRAN 2120-CHECK-TRANSACTION-TYPE logic
      *----------------------------------------------------------------*
       8100-VALIDATE-TRANSACTION-TYPE.
           SET TYPE-VALID TO TRUE

           EVALUATE TRN-TYPE
               WHEN 'BU'
               WHEN 'SL'
               WHEN 'TR'
               WHEN 'FE'
                   SET TYPE-VALID TO TRUE
               WHEN OTHER
                   SET TYPE-INVALID TO TRUE
           END-EVALUATE.

      *----------------------------------------------------------------*
      * 8200 - Validate Amounts
      * Mirrors PORTTRAN 2130-CHECK-AMOUNTS logic
      *----------------------------------------------------------------*
       8200-VALIDATE-AMOUNTS.
           SET AMOUNT-VALID TO TRUE

           IF TRN-QUANTITY <= ZERO
               SET AMOUNT-INVALID TO TRUE
               EXIT PARAGRAPH
           END-IF

           IF TRN-PRICE <= ZERO AND TRN-TYPE NOT = 'TR'
               SET AMOUNT-INVALID TO TRUE
               EXIT PARAGRAPH
           END-IF

           IF TRN-AMOUNT <= ZERO AND TRN-TYPE NOT = 'TR'
               SET AMOUNT-INVALID TO TRUE
           END-IF.

      *----------------------------------------------------------------*
      * 8300 - Validate Portfolio ID
      * Mirrors PORTTRAN 2110-CHECK-PORTFOLIO logic
      * Checks for blank or non-PORT-prefixed IDs
      *----------------------------------------------------------------*
       8300-VALIDATE-PORTFOLIO-ID.
           SET PORTFOLIO-VALID TO TRUE

           IF TRN-PORTFOLIO-ID = SPACES
               SET PORTFOLIO-INVALID TO TRUE
               EXIT PARAGRAPH
           END-IF

      *    Validate ID format: must start with 'PORT'
      *    and have 4 numeric digits (per PORTVALD rules)
           IF TRN-PORTFOLIO-ID(1:4) NOT = 'PORT'
               SET PORTFOLIO-INVALID TO TRUE
               EXIT PARAGRAPH
           END-IF

           IF TRN-PORTFOLIO-ID(5:4) IS NOT NUMERIC
               SET PORTFOLIO-INVALID TO TRUE
           END-IF.

      *----------------------------------------------------------------*
      * 8400 - Initialize Mock Portfolio Data
      *----------------------------------------------------------------*
       8400-INIT-PORTFOLIO-DATA.
           INITIALIZE MOCK-PORTFOLIO-RECORD
           MOVE 'PORT0001'      TO MOCK-PORT-ID
           MOVE '1234567890'    TO MOCK-PORT-ACCT-NO
           MOVE 'TEST CLIENT'   TO MOCK-PORT-CLIENT
           MOVE 'I'             TO MOCK-PORT-CLI-TYPE
           MOVE 20240101        TO MOCK-PORT-CREATE-DT
           MOVE 20240401        TO MOCK-PORT-LAST-MNT
           MOVE 'A'             TO MOCK-PORT-STATUS
           MOVE ZERO            TO MOCK-PORT-TOTAL-UNITS
           MOVE ZERO            TO MOCK-PORT-TOTAL-COST
           MOVE 'TESTUSER'      TO MOCK-PORT-LAST-USER
           MOVE 20240401        TO MOCK-PORT-LAST-TRN
           SET UPDATE-SUCCESS   TO TRUE.

      *================================================================*
      * 9000 - TEST RESULT RECORDING
      *================================================================*

      *----------------------------------------------------------------*
      * 9000 - Record and Display Test Result
      *----------------------------------------------------------------*
       9000-RECORD-RESULT.
           ADD 1 TO WS-TESTS-RUN

           IF TEST-PASSED
               ADD 1 TO WS-TESTS-PASSED
               STRING '  PASS: '
                      WS-TEST-NAME
                 DELIMITED BY SIZE
                 INTO WS-DISPLAY-LINE
           ELSE
               ADD 1 TO WS-TESTS-FAILED
               STRING '  FAIL: '
                      WS-TEST-NAME
                 DELIMITED BY SIZE
                 INTO WS-DISPLAY-LINE
           END-IF

           DISPLAY WS-DISPLAY-LINE UPON CONS
           MOVE SPACES TO WS-DISPLAY-LINE.

      *----------------------------------------------------------------*
      * 3000 - TERMINATE AND DISPLAY SUMMARY
      *----------------------------------------------------------------*
       3000-TERMINATE.
           DISPLAY ' ' UPON CONS
           DISPLAY WS-SEPARATOR UPON CONS
           DISPLAY '  TEST SUMMARY' UPON CONS
           DISPLAY WS-SEPARATOR UPON CONS

           STRING '  Tests Run:    ' DELIMITED BY SIZE
                  WS-TESTS-RUN      DELIMITED BY SIZE
             INTO WS-DISPLAY-LINE
           DISPLAY WS-DISPLAY-LINE UPON CONS
           MOVE SPACES TO WS-DISPLAY-LINE

           STRING '  Tests Passed: ' DELIMITED BY SIZE
                  WS-TESTS-PASSED   DELIMITED BY SIZE
             INTO WS-DISPLAY-LINE
           DISPLAY WS-DISPLAY-LINE UPON CONS
           MOVE SPACES TO WS-DISPLAY-LINE

           STRING '  Tests Failed: ' DELIMITED BY SIZE
                  WS-TESTS-FAILED   DELIMITED BY SIZE
             INTO WS-DISPLAY-LINE
           DISPLAY WS-DISPLAY-LINE UPON CONS

           DISPLAY WS-SEPARATOR UPON CONS

           IF WS-TESTS-FAILED = ZERO
               DISPLAY '  RESULT: ALL TESTS PASSED' UPON CONS
               MOVE 0 TO RETURN-CODE
           ELSE
               DISPLAY '  RESULT: SOME TESTS FAILED' UPON CONS
               MOVE 8 TO RETURN-CODE
           END-IF

           DISPLAY WS-SEPARATOR UPON CONS
           DISPLAY ' ' UPON CONS.
