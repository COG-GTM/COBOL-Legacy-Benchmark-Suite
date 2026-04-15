       *================================================================*
      * Program Name: RCVPRC00
      * Description: Process Recovery Handler
      * Version: 1.0
      * Date: 2024
      *
      * Handles recovery of failed batch processes by reading the
      * batch control and process sequence files and determining
      * the appropriate recovery action for each failed process.
      *
      * Called By: JCL recovery jobs, operator recovery requests
      * Calls:    ERRPROC (error handler)
      * Files:
      *   BCHCTL - Batch Control VSAM KSDS (I/O)
      *   PRCSEQ - Process Sequence VSAM KSDS (Input)
      *
      * Function Codes (via Linkage):
      *   INIT - Initialize recovery environment
      *   RECV - Execute recovery for specified scope
      *   TERM - Terminate recovery and report results
      *
      * Recovery Modes (LS-RECOVERY-TYPE):
      *   P - Recover a single process (requires LS-PROCESS-ID)
      *   S - Recover all processes in a date sequence
      *   A - Recover all processes across all dates
      *
      * Recovery Actions (determined by process definition):
      *   Restart   - Reset to READY for re-execution
      *   Bypass    - Mark DONE with WARNING return code
      *   Terminate - Mark ERROR when max restarts exceeded
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. RCVPRC00.
       
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.
       
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT BATCH-CONTROL-FILE
               ASSIGN TO BCHCTL
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS BCT-KEY
               FILE STATUS IS WS-BCT-STATUS.
               
           SELECT PROCESS-SEQ-FILE
               ASSIGN TO PRCSEQ
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS PSR-KEY
               FILE STATUS IS WS-PSR-STATUS.
       
       DATA DIVISION.
       FILE SECTION.
       FD  BATCH-CONTROL-FILE.
           COPY BCHCTL.
           
       FD  PROCESS-SEQ-FILE.
           COPY PRCSEQ.
       
       WORKING-STORAGE SECTION.
           COPY BCHCON.
           COPY ERRHAND.
           
       01  WS-FILE-STATUS.
           05  WS-BCT-STATUS         PIC X(2).
           05  WS-PSR-STATUS         PIC X(2).
           
       01  WS-WORK-AREAS.
           05  WS-CURRENT-TIME       PIC X(26).
      * Recovery scope: P=single process, S=sequence, A=all
           05  WS-RECOVERY-MODE      PIC X(1).
               88  WS-RECOVER-PROCESS  VALUE 'P'.
               88  WS-RECOVER-SEQUENCE VALUE 'S'.
               88  WS-RECOVER-ALL      VALUE 'A'.
      * Action chosen for current process based on its definition
           05  WS-RECOVERY-ACTION    PIC X(1).
               88  WS-ACTION-RESTART   VALUE 'R'.
               88  WS-ACTION-BYPASS    VALUE 'B'.
               88  WS-ACTION-TERMINATE VALUE 'T'.
           
       LINKAGE SECTION.
       01  LS-RECOVERY-REQUEST.
           05  LS-FUNCTION          PIC X(4).
               88  FUNC-INIT          VALUE 'INIT'.
               88  FUNC-RECV          VALUE 'RECV'.
               88  FUNC-TERM          VALUE 'TERM'.
           05  LS-PROCESS-DATE     PIC X(8).
           05  LS-PROCESS-ID       PIC X(8).
           05  LS-RECOVERY-TYPE    PIC X(1).
           05  LS-RECOVERY-PARM    PIC X(50).
           05  LS-RETURN-CODE      PIC S9(4) COMP.
       
       PROCEDURE DIVISION USING LS-RECOVERY-REQUEST.
      *----------------------------------------------------------------*
      * 0000-MAIN: Dispatch to init, recovery, or termination based    *
      *   on the function code provided by the caller.                 *
      *----------------------------------------------------------------*
       0000-MAIN.
           EVALUATE TRUE
               WHEN FUNC-INIT
                   PERFORM 1000-INITIALIZE-RECOVERY
               WHEN FUNC-RECV
                   PERFORM 2000-PROCESS-RECOVERY
               WHEN FUNC-TERM
                   PERFORM 3000-TERMINATE-RECOVERY
               WHEN OTHER
                   MOVE 'Invalid function code' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-EVALUATE
           
           MOVE LS-RETURN-CODE TO RETURN-CODE
           GOBACK
           .
           
      *----------------------------------------------------------------*
      * 1000-INITIALIZE-RECOVERY: Open files, validate the request     *
      *   parameters, and set the recovery scope.                      *
      *----------------------------------------------------------------*
       1000-INITIALIZE-RECOVERY.
           PERFORM 1100-OPEN-FILES
           PERFORM 1200-VALIDATE-REQUEST
           PERFORM 1300-SET-RECOVERY-MODE
           .
           
      *----------------------------------------------------------------*
      * 2000-PROCESS-RECOVERY: Route to the correct recovery scope     *
      *   handler - single process, sequence, or all.                  *
      *----------------------------------------------------------------*
       2000-PROCESS-RECOVERY.
           EVALUATE WS-RECOVERY-MODE
               WHEN 'P'
                   PERFORM 2100-RECOVER-PROCESS
               WHEN 'S'
                   PERFORM 2200-RECOVER-SEQUENCE
               WHEN 'A'
                   PERFORM 2300-RECOVER-ALL
           END-EVALUATE
           .
           
      *----------------------------------------------------------------*
      * 3000-TERMINATE-RECOVERY: Log final status and close files.     *
      *----------------------------------------------------------------*
       3000-TERMINATE-RECOVERY.
           PERFORM 3100-UPDATE-FINAL-STATUS
           PERFORM 3200-CLOSE-FILES
           .
           
      *----------------------------------------------------------------*
      * 9000-ERROR-ROUTINE: Log error via ERRPROC, set ERROR RC.       *
      *----------------------------------------------------------------*
       9000-ERROR-ROUTINE.
           MOVE 'RCVPRC00' TO ERR-PROGRAM
           MOVE BCT-RC-ERROR TO LS-RETURN-CODE
           CALL 'ERRPROC' USING ERR-MESSAGE
           .
      *================================================================*
      * Recovery Implementation Procedures                              *
      *================================================================*
      *----------------------------------------------------------------*
      * 1100-OPEN-FILES: Open batch control (I/O) and process          *
      *   sequence (input) VSAM files.                                 *
      *----------------------------------------------------------------*
       1100-OPEN-FILES.
           OPEN I-O BATCH-CONTROL-FILE
           IF WS-BCT-STATUS NOT = '00'
               MOVE 'Error opening control file' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           
           OPEN INPUT PROCESS-SEQ-FILE
           IF WS-PSR-STATUS NOT = '00'
               MOVE 'Error opening sequence file' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 1200-VALIDATE-REQUEST: Ensure date is provided and recovery    *
      *   type is one of P, S, or A.                                   *
      *----------------------------------------------------------------*
       1200-VALIDATE-REQUEST.
           IF LS-PROCESS-DATE = SPACES
               MOVE 'Process date required' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           
           EVALUATE LS-RECOVERY-TYPE
               WHEN 'P'
               WHEN 'S'
               WHEN 'A'
                   CONTINUE
               WHEN OTHER
                   MOVE 'Invalid recovery type' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-EVALUATE
           .
           
      *----------------------------------------------------------------*
      * 1300-SET-RECOVERY-MODE: Copy type to working storage and       *
      *   verify process ID is given when mode is 'P'.                 *
      *----------------------------------------------------------------*
       1300-SET-RECOVERY-MODE.
           MOVE LS-RECOVERY-TYPE TO WS-RECOVERY-MODE
           
           IF WS-RECOVER-PROCESS AND LS-PROCESS-ID = SPACES
               MOVE 'Process ID required for process recovery'
                 TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 2100-RECOVER-PROCESS: Recover a single process - read its      *
      *   control record, determine action, and execute recovery.      *
      *----------------------------------------------------------------*
       2100-RECOVER-PROCESS.
           MOVE LS-PROCESS-ID   TO BCT-JOB-NAME
           MOVE LS-PROCESS-DATE TO BCT-PROCESS-DATE
           
           READ BATCH-CONTROL-FILE
               INVALID KEY
                   MOVE 'Process record not found' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-READ
           
           PERFORM 2110-DETERMINE-ACTION
           PERFORM 2120-EXECUTE-RECOVERY
           .
           
      *----------------------------------------------------------------*
      * 2110-DETERMINE-ACTION: Look up the process definition to       *
      *   decide restart vs bypass vs terminate.                       *
      *----------------------------------------------------------------*
       2110-DETERMINE-ACTION.
           MOVE LS-PROCESS-ID TO PSR-PROCESS-ID
           
           READ PROCESS-SEQ-FILE
               INVALID KEY
                   MOVE 'Process definition not found' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-READ
           
           IF PSR-RESTARTABLE
               SET WS-ACTION-RESTART TO TRUE
           ELSE
               IF BCT-RESTART-COUNT > BCT-MAX-RESTARTS
                   SET WS-ACTION-TERMINATE TO TRUE
               ELSE
                   SET WS-ACTION-BYPASS TO TRUE
               END-IF
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 2120-EXECUTE-RECOVERY: Dispatch to the chosen recovery action. *
      *----------------------------------------------------------------*
       2120-EXECUTE-RECOVERY.
           EVALUATE TRUE
               WHEN WS-ACTION-RESTART
                   PERFORM 2121-RESTART-PROCESS
               WHEN WS-ACTION-BYPASS
                   PERFORM 2122-BYPASS-PROCESS
               WHEN WS-ACTION-TERMINATE
                   PERFORM 2123-TERMINATE-PROCESS
           END-EVALUATE
           .
           
      *----------------------------------------------------------------*
      * 2121-RESTART-PROCESS: Reset status to READY, increment the     *
      *   restart count, and timestamp the attempt.                    *
      *----------------------------------------------------------------*
       2121-RESTART-PROCESS.
           MOVE BCT-STAT-READY TO BCT-STATUS
           ADD 1 TO BCT-RESTART-COUNT
           ACCEPT WS-CURRENT-TIME FROM TIME STAMP
           MOVE WS-CURRENT-TIME TO BCT-ATTEMPT-TS
           
           REWRITE BATCH-CONTROL-RECORD
               INVALID KEY
                   MOVE 'Error updating control record' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-REWRITE
           .
           
      *----------------------------------------------------------------*
      * 2122-BYPASS-PROCESS: Mark as DONE with WARNING RC and record   *
      *   that the process was bypassed by recovery.                   *
      *----------------------------------------------------------------*
       2122-BYPASS-PROCESS.
           MOVE BCT-STAT-DONE  TO BCT-STATUS
           MOVE BCT-RC-WARNING TO BCT-RETURN-CODE
           MOVE 'Process bypassed by recovery' TO BCT-ERROR-DESC
           
           REWRITE BATCH-CONTROL-RECORD
               INVALID KEY
                   MOVE 'Error updating control record' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-REWRITE
           .
           
      *----------------------------------------------------------------*
      * 2123-TERMINATE-PROCESS: Mark as ERROR with error description   *
      *   when the process cannot be restarted.                        *
      *----------------------------------------------------------------*
       2123-TERMINATE-PROCESS.
           MOVE BCT-STAT-ERROR TO BCT-STATUS
           MOVE BCT-RC-ERROR  TO BCT-RETURN-CODE
           MOVE 'Process terminated by recovery' TO BCT-ERROR-DESC
           
           REWRITE BATCH-CONTROL-RECORD
               INVALID KEY
                   MOVE 'Error updating control record' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-REWRITE
           .
           
      *----------------------------------------------------------------*
      * 2200-RECOVER-SEQUENCE: Browse batch control for all processes  *
      *   matching the given date and recover each one.                *
      *----------------------------------------------------------------*
       2200-RECOVER-SEQUENCE.
           MOVE LS-PROCESS-DATE TO BCT-PROCESS-DATE
           MOVE LOW-VALUES TO BCT-JOB-NAME
           
           START BATCH-CONTROL-FILE KEY > BCT-KEY
               INVALID KEY
                   MOVE 'No processes found for date' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-START
           
           PERFORM UNTIL WS-BCT-STATUS = '10'
               READ BATCH-CONTROL-FILE NEXT RECORD
                   AT END
                       MOVE '10' TO WS-BCT-STATUS
                   NOT AT END
                       IF BCT-PROCESS-DATE = LS-PROCESS-DATE
                           PERFORM 2100-RECOVER-PROCESS
                       END-IF
               END-READ
           END-PERFORM
           .
           
      *----------------------------------------------------------------*
      * 2300-RECOVER-ALL: Browse entire batch control file and         *
      *   recover every process regardless of date.                    *
      *----------------------------------------------------------------*
       2300-RECOVER-ALL.
           MOVE LOW-VALUES TO BCT-KEY
           
           START BATCH-CONTROL-FILE KEY > BCT-KEY
               INVALID KEY
                   MOVE 'No processes found' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-START
           
           PERFORM UNTIL WS-BCT-STATUS = '10'
               READ BATCH-CONTROL-FILE NEXT RECORD
                   AT END
                       MOVE '10' TO WS-BCT-STATUS
                   NOT AT END
                       MOVE BCT-JOB-NAME TO LS-PROCESS-ID
                       PERFORM 2100-RECOVER-PROCESS
               END-READ
           END-PERFORM
           .
           
      *----------------------------------------------------------------*
      * 3100-UPDATE-FINAL-STATUS: Log success or error summary via     *
      *   ERRPROC based on the accumulated return code.                *
      *----------------------------------------------------------------*
       3100-UPDATE-FINAL-STATUS.
           IF LS-RETURN-CODE = ZERO
               MOVE 'Recovery completed successfully' TO ERR-TEXT
           ELSE
               MOVE 'Recovery completed with errors' TO ERR-TEXT
           END-IF
           
           CALL 'ERRPROC' USING ERR-MESSAGE
           .
           
      *----------------------------------------------------------------*
      * 3200-CLOSE-FILES: Close VSAM files and report any close        *
      *   errors.                                                      *
      *----------------------------------------------------------------*
       3200-CLOSE-FILES.
           CLOSE BATCH-CONTROL-FILE
                 PROCESS-SEQ-FILE
                 
           IF WS-BCT-STATUS NOT = '00' OR
              WS-PSR-STATUS NOT = '00'
               MOVE 'Error closing files' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .
