      *================================================================*
      * Program Name: PVDRIVR
      * Description: Oracle driver for the PORTVALD translation pair.
      *             Reads a case file, calls PORTVALD once per case and
      *             writes the linkage results as the golden file used
      *             by the Python parity tests.
      * Case record: cols 1-4 case id, col 6 validation type,
      *             cols 8-57 input value.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PVDRIVR.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT CASE-FILE ASSIGN TO 'CASES'
               ORGANIZATION IS LINE SEQUENTIAL.
           SELECT GOLDEN-FILE ASSIGN TO 'GOLDEN'
               ORGANIZATION IS LINE SEQUENTIAL.

       DATA DIVISION.
       FILE SECTION.
       FD  CASE-FILE.
       01  CASE-REC.
           05  CR-ID               PIC X(4).
           05  FILLER              PIC X(1).
           05  CR-TYPE             PIC X(1).
           05  FILLER              PIC X(1).
           05  CR-VALUE            PIC X(50).

       FD  GOLDEN-FILE.
       01  GOLDEN-REC              PIC X(80).

       WORKING-STORAGE SECTION.
       01  WS-EOF                  PIC X(1) VALUE 'N'.
       01  WS-RETURN-DISPLAY       PIC -(4)9.
       01  WS-VALIDATION-REQUEST.
           05  WS-VALIDATE-TYPE    PIC X(1).
           05  WS-INPUT-VALUE      PIC X(50).
           05  WS-RETURN-CODE      PIC S9(4) COMP.
           05  WS-ERROR-MSG        PIC X(50).

       PROCEDURE DIVISION.
       0000-MAIN.
           OPEN INPUT CASE-FILE
           OPEN OUTPUT GOLDEN-FILE

           PERFORM UNTIL WS-EOF = 'Y'
               READ CASE-FILE
                   AT END
                       MOVE 'Y' TO WS-EOF
                   NOT AT END
                       PERFORM 1000-RUN-CASE
               END-READ
           END-PERFORM

           CLOSE CASE-FILE
           CLOSE GOLDEN-FILE
           GOBACK
           .

       1000-RUN-CASE.
           IF CR-ID = SPACES OR CR-ID(1:1) = '*'
               EXIT PARAGRAPH
           END-IF

           MOVE CR-TYPE TO WS-VALIDATE-TYPE
           MOVE CR-VALUE TO WS-INPUT-VALUE
           MOVE ZERO TO WS-RETURN-CODE
           MOVE SPACES TO WS-ERROR-MSG

           CALL 'PORTVALD' USING WS-VALIDATION-REQUEST

           MOVE WS-RETURN-CODE TO WS-RETURN-DISPLAY
           MOVE SPACES TO GOLDEN-REC
           STRING CR-ID              DELIMITED BY SIZE
                  '|'                DELIMITED BY SIZE
                  WS-RETURN-DISPLAY  DELIMITED BY SIZE
                  '|'                DELIMITED BY SIZE
                  WS-ERROR-MSG       DELIMITED BY SIZE
               INTO GOLDEN-REC
           END-STRING

           WRITE GOLDEN-REC
           .
