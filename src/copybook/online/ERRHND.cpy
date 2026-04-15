      ******************************************************************
      * Copybook Name: ERRHND                                          *
      * Description: Online Error Handling Area                        *
      *   Standard error context for CICS online programs:             *
      *   ERR-PROGRAM/PARAGRAPH - where the error occurred             *
      *   ERR-SQLCODE/CICS-RESP - DB2 and CICS response codes          *
      *   ERR-SEVERITY          - F=Fatal, W=Warning, I=Info           *
      *   ERR-ACTION            - R=Return, C=Continue, A=Abend        *
      *   ERR-TRACE             - unique trace ID and timestamp        *
      * Used By: ERRHNDL, INQONLN, INQPORT, INQHIST, SECMGR          *
      ******************************************************************
       01  ERROR-HANDLING.
           05 ERR-PROGRAM          PIC X(8).
           05 ERR-PARAGRAPH        PIC X(30).
           05 ERR-SQLCODE          PIC S9(9) COMP.
           05 ERR-CICS-RESP        PIC S9(8) COMP.
           05 ERR-CICS-RESP2      PIC S9(8) COMP.
           05 ERR-SEVERITY         PIC X.
              88 ERR-FATAL              VALUE 'F'.
              88 ERR-WARNING            VALUE 'W'.
              88 ERR-INFO               VALUE 'I'.
           05 ERR-MESSAGE          PIC X(80).
           05 ERR-ACTION           PIC X.
              88 ERR-RETURN            VALUE 'R'.
              88 ERR-CONTINUE          VALUE 'C'.
              88 ERR-ABEND             VALUE 'A'.
           05 ERR-TRACE.
              10 ERR-TRACE-ID      PIC X(16).
              10 ERR-TIMESTAMP     PIC X(26).  