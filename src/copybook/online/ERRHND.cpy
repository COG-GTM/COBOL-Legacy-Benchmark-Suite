      ******************************************************************
      * Copybook Name: ERRHND                                          *
      * Description:  Online Error Handling Communication Area          *
      *                                                                *
      * Defines the error context structure passed to the ERRHNDL      *
      * centralized error handler in online (CICS) programs.           *
      * Captures program name, paragraph, SQLCODE, CICS response       *
      * codes, severity, action to take (return/continue/abend),       *
      * and a trace ID for correlation across error logs.               *
      *                                                                *
      * Used by: All online programs via CALL to ERRHNDL                *
      ******************************************************************
       01  ERROR-HANDLING.
           05 ERR-PROGRAM          PIC X(8).
           05 ERR-PARAGRAPH        PIC X(30).
      *    DB2 and CICS diagnostic codes captured at point of failure
           05 ERR-SQLCODE          PIC S9(9) COMP.
           05 ERR-CICS-RESP        PIC S9(8) COMP.
           05 ERR-CICS-RESP2      PIC S9(8) COMP.
      *    Severity determines if error is logged, displayed, or abends
           05 ERR-SEVERITY         PIC X.
              88 ERR-FATAL              VALUE 'F'.
              88 ERR-WARNING            VALUE 'W'.
              88 ERR-INFO               VALUE 'I'.
           05 ERR-MESSAGE          PIC X(80).
      *    Action flag tells ERRHNDL how to proceed after logging
           05 ERR-ACTION           PIC X.
              88 ERR-RETURN            VALUE 'R'.
              88 ERR-CONTINUE          VALUE 'C'.
              88 ERR-ABEND             VALUE 'A'.
      *    Correlation trace for linking related error events
           05 ERR-TRACE.
              10 ERR-TRACE-ID      PIC X(16).
              10 ERR-TIMESTAMP     PIC X(26).   