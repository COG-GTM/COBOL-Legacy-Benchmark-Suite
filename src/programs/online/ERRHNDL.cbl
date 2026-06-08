       IDENTIFICATION DIVISION.
       PROGRAM-ID. ERRHNDL.
      *****************************************************************
      * Centralized Error Handler
      * - Processes all online errors
      * - Logs errors to DB2
      * - Formats error messages
      * - Controls error recovery
      *****************************************************************

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
           COPY ERRHND.
           COPY SQLCA.

       01  WS-ERRLOG-RECORD.
           05 LOG-TIMESTAMP        PIC X(26).
           05 LOG-PROGRAM          PIC X(8).
           05 LOG-PARAGRAPH        PIC X(30).
           05 LOG-SQLCODE          PIC S9(9) COMP.
           05 LOG-CICS-RESP        PIC S9(8) COMP.
           05 LOG-SEVERITY         PIC X.
           05 LOG-MESSAGE          PIC X(80).
           05 LOG-TRACE-ID         PIC X(16).

       LINKAGE SECTION.
       01  DFHCOMMAREA              PIC X(200).

       PROCEDURE DIVISION.
           PERFORM P100-INIT-ERROR-HANDLER
              THRU P100-EXIT.

           PERFORM P200-LOG-ERROR
              THRU P200-EXIT.

           PERFORM P300-FORMAT-MESSAGE
              THRU P300-EXIT.

           PERFORM P400-DETERMINE-ACTION
              THRU P400-EXIT.

           STOP RUN.

       P100-INIT-ERROR-HANDLER.
           MOVE FUNCTION CURRENT-DATE TO ERR-TIMESTAMP.
       P100-EXIT.
           EXIT.

       P200-LOG-ERROR.
           MOVE ERR-TIMESTAMP    TO LOG-TIMESTAMP.
           MOVE ERR-PROGRAM      TO LOG-PROGRAM.
           MOVE ERR-PARAGRAPH    TO LOG-PARAGRAPH.
           MOVE ERR-SQLCODE      TO LOG-SQLCODE.
           MOVE ERR-CICS-RESP    TO LOG-CICS-RESP.
           MOVE ERR-SEVERITY     TO LOG-SEVERITY.
           MOVE ERR-MESSAGE      TO LOG-MESSAGE.
           MOVE ERR-TRACE-ID     TO LOG-TRACE-ID.

           EXEC SQL
                INSERT INTO ERRLOG
                VALUES (:LOG-TIMESTAMP,
                       :LOG-PROGRAM,
                       :LOG-PARAGRAPH,
                       :LOG-SQLCODE,
                       :LOG-CICS-RESP,
                       :LOG-SEVERITY,
                       :LOG-MESSAGE,
                       :LOG-TRACE-ID)
           END-EXEC.

           IF SQLCODE NOT = 0
              MOVE 'Error logging failed' TO ERR-MESSAGE
              SET ERR-FATAL TO TRUE
           END-IF.
       P200-EXIT.
           EXIT.

       P300-FORMAT-MESSAGE.
           STRING 'Error in ' DELIMITED BY SIZE
                  ERR-PROGRAM DELIMITED BY SPACE
                  ' - ' DELIMITED BY SIZE
                  ERR-MESSAGE DELIMITED BY SIZE
                  ' (' DELIMITED BY SIZE
                  ERR-TRACE-ID DELIMITED BY SIZE
                  ')' DELIMITED BY SIZE
                  INTO ERR-MESSAGE.
       P300-EXIT.
           EXIT.

       P400-DETERMINE-ACTION.
           EVALUATE TRUE
               WHEN ERR-FATAL
                    SET ERR-ABEND TO TRUE
               WHEN ERR-WARNING
                    SET ERR-CONTINUE TO TRUE
               WHEN ERR-INFO
                    SET ERR-CONTINUE TO TRUE
               WHEN OTHER
                    SET ERR-RETURN TO TRUE
           END-EVALUATE.
       P400-EXIT.
           EXIT.
