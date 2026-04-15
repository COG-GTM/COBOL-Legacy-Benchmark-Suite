      *================================================================*
      * Program Name: AUDPROC
      * Description: Audit Trail Processing Subroutine
      *             Callable service that appends a single audit
      *             record to a sequential audit log file.
      *
      * Processing:  Opens the audit file in EXTEND mode, maps the
      *              caller's request fields into the AUDITLOG
      *              copybook layout, timestamps the entry, writes
      *              the record, and closes the file.
      *
      * Files:       AUDFILE - Sequential audit log (EXTEND mode)
      *
      * Copybooks:   AUDITLOG - Audit record layout
      *
      * Called By:   PORTMSTR, PORTDEL, PORTTRAN, and any program
      *              needing audit trail persistence.
      *
      * Return Codes: 0 = Success, 8 = File I/O error
      *
      * Author: [Author name]
      * Date Written: 2024-03-20
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. AUDPROC.
       
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.
       
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT AUDIT-FILE
               ASSIGN TO AUDFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-FILE-STATUS.
       
       DATA DIVISION.
       FILE SECTION.
       FD  AUDIT-FILE
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
           COPY AUDITLOG.
       
       WORKING-STORAGE SECTION.
       01  WS-FILE-STATUS          PIC X(2).
       01  WS-FORMATTED-TIME       PIC X(26).
           
       LINKAGE SECTION.
      *    Request area passed by calling program
       01  LS-AUDIT-REQUEST.
      *    System context: who/what/where triggered the audit
           05  LS-SYSTEM-INFO.
               10  LS-SYSTEM-ID    PIC X(8).
               10  LS-USER-ID      PIC X(8).
               10  LS-PROGRAM      PIC X(8).
               10  LS-TERMINAL     PIC X(8).
      *    Audit classification (e.g., CRUD, TRAN, SECU)
           05  LS-TYPE            PIC X(4).
      *    Action performed (e.g., CREATE, UPDATE, DELETE)
           05  LS-ACTION          PIC X(8).
      *    Result status (e.g., SUCC, FAIL)
           05  LS-STATUS          PIC X(4).
      *    Portfolio key identifying the affected record
           05  LS-KEY-INFO.
               10  LS-PORT-ID     PIC X(8).
               10  LS-ACCT-NO     PIC X(10).
      *    Before/after record images for change tracking
           05  LS-BEFORE-IMAGE    PIC X(100).
           05  LS-AFTER-IMAGE     PIC X(100).
      *    Free-text message describing the event
           05  LS-MESSAGE         PIC X(100).
      *    Return code set by this program (0=OK, 8=Error)
           05  LS-RETURN-CODE     PIC S9(4) COMP.
       
       PROCEDURE DIVISION USING LS-AUDIT-REQUEST.
      *----------------------------------------------------------------*
      * Main control: initialize, write audit record, close file.      *
      *----------------------------------------------------------------*
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS-AUDIT
           PERFORM 3000-TERMINATE
           GOBACK
           .
           
      *----------------------------------------------------------------*
      * 1000-INITIALIZE: Capture current timestamp and open audit      *
      * file in EXTEND mode to append without overwriting.             *
      *----------------------------------------------------------------*
       1000-INITIALIZE.
           ACCEPT WS-FORMATTED-TIME FROM TIME STAMP
           
           OPEN EXTEND AUDIT-FILE
           IF WS-FILE-STATUS NOT = '00'
               DISPLAY 'Error opening audit file: ' WS-FILE-STATUS
               MOVE 8 TO LS-RETURN-CODE
               PERFORM 3000-TERMINATE
               GOBACK
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 2000-PROCESS-AUDIT: Map all request fields into the audit      *
      * record layout and write the record to the audit file.          *
      *----------------------------------------------------------------*
       2000-PROCESS-AUDIT.
           INITIALIZE AUDIT-RECORD
           
           MOVE WS-FORMATTED-TIME  TO AUD-TIMESTAMP
           MOVE LS-SYSTEM-INFO     TO AUD-HEADER
           MOVE LS-TYPE            TO AUD-TYPE
           MOVE LS-ACTION          TO AUD-ACTION
           MOVE LS-STATUS          TO AUD-STATUS
           MOVE LS-KEY-INFO        TO AUD-KEY-INFO
           MOVE LS-BEFORE-IMAGE    TO AUD-BEFORE-IMAGE
           MOVE LS-AFTER-IMAGE     TO AUD-AFTER-IMAGE
           MOVE LS-MESSAGE         TO AUD-MESSAGE
           
           WRITE AUDIT-RECORD
           
           IF WS-FILE-STATUS NOT = '00'
               DISPLAY 'Error writing audit record: ' WS-FILE-STATUS
               MOVE 8 TO LS-RETURN-CODE
           ELSE
               MOVE 0 TO LS-RETURN-CODE
           END-IF
           .
           
      *----------------------------------------------------------------*
      * 3000-TERMINATE: Close the audit file.                          *
      *----------------------------------------------------------------*
       3000-TERMINATE.
           CLOSE AUDIT-FILE
           .   