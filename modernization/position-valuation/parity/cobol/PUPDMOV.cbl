      *================================================================*
      * Program Name: PUPDMOV
      * Description: Parity harness for the 'V' branch of
      *              PORTUPDT 2200-APPLY-UPDATE:
      *                  MOVE UPDT-NEW-VALUE  TO WS-NUMERIC-WORK
      *                  MOVE WS-NUMERIC-WORK TO PORT-TOTAL-VALUE
      *              i.e. an alphanumeric PIC X(50) moved into
      *              PIC S9(13)V99 and then into PIC S9(13)V99 COMP-3.
      *              Emits: op|input|result|edited
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PUPDMOV.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-CASES.
           05  FILLER PIC X(50) VALUE '1250000'.
           05  FILLER PIC X(50) VALUE '000000000001250000'.
           05  FILLER PIC X(50) VALUE '0'.
           05  FILLER PIC X(50) VALUE '999999999999999'.
           05  FILLER PIC X(50) VALUE '1'.
           05  FILLER PIC X(50) VALUE '  1250000'.
           05  FILLER PIC X(50) VALUE '0000000000000001250000'.
           05  FILLER PIC X(50) VALUE '12500.00'.
       01  FILLER REDEFINES WS-CASES.
           05  WS-CASE-VALUE OCCURS 8 TIMES PIC X(50).

       01  WS-IDX                 PIC 9(4) COMP VALUE 0.
       01  WS-NUMERIC-WORK        PIC S9(13)V99.
       01  WS-TOTAL-VALUE         PIC S9(13)V99 COMP-3.
       01  WS-EDIT-VALUE          PIC -9(13).9(2).

       PROCEDURE DIVISION.
       0000-MAIN.
           DISPLAY 'op|input|result|edited'
           PERFORM VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > 8
               MOVE WS-CASE-VALUE(WS-IDX) TO WS-NUMERIC-WORK
               MOVE WS-NUMERIC-WORK       TO WS-TOTAL-VALUE
               MOVE WS-TOTAL-VALUE        TO WS-EDIT-VALUE
               DISPLAY 'UV|'
                       FUNCTION TRIM(WS-CASE-VALUE(WS-IDX)) '|'
                       FUNCTION TRIM(WS-EDIT-VALUE) '|'
                       WS-EDIT-VALUE
           END-PERFORM
           GOBACK.
