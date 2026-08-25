      *================================================================*
      * Program Name: GOLDDUMP
      * Description: Dumps the post-run state of the portfolio KSDS and
      *              the PORTDEL audit file as pipe-delimited canonical
      *              text, so the COBOL "before" state can be diffed
      *              against the JS "after" state field by field rather
      *              than byte by byte.
      *
      * COMP-3 fields are rendered through numeric-edited PICs, i.e. the
      * COBOL runtime decides the decimal value. The parity harness
      * compares those decimal values, never raw packed bytes
      * (CONTRACTS section 3).
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. GOLDDUMP.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT PORTFOLIO-FILE
               ASSIGN TO PORTFILE
               ORGANIZATION IS INDEXED
               ACCESS MODE IS SEQUENTIAL
               RECORD KEY IS PORT-KEY
               FILE STATUS IS WS-PORT-ST.

           SELECT AUDIT-FILE
               ASSIGN TO AUDFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-AUD-ST.

       DATA DIVISION.
       FILE SECTION.
       FD  PORTFOLIO-FILE.
           COPY PORTFLIO.

       FD  AUDIT-FILE.
       01  AUDIT-RECORD.
           05  AUD-TIMESTAMP      PIC X(26).
           05  AUD-ACTION         PIC X(6).
           05  AUD-KEY            PIC X(18).
           05  AUD-REASON         PIC X(2).
           05  AUD-STATUS         PIC X(1).
           05  AUD-FILLER         PIC X(27).

       WORKING-STORAGE SECTION.
       01  WS-STATUS.
           05  WS-PORT-ST          PIC X(2).
           05  WS-AUD-ST           PIC X(2).

       01  WS-FLAGS.
           05  WS-PORT-EOF         PIC X VALUE 'N'.
               88  PORT-EOF          VALUE 'Y'.
           05  WS-AUD-EOF          PIC X VALUE 'N'.
               88  AUD-EOF           VALUE 'Y'.

       01  WS-EDIT.
           05  WS-VALUE-ED         PIC -(14)9.99.
           05  WS-CASH-ED          PIC -(14)9.99.

       01  WS-COUNTS.
           05  WS-PORT-COUNT       PIC 9(5) VALUE ZERO.
           05  WS-AUD-COUNT        PIC 9(5) VALUE ZERO.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-DUMP-PORTFOLIOS
           PERFORM 2000-DUMP-AUDIT
           GOBACK.

       1000-DUMP-PORTFOLIOS.
           OPEN INPUT PORTFOLIO-FILE
           IF WS-PORT-ST NOT = '00'
               DISPLAY 'PORTFILE-OPEN-FAILED|' WS-PORT-ST
               EXIT PARAGRAPH
           END-IF

           PERFORM UNTIL PORT-EOF
               READ PORTFOLIO-FILE NEXT
                   AT END
                       SET PORT-EOF TO TRUE
                   NOT AT END
                       PERFORM 1100-DISPLAY-PORTFOLIO
               END-READ
           END-PERFORM

           DISPLAY 'PORTCOUNT|' WS-PORT-COUNT
           CLOSE PORTFOLIO-FILE
           .

       1100-DISPLAY-PORTFOLIO.
           ADD 1 TO WS-PORT-COUNT
           MOVE PORT-TOTAL-VALUE  TO WS-VALUE-ED
           MOVE PORT-CASH-BALANCE TO WS-CASH-ED
           DISPLAY 'PORT|'
                   PORT-ID           '|'
                   PORT-ACCOUNT-NO   '|'
                   PORT-CLIENT-NAME  '|'
                   PORT-CLIENT-TYPE  '|'
                   PORT-CREATE-DATE  '|'
                   PORT-LAST-MAINT   '|'
                   PORT-STATUS       '|'
                   WS-VALUE-ED       '|'
                   WS-CASH-ED        '|'
                   PORT-LAST-USER    '|'
                   PORT-LAST-TRANS
           .

       2000-DUMP-AUDIT.
           OPEN INPUT AUDIT-FILE
           IF WS-AUD-ST NOT = '00'
               DISPLAY 'AUDFILE-ABSENT|' WS-AUD-ST
               EXIT PARAGRAPH
           END-IF

           PERFORM UNTIL AUD-EOF
               READ AUDIT-FILE
                   AT END
                       SET AUD-EOF TO TRUE
                   NOT AT END
                       PERFORM 2100-DISPLAY-AUDIT
               END-READ
           END-PERFORM

           DISPLAY 'AUDCOUNT|' WS-AUD-COUNT
           CLOSE AUDIT-FILE
           .

      *----------------------------------------------------------------*
      * AUD-TIMESTAMP is deliberately not displayed: it is wall-clock
      * and cannot be reproduced. The harness records that the field was
      * populated; every other audit field is compared exactly.
      *----------------------------------------------------------------*
       2100-DISPLAY-AUDIT.
           ADD 1 TO WS-AUD-COUNT
           DISPLAY 'AUD|'
                   AUD-ACTION  '|'
                   AUD-KEY     '|'
                   AUD-REASON  '|'
                   AUD-STATUS
           .
