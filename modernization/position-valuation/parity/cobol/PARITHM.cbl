      *================================================================*
      * Program Name: PARITHM
      * Description: Parity harness for the packed-decimal arithmetic
      *              of the position valuation / update slice.
      *              Re-executes, with the EXACT PIC clauses used by
      *              POSREC / TRNREC / RPTPOS00, the arithmetic of:
      *                PORTTRAN 2210-PROCESS-BUY   (op AQ / AA)
      *                PORTTRAN 2220-PROCESS-SELL  (op SQ / SA)
      *                PORTTRAN 2240-PROCESS-FEE   (op SA)
      *                RPTPOS00 2110-FORMAT-POSITION (op PC)
      *              and emits one CSV row per case:
      *                  op|operand-a|operand-b|result|edited
      *              The emitted CSV is the golden vector file used by
      *              the Java parity tests.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PARITHM.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-CASES.
      *    ---- op --------- operand A ------- operand B -------------*
           05  FILLER PIC X(02) VALUE 'AQ'.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC X(02) VALUE 'AQ'.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC S9(13)V9(4) VALUE +25.5.
           05  FILLER PIC X(02) VALUE 'AQ'.
           05  FILLER PIC S9(13)V9(4) VALUE +1.0001.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC X(02) VALUE 'AQ'.
           05  FILLER PIC S9(13)V9(4) VALUE +0.9999.
           05  FILLER PIC S9(13)V9(4) VALUE +0.0001.
           05  FILLER PIC X(02) VALUE 'AQ'.
           05  FILLER PIC S9(13)V9(4) VALUE -0.9999.
           05  FILLER PIC S9(13)V9(4) VALUE -0.0001.
           05  FILLER PIC X(02) VALUE 'AQ'.
           05  FILLER PIC S9(13)V9(4) VALUE +99999999999.9999.
           05  FILLER PIC S9(13)V9(4) VALUE +0.0001.
           05  FILLER PIC X(02) VALUE 'AQ'.
           05  FILLER PIC S9(13)V9(4) VALUE +99999999999.9999.
           05  FILLER PIC S9(13)V9(4) VALUE +1.
           05  FILLER PIC X(02) VALUE 'SQ'.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC S9(13)V9(4) VALUE +25.5.
           05  FILLER PIC X(02) VALUE 'SQ'.
           05  FILLER PIC S9(13)V9(4) VALUE +25.5.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC X(02) VALUE 'SQ'.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC S9(13)V9(4) VALUE +0.0001.
           05  FILLER PIC X(02) VALUE 'AA'.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC X(02) VALUE 'AA'.
           05  FILLER PIC S9(13)V9(4) VALUE +12500.
           05  FILLER PIC S9(13)V9(4) VALUE +37.55.
           05  FILLER PIC X(02) VALUE 'AA'.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC S9(13)V9(4) VALUE +0.005.
           05  FILLER PIC X(02) VALUE 'AA'.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC S9(13)V9(4) VALUE +0.999.
           05  FILLER PIC X(02) VALUE 'AA'.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC S9(13)V9(4) VALUE -0.999.
           05  FILLER PIC X(02) VALUE 'AA'.
           05  FILLER PIC S9(13)V9(4) VALUE +9999999999999.99.
           05  FILLER PIC S9(13)V9(4) VALUE +0.01.
           05  FILLER PIC X(02) VALUE 'SA'.
           05  FILLER PIC S9(13)V9(4) VALUE +12500.
           05  FILLER PIC S9(13)V9(4) VALUE +37.55.
           05  FILLER PIC X(02) VALUE 'SA'.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC S9(13)V9(4) VALUE +250.75.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +110.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +90.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +100.005.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +1000.
           05  FILLER PIC S9(13)V9(4) VALUE +3000.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +2000.
           05  FILLER PIC S9(13)V9(4) VALUE +3000.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +12345.67.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +0.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE -50.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
           05  FILLER PIC S9(13)V9(4) VALUE -100.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +3.
           05  FILLER PIC S9(13)V9(4) VALUE +7.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +7.
           05  FILLER PIC S9(13)V9(4) VALUE +3.
           05  FILLER PIC X(02) VALUE 'PC'.
           05  FILLER PIC S9(13)V9(4) VALUE +100.01.
           05  FILLER PIC S9(13)V9(4) VALUE +100.
       01  FILLER REDEFINES WS-CASES.
           05  WS-CASE OCCURS 31 TIMES.
               10  WS-OP          PIC X(02).
               10  WS-A           PIC S9(13)V9(4).
               10  WS-B           PIC S9(13)V9(4).

       01  WS-IDX                 PIC 9(4) COMP VALUE 0.

      *----------------------------------------------------------------*
      * Target fields, with the PIC clauses of the production records
      *----------------------------------------------------------------*
       01  WS-TARGETS.
           05  WS-QTY             PIC S9(11)V9(4) COMP-3.
           05  WS-QTY-OPERAND     PIC S9(11)V9(4) COMP-3.
           05  WS-AMT             PIC S9(13)V9(2) COMP-3.
           05  WS-AMT-OPERAND     PIC S9(13)V9(2) COMP-3.
           05  WS-CUR-VALUE       PIC S9(13)V9(2) COMP-3.
           05  WS-PRV-VALUE       PIC S9(13)V9(2) COMP-3.
           05  WS-CHANGE-PCT      PIC +ZZ9.99.

       01  WS-EDIT-A              PIC -9(13).9(4).
       01  WS-EDIT-B              PIC -9(13).9(4).
       01  WS-EDIT-QTY            PIC -9(11).9(4).
       01  WS-EDIT-AMT            PIC -9(13).9(2).

       PROCEDURE DIVISION.
       0000-MAIN.
           DISPLAY 'op|a|b|result|edited'
           PERFORM VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > 31
               MOVE WS-A(WS-IDX) TO WS-EDIT-A
               MOVE WS-B(WS-IDX) TO WS-EDIT-B
               EVALUATE WS-OP(WS-IDX)
                   WHEN 'AQ' PERFORM 1000-ADD-QUANTITY
                   WHEN 'SQ' PERFORM 1100-SUB-QUANTITY
                   WHEN 'AA' PERFORM 1200-ADD-AMOUNT
                   WHEN 'SA' PERFORM 1300-SUB-AMOUNT
                   WHEN 'PC' PERFORM 1400-CHANGE-PCT
               END-EVALUATE
           END-PERFORM
           GOBACK.

      *----------------------------------------------------------------*
      * PORTTRAN 2210-PROCESS-BUY : ADD TRN-QUANTITY TO PORT-TOTAL-UNITS
      *----------------------------------------------------------------*
       1000-ADD-QUANTITY.
           MOVE WS-A(WS-IDX) TO WS-QTY
           MOVE WS-B(WS-IDX) TO WS-QTY-OPERAND
           ADD WS-QTY-OPERAND TO WS-QTY
           MOVE WS-QTY TO WS-EDIT-QTY
           PERFORM 9000-EMIT-QTY.

      *----------------------------------------------------------------*
      * PORTTRAN 2220-PROCESS-SELL : SUBTRACT TRN-QUANTITY FROM ...
      *----------------------------------------------------------------*
       1100-SUB-QUANTITY.
           MOVE WS-A(WS-IDX) TO WS-QTY
           MOVE WS-B(WS-IDX) TO WS-QTY-OPERAND
           SUBTRACT WS-QTY-OPERAND FROM WS-QTY
           MOVE WS-QTY TO WS-EDIT-QTY
           PERFORM 9000-EMIT-QTY.

      *----------------------------------------------------------------*
      * PORTTRAN 2210-PROCESS-BUY : ADD TRN-AMOUNT TO PORT-TOTAL-COST
      *----------------------------------------------------------------*
       1200-ADD-AMOUNT.
           MOVE WS-A(WS-IDX) TO WS-AMT
           MOVE WS-B(WS-IDX) TO WS-AMT-OPERAND
           ADD WS-AMT-OPERAND TO WS-AMT
           MOVE WS-AMT TO WS-EDIT-AMT
           PERFORM 9000-EMIT-AMT.

      *----------------------------------------------------------------*
      * PORTTRAN 2220-PROCESS-SELL / 2240-PROCESS-FEE : SUBTRACT amount
      *----------------------------------------------------------------*
       1300-SUB-AMOUNT.
           MOVE WS-A(WS-IDX) TO WS-AMT
           MOVE WS-B(WS-IDX) TO WS-AMT-OPERAND
           SUBTRACT WS-AMT-OPERAND FROM WS-AMT
           MOVE WS-AMT TO WS-EDIT-AMT
           PERFORM 9000-EMIT-AMT.

      *----------------------------------------------------------------*
      * RPTPOS00 2110-FORMAT-POSITION
      *----------------------------------------------------------------*
       1400-CHANGE-PCT.
           MOVE WS-A(WS-IDX) TO WS-CUR-VALUE
           MOVE WS-B(WS-IDX) TO WS-PRV-VALUE
           COMPUTE WS-CHANGE-PCT =
               (WS-CUR-VALUE - WS-PRV-VALUE) /
                WS-PRV-VALUE * 100
           DISPLAY WS-OP(WS-IDX) '|'
                   FUNCTION TRIM(WS-EDIT-A) '|'
                   FUNCTION TRIM(WS-EDIT-B) '|'
                   FUNCTION TRIM(WS-CHANGE-PCT) '|'
                   WS-CHANGE-PCT.

       9000-EMIT-QTY.
           DISPLAY WS-OP(WS-IDX) '|'
                   FUNCTION TRIM(WS-EDIT-A) '|'
                   FUNCTION TRIM(WS-EDIT-B) '|'
                   FUNCTION TRIM(WS-EDIT-QTY) '|'
                   WS-EDIT-QTY.

       9000-EMIT-AMT.
           DISPLAY WS-OP(WS-IDX) '|'
                   FUNCTION TRIM(WS-EDIT-A) '|'
                   FUNCTION TRIM(WS-EDIT-B) '|'
                   FUNCTION TRIM(WS-EDIT-AMT) '|'
                   WS-EDIT-AMT.
