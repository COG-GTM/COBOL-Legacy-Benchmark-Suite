       IDENTIFICATION DIVISION.
       PROGRAM-ID. UTLMNT00.
       AUTHOR. CLAUDE.
       DATE-WRITTEN. 2024-04-09.
      *****************************************************************
      * File Maintenance Utility                                       *
      *                                                               *
      * Batch program that performs scheduled maintenance tasks on     *
      * system files, driven by a control file specifying which       *
      * operation to perform on which dataset.                         *
      *                                                               *
      * Functions (via CTL-FUNCTION):                                 *
      *   ARCHIVE  - Copy aged records to archive file                *
      *   CLEANUP  - Analyze space, delete old data, update catalog   *
      *   REORG    - Export, delete/define, and reimport VSAM data    *
      *   ANALYZE  - Collect statistics and produce a health report   *
      *                                                               *
      * Files:                                                        *
      *   CTLFILE  - Input  : Control file (function + dataset name)  *
      *   ARCHFILE - Output : Archive destination (variable-length)   *
      *   RPTFILE  - Output : Maintenance activity report             *
      *                                                               *
      * Copybooks: RTNCODE, ERRHAND                                  *
      *                                                               *
      * Return Codes: 0 = Success, 12 = Excessive errors (>100)       *
      *****************************************************************
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SPECIAL-NAMES.
           CONSOLE IS CONS.
           
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT CONTROL-FILE ASSIGN TO CTLFILE
               ORGANIZATION IS SEQUENTIAL
               ACCESS MODE IS SEQUENTIAL
               FILE STATUS IS WS-CTL-STATUS.

           SELECT ARCHIVE-FILE ASSIGN TO ARCHFILE
               ORGANIZATION IS SEQUENTIAL
               ACCESS MODE IS SEQUENTIAL
               FILE STATUS IS WS-ARCH-STATUS.

           SELECT REPORT-FILE ASSIGN TO RPTFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-REPORT-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  CONTROL-FILE
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  CONTROL-RECORD.
           05  CTL-FUNCTION         PIC X(8).
           05  CTL-FILE-NAME        PIC X(44).
           05  CTL-PARAMETERS       PIC X(100).

       FD  ARCHIVE-FILE
           RECORDING MODE IS V
           BLOCK CONTAINS 0 RECORDS.
       01  ARCHIVE-RECORD          PIC X(32760).

       FD  REPORT-FILE
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  REPORT-RECORD           PIC X(132).

       WORKING-STORAGE SECTION.
           COPY RTNCODE.
           COPY ERRHAND.

       01  WS-FILE-STATUS.
           05  WS-CTL-STATUS        PIC XX.
           05  WS-ARCH-STATUS       PIC XX.
           05  WS-REPORT-STATUS     PIC XX.

       01  WS-PROCESSING-FLAGS.
           05  WS-END-OF-CTL        PIC X VALUE 'N'.
               88  END-OF-CONTROL   VALUE 'Y'.
           05  WS-FUNCTION-FLAG     PIC X VALUE 'N'.
               88  VALID-FUNCTION   VALUE 'Y'.

      *    Constants for maintenance function dispatch
       01  WS-FUNCTIONS.
           05  WS-ARCHIVE           PIC X(8) VALUE 'ARCHIVE'.
           05  WS-CLEANUP           PIC X(8) VALUE 'CLEANUP'.
           05  WS-REORG            PIC X(8) VALUE 'REORG'.
           05  WS-ANALYZE          PIC X(8) VALUE 'ANALYZE'.

       01  WS-COUNTERS.
           05  WS-RECORDS-READ      PIC 9(9) VALUE ZERO.
           05  WS-RECORDS-WRITTEN   PIC 9(9) VALUE ZERO.
           05  WS-ERROR-COUNT       PIC 9(9) VALUE ZERO.

      *    Work area for VSAM operations (reorg, analyze)
       01  WS-VSAM-CONTROL.
           05  WS-VSAM-NAME         PIC X(44).
           05  WS-VSAM-FUNCTION     PIC X(8).
           05  WS-VSAM-STATUS       PIC XX.

       PROCEDURE DIVISION.
      *----------------------------------------------------------------*
      * Main control: initialize, process control file, clean up.      *
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS
           PERFORM 3000-CLEANUP
           GOBACK.

      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Open files and reset counters.                *
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           PERFORM 1100-OPEN-FILES
           PERFORM 1200-INIT-PROCESSING.

       1100-OPEN-FILES.
           OPEN INPUT CONTROL-FILE
           IF WS-CTL-STATUS NOT = '00'
               MOVE 'ERROR OPENING CONTROL FILE'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF

           OPEN OUTPUT ARCHIVE-FILE
           IF WS-ARCH-STATUS NOT = '00'
               MOVE 'ERROR OPENING ARCHIVE FILE'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF

           OPEN OUTPUT REPORT-FILE
           IF WS-REPORT-STATUS NOT = '00'
               MOVE 'ERROR OPENING REPORT FILE'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF.

       1200-INIT-PROCESSING.
           INITIALIZE WS-COUNTERS.

      *----------------------------------------------------------------*
      * 2000-PROCESS: Read control records and dispatch to the         *
      * appropriate maintenance function handler.                      *
      *----------------------------------------------------------------*
       2000-PROCESS.
           PERFORM UNTIL END-OF-CONTROL
               READ CONTROL-FILE
                   AT END
                       SET END-OF-CONTROL TO TRUE
                   NOT AT END
                       PERFORM 2100-PROCESS-FUNCTION
               END-READ
           END-PERFORM.

      *----------------------------------------------------------------*
      * 2100-PROCESS-FUNCTION: Route to handler based on function.     *
      *----------------------------------------------------------------*
       2100-PROCESS-FUNCTION.
           EVALUATE CTL-FUNCTION
               WHEN WS-ARCHIVE
                   PERFORM 2200-ARCHIVE-PROCESS
               WHEN WS-CLEANUP
                   PERFORM 2300-CLEANUP-PROCESS
               WHEN WS-REORG
                   PERFORM 2400-REORG-PROCESS
               WHEN WS-ANALYZE
                   PERFORM 2500-ANALYZE-PROCESS
               WHEN OTHER
                   MOVE 'INVALID FUNCTION SPECIFIED'
                     TO WS-ERROR-MESSAGE
                   PERFORM 9999-ERROR-HANDLER
           END-EVALUATE.

      *----------------------------------------------------------------*
      * 2200-ARCHIVE-PROCESS: Open source VSAM, copy eligible aged     *
      * records to the archive file, then close the source.            *
      *----------------------------------------------------------------*
       2200-ARCHIVE-PROCESS.
           MOVE CTL-FILE-NAME TO WS-VSAM-NAME
           PERFORM 2210-OPEN-VSAM
           PERFORM 2220-ARCHIVE-RECORDS
           PERFORM 2230-CLOSE-VSAM.

      *----------------------------------------------------------------*
      * 2300-CLEANUP-PROCESS: Analyze space usage, purge obsolete      *
      * records, and update the system catalog.                        *
      *----------------------------------------------------------------*
       2300-CLEANUP-PROCESS.
           MOVE CTL-FILE-NAME TO WS-VSAM-NAME
           PERFORM 2310-ANALYZE-SPACE
           PERFORM 2320-DELETE-OLD
           PERFORM 2330-UPDATE-CATALOG.

      *----------------------------------------------------------------*
      * 2400-REORG-PROCESS: Full VSAM reorganization via export,       *
      * delete/define to reclaim space, and reimport data.             *
      *----------------------------------------------------------------*
       2400-REORG-PROCESS.
           MOVE CTL-FILE-NAME TO WS-VSAM-NAME
           PERFORM 2410-EXPORT-DATA
           PERFORM 2420-DELETE-DEFINE
           PERFORM 2430-IMPORT-DATA.

      *----------------------------------------------------------------*
      * 2500-ANALYZE-PROCESS: Collect dataset statistics and produce   *
      * a health/capacity report.                                      *
      *----------------------------------------------------------------*
       2500-ANALYZE-PROCESS.
           MOVE CTL-FILE-NAME TO WS-VSAM-NAME
           PERFORM 2510-COLLECT-STATS
           PERFORM 2520-GENERATE-REPORT.

      *----------------------------------------------------------------*
      * 3000-CLEANUP: Close all files.                                 *
      *----------------------------------------------------------------*
       3000-CLEANUP.
           CLOSE CONTROL-FILE
                ARCHIVE-FILE
                REPORT-FILE.

      *----------------------------------------------------------------*
      * 9999-ERROR-HANDLER: Display error; abort if count > 100.       *
      *----------------------------------------------------------------*
       9999-ERROR-HANDLER.
           ADD 1 TO WS-ERROR-COUNT
           DISPLAY WS-ERROR-MESSAGE UPON CONS
           IF WS-ERROR-COUNT > 100
               MOVE 12 TO RETURN-CODE
               GOBACK
           END-IF.  