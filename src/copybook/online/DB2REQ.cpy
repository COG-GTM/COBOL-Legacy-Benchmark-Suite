      ******************************************************************
      * Copybook Name: DB2REQ                                          *
      * Description:  DB2 Request Area for Online Programs              *
      *                                                                *
      * Defines the communication area passed to the DB2ONLN           *
      * connection pool manager. Online programs set the request type   *
      * (Connect, Disconnect, or Status) and receive a connection      *
      * token and any error information in response.                    *
      *                                                                *
      * Used by: INQONLN, INQHIST, DB2ONLN, DB2RECV                   *
      ******************************************************************
       01  DB2-REQUEST-AREA.
      *    Operation requested: Connect, Disconnect, or Status check
           05 DB2-REQUEST-TYPE        PIC X.
              88 DB2-CONNECT              VALUE 'C'.
              88 DB2-DISCONNECT           VALUE 'D'.
              88 DB2-STATUS               VALUE 'S'.
           05 DB2-RESPONSE-CODE       PIC S9(8) COMP.
           05 DB2-CONNECTION-TOKEN    PIC X(16).
           05 DB2-ERROR-INFO.
              10 DB2-SQLCODE          PIC S9(9) COMP.
              10 DB2-ERROR-MSG        PIC X(80).  