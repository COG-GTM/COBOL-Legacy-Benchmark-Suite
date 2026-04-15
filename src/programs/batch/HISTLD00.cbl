       *================================================================*
      * Program Name: HISTLD00
      * Description: Position History DB2 Load Program
      *   Reads transaction history records from a VSAM KSDS file
      *   and bulk-inserts them into the DB2 POSHIST table.
      *   Implements checkpoint/restart logic by committing every
      *   WS-COMMIT-THRESHOLD records and updating the batch
      *   control file with progress counters.
      *
      * Called By: JCL batch job
      * Calls:    ERRPROC (error handler), DB2 (embedded SQL)
      * Files:    TRANHIST (Transaction History VSAM KSDS - Input)
      *           BCHCTL   (Batch Control VSAM KSDS - I/O)
      * Tables:   POSHIST  (DB2 position history - Insert)
      *
      * Processing Flow:
      *   1. Open VSAM files and connect to DB2
      *   2. Read history records sequentially
      *   3. Map VSAM fields to DB2 host variables
      *   4. INSERT into POSHIST; skip duplicates (SQLCODE -803)
      *   5. Commit every 1000 records and update checkpoint
      *   6. Final commit, display statistics, disconnect
      *
      * Abend Conditions:
      *   - Error opening files -> ERRPROC
      *   - >100 DB2 insert errors -> stops processing
      *
      * Version: 1.0
      * Date: 2024
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. HISTLD00.
       
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.
       
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
      *    Transaction History: Source VSAM file containing records
      *    to be loaded into DB2. Read sequentially from beginning.
           SELECT TRANSACTION-HISTORY
               ASSIGN TO TRANHIST
               ORGANIZATION IS INDEXED
               ACCESS MODE IS SEQUENTIAL
               RECORD KEY IS TH-KEY
               FILE STATUS IS WS-TH-STATUS.
               
      *    Batch Control: Tracks checkpoint/restart state for this
      *    load job. Updated with record counts after each commit.
           SELECT BATCH-CONTROL-FILE
               ASSIGN TO BCHCTL
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS BCT-KEY
               FILE STATUS IS WS-BCT-STATUS.
       
       DATA DIVISION.
       FILE SECTION.
       FD  TRANSACTION-HISTORY.
           COPY HISTREC.
       
       FD  BATCH-CONTROL-FILE.
           COPY BCHCTL.
       
       WORKING-STORAGE SECTION.
      *    DB2 host variable declarations for INSERT operations
           EXEC SQL BEGIN DECLARE SECTION END-EXEC.
           COPY DBTBLS.
           EXEC SQL END DECLARE SECTION END-EXEC.
           
      *    SQLCA: DB2 SQL communication area for return codes
           COPY SQLCA.
      *    DBPROC: DB2 connection/disconnect procedures
           COPY DBPROC.
      *    ERRHAND: Shared error handling fields
           COPY ERRHAND.
      *    BCHCON: Batch control constants (statuses, return codes)
           COPY BCHCON.
           
       01  WS-FILE-STATUS.
           05  WS-TH-STATUS          PIC X(2).
           05  WS-BCT-STATUS         PIC X(2).
           
      *    Processing counters for statistics and checkpoint tracking
       01  WS-COUNTERS.
           05  WS-RECORDS-READ       PIC S9(9) COMP VALUE 0.
           05  WS-RECORDS-WRITTEN    PIC S9(9) COMP VALUE 0.
           05  WS-ERROR-COUNT        PIC S9(9) COMP VALUE 0.
           05  WS-COMMIT-COUNT       PIC S9(4) COMP VALUE 0.
           
      *    Number of records between DB2 commits (for restartability)
       01  WS-COMMIT-THRESHOLD       PIC S9(4) COMP VALUE 1000.
       
       01  WS-SWITCHES.
           05  WS-END-OF-FILE-SW     PIC X(1) VALUE 'N'.
               88  END-OF-FILE         VALUE 'Y'.
               88  MORE-RECORDS        VALUE 'N'.
               
       PROCEDURE DIVISION.
      *----------------------------------------------------------------*
      * 0000-MAIN: Driver loop - initialize, process all records,
      *   then terminate. Stops early if error count exceeds 100.
      *   Sets RETURN-CODE to the total error count for JCL checking.
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           
           PERFORM 2000-PROCESS
               UNTIL END-OF-FILE
               OR WS-ERROR-COUNT > 100
           
           PERFORM 3000-TERMINATE
           
           MOVE WS-ERROR-COUNT TO RETURN-CODE
           GOBACK
           .
           
      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Open input files, connect to DB2, and
      *   read the checkpoint control record for restart support.
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           PERFORM 1100-OPEN-FILES
           PERFORM 1200-CONNECT-DB2
           PERFORM 1300-INIT-CHECKPOINTS
           .
           
      *----------------------------------------------------------------*
      * 2000-PROCESS: Read one history record and, if not at EOF,
      *   insert it into DB2 and check whether a commit is due.
      *----------------------------------------------------------------*
       2000-PROCESS.
           PERFORM 2100-READ-HISTORY
           
           IF MORE-RECORDS
               PERFORM 2200-LOAD-TO-DB2
               PERFORM 2300-CHECK-COMMIT
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 3000-TERMINATE: Issue final commit, close files, disconnect
      *   from DB2, and display processing statistics.
      *----------------------------------------------------------------*
       3000-TERMINATE.
           PERFORM 3100-FINAL-COMMIT
           PERFORM 3200-CLOSE-FILES
           PERFORM 3300-DISCONNECT-DB2
           PERFORM 3400-DISPLAY-STATS
           .
           
      *----------------------------------------------------------------*
      * 1100-OPEN-FILES: Open the transaction history file for
      *   sequential input and the batch control file for I/O.
      *----------------------------------------------------------------*
       1100-OPEN-FILES.
           OPEN INPUT TRANSACTION-HISTORY
           IF WS-TH-STATUS NOT = '00'
               MOVE 'Error opening history file' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           
           OPEN I-O BATCH-CONTROL-FILE
           IF WS-BCT-STATUS NOT = '00'
               MOVE 'Error opening control file' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .
           
       1200-CONNECT-DB2.
           PERFORM CONNECT-TO-DB2
           .
           
      *----------------------------------------------------------------*
      * 1300-INIT-CHECKPOINTS: Read this job's control record and
      *   mark it ACTIVE so other jobs can see it is running.
      *----------------------------------------------------------------*
       1300-INIT-CHECKPOINTS.
           MOVE SPACES TO BCT-KEY
           MOVE 'HISTLD00' TO BCT-JOB-NAME
           
           READ BATCH-CONTROL-FILE
               INVALID KEY
                   MOVE 'Control record not found' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-READ
           
           MOVE BCT-STAT-ACTIVE TO BCT-STATUS
           REWRITE BATCH-CONTROL-RECORD
           .
           
      *----------------------------------------------------------------*
      * 2100-READ-HISTORY: Read next sequential record from VSAM.
      *   Sets END-OF-FILE when no more records remain.
      *----------------------------------------------------------------*
       2100-READ-HISTORY.
           READ TRANSACTION-HISTORY
               AT END
                   SET END-OF-FILE TO TRUE
               NOT AT END
                   ADD 1 TO WS-RECORDS-READ
           END-READ
           .
           
      *----------------------------------------------------------------*
      * 2200-LOAD-TO-DB2: Map VSAM transaction fields to DB2 host
      *   variables and INSERT into POSHIST. Duplicate keys
      *   (SQLCODE -803) are silently skipped; other errors are
      *   counted and logged via DB2-ERROR-ROUTINE.
      *----------------------------------------------------------------*
       2200-LOAD-TO-DB2.
           INITIALIZE POSHIST-RECORD
           
           MOVE TH-ACCOUNT-NO    TO PH-ACCOUNT-NO
           MOVE TH-PORTFOLIO-ID  TO PH-PORTFOLIO-ID
           MOVE TH-TRANS-DATE    TO PH-TRANS-DATE
           MOVE TH-TRANS-TIME    TO PH-TRANS-TIME
           MOVE TH-TRANS-TYPE    TO PH-TRANS-TYPE
           MOVE TH-SECURITY-ID   TO PH-SECURITY-ID
           MOVE TH-QUANTITY      TO PH-QUANTITY
           MOVE TH-PRICE         TO PH-PRICE
           MOVE TH-AMOUNT        TO PH-AMOUNT
           MOVE TH-FEES          TO PH-FEES
           MOVE TH-TOTAL-AMOUNT  TO PH-TOTAL-AMOUNT
           MOVE TH-COST-BASIS    TO PH-COST-BASIS
           MOVE TH-GAIN-LOSS     TO PH-GAIN-LOSS
           
           EXEC SQL
               INSERT INTO POSHIST
               VALUES (:POSHIST-RECORD)
           END-EXEC
           
           IF SQLCODE = 0
               ADD 1 TO WS-RECORDS-WRITTEN
           ELSE
               IF SQLCODE = -803
                   CONTINUE
               ELSE
                   ADD 1 TO WS-ERROR-COUNT
                   PERFORM DB2-ERROR-ROUTINE
               END-IF
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 2300-CHECK-COMMIT: Implements interval-based commits.
      *   After every WS-COMMIT-THRESHOLD inserts, issues a DB2
      *   COMMIT and saves progress to the batch control file.
      *----------------------------------------------------------------*
       2300-CHECK-COMMIT.
           ADD 1 TO WS-COMMIT-COUNT
           
           IF WS-COMMIT-COUNT >= WS-COMMIT-THRESHOLD
               EXEC SQL
                   COMMIT WORK
               END-EXEC
               
               MOVE 0 TO WS-COMMIT-COUNT
               
               PERFORM 2310-UPDATE-CHECKPOINT
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 2310-UPDATE-CHECKPOINT: Persist current record counts to
      *   the batch control file so a restart can resume from here.
      *----------------------------------------------------------------*
       2310-UPDATE-CHECKPOINT.
           MOVE WS-RECORDS-READ TO BCT-RECORDS-READ
           MOVE WS-RECORDS-WRITTEN TO BCT-RECORDS-WRITTEN
           
           REWRITE BATCH-CONTROL-RECORD
               INVALID KEY
                   MOVE 'Error updating checkpoint' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-REWRITE
           .
           
      *----------------------------------------------------------------*
      * 3100-FINAL-COMMIT: Commit any remaining uncommitted rows
      *   and save final checkpoint before termination.
      *----------------------------------------------------------------*
       3100-FINAL-COMMIT.
           EXEC SQL
               COMMIT WORK
           END-EXEC
           
           PERFORM 2310-UPDATE-CHECKPOINT
           .
           
       3200-CLOSE-FILES.
           CLOSE TRANSACTION-HISTORY
                 BATCH-CONTROL-FILE
           .
           
       3300-DISCONNECT-DB2.
           PERFORM DISCONNECT-FROM-DB2
           .
           
       3400-DISPLAY-STATS.
           DISPLAY 'HISTLD00 Processing Statistics:'
           DISPLAY '  Records Read:    ' WS-RECORDS-READ
           DISPLAY '  Records Written: ' WS-RECORDS-WRITTEN
           DISPLAY '  Errors:         ' WS-ERROR-COUNT
           .
           
      *----------------------------------------------------------------*
      * 9000-ERROR-ROUTINE: Log the error via ERRPROC and issue a
      *   DB2 ROLLBACK to undo any uncommitted changes.
      *----------------------------------------------------------------*
       9000-ERROR-ROUTINE.
           MOVE 'HISTLD00' TO ERR-PROGRAM
           CALL 'ERRPROC' USING ERR-MESSAGE
           
           EXEC SQL
               ROLLBACK WORK
           END-EXEC
           .
