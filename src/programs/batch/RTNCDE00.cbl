       IDENTIFICATION DIVISION.
       PROGRAM-ID. RTNCDE00.
      *****************************************************************
      * Program Name: RTNCDE00                                        *
      * Description:  Standard Return Code Handler                     *
      *                                                               *
      * Provides a centralised return-code management service that    *
      * other programs call via Linkage. Supports five operations:    *
      *   RC-INITIALIZE - Reset all code fields to initial state      *
      *   RC-SET-CODE   - Set a new code and track highest            *
      *   RC-GET-CODE   - Retrieve current/highest codes and status   *
      *   RC-LOG-CODE   - INSERT current state into DB2 RTNCODES      *
      *   RC-ANALYZE    - Query DB2 for min/max/count by program      *
      *                                                               *
      * Called By: Any program needing return-code tracking            *
      * Tables:   RTNCODES (DB2 - Insert/Select)                      *
      *                                                               *
      * Code Ranges:                                                  *
      *   0       = Success                                           *
      *   1 - 4   = Warning                                           *
      *   5 - 8   = Error                                             *
      *   9+      = Severe                                            *
      *****************************************************************
       
       ENVIRONMENT DIVISION.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-CURRENT-TIME.
           05 WS-CURRENT-DATE.
              10 WS-CURRENT-YEAR     PIC 9(4).
              10 WS-CURRENT-MONTH    PIC 9(2).
              10 WS-CURRENT-DAY      PIC 9(2).
           05 WS-CURRENT-HOURS       PIC 9(2).
           05 WS-CURRENT-MINUTES     PIC 9(2).
           05 WS-CURRENT-SECONDS     PIC 9(2).
           05 WS-CURRENT-MILLISEC    PIC 9(2).
           
      * DB2 communication area for SQL operations
       01  WS-DB2-AREA.
           EXEC SQL INCLUDE SQLCA END-EXEC.
           
       LINKAGE SECTION.
      * Caller passes the request/response structure via Linkage
       01  RC-REQUEST-AREA.
           COPY RTNCODE.
           
       PROCEDURE DIVISION USING RC-REQUEST-AREA.
      *----------------------------------------------------------------*
      * Main dispatch: route to the appropriate handler based on the   *
      * function flag set in the RC-REQUEST-AREA copybook.             *
      *----------------------------------------------------------------*
           EVALUATE TRUE
               WHEN RC-INITIALIZE
                    PERFORM P100-INIT-RETURN-CODES
                       THRU P100-EXIT
               WHEN RC-SET-CODE
                    PERFORM P200-SET-RETURN-CODE
                       THRU P200-EXIT
               WHEN RC-GET-CODE
                    PERFORM P300-GET-RETURN-CODE
                       THRU P300-EXIT
               WHEN RC-LOG-CODE
                    PERFORM P400-LOG-RETURN-CODE
                       THRU P400-EXIT
               WHEN RC-ANALYZE
                    PERFORM P500-ANALYZE-CODES
                       THRU P500-EXIT
           END-EVALUATE.
           
           GOBACK.
           
      *----------------------------------------------------------------*
      * P100: Reset all return-code fields to their initial state.     *
      *----------------------------------------------------------------*
       P100-INIT-RETURN-CODES.
           INITIALIZE RC-CODES-AREA.
           MOVE SPACES TO RC-PROGRAM-ID.
           MOVE 0 TO RC-CURRENT-CODE.
           MOVE 0 TO RC-HIGHEST-CODE.
           SET RC-STATUS-SUCCESS TO TRUE.
           MOVE 0 TO RC-RESPONSE-CODE.
       P100-EXIT.
           EXIT.
           
      *----------------------------------------------------------------*
      * P200: Accept a new code from the caller, track the highest     *
      *   code seen, and classify the severity bucket.                 *
      *----------------------------------------------------------------*
       P200-SET-RETURN-CODE.
           IF RC-NEW-CODE > RC-HIGHEST-CODE
              MOVE RC-NEW-CODE TO RC-HIGHEST-CODE
           END-IF.
           
           MOVE RC-NEW-CODE TO RC-CURRENT-CODE.
           
           EVALUATE RC-NEW-CODE
               WHEN 0
                    SET RC-STATUS-SUCCESS TO TRUE
               WHEN 1 THRU 4
                    SET RC-STATUS-WARNING TO TRUE
               WHEN 5 THRU 8
                    SET RC-STATUS-ERROR TO TRUE
               WHEN OTHER
                    SET RC-STATUS-SEVERE TO TRUE
           END-EVALUATE.
           
           MOVE 0 TO RC-RESPONSE-CODE.
       P200-EXIT.
           EXIT.
           
      *----------------------------------------------------------------*
      * P300: Copy current/highest codes and status back to the        *
      *   caller's return fields.                                      *
      *----------------------------------------------------------------*
       P300-GET-RETURN-CODE.
           MOVE RC-CURRENT-CODE TO RC-RETURN-VALUE.
           MOVE RC-HIGHEST-CODE TO RC-HIGHEST-RETURN.
           MOVE RC-STATUS TO RC-RETURN-STATUS.
           MOVE 0 TO RC-RESPONSE-CODE.
       P300-EXIT.
           EXIT.
           
      *----------------------------------------------------------------*
      * P400: INSERT the current return-code state into DB2 RTNCODES   *
      *   for audit trail purposes.                                    *
      *----------------------------------------------------------------*
       P400-LOG-RETURN-CODE.
           MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-TIME.
           
           EXEC SQL
                INSERT INTO RTNCODES
                (TIMESTAMP,
                 PROGRAM_ID,
                 RETURN_CODE,
                 HIGHEST_CODE,
                 STATUS_CODE,
                 MESSAGE_TEXT)
                VALUES
                (:WS-CURRENT-TIME,
                 :RC-PROGRAM-ID,
                 :RC-CURRENT-CODE,
                 :RC-HIGHEST-CODE,
                 :RC-STATUS,
                 :RC-MESSAGE)
           END-EXEC.
           
           IF SQLCODE = 0
              MOVE 0 TO RC-RESPONSE-CODE
           ELSE
              MOVE 8 TO RC-RESPONSE-CODE
           END-IF.
       P400-EXIT.
           EXIT.
           
      *----------------------------------------------------------------*
      * P500: Query DB2 for count/max/min of return codes by program   *
      *   within the specified time range.                             *
      *----------------------------------------------------------------*
       P500-ANALYZE-CODES.
           EXEC SQL
                SELECT COUNT(*),
                       MAX(RETURN_CODE),
                       MIN(RETURN_CODE)
                INTO :RC-TOTAL-CODES,
                     :RC-MAX-CODE,
                     :RC-MIN-CODE
                FROM RTNCODES
                WHERE PROGRAM_ID = :RC-PROGRAM-ID
                  AND TIMESTAMP >= :RC-START-TIME
                  AND TIMESTAMP <= :RC-END-TIME
           END-EXEC.
           
           IF SQLCODE = 0
              MOVE 0 TO RC-RESPONSE-CODE
           ELSE
              MOVE 8 TO RC-RESPONSE-CODE
           END-IF.
       P500-EXIT.
           EXIT.
