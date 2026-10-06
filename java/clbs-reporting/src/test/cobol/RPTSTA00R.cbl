       IDENTIFICATION DIVISION.
       PROGRAM-ID. RPTSTA00R.
      *****************************************************************
      * Compilable GnuCOBOL reference for RPTSTA00 (System Statistics  *
      * Report). It produces the golden RPTFILE the Java               *
      * StatisticsReport is compared against. Differences from         *
      * src/programs/batch:                                            *
      *  - DB2STATS carries the WS-STATS-RECORD layout declared in     *
      *    DB2STAT.cbl (the DB2STAT copybook was never checked in);    *
      *    BCHSTATS carries BCHCTL.cpy. Both are fixed-length          *
      *    sequential files.                                           *
      *  - The accumulate/calculate paragraphs, which have no body in  *
      *    the original, are defined as: DB2 calls = sum of row ops,   *
      *    commits and rollbacks; avg response = elapsed / calls;      *
      *    batch success = BCT-STATUS-DONE, failed = BCT-STATUS-ERROR; *
      *    success rate = success / jobs * 100. Divisions by zero      *
      *    yield zero. 2430-WRITE-TREND-ANALYSIS writes nothing.       *
      *  - RPTDATE (YYMMDD) overrides ACCEPT FROM DATE.                *
      *****************************************************************
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT DB2-STATS ASSIGN TO WS-DB2STATS-NAME
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-DB2-STATUS.
           SELECT BATCH-STATS ASSIGN TO WS-BCHSTATS-NAME
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-BCH-STATUS.
           SELECT REPORT-FILE ASSIGN TO WS-RPTFILE-NAME
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-REPORT-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  DB2-STATS
           RECORDING MODE IS F.
       01  DB2-STATS-RECORD.
           05  DBS-PROGRAM-ID      PIC X(8).
           05  DBS-START-TIME      PIC X(26).
           05  DBS-END-TIME        PIC X(26).
           05  DBS-ROWS-READ       PIC S9(9) COMP.
           05  DBS-ROWS-INSERTED   PIC S9(9) COMP.
           05  DBS-ROWS-UPDATED    PIC S9(9) COMP.
           05  DBS-ROWS-DELETED    PIC S9(9) COMP.
           05  DBS-COMMITS         PIC S9(9) COMP.
           05  DBS-ROLLBACKS       PIC S9(9) COMP.
           05  DBS-CPU-TIME        PIC S9(9)V99 COMP-3.
           05  DBS-ELAPSED-TIME    PIC S9(9)V99 COMP-3.
       FD  BATCH-STATS
           RECORDING MODE IS F.
           COPY BCHCTL.
       FD  REPORT-FILE
           RECORDING MODE IS F.
       01  REPORT-RECORD             PIC X(132).

       WORKING-STORAGE SECTION.
       01  WS-DB2STATS-NAME          PIC X(256).
       01  WS-BCHSTATS-NAME          PIC X(256).
       01  WS-RPTFILE-NAME           PIC X(256).
       01  WS-ENV-DATE               PIC X(10).
       01  WS-FILE-STATUS.
           05  WS-DB2-STATUS         PIC XX.
               88  END-OF-DB2-STATS  VALUE '10'.
           05  WS-BCH-STATUS         PIC XX.
               88  END-OF-BATCH-STATS VALUE '10'.
           05  WS-REPORT-STATUS      PIC XX.

       01  WS-REPORT-HEADERS.
           05  WS-HEADER1.
               10  FILLER            PIC X(132) VALUE ALL '*'.
           05  WS-HEADER2.
               10  FILLER            PIC X(35) VALUE SPACES.
               10  FILLER            PIC X(62)
                   VALUE 'SYSTEM STATISTICS AND PERFORMANCE REPORT'.
               10  FILLER            PIC X(35) VALUE SPACES.
           05  WS-HEADER3.
               10  FILLER            PIC X(15) VALUE 'REPORT DATE:'.
               10  WS-REPORT-DATE    PIC X(10).
               10  FILLER            PIC X(107) VALUE SPACES.

       01  WS-PERFORMANCE-METRICS.
           05  WS-DB2-METRICS.
               10  WS-DB2-CALLS         PIC 9(9).
               10  WS-DB2-ELAPSED       PIC 9(9)V99.
               10  WS-DB2-CPU           PIC 9(9)V99.
               10  WS-DB2-WAIT          PIC 9(9)V99.
           05  WS-BATCH-METRICS.
               10  WS-BATCH-JOBS        PIC 9(9).
               10  WS-BATCH-SUCCESS     PIC 9(9).
               10  WS-BATCH-FAILED      PIC 9(9).
               10  WS-BATCH-ELAPSED     PIC 9(9)V99.

       01  WS-DETAIL-LINES.
           05  WS-DB2-DETAIL.
               10  FILLER               PIC X(20) VALUE 'DB2 CALLS:'.
               10  WS-DB2-CALLS-OUT     PIC ZZZ,ZZZ,ZZ9.
               10  FILLER               PIC X(20) VALUE SPACES.
               10  FILLER               PIC X(20) VALUE 'AVG RESPONSE:'.
               10  WS-DB2-AVG-RESP      PIC ZZ,ZZ9.999.
               10  FILLER               PIC X(40) VALUE SPACES.
           05  WS-BATCH-DETAIL.
               10  FILLER               PIC X(20) VALUE 'BATCH JOBS:'.
               10  WS-BATCH-TOTAL       PIC ZZZ,ZZ9.
               10  FILLER               PIC X(10) VALUE SPACES.
               10  FILLER               PIC X(20) VALUE 'SUCCESS RATE:'.
               10  WS-SUCCESS-RATE      PIC ZZ9.99.
               10  FILLER               PIC X(05) VALUE '%'.
               10  FILLER               PIC X(40) VALUE SPACES.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2100-PROCESS-DB2-STATS
           PERFORM 2200-PROCESS-BATCH-STATS
           PERFORM 2300-CALCULATE-METRICS
           PERFORM 2400-WRITE-REPORT
           PERFORM 3000-CLEANUP
           GOBACK.

       1000-INITIALIZE.
           ACCEPT WS-DB2STATS-NAME FROM ENVIRONMENT 'DB2STATS'
           ACCEPT WS-BCHSTATS-NAME FROM ENVIRONMENT 'BCHSTATS'
           ACCEPT WS-RPTFILE-NAME  FROM ENVIRONMENT 'RPTFILE'
           OPEN INPUT DB2-STATS
           IF WS-DB2-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING DB2 STATS ' WS-DB2-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN INPUT BATCH-STATS
           IF WS-BCH-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING BATCH STATS ' WS-BCH-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN OUTPUT REPORT-FILE
           IF WS-REPORT-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING REPORT FILE ' WS-REPORT-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           PERFORM 1200-WRITE-HEADERS
           INITIALIZE WS-PERFORMANCE-METRICS.

       1200-WRITE-HEADERS.
           ACCEPT WS-ENV-DATE FROM ENVIRONMENT 'RPTDATE'
           IF WS-ENV-DATE = SPACES
               ACCEPT WS-REPORT-DATE FROM DATE
           ELSE
               MOVE WS-ENV-DATE TO WS-REPORT-DATE
           END-IF
           WRITE REPORT-RECORD FROM WS-HEADER1
           WRITE REPORT-RECORD FROM WS-HEADER2
           WRITE REPORT-RECORD FROM WS-HEADER3.

       2100-PROCESS-DB2-STATS.
           READ DB2-STATS
           PERFORM UNTIL END-OF-DB2-STATS
               ADD DBS-ROWS-READ DBS-ROWS-INSERTED DBS-ROWS-UPDATED
                   DBS-ROWS-DELETED DBS-COMMITS DBS-ROLLBACKS
                   TO WS-DB2-CALLS
               ADD DBS-ELAPSED-TIME TO WS-DB2-ELAPSED
               ADD DBS-CPU-TIME     TO WS-DB2-CPU
               READ DB2-STATS
           END-PERFORM.

       2200-PROCESS-BATCH-STATS.
           READ BATCH-STATS
           PERFORM UNTIL END-OF-BATCH-STATS
               ADD 1 TO WS-BATCH-JOBS
               IF BCT-STATUS-DONE
                   ADD 1 TO WS-BATCH-SUCCESS
               END-IF
               IF BCT-STATUS-ERROR
                   ADD 1 TO WS-BATCH-FAILED
               END-IF
               READ BATCH-STATS
           END-PERFORM.

       2300-CALCULATE-METRICS.
           IF WS-DB2-CALLS = ZERO
               MOVE ZERO TO WS-DB2-AVG-RESP
           ELSE
               COMPUTE WS-DB2-AVG-RESP =
                   WS-DB2-ELAPSED / WS-DB2-CALLS
           END-IF
           IF WS-BATCH-JOBS = ZERO
               MOVE ZERO TO WS-SUCCESS-RATE
           ELSE
               COMPUTE WS-SUCCESS-RATE =
                   WS-BATCH-SUCCESS / WS-BATCH-JOBS * 100
           END-IF.

       2400-WRITE-REPORT.
           MOVE WS-DB2-CALLS  TO WS-DB2-CALLS-OUT
           MOVE WS-BATCH-JOBS TO WS-BATCH-TOTAL
           WRITE REPORT-RECORD FROM WS-DB2-DETAIL
           WRITE REPORT-RECORD FROM WS-BATCH-DETAIL.

       3000-CLEANUP.
           CLOSE DB2-STATS
                 BATCH-STATS
                 REPORT-FILE.
