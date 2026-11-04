       IDENTIFICATION DIVISION.
       PROGRAM-ID. CODECWR.
      * Writes three CODEC-RECORDs (positive, negative, extremes) so the
      * Python decoder can be checked against bytes a COBOL compiler made.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT OUT-FILE ASSIGN TO "codec.dat"
               ORGANIZATION IS SEQUENTIAL.
       DATA DIVISION.
       FILE SECTION.
       FD  OUT-FILE.
       COPY CODECREC.
       PROCEDURE DIVISION.
           OPEN OUTPUT OUT-FILE
           INITIALIZE CODEC-RECORD
           MOVE 1234          TO C-S4-COMP
           MOVE 42            TO C-U2-COMP
           MOVE 12345678      TO C-S8-COMP
           MOVE 123456789012345678 TO C-S18-COMP
           MOVE 1234.56       TO C-S4V2-COMP
           MOVE 123456789     TO C-S9-BIN
           MOVE 1234          TO C-S4-COMP5
           MOVE 9999          TO C-U4-COMPX
           MOVE 65535         TO C-X2-COMPX
           MOVE 1234567.89    TO C-S7V2-COMP3
           MOVE 12345         TO C-U5-COMP3
           MOVE 12345.67      TO C-S5V2-DISP
           MOVE 123456        TO C-U6-DISP
           MOVE 12345         TO C-SEP-DISP
           MOVE "POSITIVE"    TO C-TEXT
           WRITE CODEC-RECORD
           INITIALIZE CODEC-RECORD
           MOVE -1234         TO C-S4-COMP
           MOVE 7             TO C-U2-COMP
           MOVE -12345678     TO C-S8-COMP
           MOVE -123456789012345678 TO C-S18-COMP
           MOVE -0.01         TO C-S4V2-COMP
           MOVE -123456789    TO C-S9-BIN
           MOVE -32768        TO C-S4-COMP5
           MOVE 0             TO C-U4-COMPX
           MOVE 256           TO C-X2-COMPX
           MOVE -1234567.89   TO C-S7V2-COMP3
           MOVE 0             TO C-U5-COMP3
           MOVE -12345.67     TO C-S5V2-DISP
           MOVE 0             TO C-U6-DISP
           MOVE -12345        TO C-SEP-DISP
           MOVE "NEGATIVE"    TO C-TEXT
           WRITE CODEC-RECORD
           INITIALIZE CODEC-RECORD
           MOVE 9999          TO C-S4-COMP
           MOVE 99            TO C-U2-COMP
           MOVE 99999999      TO C-S8-COMP
           MOVE 999999999999999999 TO C-S18-COMP
           MOVE -9999.99      TO C-S4V2-COMP
           MOVE 999999999     TO C-S9-BIN
           MOVE 32767         TO C-S4-COMP5
           MOVE 65535         TO C-U4-COMPX
           MOVE 1             TO C-X2-COMPX
           MOVE 9999999.99    TO C-S7V2-COMP3
           MOVE 99999         TO C-U5-COMP3
           MOVE -0.01         TO C-S5V2-DISP
           MOVE 999999        TO C-U6-DISP
           MOVE 0             TO C-SEP-DISP
           MOVE "EXTREMES"    TO C-TEXT
           WRITE CODEC-RECORD
           CLOSE OUT-FILE
           STOP RUN.
