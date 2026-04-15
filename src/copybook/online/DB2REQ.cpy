      ******************************************************************
      * Copybook Name: DB2REQ                                          *
      * Description: DB2 Request Area                                  *
      *   Communication area passed between the online CICS            *
      *   programs and the DB2 connection manager (DB2ONLN):           *
      *   DB2-REQUEST-TYPE   - C=Connect, D=Disconnect, S=Status       *
      *   DB2-RESPONSE-CODE  - numeric result from the request         *
      *   DB2-CONNECTION-TOKEN- opaque token for the DB2 session       *
      *   DB2-ERROR-INFO     - SQLCODE and descriptive message         *
      * Used By: DB2ONLN, INQONLN, INQPORT, INQHIST                   *
      ******************************************************************
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