      ******************************************************************
      * Copybook Name: RTNCODE                                         *
      * Description:  Return Code Management Interface                  *
      *                                                                *
      * Provides a request/response interface for centralized return    *
      * code management. Programs use request types (Initialize, Set,   *
      * Get, Log, Analyze) to interact with the return code service.   *
      * Tracks current and highest return codes, supports analysis     *
      * with start/end timestamps and aggregate statistics.             *
      *                                                                *
      * Used by: RTNCDE00 (return code processor), RTNANA00            *
      ******************************************************************
       01  RETURN-CODE-AREA.
      *    Request type determines which operation to perform
           05 RC-REQUEST-TYPE        PIC X.
              88 RC-INITIALIZE           VALUE 'I'.
              88 RC-SET-CODE             VALUE 'S'.
              88 RC-GET-CODE             VALUE 'G'.
              88 RC-LOG-CODE             VALUE 'L'.
              88 RC-ANALYZE              VALUE 'A'.
           05 RC-PROGRAM-ID         PIC X(8).
      *    Return code tracking: current, highest, and new values
           05 RC-CODES-AREA.
              10 RC-CURRENT-CODE    PIC S9(4) COMP.
              10 RC-HIGHEST-CODE    PIC S9(4) COMP.
              10 RC-NEW-CODE        PIC S9(4) COMP.
              10 RC-STATUS          PIC X.
                 88 RC-STATUS-SUCCESS    VALUE 'S'.
                 88 RC-STATUS-WARNING    VALUE 'W'.
                 88 RC-STATUS-ERROR      VALUE 'E'.
                 88 RC-STATUS-SEVERE     VALUE 'F'.
           05 RC-MESSAGE           PIC X(80).
           05 RC-RESPONSE-CODE     PIC S9(8) COMP.
      *    Analysis data: time range and aggregate counts
           05 RC-ANALYSIS-DATA.
              10 RC-START-TIME     PIC X(26).
              10 RC-END-TIME       PIC X(26).
              10 RC-TOTAL-CODES    PIC S9(8) COMP.
              10 RC-MAX-CODE       PIC S9(4) COMP.
              10 RC-MIN-CODE       PIC S9(4) COMP.
      *    Response values returned by the service
           05 RC-RETURN-DATA.
              10 RC-RETURN-VALUE   PIC S9(4) COMP.
              10 RC-HIGHEST-RETURN PIC S9(4) COMP.
              10 RC-RETURN-STATUS  PIC X.
