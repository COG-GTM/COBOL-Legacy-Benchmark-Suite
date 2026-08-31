      *================================================================*
      * Program Name: PVDRIVE
      * Description: Parity harness driver for PORTVALD.
      *              Calls the UNMODIFIED PORTVALD subroutine with a
      *              table of inputs and emits one CSV row per case:
      *                  type|input|return-code|error-message
      *              The emitted CSV is the golden vector file used by
      *              the Java parity tests.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PVDRIVE.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-CASES.
           05  FILLER PIC X(51) VALUE 'IPORT0001'.
           05  FILLER PIC X(51) VALUE 'IPORT9999'.
           05  FILLER PIC X(51) VALUE 'IPORT0001XXXX'.
           05  FILLER PIC X(51) VALUE 'IPORT001A'.
           05  FILLER PIC X(51) VALUE 'IPORT 001'.
           05  FILLER PIC X(51) VALUE 'IPRTF0001'.
           05  FILLER PIC X(51) VALUE 'Iport0001'.
           05  FILLER PIC X(51) VALUE 'IPORT'.
           05  FILLER PIC X(51) VALUE 'I'.
           05  FILLER PIC X(51) VALUE 'A0000000001'.
           05  FILLER PIC X(51) VALUE 'A1234567890'.
           05  FILLER PIC X(51) VALUE 'A0000000000'.
           05  FILLER PIC X(51) VALUE 'AABCDEFGHIJ'.
           05  FILLER PIC X(51) VALUE 'A'.
           05  FILLER PIC X(51) VALUE
           'A11111111111111111111111111111111111111111111111111'.
           05  FILLER PIC X(51) VALUE
           'A00000000000000000000000000000000000000000000000000'.
           05  FILLER PIC X(51) VALUE 'TSTK'.
           05  FILLER PIC X(51) VALUE 'TBND'.
           05  FILLER PIC X(51) VALUE 'TMMF'.
           05  FILLER PIC X(51) VALUE 'TETF'.
           05  FILLER PIC X(51) VALUE 'TXYZ'.
           05  FILLER PIC X(51) VALUE 'Tstk'.
           05  FILLER PIC X(51) VALUE 'T'.
           05  FILLER PIC X(51) VALUE 'M0'.
           05  FILLER PIC X(51) VALUE 'M100'.
           05  FILLER PIC X(51) VALUE 'M000000000010000'.
           05  FILLER PIC X(51) VALUE 'M-100'.
           05  FILLER PIC X(51) VALUE 'M999999999999999'.
           05  FILLER PIC X(51) VALUE 'M1000000000000000'.
           05  FILLER PIC X(51) VALUE 'MABC'.
           05  FILLER PIC X(51) VALUE 'M'.
           05  FILLER PIC X(51) VALUE 'X0001'.
           05  FILLER PIC X(51) VALUE ' 0001'.
       01  FILLER REDEFINES WS-CASES.
           05  WS-CASE OCCURS 33 TIMES.
               10  WS-CASE-TYPE   PIC X(01).
               10  WS-CASE-VALUE  PIC X(50).

       01  WS-IDX                 PIC 9(4) COMP VALUE 0.
       01  WS-RC-EDIT             PIC -9(4).

       01  WS-REQUEST.
           05  WS-VALIDATE-TYPE   PIC X(1).
           05  WS-INPUT-VALUE     PIC X(50).
           05  WS-RETURN-CODE     PIC S9(4) COMP.
           05  WS-ERROR-MSG       PIC X(50).

       PROCEDURE DIVISION.
       0000-MAIN.
           DISPLAY 'type|input|rc|message'
           PERFORM VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > 33
               MOVE WS-CASE-TYPE(WS-IDX)  TO WS-VALIDATE-TYPE
               MOVE WS-CASE-VALUE(WS-IDX) TO WS-INPUT-VALUE
               MOVE 9999 TO WS-RETURN-CODE
               MOVE ALL '?' TO WS-ERROR-MSG
               CALL 'PORTVALD' USING WS-REQUEST
               MOVE WS-RETURN-CODE TO WS-RC-EDIT
               DISPLAY WS-VALIDATE-TYPE '|'
                       FUNCTION TRIM(WS-INPUT-VALUE) '|'
                       FUNCTION TRIM(WS-RC-EDIT) '|'
                       FUNCTION TRIM(WS-ERROR-MSG)
           END-PERFORM
           GOBACK.
