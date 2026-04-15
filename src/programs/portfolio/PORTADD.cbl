       *================================================================*
      * Program Name: PORTADD
      * Description: Portfolio Addition Program
      *             Batch-loads new portfolio records from a sequential
      *             input file into the indexed VSAM portfolio master.
      *
      * Processing:  Reads INPTFILE sequentially, validates each
      *              record, and writes to PORTFILE. Tracks counts
      *              of successful adds, duplicates, and errors.
      *
      * Files:       PORTFILE - Indexed VSAM portfolio master (I-O)
      *              INPTFILE - Sequential input of new portfolios
      *
      * Copybooks:   PORTFLIO - Portfolio record layout
      *
      * Return Codes: 0 = Success, 8 = File open error
      *
      * Author: [Author name]
      * Date Written: 2024-03-20
      * Maintenance Log:
      * Date       Author        Description
      * ---------- ------------- -------------------------------------
      * 2024-03-20 [Author]     Initial Creation
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PORTADD.
       
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.
       
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT PORTFOLIO-FILE
               ASSIGN TO PORTFILE
               ORGANIZATION IS INDEXED
               ACCESS MODE IS RANDOM
               RECORD KEY IS PORT-KEY
               FILE STATUS IS WS-FILE-STATUS.
           
           SELECT INPUT-FILE
               ASSIGN TO INPTFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-INPUT-STATUS.
       
       DATA DIVISION.
       FILE SECTION.
       FD  PORTFOLIO-FILE.
           COPY PORTFLIO.
           
       FD  INPUT-FILE.
           COPY PORTFLIO.
       
       WORKING-STORAGE SECTION.
      *----------------------------------------------------------------*
      * Constants and switches
      *----------------------------------------------------------------*
       01  WS-CONSTANTS.
           05  WS-PROGRAM-NAME     PIC X(08) VALUE 'PORTADD  '.
           05  WS-SUCCESS          PIC S9(4) VALUE +0.
           05  WS-ERROR            PIC S9(4) VALUE +8.
           
       01  WS-SWITCHES.
      *    VSAM file status for portfolio master I/O
           05  WS-FILE-STATUS      PIC X(02).
               88  WS-SUCCESS-STATUS     VALUE '00'.
               88  WS-DUP-STATUS         VALUE '22'.
               88  WS-EOF-STATUS         VALUE '10'.
      *    Sequential input file status
           05  WS-INPUT-STATUS     PIC X(02).
               88  WS-INPUT-SUCCESS      VALUE '00'.
               88  WS-INPUT-EOF          VALUE '10'.
           
           05  WS-END-OF-FILE-SW   PIC X     VALUE 'N'.
               88  END-OF-FILE              VALUE 'Y'.
               88  NOT-END-OF-FILE          VALUE 'N'.
           
      *----------------------------------------------------------------*
      * Work areas
      *----------------------------------------------------------------*
       01  WS-WORK-AREAS.
      *    Counters for processing summary
           05  WS-ADD-COUNT        PIC 9(7) VALUE ZERO.
           05  WS-ERROR-COUNT      PIC 9(7) VALUE ZERO.
           05  WS-DUP-COUNT        PIC 9(7) VALUE ZERO.
           05  WS-RETURN-CODE      PIC S9(4) VALUE +0.
           05  WS-CURRENT-DATE     PIC 9(8).
           
       PROCEDURE DIVISION.
      *----------------------------------------------------------------*
      * Main control: open files, process all input records, close.    *
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           
           PERFORM 2000-PROCESS
              UNTIL END-OF-FILE
           
           PERFORM 3000-TERMINATE
           
           GOBACK.
           
      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Open both files and validate file statuses.   *
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           INITIALIZE WS-WORK-AREAS
           
           ACCEPT WS-CURRENT-DATE FROM DATE YYYYMMDD
           
           OPEN I-O   PORTFOLIO-FILE
           OPEN INPUT INPUT-FILE
           
           IF NOT WS-SUCCESS-STATUS OR 
              NOT WS-INPUT-SUCCESS
              DISPLAY 'Error opening files: ' 
                      'PORT=' WS-FILE-STATUS
                      'INPT=' WS-INPUT-STATUS
              MOVE WS-ERROR TO WS-RETURN-CODE
              PERFORM 3000-TERMINATE
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 2000-PROCESS: Read next input record; delegate to validation.  *
      *----------------------------------------------------------------*
       2000-PROCESS.
           READ INPUT-FILE INTO PORT-RECORD
               AT END
                   SET END-OF-FILE TO TRUE
               NOT AT END
                   PERFORM 2100-VALIDATE-AND-ADD
           END-READ
           .
           
      *----------------------------------------------------------------*
      * 2100-VALIDATE-AND-ADD: Reject records with blank ID/name or   *
      * non-Active status, then write valid records to VSAM file.      *
      *----------------------------------------------------------------*
       2100-VALIDATE-AND-ADD.
           IF PORT-ID EQUAL SPACES OR
              PORT-CLIENT-NAME EQUAL SPACES OR
              PORT-STATUS NOT EQUAL 'A'
               ADD 1 TO WS-ERROR-COUNT
               DISPLAY 'Invalid record data: ' PORT-ID
               EXIT PARAGRAPH
           END-IF
           
           MOVE WS-CURRENT-DATE TO PORT-CREATE-DATE
           MOVE WS-CURRENT-DATE TO PORT-LAST-MAINT
           
           WRITE PORT-RECORD
           
           EVALUATE TRUE
               WHEN WS-SUCCESS-STATUS
                   ADD 1 TO WS-ADD-COUNT
               WHEN WS-DUP-STATUS
                   ADD 1 TO WS-DUP-COUNT
                   DISPLAY 'Duplicate record: ' PORT-ID
               WHEN OTHER
                   ADD 1 TO WS-ERROR-COUNT
                   DISPLAY 'Write error for: ' PORT-ID
           END-EVALUATE
           .
           
      *----------------------------------------------------------------*
      * 3000-TERMINATE: Close files and display processing summary.    *
      *----------------------------------------------------------------*
       3000-TERMINATE.
           CLOSE PORTFOLIO-FILE
                 INPUT-FILE
           
           DISPLAY 'Records added:    ' WS-ADD-COUNT
           DISPLAY 'Duplicate records:' WS-DUP-COUNT
           DISPLAY 'Errors occurred:  ' WS-ERROR-COUNT
           
           MOVE WS-RETURN-CODE TO RETURN-CODE
           .
