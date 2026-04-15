  *================================================================*
      * Program Name: PORTTEST
      * Description: Portfolio Test Data Generator
      *   Generates synthetic portfolio test records with random
      *   client types, statuses, and financial values. Creates
      *   up to 100 records with sequentially numbered IDs
      *   (PORT00001..PORT00100) for testing and benchmarking.
      * Files: TESTFILE (sequential output)
      * Copybooks: PORTFLIO, ERRHAND
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
           05  WS-RECORD-COUNT     PIC 9(5) VALUE 0.
           05  WS-MAX-RECORDS      PIC 9(5) VALUE 100.
           05  WS-CURRENT-DATE     PIC 9(8).
           
       01  WS-TEST-VALUES.
           05  WS-CLIENT-TYPES     PIC X(3) VALUE 'ICT'.
           05  WS-STATUS-TYPES     PIC X(3) VALUE 'ACS'.
           05  WS-NAME-PREFIX      PIC X(4) VALUE 'TEST'.
           
       01  WS-SUBSCRIPTS.
           05  WS-TYPE-SUB         PIC 9(1).
           05  WS-STATUS-SUB       PIC 9(1).
           
       PROCEDURE DIVISION.
      *----------------------------------------------------------------*
      * Main control flow: Initialize, generate records up to
      * the configured maximum, then close and report count.
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-GENERATE-RECORDS
              UNTIL WS-RECORD-COUNT >= WS-MAX-RECORDS
           PERFORM 3000-TERMINATE
           GOBACK
           .
           
      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Gets current date and opens the output
      * test file.
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
      * 2000-GENERATE-RECORDS: Builds one test record by
      * generating key, client info, portfolio info, and
      * financial data, then writes it to the output file.
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
      * 2100-GENERATE-KEY: Creates a portfolio ID (PORT + count)
      * and a sequential 10-digit account number.
      *----------------------------------------------------------------*
       2100-GENERATE-KEY.
           STRING 'PORT' WS-RECORD-COUNT
               DELIMITED BY SIZE
               INTO PORT-ID
           
           MOVE FUNCTION RANDOM(WS-RECORD-COUNT) TO WS-TYPE-SUB
           COMPUTE PORT-ACCOUNT-NO = WS-RECORD-COUNT + 1000000000
           .
           
      *----------------------------------------------------------------*
      * 2200-GENERATE-CLIENT-INFO: Generates a client name and
      * randomly assigns a client type (I/C/T).
      *----------------------------------------------------------------*
       2200-GENERATE-CLIENT-INFO.
           STRING WS-NAME-PREFIX WS-RECORD-COUNT
               DELIMITED BY SIZE
               INTO PORT-CLIENT-NAME
           
           MOVE WS-CLIENT-TYPES(WS-TYPE-SUB:1) TO PORT-CLIENT-TYPE
           .
           
      *----------------------------------------------------------------*
      * 2300-GENERATE-PORTFOLIO-INFO: Sets creation and last-
      * maintenance dates; randomly assigns status (A/C/S).
      *----------------------------------------------------------------*
       2300-GENERATE-PORTFOLIO-INFO.
           MOVE WS-CURRENT-DATE TO PORT-CREATE-DATE
           MOVE WS-CURRENT-DATE TO PORT-LAST-MAINT
           
           COMPUTE WS-STATUS-SUB = FUNCTION RANDOM * 3 + 1
           MOVE WS-STATUS-TYPES(WS-STATUS-SUB:1) TO PORT-STATUS
           .
           
      *----------------------------------------------------------------*
      * 2400-GENERATE-FINANCIAL-INFO: Generates random total
      * value (up to 1M) and sets cash balance at 10%.
      *----------------------------------------------------------------*
       2400-GENERATE-FINANCIAL-INFO.
           COMPUTE PORT-TOTAL-VALUE = 
               FUNCTION RANDOM * 1000000
           
           COMPUTE PORT-CASH-BALANCE =
               PORT-TOTAL-VALUE * .10
           .
           
      *----------------------------------------------------------------*
      * 3000-TERMINATE: Closes output file and displays count.
      *----------------------------------------------------------------*
       3000-TERMINATE.
           CLOSE TEST-FILE
           
           DISPLAY 'Records generated: ' WS-RECORD-COUNT
           .  