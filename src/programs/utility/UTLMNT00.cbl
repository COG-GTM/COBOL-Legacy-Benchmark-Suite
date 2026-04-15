       IDENTIFICATION DIVISION.
       PROGRAM-ID. UTLMNT00.
       AUTHOR. CLAUDE.
       DATE-WRITTEN. 2024-04-09.
      *****************************************************************
      * Program Name: UTLMNT00                                        *
      * Description: File Maintenance Utility                         *
      *   Reads a control file specifying maintenance functions and   *
      *   performs the requested operation on each target file:       *
      *   - ARCHIVE:  copies aged records to an archive dataset      *
      *   - CLEANUP:  reclaims space and purges expired records      *
      *   - REORG:    exports, deletes/defines, and reimports VSAM   *
      *   - ANALYZE:  collects file statistics for capacity reports  *
      *   Halts after 100 cumulative errors.                         *
      * Files: CTLFILE (control input), ARCHFILE (archive output),   *
      *        RPTFILE (report output)                               *
      * Copybooks: RTNCODE, ERRHAND                                  *
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

       01  WS-FUNCTIONS.
           05  WS-ARCHIVE           PIC X(8) VALUE 'ARCHIVE'.
           05  WS-CLEANUP           PIC X(8) VALUE 'CLEANUP'.
           05  WS-REORG            PIC X(8) VALUE 'REORG'.
           05  WS-ANALYZE          PIC X(8) VALUE 'ANALYZE'.

       01  WS-COUNTERS.
           05  WS-RECORDS-READ      PIC 9(9) VALUE ZERO.
           05  WS-RECORDS-WRITTEN   PIC 9(9) VALUE ZERO.
           05  WS-ERROR-COUNT       PIC 9(9) VALUE ZERO.

       01  WS-VSAM-CONTROL.
           05  WS-VSAM-NAME         PIC X(44).
           05  WS-VSAM-FUNCTION     PIC X(8).
           05  WS-VSAM-STATUS       PIC XX.

       PROCEDURE DIVISION.
      *----------------------------------------------------------------*
      * Main control flow: Open files, process each maintenance
      * function from the control file, then close files.
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS
           PERFORM 3000-CLEANUP
           GOBACK.

      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Opens files and resets counters.
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           PERFORM 1100-OPEN-FILES
           PERFORM 1200-INIT-PROCESSING.

      *----------------------------------------------------------------*
      * 1100-OPEN-FILES: Opens control (input), archive (output),
      * and report (output) files.
      *----------------------------------------------------------------*
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

      *----------------------------------------------------------------*
      * 1200-INIT-PROCESSING: Resets record and error counters.
      *----------------------------------------------------------------*
       1200-INIT-PROCESSING.
           INITIALIZE WS-COUNTERS.

      *----------------------------------------------------------------*
      * 2000-PROCESS: Reads control records until EOF.
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
      * 2100-PROCESS-FUNCTION: Dispatches to the maintenance
      * operation matching the control record function code.
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
      * 2200-ARCHIVE-PROCESS: Opens the VSAM file, copies aged
      * records to the archive, then closes.
      *----------------------------------------------------------------*
       2200-ARCHIVE-PROCESS.
           MOVE CTL-FILE-NAME TO WS-VSAM-NAME
           PERFORM 2210-OPEN-VSAM
           PERFORM 2220-ARCHIVE-RECORDS
           PERFORM 2230-CLOSE-VSAM.

      *----------------------------------------------------------------*
      * 2300-CLEANUP-PROCESS: Analyzes space usage, purges
      * expired records, and updates the catalog.
      *----------------------------------------------------------------*
       2300-CLEANUP-PROCESS.
           MOVE CTL-FILE-NAME TO WS-VSAM-NAME
           PERFORM 2310-ANALYZE-SPACE
           PERFORM 2320-DELETE-OLD
           PERFORM 2330-UPDATE-CATALOG.

      *----------------------------------------------------------------*
      * 2400-REORG-PROCESS: Reorganizes a VSAM file by exporting
      * data, deleting/redefining the cluster, and reimporting.
      *----------------------------------------------------------------*
       2400-REORG-PROCESS.
           MOVE CTL-FILE-NAME TO WS-VSAM-NAME
           PERFORM 2410-EXPORT-DATA
           PERFORM 2420-DELETE-DEFINE
           PERFORM 2430-IMPORT-DATA.

      *----------------------------------------------------------------*
      * 2500-ANALYZE-PROCESS: Collects file statistics and writes
      * a capacity/health report.
      *----------------------------------------------------------------*
       2500-ANALYZE-PROCESS.
           MOVE CTL-FILE-NAME TO WS-VSAM-NAME
           PERFORM 2510-COLLECT-STATS
           PERFORM 2520-GENERATE-REPORT.

      *----------------------------------------------------------------*
      * 3000-CLEANUP: Closes all files.
      *----------------------------------------------------------------*
       3000-CLEANUP.
           CLOSE CONTROL-FILE
                ARCHIVE-FILE
                REPORT-FILE.

      *----------------------------------------------------------------*
      * 9999-ERROR-HANDLER: Logs error to console and aborts
      * (RC=12) after 100 cumulative errors.
      *----------------------------------------------------------------*
       9999-ERROR-HANDLER.
           ADD 1 TO WS-ERROR-COUNT
           DISPLAY WS-ERROR-MESSAGE UPON CONS
           IF WS-ERROR-COUNT > 100
               MOVE 12 TO RETURN-CODE
               GOBACK
           END-IF.   