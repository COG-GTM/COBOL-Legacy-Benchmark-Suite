        IDENTIFICATION DIVISION.
       PROGRAM-ID. DB2ONLN.
      *****************************************************************
      * Program Name: DB2ONLN                                         *
      * Description:  Online DB2 Connection Pool Manager               *
      *                                                               *
      * Provides a connection-pooling service for CICS online          *
      * programs. Callers pass a request type via Linkage:            *
      *   C - Connect:    Establish a new DB2 connection if the pool  *
      *                   has not reached WS-MAX-CONNECTIONS (100).   *
      *                   Returns a connection token.                 *
      *   D - Disconnect: Release a DB2 connection and decrement the  *
      *                   active count.                               *
      *   S - Status:     Probe the DB2 subsystem (CURRENT SERVER)   *
      *                   and return the active connection count.     *
      *                                                               *
      * Called By: INQHIST, DB2RECV, other online programs            *
      * Tables:   N/A (uses DB2 CONNECT/DISCONNECT/SELECT)            *
      *****************************************************************
       
       ENVIRONMENT DIVISION.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-DB2-AREA.
           EXEC SQL INCLUDE SQLCA END-EXEC.
           
      * Pool counters - persist across CICS invocations
       01  WS-POOL-STATS.
           05 WS-TOTAL-CONNECTIONS    PIC S9(8) COMP VALUE 0.
           05 WS-ACTIVE-CONNECTIONS   PIC S9(8) COMP VALUE 0.
           05 WS-AVAILABLE-CONNECTIONS PIC S9(8) COMP VALUE 0.
           05 WS-MAX-CONNECTIONS      PIC S9(8) COMP VALUE 100.
           
       01  WS-ERROR-AREA.
           COPY ERRHND.
           
       LINKAGE SECTION.
       01  DB2-REQUEST-AREA.
           05 DB2-REQUEST-TYPE        PIC X.
              88 DB2-CONNECT              VALUE 'C'.
              88 DB2-DISCONNECT           VALUE 'D'.
              88 DB2-STATUS               VALUE 'S'.
           05 DB2-RESPONSE-CODE       PIC S9(8) COMP.
           05 DB2-CONNECTION-TOKEN    PIC X(16).
           05 DB2-ERROR-INFO.
              10 DB2-SQLCODE          PIC S9(9) COMP.
              10 DB2-ERROR-MSG        PIC X(80).
           
       PROCEDURE DIVISION USING DB2-REQUEST-AREA.
      *----------------------------------------------------------------*
      * Main dispatch: route to connect / disconnect / status.         *
      *----------------------------------------------------------------*
           EVALUATE TRUE
               WHEN DB2-CONNECT
                    PERFORM P100-PROCESS-CONNECT
                       THRU P100-EXIT
               WHEN DB2-DISCONNECT
                    PERFORM P200-PROCESS-DISCONNECT
                       THRU P200-EXIT
               WHEN DB2-STATUS
                    PERFORM P300-CHECK-STATUS
                       THRU P300-EXIT
           END-EVALUATE.
           
           EXEC CICS RETURN END-EXEC.
           
      *----------------------------------------------------------------*
      * P100: Guard against pool exhaustion, then establish connection. *
      *----------------------------------------------------------------*
       P100-PROCESS-CONNECT.
           IF WS-ACTIVE-CONNECTIONS < WS-MAX-CONNECTIONS
              PERFORM P110-ESTABLISH-CONNECTION
                 THRU P110-EXIT
           ELSE
              MOVE 'Maximum connections reached' 
                TO DB2-ERROR-MSG
              MOVE -1 TO DB2-RESPONSE-CODE
           END-IF.
       P100-EXIT.
           EXIT.
           
      *----------------------------------------------------------------*
      * P110: Issue SQL CONNECT TO POSMVP. On success, increment the   *
      *   active count and generate a unique connection token.         *
      *----------------------------------------------------------------*
       P110-ESTABLISH-CONNECTION.
           EXEC SQL CONNECT TO POSMVP END-EXEC.
           
           IF SQLCODE = 0
              ADD 1 TO WS-ACTIVE-CONNECTIONS
              MOVE SQLCODE TO DB2-SQLCODE
              MOVE 0 TO DB2-RESPONSE-CODE
              PERFORM P120-GENERATE-TOKEN
                 THRU P120-EXIT
           ELSE
              MOVE SQLCODE TO DB2-SQLCODE
              MOVE SQLERRMC TO DB2-ERROR-MSG
              MOVE -1 TO DB2-RESPONSE-CODE
           END-IF.
       P110-EXIT.
           EXIT.
           
      *----------------------------------------------------------------*
      * P120: Build a unique token from timestamp + connection count.   *
      *----------------------------------------------------------------*
       P120-GENERATE-TOKEN.
           MOVE FUNCTION CURRENT-DATE TO DB2-CONNECTION-TOKEN.
           STRING DB2-CONNECTION-TOKEN DELIMITED BY SIZE
                  WS-ACTIVE-CONNECTIONS DELIMITED BY SIZE
                  INTO DB2-CONNECTION-TOKEN.
       P120-EXIT.
           EXIT.
           
      *----------------------------------------------------------------*
      * P200: Issue SQL DISCONNECT, decrement active count on success.  *
      *----------------------------------------------------------------*
       P200-PROCESS-DISCONNECT.
           EXEC SQL DISCONNECT END-EXEC.
           
           IF SQLCODE = 0
              SUBTRACT 1 FROM WS-ACTIVE-CONNECTIONS
              MOVE 0 TO DB2-RESPONSE-CODE
           ELSE
              MOVE SQLCODE TO DB2-SQLCODE
              MOVE SQLERRMC TO DB2-ERROR-MSG
              MOVE -1 TO DB2-RESPONSE-CODE
           END-IF.
       P200-EXIT.
           EXIT.
           
      *----------------------------------------------------------------*
      * P300: Probe DB2 with CURRENT SERVER, return active count.      *
      *----------------------------------------------------------------*
       P300-CHECK-STATUS.
           EXEC SQL SELECT CURRENT SERVER 
                    INTO :DB2-ERROR-MSG
           END-EXEC.
           
           IF SQLCODE = 0
              MOVE 0 TO DB2-RESPONSE-CODE
           ELSE
              MOVE SQLCODE TO DB2-SQLCODE
              MOVE -1 TO DB2-RESPONSE-CODE
           END-IF.
           
           MOVE WS-ACTIVE-CONNECTIONS 
             TO DB2-RESPONSE-CODE.
       P300-EXIT.
           EXIT.
