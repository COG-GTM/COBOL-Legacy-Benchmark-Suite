      ******************************************************************
      * Copybook Name: INQCOM                                          *
      * Description: Online Inquiry Communication Area                 *
      *   COMMAREA passed between CICS inquiry transactions:           *
      *   INQCOM-FUNCTION    - MENU, INQP (portfolio), INQH           *
      *                        (history), or EXIT                      *
      *   INQCOM-ACCOUNT-NO  - account number being queried            *
      *   INQCOM-RESPONSE-CODE - numeric result of the inquiry         *
      *   INQCOM-ERROR-MSG   - descriptive error text                  *
      * Used By: INQONLN, INQPORT, INQHIST                            *
      ******************************************************************
       01  INQCOM-AREA.
           05 INQCOM-FUNCTION         PIC X(4).
              88 INQCOM-MENU               VALUE 'MENU'.
              88 INQCOM-PORTFOLIO          VALUE 'INQP'.
              88 INQCOM-HISTORY            VALUE 'INQH'.
              88 INQCOM-EXIT               VALUE 'EXIT'.
           05 INQCOM-ACCOUNT-NO       PIC X(10).
           05 INQCOM-RESPONSE-CODE    PIC S9(8) COMP.
           05 INQCOM-ERROR-MSG        PIC X(80).  