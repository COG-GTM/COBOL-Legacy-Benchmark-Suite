       IDENTIFICATION DIVISION.
       PROGRAM-ID. VALDRIVR.
      * Parity helper: feeds VALDREQ lines (type X(1) + value X(50))
      * to PORTVALD and displays the return code and message.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT REQ-FILE ASSIGN TO VALDREQ
               ORGANIZATION IS LINE SEQUENTIAL FILE STATUS IS WS-FS.
       DATA DIVISION.
       FILE SECTION.
       FD  REQ-FILE.
       01  REQ-LINE.
           05  REQ-TYPE   PIC X(1).
           05  REQ-VALUE  PIC X(50).
       WORKING-STORAGE SECTION.
       01  WS-FS PIC XX.
       01  WS-EOF PIC X VALUE 'N'.
       01  WS-RC-DISP PIC 9(4).
       01  WS-REQUEST.
           05  WS-TYPE    PIC X(1).
           05  WS-VALUE   PIC X(50).
           05  WS-RC      PIC S9(4) COMP.
           05  WS-MSG     PIC X(50).
       PROCEDURE DIVISION.
           OPEN INPUT REQ-FILE
           PERFORM UNTIL WS-EOF = 'Y'
               READ REQ-FILE
                   AT END MOVE 'Y' TO WS-EOF
                   NOT AT END
                       MOVE REQ-TYPE TO WS-TYPE
                       MOVE REQ-VALUE TO WS-VALUE
                       MOVE 0 TO WS-RC
                       MOVE SPACES TO WS-MSG
                       CALL 'PORTVALD' USING WS-REQUEST
                       MOVE WS-RC TO WS-RC-DISP
                       DISPLAY REQ-TYPE '|' FUNCTION TRIM(REQ-VALUE)
                               '|' WS-RC-DISP '|'
                               FUNCTION TRIM(WS-MSG)
               END-READ
           END-PERFORM
           CLOSE REQ-FILE
           GOBACK.
