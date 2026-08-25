      *================================================================*
      * Program Name: VALDRV
      * Description: Driver that CALLs the real, unmodified PORTVALD for
      *              every golden validation vector and emits the actual
      *              return code and error message as pipe-delimited
      *              text. This is the EXECUTED "before" baseline for
      *              PORTVALD - no expectation here is hand-derived.
      *
      * Output line: VAL|<case-id>|<type>|<input>|<rc>|<message>
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. VALDRV.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  LS-VALIDATION-REQUEST.
           05  LS-VALIDATE-TYPE    PIC X(1).
           05  LS-INPUT-VALUE      PIC X(50).
           05  LS-RETURN-CODE      PIC S9(4) COMP.
           05  LS-ERROR-MSG        PIC X(50).

       01  WS-CASES.
           05  WS-CASE OCCURS 20 TIMES.
               10  WS-CASE-ID      PIC X(10).
               10  WS-CASE-TYPE    PIC X(1).
               10  WS-CASE-INPUT   PIC X(50).

       01  WS-I                    PIC 9(2) VALUE ZERO.
       01  WS-RC-ED                PIC -9.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-LOAD-CASES
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 20
               IF WS-CASE-ID(WS-I) NOT = SPACES
                   PERFORM 2000-RUN-CASE
               END-IF
           END-PERFORM
           GOBACK.

      *----------------------------------------------------------------*
      * Golden validation vectors. Types: I id, A account, T investment
      * type, M amount, plus one unknown type to pin the WHEN OTHER arm.
      * The ID and account vectors deliberately include values that are
      * well-formed by the documented rules, because the legacy code
      * rejects those too (CONTRACTS section 4.2) and the harness must
      * record that.
      *----------------------------------------------------------------*
       1000-LOAD-CASES.
           INITIALIZE WS-CASES

           MOVE 'VAL-01'   TO WS-CASE-ID(1)
           MOVE 'I'        TO WS-CASE-TYPE(1)
           MOVE 'PORT0001' TO WS-CASE-INPUT(1)

           MOVE 'VAL-02'   TO WS-CASE-ID(2)
           MOVE 'I'        TO WS-CASE-TYPE(2)
           MOVE 'PORT9999' TO WS-CASE-INPUT(2)

           MOVE 'VAL-03'   TO WS-CASE-ID(3)
           MOVE 'I'        TO WS-CASE-TYPE(3)
           MOVE 'XXXX0001' TO WS-CASE-INPUT(3)

           MOVE 'VAL-04'   TO WS-CASE-ID(4)
           MOVE 'I'        TO WS-CASE-TYPE(4)
           MOVE 'PORTABCD' TO WS-CASE-INPUT(4)

           MOVE 'VAL-05'   TO WS-CASE-ID(5)
           MOVE 'I'        TO WS-CASE-TYPE(5)
           MOVE SPACES     TO WS-CASE-INPUT(5)

           MOVE 'VAL-06'     TO WS-CASE-ID(6)
           MOVE 'A'          TO WS-CASE-TYPE(6)
           MOVE '0000000001' TO WS-CASE-INPUT(6)

           MOVE 'VAL-07'     TO WS-CASE-ID(7)
           MOVE 'A'          TO WS-CASE-TYPE(7)
           MOVE '1234567890' TO WS-CASE-INPUT(7)

           MOVE 'VAL-08'     TO WS-CASE-ID(8)
           MOVE 'A'          TO WS-CASE-TYPE(8)
           MOVE '0000000000' TO WS-CASE-INPUT(8)

           MOVE 'VAL-09'     TO WS-CASE-ID(9)
           MOVE 'A'          TO WS-CASE-TYPE(9)
           MOVE '12345ABCDE' TO WS-CASE-INPUT(9)

           MOVE 'VAL-10'   TO WS-CASE-ID(10)
           MOVE 'T'        TO WS-CASE-TYPE(10)
           MOVE 'STK'      TO WS-CASE-INPUT(10)

           MOVE 'VAL-11'   TO WS-CASE-ID(11)
           MOVE 'T'        TO WS-CASE-TYPE(11)
           MOVE 'BND'      TO WS-CASE-INPUT(11)

           MOVE 'VAL-12'   TO WS-CASE-ID(12)
           MOVE 'T'        TO WS-CASE-TYPE(12)
           MOVE 'MMF'      TO WS-CASE-INPUT(12)

           MOVE 'VAL-13'   TO WS-CASE-ID(13)
           MOVE 'T'        TO WS-CASE-TYPE(13)
           MOVE 'ETF'      TO WS-CASE-INPUT(13)

           MOVE 'VAL-14'   TO WS-CASE-ID(14)
           MOVE 'T'        TO WS-CASE-TYPE(14)
           MOVE 'XYZ'      TO WS-CASE-INPUT(14)

           MOVE 'VAL-15'   TO WS-CASE-ID(15)
           MOVE 'T'        TO WS-CASE-TYPE(15)
           MOVE 'stk'      TO WS-CASE-INPUT(15)

           MOVE 'VAL-16'          TO WS-CASE-ID(16)
           MOVE 'M'               TO WS-CASE-TYPE(16)
           MOVE '000000000100000' TO WS-CASE-INPUT(16)

           MOVE 'VAL-17'          TO WS-CASE-ID(17)
           MOVE 'M'               TO WS-CASE-TYPE(17)
           MOVE '999999999999999' TO WS-CASE-INPUT(17)

           MOVE 'VAL-18'   TO WS-CASE-ID(18)
           MOVE 'M'        TO WS-CASE-TYPE(18)
           MOVE '1000.00'  TO WS-CASE-INPUT(18)

           MOVE 'VAL-19'   TO WS-CASE-ID(19)
           MOVE 'M'        TO WS-CASE-TYPE(19)
           MOVE 'NOTANUM'  TO WS-CASE-INPUT(19)

           MOVE 'VAL-20'   TO WS-CASE-ID(20)
           MOVE 'Z'        TO WS-CASE-TYPE(20)
           MOVE 'PORT0001' TO WS-CASE-INPUT(20)
           .

       2000-RUN-CASE.
           MOVE WS-CASE-TYPE(WS-I)  TO LS-VALIDATE-TYPE
           MOVE WS-CASE-INPUT(WS-I) TO LS-INPUT-VALUE
           MOVE ZERO                TO LS-RETURN-CODE
           MOVE SPACES              TO LS-ERROR-MSG

           CALL 'PORTVALD' USING LS-VALIDATION-REQUEST

           MOVE LS-RETURN-CODE TO WS-RC-ED
           DISPLAY 'VAL|'
                   WS-CASE-ID(WS-I)    '|'
                   WS-CASE-TYPE(WS-I)  '|'
                   WS-CASE-INPUT(WS-I) '|'
                   WS-RC-ED            '|'
                   LS-ERROR-MSG
           .
