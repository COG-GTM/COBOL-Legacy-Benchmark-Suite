       *================================================================*
      * Program Name: DB2CMT
      * Description: DB2 Commit Controller
      *             Callable service providing centralized DB2
      *             transaction control: commit, rollback, and
      *             savepoint management with usage statistics.
      *
      * Functions (via LS-FUNCTION):
      *   INIT - Reset internal counters
      *   CMIT - Commit work (frequency-based or forced)
      *   RBAK - Rollback current unit of work
      *   SAVE - Create a named savepoint (retains cursors)
      *   REST - Restore to a named savepoint
      *   STAT - Display commit/rollback/savepoint counts
      *
      * Copybooks:   SQLCA    - SQL communication area
      *              DBPROC   - DB2 processing constants
      *              ERRHAND  - Error handling data areas
      *
      * Called By:   Batch and online programs needing DB2 commits
      * Calls:       ERRPROC  - Error processing subroutine
      *              DB2ERR   - DB2 error logging
      *
      * Return Codes: 0 = Success, 8 = SQL error, 12 = Invalid func
      *
      * Version: 1.0
      * Date: 2024
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DB2CMT.
       
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL BEGIN DECLARE SECTION END-EXEC.
           01  WS-SAVEPOINT-ID      PIC X(18).
           EXEC SQL END DECLARE SECTION END-EXEC.
           
           COPY SQLCA.
           COPY DBPROC.
           COPY ERRHAND.
           
      *    Running counters for commit controller activity
       01  WS-COMMIT-STATS.
           05  WS-COMMIT-COUNT      PIC S9(9) COMP VALUE 0.
           05  WS-ROLLBACK-COUNT    PIC S9(9) COMP VALUE 0.
           05  WS-SAVEPOINT-COUNT   PIC S9(9) COMP VALUE 0.
           
       01  WS-CURRENT-TIMESTAMP    PIC X(26).
       
       LINKAGE SECTION.
      *    Request area passed by calling program
       01  LS-COMMIT-REQUEST.
      *    Function code: INIT/CMIT/RBAK/SAVE/REST/STAT
           05  LS-FUNCTION         PIC X(4).
               88  FUNC-INIT         VALUE 'INIT'.
               88  FUNC-CMIT         VALUE 'CMIT'.
               88  FUNC-RBACK        VALUE 'RBAK'.
               88  FUNC-SAVE         VALUE 'SAVE'.
               88  FUNC-REST         VALUE 'REST'.
               88  FUNC-STAT         VALUE 'STAT'.
      *    Savepoint name for SAVE/REST functions
           05  LS-SAVEPOINT-NAME   PIC X(18).
      *    Commit control parameters
           05  LS-COMMIT-PARMS.
      *        Records processed since last commit
               10  LS-RECORDS-PROC PIC S9(9) COMP.
      *        Commit every N records
               10  LS-COMMIT-FREQ  PIC S9(4) COMP.
      *        Y = commit immediately regardless of frequency
               10  LS-FORCE-FLAG   PIC X(1).
                   88  LS-FORCE-COMMIT VALUE 'Y'.
           05  LS-RETURN-CODE      PIC S9(4) COMP.
           05  LS-ERROR-INFO.
               10  LS-SQLCODE      PIC S9(9) COMP.
               10  LS-ERROR-MSG    PIC X(80).
       
       PROCEDURE DIVISION USING LS-COMMIT-REQUEST.
      *----------------------------------------------------------------*
      * Main dispatch: route to handler based on function code.        *
      *----------------------------------------------------------------*
       0000-MAIN.
           EVALUATE TRUE
               WHEN FUNC-INIT
                   PERFORM 1000-INITIALIZE
               WHEN FUNC-CMIT
                   PERFORM 2000-COMMIT
               WHEN FUNC-RBACK
                   PERFORM 3000-ROLLBACK
               WHEN FUNC-SAVE
                   PERFORM 4000-SAVEPOINT
               WHEN FUNC-REST
                   PERFORM 5000-RESTORE
               WHEN FUNC-STAT
                   PERFORM 6000-STATISTICS
               WHEN OTHER
                   MOVE 'Invalid function code' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-EVALUATE
           
           GOBACK
           .
           
      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Reset all counters for a new processing run.  *
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           INITIALIZE WS-COMMIT-STATS
           MOVE 0 TO LS-RETURN-CODE
           .
           
      *----------------------------------------------------------------*
      * 2000-COMMIT: Issue commit only when record count reaches       *
      * the configured frequency threshold or force flag is set.       *
      *----------------------------------------------------------------*
       2000-COMMIT.
           IF LS-RECORDS-PROC >= LS-COMMIT-FREQ
           OR LS-FORCE-COMMIT
               PERFORM 2100-ISSUE-COMMIT
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 2100-ISSUE-COMMIT: Execute SQL COMMIT WORK and track result.   *
      *----------------------------------------------------------------*
       2100-ISSUE-COMMIT.
           EXEC SQL
               COMMIT WORK
           END-EXEC
           
           IF SQLCODE = 0
               ADD 1 TO WS-COMMIT-COUNT
               MOVE 0 TO LS-RETURN-CODE
           ELSE
               MOVE SQLCODE TO LS-SQLCODE
               MOVE 'Commit failed' TO LS-ERROR-MSG
               MOVE 8 TO LS-RETURN-CODE
               PERFORM 9100-LOG-ERROR
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 3000-ROLLBACK: Execute SQL ROLLBACK WORK for the current       *
      * unit of work. Logs failure via DB2ERR if rollback fails.       *
      *----------------------------------------------------------------*
       3000-ROLLBACK.
           EXEC SQL
               ROLLBACK WORK
           END-EXEC
           
           IF SQLCODE = 0
               ADD 1 TO WS-ROLLBACK-COUNT
               MOVE 0 TO LS-RETURN-CODE
           ELSE
               MOVE SQLCODE TO LS-SQLCODE
               MOVE 'Rollback failed' TO LS-ERROR-MSG
               MOVE 8 TO LS-RETURN-CODE
               PERFORM 9100-LOG-ERROR
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 4000-SAVEPOINT: Create a named savepoint that retains open     *
      * cursors, enabling partial rollback within a transaction.       *
      *----------------------------------------------------------------*
       4000-SAVEPOINT.
           MOVE LS-SAVEPOINT-NAME TO WS-SAVEPOINT-ID
           
           EXEC SQL
               SAVEPOINT :WS-SAVEPOINT-ID ON ROLLBACK RETAIN CURSORS
           END-EXEC
           
           IF SQLCODE = 0
               ADD 1 TO WS-SAVEPOINT-COUNT
               MOVE 0 TO LS-RETURN-CODE
           ELSE
               MOVE SQLCODE TO LS-SQLCODE
               MOVE 'Savepoint creation failed' TO LS-ERROR-MSG
               MOVE 8 TO LS-RETURN-CODE
               PERFORM 9100-LOG-ERROR
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 5000-RESTORE: Roll back to a previously created savepoint,     *
      * undoing changes made after the savepoint was established.      *
      *----------------------------------------------------------------*
       5000-RESTORE.
           MOVE LS-SAVEPOINT-NAME TO WS-SAVEPOINT-ID
           
           EXEC SQL
               ROLLBACK TO SAVEPOINT :WS-SAVEPOINT-ID
           END-EXEC
           
           IF SQLCODE = 0
               ADD 1 TO WS-ROLLBACK-COUNT
               MOVE 0 TO LS-RETURN-CODE
           ELSE
               MOVE SQLCODE TO LS-SQLCODE
               MOVE 'Savepoint restore failed' TO LS-ERROR-MSG
               MOVE 8 TO LS-RETURN-CODE
               PERFORM 9100-LOG-ERROR
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 6000-STATISTICS: Display accumulated commit/rollback counts.   *
      *----------------------------------------------------------------*
       6000-STATISTICS.
           DISPLAY 'DB2 Commit Controller Statistics:'
           DISPLAY '  Commits:    ' WS-COMMIT-COUNT
           DISPLAY '  Rollbacks:  ' WS-ROLLBACK-COUNT
           DISPLAY '  Savepoints: ' WS-SAVEPOINT-COUNT
           .
           
      *----------------------------------------------------------------*
      * 9000-ERROR-ROUTINE: Delegate to ERRPROC for logging.           *
      *----------------------------------------------------------------*
       9000-ERROR-ROUTINE.
           MOVE 'DB2CMT' TO ERR-PROGRAM
           MOVE 12 TO LS-RETURN-CODE
           CALL 'ERRPROC' USING ERR-MESSAGE
           .
           
      *----------------------------------------------------------------*
      * 9100-LOG-ERROR: Delegate SQL error to DB2ERR for persistence.  *
      *----------------------------------------------------------------*
       9100-LOG-ERROR.
           CALL 'DB2ERR' USING LS-ERROR-INFO
           .
