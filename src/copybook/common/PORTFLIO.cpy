      *================================================================*
      * Copybook Name: PORTFLIO                                        *
      * Description:  Portfolio Master Record Layout                     *
      * Author: [Author name]                                          *
      * Date Written: 2024-03-20                                       *
      *                                                                *
      * Defines the VSAM KSDS record layout for the Portfolio Master    *
      * file (PORTMSTR). Each record represents one client portfolio    *
      * with identification, financial totals, and audit trail.         *
      *                                                                *
      * Key: PORT-ID (8) + PORT-ACCOUNT-NO (10) = 18 bytes             *
      * Record length: 200 bytes (fixed)                               *
      *                                                                *
      * Used by: PORTMSTR, PORTADD, PORTREAD, PORTUPDT, PORTDEL,      *
      *          PORTTRAN, PORTVALD, PORTTEST, INQPORT                 *
      *                                                                *
      * Maintenance Log:                                               *
      * Date       Author        Description                          *
      * ---------- ------------- ------------------------------------ *
      * 2024-03-20 [Author]     Initial Creation                      *
      *================================================================*
       01  PORT-RECORD.
      *    Primary key: portfolio ID + account number
           05  PORT-KEY.
               10  PORT-ID             PIC X(8).
               10  PORT-ACCOUNT-NO     PIC X(10).
      *    Client identification and classification
           05  PORT-CLIENT-INFO.
               10  PORT-CLIENT-NAME    PIC X(30).
               10  PORT-CLIENT-TYPE    PIC X(1).
                   88  PORT-INDIVIDUAL    VALUE 'I'.
                   88  PORT-CORPORATE     VALUE 'C'.
                   88  PORT-TRUST         VALUE 'T'.
      *    Portfolio lifecycle dates and current status
           05  PORT-PORTFOLIO-INFO.
               10  PORT-CREATE-DATE    PIC 9(8).
               10  PORT-LAST-MAINT     PIC 9(8).
               10  PORT-STATUS         PIC X(1).
                   88  PORT-ACTIVE       VALUE 'A'.
                   88  PORT-CLOSED       VALUE 'C'.
                   88  PORT-SUSPENDED    VALUE 'S'.
      *    Financial summary: total value and available cash
           05  PORT-FINANCIAL-INFO.
               10  PORT-TOTAL-VALUE    PIC S9(13)V99 COMP-3.
               10  PORT-CASH-BALANCE   PIC S9(13)V99 COMP-3.
      *    Last modification tracking for audit compliance
           05  PORT-AUDIT-INFO.
               10  PORT-LAST-USER      PIC X(8).
               10  PORT-LAST-TRANS     PIC 9(8).
           05  PORT-FILLER            PIC X(50).  