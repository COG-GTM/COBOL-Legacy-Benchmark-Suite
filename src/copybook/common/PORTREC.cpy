      *================================================================*
      * Copybook Name: PORTREC
      * Description: Portfolio Record Layout for Transaction Processing
      * Author: [Author name]
      * Date Written: 2024-03-20
      * Maintenance Log:
      * Date       Author        Description
      * ---------- ------------- -------------------------------------
      * 2024-03-20 [Author]     Initial Creation
      *================================================================*
       01  PORTFOLIO-RECORD.
           05  PORT-KEY.
               10  PORT-ID             PIC X(8).
               10  PORT-ACCOUNT-NO     PIC X(10).
           05  PORT-CLIENT-INFO.
               10  PORT-CLIENT-NAME    PIC X(30).
               10  PORT-CLIENT-TYPE    PIC X(1).
           05  PORT-PORTFOLIO-INFO.
               10  PORT-CREATE-DATE    PIC 9(8).
               10  PORT-LAST-MAINT     PIC 9(8).
               10  PORT-STATUS         PIC X(1).
           05  PORT-FINANCIAL-INFO.
               10  PORT-TOTAL-UNITS    PIC S9(11)V9(4) COMP-3.
               10  PORT-TOTAL-COST     PIC S9(13)V99 COMP-3.
           05  PORT-AUDIT-INFO.
               10  PORT-LAST-USER      PIC X(8).
               10  PORT-LAST-TRANS     PIC 9(8).
           05  PORT-FILLER             PIC X(50).
