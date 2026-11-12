       IDENTIFICATION DIVISION.
       PROGRAM-ID. CODECRD.
      * Reads CODEC-RECORDs written by Python and DISPLAYs every field
      * as NAME=VALUE so the test can compare what COBOL *sees*.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT IN-FILE ASSIGN TO "codec.dat"
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-STATUS.
       DATA DIVISION.
       FILE SECTION.
       FD  IN-FILE.
       COPY CODECREC.
       WORKING-STORAGE SECTION.
       01  WS-STATUS              PIC X(2).
       01  WS-EOF                 PIC X VALUE "N".
       01  E-S4                   PIC -9(4).
       01  E-U2                   PIC 9(2).
       01  E-S8                   PIC -9(8).
       01  E-S18                  PIC -9(18).
       01  E-S4V2                 PIC -9(4).99.
       01  E-S9                   PIC -9(9).
       01  E-U5                   PIC 9(5).
       01  E-U6                   PIC 9(6).
       01  E-S7V2                 PIC -9(7).99.
       01  E-S5V2                 PIC -9(5).99.
       01  E-S5                   PIC -9(5).
       PROCEDURE DIVISION.
           OPEN INPUT IN-FILE
           PERFORM UNTIL WS-EOF = "Y"
               READ IN-FILE
                   AT END MOVE "Y" TO WS-EOF
                   NOT AT END PERFORM SHOW-RECORD
               END-READ
           END-PERFORM
           CLOSE IN-FILE
           STOP RUN.
       SHOW-RECORD.
           DISPLAY "RECORD"
           MOVE C-S4-COMP TO E-S4
           DISPLAY "C-S4-COMP=" E-S4
           MOVE C-U2-COMP TO E-U2
           DISPLAY "C-U2-COMP=" E-U2
           MOVE C-S8-COMP TO E-S8
           DISPLAY "C-S8-COMP=" E-S8
           MOVE C-S18-COMP TO E-S18
           DISPLAY "C-S18-COMP=" E-S18
           MOVE C-S4V2-COMP TO E-S4V2
           DISPLAY "C-S4V2-COMP=" E-S4V2
           MOVE C-S9-BIN TO E-S9
           DISPLAY "C-S9-BIN=" E-S9
           MOVE C-S4-COMP5 TO E-S5
           DISPLAY "C-S4-COMP5=" E-S5
           MOVE C-U4-COMPX TO E-U5
           DISPLAY "C-U4-COMPX=" E-U5
           MOVE C-X2-COMPX TO E-U5
           DISPLAY "C-X2-COMPX=" E-U5
           MOVE C-S7V2-COMP3 TO E-S7V2
           DISPLAY "C-S7V2-COMP3=" E-S7V2
           MOVE C-U5-COMP3 TO E-U5
           DISPLAY "C-U5-COMP3=" E-U5
           MOVE C-S5V2-DISP TO E-S5V2
           DISPLAY "C-S5V2-DISP=" E-S5V2
           MOVE C-U6-DISP TO E-U6
           DISPLAY "C-U6-DISP=" E-U6
           MOVE C-SEP-DISP TO E-S5
           DISPLAY "C-SEP-DISP=" E-S5
           DISPLAY "C-TEXT=" C-TEXT.
