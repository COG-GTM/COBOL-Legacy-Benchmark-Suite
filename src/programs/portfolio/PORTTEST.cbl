  *================================================================*
      * Program Name: PORTTEST
      * Description: Portfolio Test Data Generator
      *             Creates synthetic portfolio records for testing
      *             by generating randomized IDs, client info,
      *             dates, statuses, and financial values.
      *
      * Processing:  Generates up to WS-MAX-RECORDS (default 100)
      *              portfolio records using RANDOM intrinsic function
      *              and writes them to a sequential output file.
      *
      * Files:       TESTFILE - Sequential output for test records
      *
      * Copybooks:   PORTFLIO - Portfolio record layout
      *              ERRHAND  - Error handling data areas
      *
      * Author: [Author name]
      * Date Written: 2024-03-20
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PORTTEST.
       
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.
       
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT TEST-FILE
               ASSIGN TO TESTFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-FILE-STATUS.
       
       DATA DIVISION.
       FILE SECTION.
       FD  TEST-FILE.
           COPY PORTFLIO.
       
       WORKING-STORAGE SECTION.
           COPY ERRHAND.
           
       01  WS-VARIABLES.
           05  WS-FILE-STATUS      PIC X(2).
      *    Running count and upper limit for generation loop
           05  WS-RECORD-COUNT     PIC 9(5) VALUE 0.
           05  WS-MAX-RECORDS      PIC 9(5) VALUE 100.
           05  WS-CURRENT-DATE     PIC 9(8).
           
      *    Lookup tables for randomized field values
       01  WS-TEST-VALUES.
      *    I=Individual, C=Corporate, T=Trust
           05  WS-CLIENT-TYPES     PIC X(3) VALUE 'ICT'.
      *    A=Active, C=Closed, S=Suspended
           05  WS-STATUS-TYPES     PIC X(3) VALUE 'ACS'.
           05  WS-NAME-PREFIX      PIC X(4) VALUE 'TEST'.
           
       01  WS-SUBSCRIPTS.
           05  WS-TYPE-SUB         PIC 9(1).
           05  WS-STATUS-SUB       PIC 9(1).
           
       PROCEDURE DIVISION.
      *----------------------------------------------------------------*
      * Main control: initialize, generate records in loop, close.     *
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-GENERATE-RECORDS
              UNTIL WS-RECORD-COUNT >= WS-MAX-RECORDS
           PERFORM 3000-TERMINATE
           GOBACK
           .
           
      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Get current date and open output test file.   *
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           ACCEPT WS-CURRENT-DATE FROM DATE YYYYMMDD
           
           OPEN OUTPUT TEST-FILE
           IF WS-FILE-STATUS NOT = '00'
               DISPLAY 'Error opening test file: ' WS-FILE-STATUS
               PERFORM 3000-TERMINATE
               GOBACK
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 2000-GENERATE-RECORDS: Build one complete portfolio record     *
      * from generated sub-fields, then write to output file.          *
      *----------------------------------------------------------------*
       2000-GENERATE-RECORDS.
           INITIALIZE PORT-RECORD
           
           PERFORM 2100-GENERATE-KEY
           PERFORM 2200-GENERATE-CLIENT-INFO
           PERFORM 2300-GENERATE-PORTFOLIO-INFO
           PERFORM 2400-GENERATE-FINANCIAL-INFO
           
           WRITE PORT-RECORD
           
           IF WS-FILE-STATUS = '00'
               ADD 1 TO WS-RECORD-COUNT
           ELSE
               DISPLAY 'Error writing record: ' WS-FILE-STATUS
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 2100-GENERATE-KEY: Build PORT-ID from prefix + counter,        *
      * derive account number from counter offset.                     *
      *----------------------------------------------------------------*
       2100-GENERATE-KEY.
           STRING 'PORT' WS-RECORD-COUNT
               DELIMITED BY SIZE
               INTO PORT-ID
           
           MOVE FUNCTION RANDOM(WS-RECORD-COUNT) TO WS-TYPE-SUB
           COMPUTE PORT-ACCOUNT-NO = WS-RECORD-COUNT + 1000000000
           .
           
      *----------------------------------------------------------------*
      * 2200-GENERATE-CLIENT-INFO: Build client name from prefix +     *
      * counter, select random client type from lookup table.          *
      *----------------------------------------------------------------*
       2200-GENERATE-CLIENT-INFO.
           STRING WS-NAME-PREFIX WS-RECORD-COUNT
               DELIMITED BY SIZE
               INTO PORT-CLIENT-NAME
           
           MOVE WS-CLIENT-TYPES(WS-TYPE-SUB:1) TO PORT-CLIENT-TYPE
           .
           
      *----------------------------------------------------------------*
      * 2300-GENERATE-PORTFOLIO-INFO: Set dates to current, pick a     *
      * random status from A/C/S lookup table.                         *
      *----------------------------------------------------------------*
       2300-GENERATE-PORTFOLIO-INFO.
           MOVE WS-CURRENT-DATE TO PORT-CREATE-DATE
           MOVE WS-CURRENT-DATE TO PORT-LAST-MAINT
           
           COMPUTE WS-STATUS-SUB = FUNCTION RANDOM * 3 + 1
           MOVE WS-STATUS-TYPES(WS-STATUS-SUB:1) TO PORT-STATUS
           .
           
      *----------------------------------------------------------------*
      * 2400-GENERATE-FINANCIAL-INFO: Generate random total value      *
      * (up to 1,000,000) and set cash balance to 10% of total.        *
      *----------------------------------------------------------------*
       2400-GENERATE-FINANCIAL-INFO.
           COMPUTE PORT-TOTAL-VALUE = 
               FUNCTION RANDOM * 1000000
           
           COMPUTE PORT-CASH-BALANCE =
               PORT-TOTAL-VALUE * .10
           .
           
      *----------------------------------------------------------------*
      * 3000-TERMINATE: Close output file and display record count.    *
      *----------------------------------------------------------------*
       3000-TERMINATE.
           CLOSE TEST-FILE
           
           DISPLAY 'Records generated: ' WS-RECORD-COUNT
           .  