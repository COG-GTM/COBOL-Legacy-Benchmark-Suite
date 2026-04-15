       *================================================================*
      * Program Name: PORTMSTR
      * Description: Portfolio Master File Maintenance Program
      *             Handles CRUD operations for Portfolio records
      *             via indexed VSAM file with dynamic access.
      *
      * Operations:  Called with a command area specifying:
      *   C - Create new portfolio record
      *   R - Read existing portfolio record by key
      *   U - Update existing portfolio record
      *   D - Delete portfolio record
      *
      * Files:       PORTFILE - Indexed VSAM portfolio master
      *              (Record key: PORT-ID, 100-byte records)
      *
      * Called By:   Online and batch programs needing portfolio
      *              maintenance (PORTTRAN, INQONLN, etc.)
      * Calls:       ERRPROC  - Error processing subroutine
      *              AUDPROC  - Audit trail logging
      *
      * Return Codes: 0 = Success, 8 = Error
      *
      * Author: [Author name]
      * Date Written: 2024-03-20
      * Maintenance Log:
      * Date       Author        Description
      * ---------- ------------- -------------------------------------
      * 2024-03-20 [Author]     Initial Creation
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PORTMSTR.
       
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.
       
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT PORTFOLIO-FILE
               ASSIGN TO PORTFILE
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS PORT-ID
               FILE STATUS IS WS-PORT-STATUS.
       
       DATA DIVISION.
       FILE SECTION.
       FD  PORTFOLIO-FILE
           RECORD CONTAINS 100 CHARACTERS.
       01  PORTFOLIO-RECORD.
      *    Portfolio unique identifier (format: PORTnnnnn)
           05  PORT-ID             PIC X(10).
      *    Descriptive portfolio name
           05  PORT-NAME           PIC X(50).
      *    Record creation date (YYYY-MM-DD)
           05  PORT-CREATE-DATE    PIC X(10).
      *    Portfolio status: A=Active, I=Inactive, C=Closed
           05  PORT-STATUS         PIC X(01).
      *    Total portfolio market value (packed decimal)
           05  PORT-TOTAL-VALUE    PIC S9(13)V99 COMP-3.
           05  FILLER              PIC X(24).
       
       WORKING-STORAGE SECTION.
      *----------------------------------------------------------------*
      * Constants and switches
      *----------------------------------------------------------------*
       01  WS-CONSTANTS.
           05  WS-PROGRAM-NAME     PIC X(08) VALUE 'PORTMSTR'.
           05  WS-SUCCESS          PIC S9(4) VALUE +0.
           05  WS-ERROR            PIC S9(4) VALUE +8.
           05  WS-ERROR-TEXT       PIC X(50) VALUE SPACES.
           
       01  WS-SWITCHES.
      *    VSAM file status after each I/O operation
           05  WS-PORT-STATUS      PIC X(02).
               88  PORT-SUCCESS    VALUE '00'.
               88  PORT-EOF        VALUE '10'.
               88  PORT-NOT-FOUND  VALUE '23'.
               88  PORT-DUP-KEY    VALUE '22'.
           
           05  WS-VALID-STATUS     PIC X(01).
               88  VALID-STATUS    VALUE 'A' 'I' 'C'.
           
           05  WS-END-OF-FILE-SW   PIC X     VALUE 'N'.
               88  END-OF-FILE              VALUE 'Y'.
               88  NOT-END-OF-FILE          VALUE 'N'.
           
      *----------------------------------------------------------------*
      * Work areas
      *----------------------------------------------------------------*
       01  WS-WORK-AREAS.
           05  WS-CURRENT-DATE     PIC X(10).
           05  WS-RETURN-CODE      PIC S9(4) COMP VALUE +0.
           
       LINKAGE SECTION.
      *    Command area passed by calling program
       01  LS-COMMAND-AREA.
      *    CRUD operation code: C=Create, R=Read, U=Update, D=Delete
           05  LS-COMMAND          PIC X(01).
               88  CREATE-PORT     VALUE 'C'.
               88  READ-PORT       VALUE 'R'.
               88  UPDATE-PORT     VALUE 'U'.
               88  DELETE-PORT     VALUE 'D'.
      *    Portfolio record buffer (input for C/U, output for R)
           05  LS-PORTFOLIO        PIC X(100).
      *    Return code set by this program (0=OK, 8=Error)
           05  LS-RETURN-CODE      PIC S9(4) COMP.
           
       PROCEDURE DIVISION USING LS-COMMAND-AREA.
      *----------------------------------------------------------------*
      * Main dispatch: initialize, route to CRUD handler, terminate.   *
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           
           EVALUATE TRUE
               WHEN CREATE-PORT
                   PERFORM 2000-CREATE-PORTFOLIO
               WHEN READ-PORT
                   PERFORM 3000-READ-PORTFOLIO
               WHEN UPDATE-PORT
                   PERFORM 4000-UPDATE-PORTFOLIO
               WHEN DELETE-PORT
                   PERFORM 5000-DELETE-PORTFOLIO
               WHEN OTHER
                   MOVE 'Invalid command' TO WS-ERROR-TEXT
                   PERFORM 9000-ERROR
           END-EVALUATE
           
           PERFORM 6000-TERMINATE
           GOBACK.
           
      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Open portfolio file for I-O and get date.     *
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           INITIALIZE WS-WORK-AREAS
           
           OPEN I-O PORTFOLIO-FILE
           IF NOT PORT-SUCCESS
               MOVE 'Error opening Portfolio file' TO WS-ERROR-TEXT
               PERFORM 9000-ERROR
           END-IF
           
           ACCEPT WS-CURRENT-DATE FROM DATE YYYYMMDD
           .
           
       2000-CREATE-PORTFOLIO.
      *----------------------------------------------------------------*
      * 2000-CREATE-PORTFOLIO: Validate then WRITE a new record.       *
      * Checks for duplicate keys before inserting.                    *
      *----------------------------------------------------------------*
           MOVE LS-PORTFOLIO TO PORTFOLIO-RECORD
           
           PERFORM 2100-VALIDATE-PORTFOLIO
           IF WS-RETURN-CODE NOT = WS-SUCCESS
               PERFORM 9000-ERROR
           END-IF
           
           WRITE PORTFOLIO-RECORD
           IF PORT-DUP-KEY
               MOVE 'Portfolio ID already exists' TO WS-ERROR-TEXT
               PERFORM 9000-ERROR
           END-IF
           
           IF NOT PORT-SUCCESS
               MOVE 'Error writing Portfolio record' TO WS-ERROR-TEXT
               PERFORM 9000-ERROR
           END-IF
           .
           
       2100-VALIDATE-PORTFOLIO.
      *----------------------------------------------------------------*
      * 2100-VALIDATE-PORTFOLIO: Enforce business rules:               *
      *   - PORT-ID must start with 'PORT' + 5 numeric digits         *
      *   - PORT-NAME must not be blank                                *
      *   - PORT-STATUS must be A (Active), I (Inactive), or C (Closed)*
      *----------------------------------------------------------------*
           IF PORT-ID(1:4) NOT = 'PORT'
              OR PORT-ID(5:5) IS NOT NUMERIC
               MOVE 'Invalid Portfolio ID format' TO WS-ERROR-TEXT
               MOVE WS-ERROR TO WS-RETURN-CODE
               EXIT PARAGRAPH
           END-IF
           
           IF PORT-NAME = SPACES
               MOVE 'Portfolio Name is required' TO WS-ERROR-TEXT
               MOVE WS-ERROR TO WS-RETURN-CODE
               EXIT PARAGRAPH
           END-IF
           
           MOVE PORT-STATUS TO WS-VALID-STATUS
           IF NOT VALID-STATUS
               MOVE 'Invalid Portfolio Status' TO WS-ERROR-TEXT
               MOVE WS-ERROR TO WS-RETURN-CODE
               EXIT PARAGRAPH
           END-IF
           .
           
       3000-READ-PORTFOLIO.
      *----------------------------------------------------------------*
      * 3000-READ-PORTFOLIO: Random read by PORT-ID key.               *
      * Returns the record in LS-PORTFOLIO if found.                   *
      *----------------------------------------------------------------*
           MOVE LS-PORTFOLIO TO PORTFOLIO-RECORD
           
           READ PORTFOLIO-FILE
           
           EVALUATE TRUE
               WHEN PORT-SUCCESS
                   MOVE PORTFOLIO-RECORD TO LS-PORTFOLIO
               WHEN PORT-NOT-FOUND
                   MOVE 'Portfolio not found' TO WS-ERROR-TEXT
                   PERFORM 9000-ERROR
               WHEN OTHER
                   MOVE 'Error reading Portfolio' TO WS-ERROR-TEXT
                   PERFORM 9000-ERROR
           END-EVALUATE
           .
           
       4000-UPDATE-PORTFOLIO.
      *----------------------------------------------------------------*
      * 4000-UPDATE-PORTFOLIO: Validate then REWRITE an existing       *
      * record. Logs the update via AUDPROC for audit trail.           *
      *----------------------------------------------------------------*
           MOVE LS-PORTFOLIO TO PORTFOLIO-RECORD
           
           PERFORM 2100-VALIDATE-PORTFOLIO
           IF WS-RETURN-CODE NOT = WS-SUCCESS
               PERFORM 9000-ERROR
           END-IF
           
           REWRITE PORTFOLIO-RECORD
           
           IF PORT-NOT-FOUND
               MOVE 'Portfolio not found for update' TO WS-ERROR-TEXT
               PERFORM 9000-ERROR
           END-IF
           
           IF NOT PORT-SUCCESS
               MOVE 'Error updating Portfolio' TO WS-ERROR-TEXT
               PERFORM 9000-ERROR
           END-IF
           
           PERFORM 2100-LOG-PORTFOLIO-UPDATE
           .
           
       5000-DELETE-PORTFOLIO.
      *----------------------------------------------------------------*
      * 5000-DELETE-PORTFOLIO: Remove record from VSAM file by key.    *
      *----------------------------------------------------------------*
           MOVE LS-PORTFOLIO TO PORTFOLIO-RECORD
           
           DELETE PORTFOLIO-FILE
           
           IF PORT-NOT-FOUND
               MOVE 'Portfolio not found for deletion' TO WS-ERROR-TEXT
               PERFORM 9000-ERROR
           END-IF
           
           IF NOT PORT-SUCCESS
               MOVE 'Error deleting Portfolio' TO WS-ERROR-TEXT
               PERFORM 9000-ERROR
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 6000-TERMINATE: Close VSAM file and propagate return code.     *
      *----------------------------------------------------------------*
       6000-TERMINATE.
           CLOSE PORTFOLIO-FILE
           
           MOVE WS-RETURN-CODE TO LS-RETURN-CODE
           .
           
      *----------------------------------------------------------------*
      * 9000-ERROR: Set error return code, terminate, and exit.        *
      *----------------------------------------------------------------*
       9000-ERROR.
           MOVE WS-ERROR TO WS-RETURN-CODE
           PERFORM 6000-TERMINATE
           GOBACK
           .

      *----------------------------------------------------------------*
      * 2100-HANDLE-VSAM-ERROR: Map VSAM status codes to error        *
      * severity/message and delegate to ERRPROC subroutine.           *
      *----------------------------------------------------------------*
       2100-HANDLE-VSAM-ERROR.
           MOVE 'PORTMSTR' TO LS-PROGRAM-ID
           MOVE ERR-CAT-VSAM TO LS-CATEGORY
           MOVE WS-FILE-STATUS TO LS-ERROR-CODE
           
           EVALUATE WS-FILE-STATUS
               WHEN ERR-VSAM-DUPKEY
                   MOVE ERR-WARNING TO LS-SEVERITY
                   MOVE ERR-VSAM-22 TO LS-ERROR-TEXT
               WHEN ERR-VSAM-NOTFND
                   MOVE ERR-WARNING TO LS-SEVERITY
                   MOVE ERR-VSAM-23 TO LS-ERROR-TEXT
               WHEN OTHER
                   MOVE ERR-ERROR TO LS-SEVERITY
                   MOVE ERR-OTHER TO LS-ERROR-TEXT
           END-EVALUATE
           
           MOVE PORT-KEY TO LS-ERROR-DETAILS
           
           CALL 'ERRPROC' USING LS-ERROR-REQUEST
           .

      *----------------------------------------------------------------*
      * 2100-LOG-PORTFOLIO-UPDATE: Build an audit record capturing     *
      * before/after images and call AUDPROC to persist it.            *
      *----------------------------------------------------------------*
       2100-LOG-PORTFOLIO-UPDATE.
           INITIALIZE LS-AUDIT-REQUEST
           
           MOVE 'PORTFOLIO' TO LS-SYSTEM-ID
           MOVE USERID      TO LS-USER-ID
           MOVE 'PORTMSTR' TO LS-PROGRAM
           MOVE TERMINAL-ID TO LS-TERMINAL
           
           MOVE 'TRAN'     TO LS-TYPE
           MOVE 'UPDATE  ' TO LS-ACTION
           MOVE 'SUCC'     TO LS-STATUS
           
           MOVE PORT-ID    TO LS-PORT-ID
           MOVE PORT-ACCOUNT-NO TO LS-ACCT-NO
           
           MOVE WS-BEFORE-IMAGE TO LS-BEFORE-IMAGE
           MOVE PORT-RECORD     TO LS-AFTER-IMAGE
           MOVE 'Portfolio updated successfully' TO LS-MESSAGE
           
           CALL 'AUDPROC' USING LS-AUDIT-REQUEST
           .
