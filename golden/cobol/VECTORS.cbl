      *================================================================*
      * VECTORS - writes COMP-3 reference bytes to a flat file so the
      * JS codec can be validated against real packed-decimal output.
      * Record = 8 bytes per value, in the documented order.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. VECTORS.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT OUT-FILE ASSIGN TO 'VECOUT'
               ORGANIZATION IS SEQUENTIAL.
       DATA DIVISION.
       FILE SECTION.
       FD  OUT-FILE.
       01  OUT-REC             PIC X(8).
       WORKING-STORAGE SECTION.
       01  WS-MONEY-GRP.
           05  WS-MONEY        PIC S9(13)V99 COMP-3.
       01  WS-MONEY-X REDEFINES WS-MONEY-GRP PIC X(8).
       01  WS-QTY-GRP.
           05  WS-QTY          PIC S9(11)V9(4) COMP-3.
       01  WS-QTY-X REDEFINES WS-QTY-GRP PIC X(8).
       01  WS-VALS.
           05  WS-MV OCCURS 8 TIMES PIC S9(13)V99.
       01  WS-QVALS.
           05  WS-QV OCCURS 4 TIMES PIC S9(11)V9(4).
       01  WS-I                PIC 9(2).
       PROCEDURE DIVISION.
       0000-MAIN.
           OPEN OUTPUT OUT-FILE
           MOVE 0                 TO WS-MV(1)
           MOVE 1                 TO WS-MV(2)
           MOVE 0.01              TO WS-MV(3)
           MOVE 12345678.90       TO WS-MV(4)
           MOVE -12345678.90      TO WS-MV(5)
           MOVE 9999999999999.99  TO WS-MV(6)
           MOVE -9999999999999.99 TO WS-MV(7)
           MOVE -0.05             TO WS-MV(8)
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 8
               MOVE WS-MV(WS-I) TO WS-MONEY
               WRITE OUT-REC FROM WS-MONEY-X
           END-PERFORM
           MOVE 0        TO WS-QV(1)
           MOVE 100.5    TO WS-QV(2)
           MOVE -100.5   TO WS-QV(3)
           MOVE 0.0001   TO WS-QV(4)
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 4
               MOVE WS-QV(WS-I) TO WS-QTY
               WRITE OUT-REC FROM WS-QTY-X
           END-PERFORM
           CLOSE OUT-FILE
           GOBACK.
