      ******************************************************************
      * Copybook Name: INQCOM                                          *
      * Description:  Online Inquiry Communication Area                 *
      *                                                                *
      * Defines the CICS COMMAREA (DFHCOMMAREA) layout passed between  *
      * the online inquiry programs (INQONLN, INQPORT, INQHIST).      *
      * The function code determines which screen/program to invoke,   *
      * the account number identifies the portfolio being queried,     *
      * and response/error fields carry results back to the caller.     *
      *                                                                *
      * Used by: INQONLN (controller), INQPORT, INQHIST               *
      ******************************************************************
       01  INQCOM-AREA.
      *    Function code selects the inquiry type or exit
           05 INQCOM-FUNCTION         PIC X(4).
              88 INQCOM-MENU               VALUE 'MENU'.
              88 INQCOM-PORTFOLIO          VALUE 'INQP'.
              88 INQCOM-HISTORY            VALUE 'INQH'.
              88 INQCOM-EXIT               VALUE 'EXIT'.
           05 INQCOM-ACCOUNT-NO       PIC X(10).
           05 INQCOM-RESPONSE-CODE    PIC S9(8) COMP.
           05 INQCOM-ERROR-MSG        PIC X(80).  