      *================================================================*
      * Program Name: PVDRIVER
      * Description: Parity harness driver for PORTVALD.
      *              Reads a pipe-delimited case file, calls PORTVALD
      *              once per case, and writes the observable outputs
      *              (return code, 50-byte error message) plus the
      *              value produced by the alphanumeric-to-packed
      *              MOVE that PORTVALD performs in 4000-VALIDATE-AMOUNT.
      *
      *              This program is NOT part of the legacy estate.
      *              It is test scaffolding added by the modernization
      *              work; PORTVALD and its copybooks are unchanged.
      *
      * Input  file: CASEFILE  case_id|validate_type|input_value|desc
      * Output file: OUTFILE   case_id|rc|amount_move|[50-byte message]
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PVDRIVER.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT CASE-FILE ASSIGN TO CASEFILE
               ORGANIZATION IS LINE SEQUENTIAL
               FILE STATUS IS WS-CASE-STATUS.
           SELECT OUT-FILE ASSIGN TO OUTFILE
               ORGANIZATION IS LINE SEQUENTIAL
               FILE STATUS IS WS-OUT-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  CASE-FILE.
       01  CASE-RECORD             PIC X(250).
       FD  OUT-FILE.
       01  OUT-RECORD              PIC X(160).

       WORKING-STORAGE SECTION.
       01  WS-CASE-STATUS          PIC X(2)  VALUE SPACES.
       01  WS-OUT-STATUS           PIC X(2)  VALUE SPACES.
       01  WS-EOF-FLAG             PIC X(1)  VALUE 'N'.
           88  WS-EOF                        VALUE 'Y'.

       01  WS-CASE-ID              PIC X(30).
       01  WS-VAL-TYPE-FLD         PIC X(10).
       01  WS-INPUT-FLD            PIC X(60).
       01  WS-DESC-FLD             PIC X(120).

      *----------------------------------------------------------------*
      * Replica of PORTVALD 4000-VALIDATE-AMOUNT working storage, used
      * to expose the otherwise invisible result of
      *     MOVE LS-INPUT-VALUE TO VAL-TEMP-NUM
      * The PICTURE clause is identical to VAL-TEMP-NUM in PORTVAL.cpy.
      *----------------------------------------------------------------*
       01  WS-TEMP-NUM             PIC S9(13)V99.
       01  WS-TEMP-EDIT            PIC -9(13).99.

       01  WS-RC-EDIT              PIC -9(4).
       01  WS-CASE-COUNT           PIC 9(5)  VALUE ZERO.

      *----------------------------------------------------------------*
      * PORTVALD parameter list (LS-VALIDATION-REQUEST)
      *----------------------------------------------------------------*
       01  WS-VALIDATION-REQUEST.
           05  WS-VALIDATE-TYPE    PIC X(1).
           05  WS-INPUT-VALUE      PIC X(50).
           05  WS-RETURN-CODE      PIC S9(4) COMP.
           05  WS-ERROR-MSG        PIC X(50).

       PROCEDURE DIVISION.
       0000-MAIN.
           OPEN INPUT CASE-FILE
           IF WS-CASE-STATUS NOT = '00'
               DISPLAY 'PVDRIVER: cannot open CASEFILE, status '
                       WS-CASE-STATUS
               MOVE 12 TO RETURN-CODE
               GOBACK
           END-IF

           OPEN OUTPUT OUT-FILE
           IF WS-OUT-STATUS NOT = '00'
               DISPLAY 'PVDRIVER: cannot open OUTFILE, status '
                       WS-OUT-STATUS
               MOVE 12 TO RETURN-CODE
               GOBACK
           END-IF

           MOVE 'case_id|rc|amount_move|[error_message]'
               TO OUT-RECORD
           WRITE OUT-RECORD

           PERFORM UNTIL WS-EOF
               READ CASE-FILE
                   AT END
                       SET WS-EOF TO TRUE
                   NOT AT END
                       PERFORM 1000-PROCESS-CASE
               END-READ
           END-PERFORM

           CLOSE CASE-FILE
           CLOSE OUT-FILE
           DISPLAY 'PVDRIVER: cases executed = ' WS-CASE-COUNT
           GOBACK
           .

       1000-PROCESS-CASE.
           IF CASE-RECORD(1:1) = '#' OR CASE-RECORD = SPACES
               EXIT PARAGRAPH
           END-IF

           MOVE SPACES TO WS-CASE-ID
           MOVE SPACES TO WS-VAL-TYPE-FLD
           MOVE SPACES TO WS-INPUT-FLD
           MOVE SPACES TO WS-DESC-FLD

           UNSTRING CASE-RECORD DELIMITED BY '|'
               INTO WS-CASE-ID
                    WS-VAL-TYPE-FLD
                    WS-INPUT-FLD
                    WS-DESC-FLD
           END-UNSTRING

      *    Build the 50-byte LS-INPUT-VALUE exactly as a caller would:
      *    left justified, space filled, truncated on the right.
           MOVE SPACES              TO WS-INPUT-VALUE
           MOVE WS-INPUT-FLD        TO WS-INPUT-VALUE
           MOVE WS-VAL-TYPE-FLD(1:1) TO WS-VALIDATE-TYPE

      *    Poison the outputs so that a path which fails to set them
      *    is visible in the evidence file rather than silently
      *    inheriting the previous case's values.
           MOVE -9999               TO WS-RETURN-CODE
           MOVE ALL '?'             TO WS-ERROR-MSG

           CALL 'PORTVALD' USING WS-VALIDATION-REQUEST

           MOVE ZERO                TO WS-TEMP-NUM
           MOVE WS-INPUT-VALUE      TO WS-TEMP-NUM
           MOVE WS-TEMP-NUM         TO WS-TEMP-EDIT

           MOVE WS-RETURN-CODE      TO WS-RC-EDIT

           MOVE SPACES TO OUT-RECORD
           STRING WS-CASE-ID DELIMITED BY SPACE
                  '|'        DELIMITED BY SIZE
                  WS-RC-EDIT DELIMITED BY SIZE
                  '|'        DELIMITED BY SIZE
                  WS-TEMP-EDIT DELIMITED BY SIZE
                  '|['       DELIMITED BY SIZE
                  WS-ERROR-MSG DELIMITED BY SIZE
                  ']'        DELIMITED BY SIZE
               INTO OUT-RECORD
           END-STRING
           WRITE OUT-RECORD

           ADD 1 TO WS-CASE-COUNT
           .
