      *================================================================*
      * Program Name: PORTLOAD
      * Description: Load sequential portfolio data into indexed file
      * Author: Build System
      * Date Written: 2024-03-20
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PORTLOAD.
       
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.
       
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT SEQ-FILE
               ASSIGN TO SEQFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-SEQ-STATUS.
           
           SELECT IDX-FILE
               ASSIGN TO IDXFILE
               ORGANIZATION IS INDEXED
               ACCESS MODE IS RANDOM
               RECORD KEY IS PORT-KEY
               FILE STATUS IS WS-IDX-STATUS.
       
       DATA DIVISION.
       FILE SECTION.
       FD  SEQ-FILE.
       01  SEQ-RECORD               PIC X(200).
       
       FD  IDX-FILE.
           COPY PORTFLIO.
       
       WORKING-STORAGE SECTION.
       01  WS-SWITCHES.
           05  WS-SEQ-STATUS         PIC X(2).
           05  WS-IDX-STATUS         PIC X(2).
           05  WS-EOF-SW             PIC X     VALUE 'N'.
               88  END-OF-FILE                 VALUE 'Y'.
               88  NOT-END-OF-FILE             VALUE 'N'.
       
       01  WS-COUNTERS.
           05  WS-READ-COUNT         PIC 9(7) VALUE ZERO.
           05  WS-WRITE-COUNT        PIC 9(7) VALUE ZERO.
           05  WS-ERROR-COUNT        PIC 9(7) VALUE ZERO.
       
       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS
               UNTIL END-OF-FILE
           PERFORM 3000-TERMINATE
           GOBACK
           .
       
       1000-INITIALIZE.
           OPEN INPUT  SEQ-FILE
           IF WS-SEQ-STATUS NOT = '00'
               DISPLAY 'Error opening input: ' WS-SEQ-STATUS
               GOBACK
           END-IF
           
           OPEN OUTPUT IDX-FILE
           IF WS-IDX-STATUS NOT = '00'
               DISPLAY 'Error opening output: ' WS-IDX-STATUS
               GOBACK
           END-IF
           .
       
       2000-PROCESS.
           READ SEQ-FILE INTO PORT-RECORD
               AT END
                   SET END-OF-FILE TO TRUE
               NOT AT END
                   ADD 1 TO WS-READ-COUNT
                   PERFORM 2100-WRITE-RECORD
           END-READ
           .
       
       2100-WRITE-RECORD.
           WRITE PORT-RECORD
           
           IF WS-IDX-STATUS = '00'
               ADD 1 TO WS-WRITE-COUNT
           ELSE
               ADD 1 TO WS-ERROR-COUNT
               DISPLAY 'Write error: ' WS-IDX-STATUS
                       ' Key: ' PORT-KEY
           END-IF
           .
       
       3000-TERMINATE.
           CLOSE SEQ-FILE
                 IDX-FILE
           
           DISPLAY 'Load Complete'
           DISPLAY 'Records read:    ' WS-READ-COUNT
           DISPLAY 'Records written: ' WS-WRITE-COUNT
           DISPLAY 'Errors:          ' WS-ERROR-COUNT
           .
