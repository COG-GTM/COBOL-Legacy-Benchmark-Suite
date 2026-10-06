       IDENTIFICATION DIVISION.
       PROGRAM-ID. RPTPOS00R.
      *****************************************************************
      * Compilable GnuCOBOL reference for RPTPOS00 (Daily Position     *
      * Report). It produces the golden RPTFILE the Java PositionReport*
      * is compared against. Differences from src/programs/batch:      *
      *  - POSITION-MASTER is read as a fixed-length sequential file   *
      *    (the unloaded VSAM data set), same bytes as POSREC.cpy.     *
      *  - POS-DESCRIPTION / POS-CURRENT-VALUE / POS-PREVIOUS-VALUE do  *
      *    not exist in POSREC; the port uses POS-INVESTMENT-ID,       *
      *    POS-MARKET-VALUE and POS-COST-BASIS.                        *
      *  - A zero cost basis prints 0.00 instead of a size error.      *
      *  - RPTDATE (YYMMDD) overrides ACCEPT FROM DATE for repeatable  *
      *    output. TRANHIST is not read: 2200-/2300- have no body.     *
      *****************************************************************
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT POSITION-MASTER ASSIGN TO WS-POSMSTRE-NAME
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-POSITION-STATUS.
           SELECT REPORT-FILE ASSIGN TO WS-RPTFILE-NAME
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-REPORT-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  POSITION-MASTER
           RECORDING MODE IS F.
           COPY POSREC.
       FD  REPORT-FILE
           RECORDING MODE IS F.
       01  REPORT-RECORD             PIC X(132).

       WORKING-STORAGE SECTION.
       01  WS-POSMSTRE-NAME          PIC X(256).
       01  WS-RPTFILE-NAME           PIC X(256).
       01  WS-ENV-DATE               PIC X(10).
       01  WS-FILE-STATUS.
           05  WS-POSITION-STATUS    PIC XX.
               88  END-OF-POSITIONS  VALUE '10'.
           05  WS-REPORT-STATUS      PIC XX.

       01  WS-REPORT-HEADERS.
           05  WS-HEADER1.
               10  FILLER            PIC X(132) VALUE ALL '*'.
           05  WS-HEADER2.
               10  FILLER            PIC X(40) VALUE SPACES.
               10  FILLER            PIC X(52)
                   VALUE 'DAILY POSITION REPORT'.
               10  FILLER            PIC X(40) VALUE SPACES.
           05  WS-HEADER3.
               10  FILLER            PIC X(15) VALUE 'REPORT DATE:'.
               10  WS-REPORT-DATE    PIC X(10).
               10  FILLER            PIC X(107) VALUE SPACES.

       01  WS-POSITION-DETAIL.
           05  WS-POS-PORTFOLIO     PIC X(10).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-POS-DESCRIPTION   PIC X(30).
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-POS-QUANTITY      PIC ZZZ,ZZZ,ZZ9.99.
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-POS-VALUE         PIC $$$$,$$$,$$9.99.
           05  FILLER               PIC X(2) VALUE SPACES.
           05  WS-POS-CHANGE-PCT    PIC +ZZ9.99.
           05  FILLER               PIC X(40) VALUE SPACES.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2100-READ-POSITIONS
           PERFORM 3000-CLEANUP
           GOBACK.

       1000-INITIALIZE.
           ACCEPT WS-POSMSTRE-NAME FROM ENVIRONMENT 'POSMSTRE'
           ACCEPT WS-RPTFILE-NAME  FROM ENVIRONMENT 'RPTFILE'
           OPEN INPUT POSITION-MASTER
           IF WS-POSITION-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING POSITION MASTER '
                       WS-POSITION-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN OUTPUT REPORT-FILE
           IF WS-REPORT-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING REPORT FILE ' WS-REPORT-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           PERFORM 1200-WRITE-HEADERS.

       1200-WRITE-HEADERS.
           ACCEPT WS-ENV-DATE FROM ENVIRONMENT 'RPTDATE'
           IF WS-ENV-DATE = SPACES
               ACCEPT WS-REPORT-DATE FROM DATE
           ELSE
               MOVE WS-ENV-DATE TO WS-REPORT-DATE
           END-IF
           WRITE REPORT-RECORD FROM WS-HEADER1
           WRITE REPORT-RECORD FROM WS-HEADER2
           WRITE REPORT-RECORD FROM WS-HEADER3.

       2100-READ-POSITIONS.
           READ POSITION-MASTER
           PERFORM UNTIL END-OF-POSITIONS
               PERFORM 2110-FORMAT-POSITION
               READ POSITION-MASTER
           END-PERFORM.

       2110-FORMAT-POSITION.
           MOVE POS-PORTFOLIO-ID   TO WS-POS-PORTFOLIO
           MOVE POS-INVESTMENT-ID  TO WS-POS-DESCRIPTION
           MOVE POS-QUANTITY       TO WS-POS-QUANTITY
           MOVE POS-MARKET-VALUE   TO WS-POS-VALUE
           IF POS-COST-BASIS = ZERO
               MOVE ZERO TO WS-POS-CHANGE-PCT
           ELSE
               COMPUTE WS-POS-CHANGE-PCT =
                   (POS-MARKET-VALUE - POS-COST-BASIS) /
                    POS-COST-BASIS * 100
           END-IF
           WRITE REPORT-RECORD FROM WS-POSITION-DETAIL.

       3000-CLEANUP.
           CLOSE POSITION-MASTER
                 REPORT-FILE.
