      *================================================================*
      * Copybook Name: AUDITLOG                                        *
      * Description:  Audit Trail Record Definitions                    *
      * Author: [Author name]                                          *
      * Date Written: 2024-03-20                                       *
      *                                                                *
      * Defines the standard audit trail record written to the audit    *
      * log file/DB2 table for every significant system action.         *
      * Records capture who, what, when, and the before/after data      *
      * images for full change traceability.                            *
      *                                                                *
      * Used by: AUDPROC (audit writer), and all programs that log      *
      *          user actions, transactions, and system events.         *
      *================================================================*
       01  AUDIT-RECORD.
      *    Origin identification: when, where, who, which program
           05  AUD-HEADER.
               10  AUD-TIMESTAMP     PIC X(26).
               10  AUD-SYSTEM-ID     PIC X(8).
               10  AUD-USER-ID       PIC X(8).
               10  AUD-PROGRAM       PIC X(8).
               10  AUD-TERMINAL      PIC X(8).
      *    Event classification: transaction, user action, or system
           05  AUD-TYPE             PIC X(4).
               88  AUD-TRANSACTION     VALUE 'TRAN'.
               88  AUD-USER-ACTION     VALUE 'USER'.
               88  AUD-SYSTEM-EVENT    VALUE 'SYST'.
      *    Specific action performed
           05  AUD-ACTION           PIC X(8).
               88  AUD-CREATE         VALUE 'CREATE  '.
               88  AUD-UPDATE         VALUE 'UPDATE  '.
               88  AUD-DELETE         VALUE 'DELETE  '.
               88  AUD-INQUIRE        VALUE 'INQUIRE '.
               88  AUD-LOGIN          VALUE 'LOGIN   '.
               88  AUD-LOGOUT         VALUE 'LOGOUT  '.
               88  AUD-STARTUP        VALUE 'STARTUP '.
               88  AUD-SHUTDOWN       VALUE 'SHUTDOWN'.
      *    Outcome of the action
           05  AUD-STATUS           PIC X(4).
               88  AUD-SUCCESS        VALUE 'SUCC'.
               88  AUD-FAILURE        VALUE 'FAIL'.
               88  AUD-WARNING        VALUE 'WARN'.
      *    Affected entity key for cross-referencing
           05  AUD-KEY-INFO.
               10  AUD-PORTFOLIO-ID  PIC X(8).
               10  AUD-ACCOUNT-NO    PIC X(10).
      *    Before/after images for change auditing
           05  AUD-BEFORE-IMAGE     PIC X(100).
           05  AUD-AFTER-IMAGE      PIC X(100).
      *    Free-text description of the event
           05  AUD-MESSAGE          PIC X(100).   