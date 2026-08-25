      *================================================================*
      * Program Name: GLDLOAD
      * Description: Golden-dataset harness loader.
      *              Loads a sequential seed file of PORTFLIO records
      *              into the indexed portfolio master (PORTFILE),
      *              creating the file.  Harness program only - it is
      *              NOT part of the legacy application under test.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. GLDLOAD.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.

       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT PORTFOLIO-FILE
               ASSIGN TO PORTFILE
               ORGANIZATION IS INDEXED
               ACCESS MODE IS RANDOM
               RECORD KEY IS PORT-KEY
               FILE STATUS IS WS-FILE-STATUS.

           SELECT SEED-FILE
               ASSIGN TO SEEDFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-SEED-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  PORTFOLIO-FILE.
           COPY PORTFLIO.

       FD  SEED-FILE.
           COPY PORTFLIO REPLACING LEADING ==PORT== BY ==SEED==.

       WORKING-STORAGE SECTION.
       01  WS-SWITCHES.
           05  WS-FILE-STATUS      PIC X(02).
               88  WS-SUCCESS-STATUS     VALUE '00'.
           05  WS-SEED-STATUS      PIC X(02).
               88  WS-SEED-SUCCESS       VALUE '00'.
           05  WS-END-OF-FILE-SW   PIC X     VALUE 'N'.
               88  END-OF-FILE              VALUE 'Y'.

       01  WS-WORK-AREAS.
           05  WS-LOAD-COUNT       PIC 9(7) VALUE ZERO.
           05  WS-ERROR-COUNT      PIC 9(7) VALUE ZERO.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS UNTIL END-OF-FILE
           PERFORM 3000-TERMINATE
           GOBACK.

       1000-INITIALIZE.
           OPEN OUTPUT PORTFOLIO-FILE
           OPEN INPUT  SEED-FILE

           IF NOT WS-SUCCESS-STATUS OR NOT WS-SEED-SUCCESS
               DISPLAY 'GLDLOAD open error: '
                       'PORT=' WS-FILE-STATUS
                       'SEED=' WS-SEED-STATUS
               STOP RUN
           END-IF
           .

       2000-PROCESS.
           READ SEED-FILE INTO PORT-RECORD
               AT END
                   SET END-OF-FILE TO TRUE
               NOT AT END
                   PERFORM 2100-WRITE-RECORD
           END-READ
           .

       2100-WRITE-RECORD.
           WRITE PORT-RECORD

           IF WS-SUCCESS-STATUS
               ADD 1 TO WS-LOAD-COUNT
           ELSE
               ADD 1 TO WS-ERROR-COUNT
               DISPLAY 'GLDLOAD write error ' WS-FILE-STATUS
                       ' key=' PORT-KEY
           END-IF
           .

       3000-TERMINATE.
           CLOSE PORTFOLIO-FILE
                 SEED-FILE

           DISPLAY 'Seed records loaded: ' WS-LOAD-COUNT
           DISPLAY 'Seed load errors:    ' WS-ERROR-COUNT
           .
