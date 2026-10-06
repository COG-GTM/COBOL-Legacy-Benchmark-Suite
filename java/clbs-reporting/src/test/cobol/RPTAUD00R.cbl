       IDENTIFICATION DIVISION.
       PROGRAM-ID. RPTAUD00R.
      *****************************************************************
      * Compilable GnuCOBOL reference for RPTAUD00 (Audit Report). It  *
      * produces the golden RPTFILE the Java AuditReport is compared   *
      * against. Differences from src/programs/batch:                  *
      *  - AUDITLOG / ERRLOG are read as fixed-length sequential files *
      *    (unloaded VSAM / DB2 ERROR_LOG), AUDITLOG.cpy bytes and the *
      *    ERROR_LOG row layout respectively (ERRHAND defines no file  *
      *    record).                                                    *
      *  - RPTDATE (YYMMDD) overrides ACCEPT FROM DATE. The summary    *
      *    paragraphs 2120-/2220-/2300- have no body and write nothing.*
      *****************************************************************
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT AUDIT-FILE ASSIGN TO WS-AUDITLOG-NAME
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-AUDIT-STATUS.
           SELECT ERROR-FILE ASSIGN TO WS-ERRLOG-NAME
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-ERROR-STATUS.
           SELECT REPORT-FILE ASSIGN TO WS-RPTFILE-NAME
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-REPORT-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  AUDIT-FILE
           RECORDING MODE IS F.
           COPY AUDITLOG.
       FD  ERROR-FILE
           RECORDING MODE IS F.
       01  ERROR-LOG-RECORD.
           05  ERL-TIMESTAMP        PIC X(26).
           05  ERL-PROGRAM-ID       PIC X(8).
           05  ERL-ERROR-TYPE       PIC X(1).
           05  ERL-SEVERITY         PIC S9(4) COMP.
           05  ERL-ERROR-CODE       PIC X(8).
           05  ERL-ERROR-MESSAGE    PIC X(200).
           05  ERL-PROCESS-DATE     PIC X(10).
           05  ERL-PROCESS-TIME     PIC X(8).
           05  ERL-USER-ID          PIC X(8).
           05  ERL-ADDITIONAL-INFO  PIC X(500).
       FD  REPORT-FILE
           RECORDING MODE IS F.
       01  REPORT-RECORD             PIC X(132).

       WORKING-STORAGE SECTION.
       01  WS-AUDITLOG-NAME          PIC X(256).
       01  WS-ERRLOG-NAME            PIC X(256).
       01  WS-RPTFILE-NAME           PIC X(256).
       01  WS-ENV-DATE               PIC X(10).
       01  WS-FILE-STATUS.
           05  WS-AUDIT-STATUS       PIC XX.
               88  END-OF-AUDIT      VALUE '10'.
           05  WS-ERROR-STATUS       PIC XX.
               88  END-OF-ERRORS     VALUE '10'.
           05  WS-REPORT-STATUS      PIC XX.

       01  WS-REPORT-HEADERS.
           05  WS-HEADER1.
               10  FILLER            PIC X(132) VALUE ALL '*'.
           05  WS-HEADER2.
               10  FILLER            PIC X(40) VALUE SPACES.
               10  FILLER            PIC X(52)
                   VALUE 'SYSTEM AUDIT REPORT'.
               10  FILLER            PIC X(40) VALUE SPACES.
           05  WS-HEADER3.
               10  FILLER            PIC X(15) VALUE 'REPORT DATE:'.
               10  WS-REPORT-DATE    PIC X(10).
               10  FILLER            PIC X(107) VALUE SPACES.

       01  WS-AUDIT-DETAIL.
           05  WS-AUD-TIMESTAMP     PIC X(26).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-AUD-PROGRAM       PIC X(8).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-AUD-TYPE          PIC X(10).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-AUD-MESSAGE       PIC X(80).

       01  WS-ERROR-DETAIL.
           05  WS-ERR-TIMESTAMP     PIC X(26).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-ERR-PROGRAM       PIC X(8).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-ERR-CODE          PIC X(4).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-ERR-MESSAGE       PIC X(80).

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2110-READ-AUDIT-RECORDS
           PERFORM 2210-READ-ERROR-RECORDS
           PERFORM 3000-CLEANUP
           GOBACK.

       1000-INITIALIZE.
           ACCEPT WS-AUDITLOG-NAME FROM ENVIRONMENT 'AUDITLOG'
           ACCEPT WS-ERRLOG-NAME   FROM ENVIRONMENT 'ERRLOG'
           ACCEPT WS-RPTFILE-NAME  FROM ENVIRONMENT 'RPTFILE'
           OPEN INPUT AUDIT-FILE
           IF WS-AUDIT-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING AUDIT FILE ' WS-AUDIT-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN INPUT ERROR-FILE
           IF WS-ERROR-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING ERROR FILE ' WS-ERROR-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN OUTPUT REPORT-FILE
           IF WS-REPORT-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING REPORT FILE ' WS-REPORT-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           PERFORM 1200-WRITE-HEADERS.

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

       2110-READ-AUDIT-RECORDS.
           READ AUDIT-FILE
           PERFORM UNTIL END-OF-AUDIT
               MOVE AUD-TIMESTAMP TO WS-AUD-TIMESTAMP
               MOVE AUD-PROGRAM   TO WS-AUD-PROGRAM
               MOVE AUD-TYPE      TO WS-AUD-TYPE
               MOVE AUD-MESSAGE   TO WS-AUD-MESSAGE
               WRITE REPORT-RECORD FROM WS-AUDIT-DETAIL
               READ AUDIT-FILE
           END-PERFORM.

       2210-READ-ERROR-RECORDS.
           READ ERROR-FILE
           PERFORM UNTIL END-OF-ERRORS
               MOVE ERL-TIMESTAMP     TO WS-ERR-TIMESTAMP
               MOVE ERL-PROGRAM-ID    TO WS-ERR-PROGRAM
               MOVE ERL-ERROR-CODE    TO WS-ERR-CODE
               MOVE ERL-ERROR-MESSAGE TO WS-ERR-MESSAGE
               WRITE REPORT-RECORD FROM WS-ERROR-DETAIL
               READ ERROR-FILE
           END-PERFORM.

       3000-CLEANUP.
           CLOSE AUDIT-FILE
                 ERROR-FILE
                 REPORT-FILE.
