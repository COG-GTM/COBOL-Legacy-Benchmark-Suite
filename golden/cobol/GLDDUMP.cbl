      *================================================================*
      * Program Name: GLDDUMP
      * Description: Golden-dataset harness dumper.
      *              Browses the indexed portfolio master (PORTFILE)
      *              in key order and writes every record, byte for
      *              byte, to a sequential file (DUMPFILE) so the JS
      *              tooling can decode the post-run VSAM state.
      *              Harness program only - it is NOT part of the
      *              legacy application under test.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. GLDDUMP.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.

       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT PORTFOLIO-FILE
               ASSIGN TO PORTFILE
               ORGANIZATION IS INDEXED
               ACCESS MODE IS SEQUENTIAL
               RECORD KEY IS PORT-KEY
               FILE STATUS IS WS-FILE-STATUS.

           SELECT DUMP-FILE
               ASSIGN TO DUMPFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-DUMP-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  PORTFOLIO-FILE.
           COPY PORTFLIO.

       FD  DUMP-FILE.
           COPY PORTFLIO REPLACING LEADING ==PORT== BY ==DUMP==.

       WORKING-STORAGE SECTION.
       01  WS-SWITCHES.
           05  WS-FILE-STATUS      PIC X(02).
               88  WS-SUCCESS-STATUS     VALUE '00'.
           05  WS-DUMP-STATUS      PIC X(02).
               88  WS-DUMP-SUCCESS       VALUE '00'.
           05  WS-END-OF-FILE-SW   PIC X     VALUE 'N'.
               88  END-OF-FILE              VALUE 'Y'.

       01  WS-WORK-AREAS.
           05  WS-DUMP-COUNT       PIC 9(7) VALUE ZERO.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS UNTIL END-OF-FILE
           PERFORM 3000-TERMINATE
           GOBACK.

       1000-INITIALIZE.
           OPEN INPUT  PORTFOLIO-FILE
           OPEN OUTPUT DUMP-FILE

           IF NOT WS-SUCCESS-STATUS OR NOT WS-DUMP-SUCCESS
               DISPLAY 'GLDDUMP open error: '
                       'PORT=' WS-FILE-STATUS
                       'DUMP=' WS-DUMP-STATUS
               STOP RUN
           END-IF
           .

       2000-PROCESS.
           READ PORTFOLIO-FILE NEXT RECORD
               AT END
                   SET END-OF-FILE TO TRUE
               NOT AT END
                   MOVE PORT-RECORD TO DUMP-RECORD
                   WRITE DUMP-RECORD
                   ADD 1 TO WS-DUMP-COUNT
           END-READ
           .

       3000-TERMINATE.
           CLOSE PORTFOLIO-FILE
                 DUMP-FILE

           DISPLAY 'Records dumped: ' WS-DUMP-COUNT
           .
