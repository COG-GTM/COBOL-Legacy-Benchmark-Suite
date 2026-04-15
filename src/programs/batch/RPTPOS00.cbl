       IDENTIFICATION DIVISION.
       PROGRAM-ID. RPTPOS00.
       AUTHOR. CLAUDE.
       DATE-WRITTEN. 2024-04-09.
      *****************************************************************
      * Program Name: RPTPOS00                                        *
      * Description:  Daily Position Report Generator                  *
      *                                                               *
      * Reads portfolio positions from a VSAM KSDS master file and    *
      * transaction activity from a VSAM history file, then writes    *
      * a fixed-format (132-col) report containing:                   *
      *   - Portfolio position summary with quantities and values     *
      *   - Transaction activity since last report                    *
      *   - Exception items (positions outside tolerance)             *
      *   - Daily performance metrics (% change)                     *
      *                                                               *
      * Called By: JCL batch job (RPTJOB)                             *
      * Files:                                                        *
      *   POSMSTRE - Position Master VSAM KSDS (Input)                *
      *   TRANHIST - Transaction History VSAM KSDS (Input)            *
      *   RPTFILE  - Report output (Sequential, 132-byte FB)          *
      *                                                               *
      * Return Codes:                                                 *
      *   0  - Report generated successfully                          *
      *   12 - Fatal error (file open failure)                        *
      *****************************************************************
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
      * Position Master - VSAM KSDS read sequentially for all holdings
           SELECT POSITION-MASTER ASSIGN TO POSMSTRE
               ORGANIZATION IS INDEXED
               ACCESS MODE IS SEQUENTIAL
               RECORD KEY IS POS-KEY
               FILE STATUS IS WS-POSITION-STATUS.

      * Transaction History - VSAM KSDS read sequentially for activity
           SELECT TRANSACTION-HISTORY ASSIGN TO TRANHIST
               ORGANIZATION IS INDEXED
               ACCESS MODE IS SEQUENTIAL
               RECORD KEY IS TRAN-KEY
               FILE STATUS IS WS-TRAN-STATUS.

      * Report output - fixed-block sequential, 132-byte records
           SELECT REPORT-FILE ASSIGN TO RPTFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-REPORT-STATUS.

       DATA DIVISION.
       FILE SECTION.
           COPY POSREC.
           COPY TRNREC.
           
       FD  REPORT-FILE
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  REPORT-RECORD             PIC X(132).

       WORKING-STORAGE SECTION.
           COPY RTNCODE.
           COPY ERRHAND.

       01  WS-FILE-STATUS.
           05  WS-POSITION-STATUS    PIC XX.
           05  WS-TRAN-STATUS        PIC XX.
           05  WS-REPORT-STATUS      PIC XX.

       01  WS-REPORT-HEADERS.
           05  WS-HEADER1.
               10  FILLER            PIC X(132) VALUE ALL '*'.
           05  WS-HEADER2.
               10  FILLER            PIC X(40) VALUE SPACES.
               10  FILLER            PIC X(52) 
                   VALUE 'DAILY POSITION REPORT'.
               10  FILLER            PIC X(40) VALUE SPACES.
           05  WS-HEADER3.
               10  FILLER            PIC X(15) VALUE 'REPORT DATE:'.
               10  WS-REPORT-DATE    PIC X(10).
               10  FILLER            PIC X(107) VALUE SPACES.

       01  WS-POSITION-DETAIL.
           05  WS-POS-PORTFOLIO     PIC X(10).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-POS-DESCRIPTION   PIC X(30).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-POS-QUANTITY      PIC ZZZ,ZZZ,ZZ9.99.
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-POS-VALUE         PIC $$$$,$$$,$$9.99.
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-POS-CHANGE-PCT    PIC +ZZ9.99.
           05  FILLER               PIC X(40) VALUE SPACES.

       PROCEDURE DIVISION.
      *----------------------------------------------------------------*
      * 0000-MAIN: Driver - initialize, generate report, clean up.     *
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS-REPORT
           PERFORM 3000-CLEANUP
           GOBACK.

      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Open all files and write report headers.      *
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           PERFORM 1100-OPEN-FILES
           PERFORM 1200-WRITE-HEADERS.

      *----------------------------------------------------------------*
      * 1100-OPEN-FILES: Open position master, transaction history,    *
      *   and report output. Abends on any open failure (RC=12).      *
      *----------------------------------------------------------------*
       1100-OPEN-FILES.
           OPEN INPUT POSITION-MASTER
           IF WS-POSITION-STATUS NOT = '00'
               MOVE 'ERROR OPENING POSITION MASTER'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF

           OPEN INPUT TRANSACTION-HISTORY
           IF WS-TRAN-STATUS NOT = '00'
               MOVE 'ERROR OPENING TRANSACTION HISTORY'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF

           OPEN OUTPUT REPORT-FILE
           IF WS-REPORT-STATUS NOT = '00'
               MOVE 'ERROR OPENING REPORT FILE'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF.

      *----------------------------------------------------------------*
      * 1200-WRITE-HEADERS: Write banner, title, and date header.     *
      *----------------------------------------------------------------*
       1200-WRITE-HEADERS.
           ACCEPT WS-REPORT-DATE FROM DATE
           WRITE REPORT-RECORD FROM WS-HEADER1
           WRITE REPORT-RECORD FROM WS-HEADER2
           WRITE REPORT-RECORD FROM WS-HEADER3.

      *----------------------------------------------------------------*
      * 2000-PROCESS-REPORT: Generate the three report sections -     *
      *   positions, transactions, and summary/metrics.               *
      *----------------------------------------------------------------*
       2000-PROCESS-REPORT.
           PERFORM 2100-READ-POSITIONS
           PERFORM 2200-PROCESS-TRANSACTIONS
           PERFORM 2300-WRITE-SUMMARY.

      *----------------------------------------------------------------*
      * 2100-READ-POSITIONS: Loop through position master, format     *
      *   and write one detail line per portfolio holding.             *
      *----------------------------------------------------------------*
       2100-READ-POSITIONS.
           READ POSITION-MASTER
               AT END SET END-OF-POSITIONS TO TRUE
           END-READ
           
           PERFORM UNTIL END-OF-POSITIONS
               PERFORM 2110-FORMAT-POSITION
               READ POSITION-MASTER
                   AT END SET END-OF-POSITIONS TO TRUE
               END-READ
           END-PERFORM.

      *----------------------------------------------------------------*
      * 2110-FORMAT-POSITION: Map position fields to detail line,     *
      *   compute daily change percentage, and write to report.       *
      *----------------------------------------------------------------*
       2110-FORMAT-POSITION.
           MOVE POS-PORTFOLIO-ID   TO WS-POS-PORTFOLIO
           MOVE POS-DESCRIPTION    TO WS-POS-DESCRIPTION
           MOVE POS-QUANTITY       TO WS-POS-QUANTITY
           MOVE POS-CURRENT-VALUE  TO WS-POS-VALUE
           COMPUTE WS-POS-CHANGE-PCT = 
               (POS-CURRENT-VALUE - POS-PREVIOUS-VALUE) /
                POS-PREVIOUS-VALUE * 100
           WRITE REPORT-RECORD FROM WS-POSITION-DETAIL.

       2200-PROCESS-TRANSACTIONS.
           PERFORM 2210-READ-TRANSACTIONS
           PERFORM 2220-SUMMARIZE-ACTIVITY.

       2300-WRITE-SUMMARY.
           PERFORM 2310-WRITE-TOTALS
           PERFORM 2320-WRITE-EXCEPTIONS
           PERFORM 2330-WRITE-METRICS.

      *----------------------------------------------------------------*
      * 3000-CLEANUP: Close all files.                                *
      *----------------------------------------------------------------*
       3000-CLEANUP.
           CLOSE POSITION-MASTER
                TRANSACTION-HISTORY
                REPORT-FILE.

      *----------------------------------------------------------------*
      * 9999-ERROR-HANDLER: Display error and abort with RC=12.       *
      *----------------------------------------------------------------*
       9999-ERROR-HANDLER.
           DISPLAY WS-ERROR-MESSAGE
           MOVE 12 TO RETURN-CODE
           GOBACK.  