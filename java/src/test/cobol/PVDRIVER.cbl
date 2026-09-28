      *================================================================*
      * Program Name: PVDRIVER
      * Description: Parity-harness driver for PORTVALD (test only).
      *              Reads fixed 51-byte case records from PVIN, CALLs
      *              the unmodified PORTVALD module once per record
      *              and writes one fixed 76-byte result record to
      *              PVOUT. Contains no validation logic of its own.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PVDRIVER.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT CASE-FILE ASSIGN TO PVIN
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-IN-STATUS.
           SELECT RESULT-FILE ASSIGN TO PVOUT
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-OUT-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  CASE-FILE.
       01  CASE-REC.
           05  CASE-VALIDATE-TYPE  PIC X(1).
           05  CASE-INPUT-VALUE    PIC X(50).

       FD  RESULT-FILE.
       01  RESULT-REC.
           05  RES-RETURN-CODE     PIC S9(4) SIGN LEADING SEPARATE.
           05  RES-ERROR-MSG       PIC X(50).
           05  RES-CALL-RC         PIC S9(4) SIGN LEADING SEPARATE.
           05  RES-MOVE-PROBE      PIC S9(13)V99
                                   SIGN LEADING SEPARATE.

       WORKING-STORAGE SECTION.
       01  WS-IN-STATUS            PIC X(2).
       01  WS-OUT-STATUS           PIC X(2).
       01  WS-EOF-FLAG             PIC X(1) VALUE 'N'.
           88  WS-EOF              VALUE 'Y'.

      * Byte-for-byte mirror of PORTVALD LS-VALIDATION-REQUEST.
       01  WS-VALIDATION-REQUEST.
           05  WS-VALIDATE-TYPE    PIC X(1).
           05  WS-INPUT-VALUE      PIC X(50).
           05  WS-RETURN-CODE      PIC S9(4) COMP.
           05  WS-ERROR-MSG        PIC X(50).

      * Same PIC as PORTVAL VAL-TEMP-NUM; replays the MOVE that
      * PORTVALD 4000-VALIDATE-AMOUNT performs so the numeric
      * conversion (not visible in PORTVALD's outputs) can be compared.
       01  WS-MOVE-PROBE           PIC S9(13)V99.

       PROCEDURE DIVISION.
       0000-MAIN.
           OPEN INPUT CASE-FILE
           IF WS-IN-STATUS NOT = '00'
               DISPLAY 'PVDRIVER: open PVIN failed ' WS-IN-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN OUTPUT RESULT-FILE
           IF WS-OUT-STATUS NOT = '00'
               DISPLAY 'PVDRIVER: open PVOUT failed ' WS-OUT-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF

           PERFORM UNTIL WS-EOF
               READ CASE-FILE
                   AT END
                       SET WS-EOF TO TRUE
                   NOT AT END
                       PERFORM 1000-RUN-CASE
               END-READ
           END-PERFORM

           CLOSE CASE-FILE RESULT-FILE
           MOVE 0 TO RETURN-CODE
           STOP RUN
           .

       1000-RUN-CASE.
      * Sentinels prove PORTVALD overwrites both outputs on every path.
           MOVE CASE-VALIDATE-TYPE TO WS-VALIDATE-TYPE
           MOVE CASE-INPUT-VALUE   TO WS-INPUT-VALUE
           MOVE -9999              TO WS-RETURN-CODE
           MOVE ALL '?'            TO WS-ERROR-MSG
           MOVE 77                 TO RETURN-CODE

           CALL 'PORTVALD' USING WS-VALIDATION-REQUEST

           MOVE WS-RETURN-CODE     TO RES-RETURN-CODE
           MOVE WS-ERROR-MSG       TO RES-ERROR-MSG
           MOVE RETURN-CODE        TO RES-CALL-RC

           MOVE CASE-INPUT-VALUE   TO WS-MOVE-PROBE
           MOVE WS-MOVE-PROBE      TO RES-MOVE-PROBE

           WRITE RESULT-REC
           IF WS-OUT-STATUS NOT = '00'
               DISPLAY 'PVDRIVER: write PVOUT failed ' WS-OUT-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           .
