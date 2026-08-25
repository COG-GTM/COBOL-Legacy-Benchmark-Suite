      *================================================================*
      * Program Name: GLDVALD
      * Description: Golden-dataset harness driver for PORTVALD.
      *              Reads one validation request per record from
      *              VALDFILE (case-id X(10), validate-type X(1),
      *              input-value X(50)), CALLs the PORTVALD
      *              subroutine unchanged and DISPLAYs the returned
      *              LS-RETURN-CODE and LS-ERROR-MSG so the JS parity
      *              runner can capture them.  Harness program only -
      *              it is NOT part of the legacy application.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. GLDVALD.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.

       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT REQUEST-FILE
               ASSIGN TO VALDFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-REQ-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  REQUEST-FILE.
       01  REQUEST-RECORD.
           05  REQ-CASE-ID         PIC X(10).
           05  REQ-VALIDATE-TYPE   PIC X(01).
           05  REQ-INPUT-VALUE     PIC X(50).

       WORKING-STORAGE SECTION.
       01  WS-SWITCHES.
           05  WS-REQ-STATUS       PIC X(02).
               88  WS-REQ-SUCCESS        VALUE '00'.
           05  WS-END-OF-FILE-SW   PIC X     VALUE 'N'.
               88  END-OF-FILE              VALUE 'Y'.

       01  WS-COUNTERS.
           05  WS-CALL-COUNT       PIC 9(7) VALUE ZERO.

       01  WS-RETURN-CODE-DISP     PIC S9(4) SIGN LEADING SEPARATE.

       01  WS-VALIDATION-REQUEST.
           05  WS-VALIDATE-TYPE    PIC X(1).
           05  WS-INPUT-VALUE      PIC X(50).
           05  WS-RETURN-CODE      PIC S9(4) COMP.
           05  WS-ERROR-MSG        PIC X(50).

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS UNTIL END-OF-FILE
           PERFORM 3000-TERMINATE
           GOBACK.

       1000-INITIALIZE.
           OPEN INPUT REQUEST-FILE

           IF NOT WS-REQ-SUCCESS
               DISPLAY 'GLDVALD open error: VALD=' WS-REQ-STATUS
               STOP RUN
           END-IF
           .

       2000-PROCESS.
           READ REQUEST-FILE
               AT END
                   SET END-OF-FILE TO TRUE
               NOT AT END
                   PERFORM 2100-CALL-VALIDATOR
           END-READ
           .

       2100-CALL-VALIDATOR.
           MOVE REQ-VALIDATE-TYPE TO WS-VALIDATE-TYPE
           MOVE REQ-INPUT-VALUE   TO WS-INPUT-VALUE
           MOVE ZERO              TO WS-RETURN-CODE
           MOVE SPACES            TO WS-ERROR-MSG

           CALL 'PORTVALD' USING WS-VALIDATION-REQUEST

           MOVE WS-RETURN-CODE TO WS-RETURN-CODE-DISP
           ADD 1 TO WS-CALL-COUNT

           DISPLAY 'VALD CASE=' REQ-CASE-ID
                   ' TYPE=' REQ-VALIDATE-TYPE
                   ' RC=' WS-RETURN-CODE-DISP
                   ' MSG=' WS-ERROR-MSG
           .

       3000-TERMINATE.
           CLOSE REQUEST-FILE
           DISPLAY 'Validations performed: ' WS-CALL-COUNT
           .
