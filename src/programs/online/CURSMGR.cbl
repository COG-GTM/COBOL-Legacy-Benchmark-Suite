       IDENTIFICATION DIVISION.
       PROGRAM-ID. CURSMGR.
      *****************************************************************
      * Program Name: CURSMGR                                         *
      * Description:  Cursor Management for Online Programs            *
      *                                                               *
      * Provides a generic cursor lifecycle service for CICS online   *
      * programs. Callers pass a request type and SQL statement via    *
      * Linkage; this program handles declare, open, fetch (single    *
      * or array), and close operations.                              *
      *                                                               *
      * Request Types (CURS-REQUEST-TYPE):                            *
      *   D - Declare cursor from supplied SQL statement               *
      *   O - Open the declared cursor                                *
      *   F - Fetch rows into CURS-DATA-AREA                          *
      *   C - Close the cursor                                        *
      *                                                               *
      * Called By: INQHIST, other online programs                     *
      * Tables:   Dynamic (based on CURS-STMT)                        *
      *****************************************************************
       
       ENVIRONMENT DIVISION.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-DB2-AREA.
           EXEC SQL INCLUDE SQLCA END-EXEC.
           
      * Cursor performance counters (reset on each OPEN)
       01  WS-CURSOR-STATS.
           05 WS-FETCH-COUNT         PIC S9(8) COMP VALUE 0.
           05 WS-ROWS-FETCHED       PIC S9(8) COMP VALUE 0.
           05 WS-FETCH-TIME         PIC S9(8) COMP VALUE 0.
           
       01  WS-ARRAY-AREA.
           05 WS-MAX-ROWS           PIC S9(4) COMP VALUE 20.
           05 WS-ARRAY-SIZE         PIC S9(4) COMP VALUE 0.
           
       LINKAGE SECTION.
       01  CURSOR-REQUEST-AREA.
           05 CURS-REQUEST-TYPE     PIC X.
              88 CURS-DECLARE           VALUE 'D'.
              88 CURS-OPEN              VALUE 'O'.
              88 CURS-FETCH             VALUE 'F'.
              88 CURS-CLOSE             VALUE 'C'.
           05 CURS-NAME             PIC X(18).
           05 CURS-STMT             PIC X(240).
           05 CURS-ARRAY-FETCH      PIC X VALUE 'N'.
              88 USE-ARRAY-FETCH         VALUE 'Y'.
              88 NO-ARRAY-FETCH          VALUE 'N'.
           05 CURS-RESPONSE-CODE    PIC S9(8) COMP.
           05 CURS-DATA-AREA        PIC X(3000).
           05 CURS-DATA-LENGTH      PIC S9(4) COMP.
           
       PROCEDURE DIVISION USING CURSOR-REQUEST-AREA.
      *----------------------------------------------------------------*
      * Main dispatch: route to declare / open / fetch / close.        *
      *----------------------------------------------------------------*
           EVALUATE TRUE
               WHEN CURS-DECLARE
                    PERFORM P100-DECLARE-CURSOR
                       THRU P100-EXIT
               WHEN CURS-OPEN
                    PERFORM P200-OPEN-CURSOR
                       THRU P200-EXIT
               WHEN CURS-FETCH
                    PERFORM P300-FETCH-DATA
                       THRU P300-EXIT
               WHEN CURS-CLOSE
                    PERFORM P400-CLOSE-CURSOR
                       THRU P400-EXIT
           END-EVALUATE.
           
           EXEC CICS RETURN END-EXEC.
           
      *----------------------------------------------------------------*
      * P100: Declare a cursor. Sets array size based on fetch mode.   *
      *----------------------------------------------------------------*
       P100-DECLARE-CURSOR.
           MOVE 0 TO CURS-RESPONSE-CODE.
           
           IF USE-ARRAY-FETCH
              MOVE WS-MAX-ROWS TO WS-ARRAY-SIZE
           ELSE
              MOVE 1 TO WS-ARRAY-SIZE
           END-IF.
           
           EXEC SQL DECLARE :CURS-NAME CURSOR FOR
                :CURS-STMT
           END-EXEC.
           
           IF SQLCODE NOT = 0
              MOVE SQLCODE TO CURS-RESPONSE-CODE
           END-IF.
       P100-EXIT.
           EXIT.
           
      *----------------------------------------------------------------*
      * P200: Open the cursor and reset fetch counters.                *
      *----------------------------------------------------------------*
       P200-OPEN-CURSOR.
           MOVE 0 TO WS-FETCH-COUNT.
           MOVE 0 TO WS-ROWS-FETCHED.
           
           EXEC SQL OPEN :CURS-NAME END-EXEC.
           
           IF SQLCODE = 0
              MOVE 0 TO CURS-RESPONSE-CODE
           ELSE
              MOVE SQLCODE TO CURS-RESPONSE-CODE
           END-IF.
       P200-EXIT
