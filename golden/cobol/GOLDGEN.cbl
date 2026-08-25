      *================================================================*
      * Program Name: GOLDGEN
      * Description: Golden-dataset generator for the COBOL -> JS parity
      *              harness. Writes the seeded, FIXED input set used as
      *              both the COBOL "before" input and the JS "after"
      *              input, so both sides consume byte-identical data.
      *
      * This replaces TSTGEN00.cbl, which does not compile (COPY ...
      * REPLACING against a copybook with no replacement marker, plus
      * ~8 paragraphs PERFORMed but never defined) and which draws from
      * a RANDOM-SEED file. Everything here is a fixed literal: there is
      * no randomness to seed, which is a stronger reproducibility
      * guarantee than a fixed seed.
      *
      * Layouts come from the real copybooks (PORTFLIO, TRNREC), so the
      * COMP-3 and fixed-width bytes are genuine COBOL output.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. GOLDGEN.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT PORTFOLIO-FILE
               ASSIGN TO PORTFILE
               ORGANIZATION IS INDEXED
               ACCESS MODE IS SEQUENTIAL
               RECORD KEY IS PORT-KEY
               FILE STATUS IS WS-PORT-ST.

           SELECT SEED-FLAT-FILE
               ASSIGN TO SEEDFLAT
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-SEED-ST.

           SELECT ADD-FILE
               ASSIGN TO ADDINPT
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-ADD-ST.

           SELECT UPDT-FILE
               ASSIGN TO UPDTINP
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-UPDT-ST.

           SELECT DELE-FILE
               ASSIGN TO DELEINP
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-DELE-ST.

           SELECT TRAN-FILE
               ASSIGN TO TRANINP
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-TRAN-ST.

       DATA DIVISION.
       FILE SECTION.
       FD  PORTFOLIO-FILE.
           COPY PORTFLIO.

       FD  SEED-FLAT-FILE.
       01  SEED-FLAT-RECORD        PIC X(148).

       FD  ADD-FILE.
       01  ADD-RECORD              PIC X(148).

       FD  UPDT-FILE.
       01  UPDT-RECORD.
           05  UPDT-ID             PIC X(8).
           05  UPDT-ACCT-NO        PIC X(10).
           05  UPDT-ACTION         PIC X(1).
           05  UPDT-NEW-VALUE      PIC X(50).

       FD  DELE-FILE.
       01  DELE-RECORD.
           05  DEL-ID              PIC X(8).
           05  DEL-ACCT-NO         PIC X(10).
           05  DEL-REASON-CODE     PIC X(2).
           05  DEL-FILLER          PIC X(60).

       FD  TRAN-FILE.
           COPY TRNREC.

       WORKING-STORAGE SECTION.
       01  WS-STATUS.
           05  WS-PORT-ST          PIC X(2).
           05  WS-SEED-ST          PIC X(2).
           05  WS-ADD-ST           PIC X(2).
           05  WS-UPDT-ST          PIC X(2).
           05  WS-DELE-ST          PIC X(2).
           05  WS-TRAN-ST          PIC X(2).

       01  WS-WORK.
           05  WS-VALUE            PIC S9(13)V99 VALUE ZERO.
           05  WS-CASH             PIC S9(13)V99 VALUE ZERO.
           05  WS-QTY              PIC S9(11)V9(4) VALUE ZERO.
           05  WS-PRICE            PIC S9(11)V9(4) VALUE ZERO.
           05  WS-AMOUNT           PIC S9(13)V9(2) VALUE ZERO.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-WRITE-SEED-PORTFOLIOS
           PERFORM 2000-WRITE-ADD-DECK
           PERFORM 3000-WRITE-UPDATE-DECK
           PERFORM 4000-WRITE-DELETE-DECK
           PERFORM 5000-WRITE-TRANSACTION-DECK
           DISPLAY 'GOLDGEN: golden input set written'
           GOBACK.

      *----------------------------------------------------------------*
      * Seed KSDS + a flat mirror of the same records. Three portfolios,
      * one per PORT-CLIENT-TYPE, covering active and suspended status,
      * a whole-dollar value, a fractional value, and a zero balance.
      * Written in PORT-KEY order (KSDS requires ascending on OPEN
      * OUTPUT sequential write).
      *----------------------------------------------------------------*
       1000-WRITE-SEED-PORTFOLIOS.
           OPEN OUTPUT PORTFOLIO-FILE
           OPEN OUTPUT SEED-FLAT-FILE

           PERFORM 1100-SEED-ONE
           PERFORM 1200-SEED-TWO
           PERFORM 1300-SEED-THREE

           CLOSE PORTFOLIO-FILE
                 SEED-FLAT-FILE
           .

       1100-SEED-ONE.
           INITIALIZE PORT-RECORD
           MOVE 'PORT0001'   TO PORT-ID
           MOVE '0000000001' TO PORT-ACCOUNT-NO
           MOVE 'ACME CAPITAL PARTNERS'  TO PORT-CLIENT-NAME
           MOVE 'C'          TO PORT-CLIENT-TYPE
           MOVE 20240301     TO PORT-CREATE-DATE
           MOVE 20240301     TO PORT-LAST-MAINT
           MOVE 'A'          TO PORT-STATUS
           MOVE 1250000.00   TO WS-VALUE
           MOVE WS-VALUE     TO PORT-TOTAL-VALUE
           MOVE 25000.00     TO WS-CASH
           MOVE WS-CASH      TO PORT-CASH-BALANCE
           MOVE 'SEEDUSER'   TO PORT-LAST-USER
           MOVE 20240301     TO PORT-LAST-TRANS
           MOVE SPACES       TO PORT-FILLER
           PERFORM 1900-EMIT-SEED
           .

       1200-SEED-TWO.
           INITIALIZE PORT-RECORD
           MOVE 'PORT0002'   TO PORT-ID
           MOVE '0000000002' TO PORT-ACCOUNT-NO
           MOVE 'JANE Q PUBLIC' TO PORT-CLIENT-NAME
           MOVE 'I'          TO PORT-CLIENT-TYPE
           MOVE 20240302     TO PORT-CREATE-DATE
           MOVE 20240302     TO PORT-LAST-MAINT
           MOVE 'A'          TO PORT-STATUS
           MOVE 45000.50     TO WS-VALUE
           MOVE WS-VALUE     TO PORT-TOTAL-VALUE
           MOVE 1500.25      TO WS-CASH
           MOVE WS-CASH      TO PORT-CASH-BALANCE
           MOVE 'SEEDUSER'   TO PORT-LAST-USER
           MOVE 20240302     TO PORT-LAST-TRANS
           MOVE SPACES       TO PORT-FILLER
           PERFORM 1900-EMIT-SEED
           .

       1300-SEED-THREE.
           INITIALIZE PORT-RECORD
           MOVE 'PORT0003'   TO PORT-ID
           MOVE '0000000003' TO PORT-ACCOUNT-NO
           MOVE 'SMITH FAMILY TRUST' TO PORT-CLIENT-NAME
           MOVE 'T'          TO PORT-CLIENT-TYPE
           MOVE 20240303     TO PORT-CREATE-DATE
           MOVE 20240303     TO PORT-LAST-MAINT
           MOVE 'S'          TO PORT-STATUS
           MOVE -780000.00   TO WS-VALUE
           MOVE WS-VALUE     TO PORT-TOTAL-VALUE
           MOVE 0.00         TO WS-CASH
           MOVE WS-CASH      TO PORT-CASH-BALANCE
           MOVE 'SEEDUSER'   TO PORT-LAST-USER
           MOVE 20240303     TO PORT-LAST-TRANS
           MOVE SPACES       TO PORT-FILLER
           PERFORM 1900-EMIT-SEED
           .

       1900-EMIT-SEED.
           WRITE PORT-RECORD
           IF WS-PORT-ST NOT = '00'
               DISPLAY 'GOLDGEN seed write failed ' PORT-KEY
                       ' status=' WS-PORT-ST
           END-IF
           MOVE PORT-RECORD TO SEED-FLAT-RECORD
           WRITE SEED-FLAT-RECORD
           .

      *----------------------------------------------------------------*
      * PORTADD input deck. PORTADD reads PORTFLIO-shaped records and
      * checks blank ID / blank name / status not 'A' before writing.
      * Order matters: case 2 duplicates a seeded key, case 6 duplicates
      * case 1 within the same run.
      *   ADD-01 canonical create                    -> added
      *   ADD-02 duplicate of seeded PORT0001        -> FILE STATUS 22
      *   ADD-03 blank PORT-ID                       -> rejected
      *   ADD-04 blank PORT-CLIENT-NAME              -> rejected
      *   ADD-05 PORT-STATUS 'S' (not 'A')           -> rejected
      *   ADD-06 duplicate of ADD-01 within the run  -> FILE STATUS 22
      *----------------------------------------------------------------*
       2000-WRITE-ADD-DECK.
           OPEN OUTPUT ADD-FILE

           PERFORM 2100-ADD-CANONICAL
           PERFORM 2200-ADD-DUP-SEEDED
           PERFORM 2300-ADD-BLANK-ID
           PERFORM 2400-ADD-BLANK-NAME
           PERFORM 2500-ADD-BAD-STATUS
           PERFORM 2100-ADD-CANONICAL

           CLOSE ADD-FILE
           .

       2100-ADD-CANONICAL.
           PERFORM 2900-CLEAR-ADD
           MOVE 'PORT0010'   TO PORT-ID
           MOVE '0000000010' TO PORT-ACCOUNT-NO
           MOVE 'NEW CLIENT LTD' TO PORT-CLIENT-NAME
           MOVE 'C'          TO PORT-CLIENT-TYPE
           MOVE 'A'          TO PORT-STATUS
           MOVE 100000.00    TO WS-VALUE
           MOVE WS-VALUE     TO PORT-TOTAL-VALUE
           MOVE 5000.00      TO WS-CASH
           MOVE WS-CASH      TO PORT-CASH-BALANCE
           PERFORM 2950-EMIT-ADD
           .

       2200-ADD-DUP-SEEDED.
           PERFORM 2900-CLEAR-ADD
           MOVE 'PORT0001'   TO PORT-ID
           MOVE '0000000001' TO PORT-ACCOUNT-NO
           MOVE 'DUPLICATE ATTEMPT' TO PORT-CLIENT-NAME
           MOVE 'C'          TO PORT-CLIENT-TYPE
           MOVE 'A'          TO PORT-STATUS
           PERFORM 2950-EMIT-ADD
           .

       2300-ADD-BLANK-ID.
           PERFORM 2900-CLEAR-ADD
           MOVE SPACES       TO PORT-ID
           MOVE '0000000011' TO PORT-ACCOUNT-NO
           MOVE 'NO ID CLIENT' TO PORT-CLIENT-NAME
           MOVE 'I'          TO PORT-CLIENT-TYPE
           MOVE 'A'          TO PORT-STATUS
           PERFORM 2950-EMIT-ADD
           .

       2400-ADD-BLANK-NAME.
           PERFORM 2900-CLEAR-ADD
           MOVE 'PORT0012'   TO PORT-ID
           MOVE '0000000012' TO PORT-ACCOUNT-NO
           MOVE SPACES       TO PORT-CLIENT-NAME
           MOVE 'I'          TO PORT-CLIENT-TYPE
           MOVE 'A'          TO PORT-STATUS
           PERFORM 2950-EMIT-ADD
           .

       2500-ADD-BAD-STATUS.
           PERFORM 2900-CLEAR-ADD
           MOVE 'PORT0013'   TO PORT-ID
           MOVE '0000000013' TO PORT-ACCOUNT-NO
           MOVE 'SUSPENDED ON CREATE' TO PORT-CLIENT-NAME
           MOVE 'I'          TO PORT-CLIENT-TYPE
           MOVE 'S'          TO PORT-STATUS
           PERFORM 2950-EMIT-ADD
           .

       2900-CLEAR-ADD.
           INITIALIZE PORT-RECORD
           MOVE ZERO         TO PORT-CREATE-DATE
           MOVE ZERO         TO PORT-LAST-MAINT
           MOVE ZERO         TO PORT-LAST-TRANS
           MOVE 'GOLDUSER'   TO PORT-LAST-USER
           MOVE SPACES       TO PORT-FILLER
           .

       2950-EMIT-ADD.
           MOVE PORT-RECORD TO ADD-RECORD
           WRITE ADD-RECORD
           .

      *----------------------------------------------------------------*
      * PORTUPDT input deck. UPDT-ACTION: S=status, V=total value,
      * N=client name. Anything else falls through PORTUPDT's EVALUATE
      * with no WHEN OTHER and is still counted as a successful update
      * (CONTRACTS section 8 defect 3) - UPD-05 pins that.
      *   UPD-01 status  PORT0002 -> 'C'
      *   UPD-02 name    PORT0002 -> 'JANE Q PUBLIC-DOE'
      *   UPD-03 value   PORT0002 -> 99999.99
      *   UPD-04 not found PORT9998
      *   UPD-05 unknown action 'X' on PORT0001
      *----------------------------------------------------------------*
       3000-WRITE-UPDATE-DECK.
           OPEN OUTPUT UPDT-FILE

           MOVE 'PORT0002'   TO UPDT-ID
           MOVE '0000000002' TO UPDT-ACCT-NO
           MOVE 'S'          TO UPDT-ACTION
           MOVE 'C'          TO UPDT-NEW-VALUE
           WRITE UPDT-RECORD

           MOVE 'PORT0002'   TO UPDT-ID
           MOVE '0000000002' TO UPDT-ACCT-NO
           MOVE 'N'          TO UPDT-ACTION
           MOVE 'JANE Q PUBLIC-DOE' TO UPDT-NEW-VALUE
           WRITE UPDT-RECORD

           MOVE 'PORT0002'   TO UPDT-ID
           MOVE '0000000002' TO UPDT-ACCT-NO
           MOVE 'V'          TO UPDT-ACTION
           MOVE '99999.99'   TO UPDT-NEW-VALUE
           WRITE UPDT-RECORD

           MOVE 'PORT9998'   TO UPDT-ID
           MOVE '0000009998' TO UPDT-ACCT-NO
           MOVE 'S'          TO UPDT-ACTION
           MOVE 'C'          TO UPDT-NEW-VALUE
           WRITE UPDT-RECORD

           MOVE 'PORT0001'   TO UPDT-ID
           MOVE '0000000001' TO UPDT-ACCT-NO
           MOVE 'X'          TO UPDT-ACTION
           MOVE 'IGNORED'    TO UPDT-NEW-VALUE
           WRITE UPDT-RECORD

           CLOSE UPDT-FILE
           .

      *----------------------------------------------------------------*
      * PORTDEL input deck. DEL-REASON-CODE 01/02/03.
      *   DEL-01 delete seeded PORT0003, reason 01 -> deleted + audit
      *   DEL-02 delete PORT9999 (absent)          -> FILE STATUS 23
      *   DEL-03 delete PORT0003 again             -> FILE STATUS 23
      *----------------------------------------------------------------*
       4000-WRITE-DELETE-DECK.
           OPEN OUTPUT DELE-FILE

           MOVE SPACES       TO DELE-RECORD
           MOVE 'PORT0003'   TO DEL-ID
           MOVE '0000000003' TO DEL-ACCT-NO
           MOVE '01'         TO DEL-REASON-CODE
           WRITE DELE-RECORD

           MOVE SPACES       TO DELE-RECORD
           MOVE 'PORT9999'   TO DEL-ID
           MOVE '0000009999' TO DEL-ACCT-NO
           MOVE '02'         TO DEL-REASON-CODE
           WRITE DELE-RECORD

           MOVE SPACES       TO DELE-RECORD
           MOVE 'PORT0003'   TO DEL-ID
           MOVE '0000000003' TO DEL-ACCT-NO
           MOVE '03'         TO DEL-REASON-CODE
           WRITE DELE-RECORD

           CLOSE DELE-FILE
           .

      *----------------------------------------------------------------*
      * PORTTRAN input deck (TRNREC layout). PORTTRAN cannot execute
      * (missing PORTREC copybook), so these cases drive the JS side and
      * their expectations are DERIVED. The deck still covers every
      * branch PORTTRAN's source contains.
      *   TRN-01 BU valid                     -> buy, units/cost up
      *   TRN-02 SL valid within holdings     -> sell, units/cost down
      *   TRN-03 SL exceeding holdings        -> Insufficient units for sale
      *   TRN-04 TR                           -> Transfer processing not implemented
      *   TRN-05 FE valid                     -> fee, cost down
      *   TRN-06 unknown type 'ZZ'            -> Invalid Transaction Type
      *   TRN-07 unknown portfolio PORT9997   -> Invalid Portfolio ID
      *   TRN-08 zero quantity                -> Quantity must be greater than zero
      *   TRN-09 blank portfolio id           -> Portfolio ID is required
      *----------------------------------------------------------------*
       5000-WRITE-TRANSACTION-DECK.
           OPEN OUTPUT TRAN-FILE

           PERFORM 5900-CLEAR-TRAN
           MOVE '000001'     TO TRN-SEQUENCE-NO
           MOVE 'PORT0001'   TO TRN-PORTFOLIO-ID
           MOVE 'AAPL      ' TO TRN-INVESTMENT-ID
           MOVE 'BU'         TO TRN-TYPE
           MOVE 100.0000     TO WS-QTY
           MOVE WS-QTY       TO TRN-QUANTITY
           MOVE 150.2500     TO WS-PRICE
           MOVE WS-PRICE     TO TRN-PRICE
           MOVE 15025.00     TO WS-AMOUNT
           MOVE WS-AMOUNT    TO TRN-AMOUNT
           WRITE TRANSACTION-RECORD

           PERFORM 5900-CLEAR-TRAN
           MOVE '000002'     TO TRN-SEQUENCE-NO
           MOVE 'PORT0001'   TO TRN-PORTFOLIO-ID
           MOVE 'AAPL      ' TO TRN-INVESTMENT-ID
           MOVE 'SL'         TO TRN-TYPE
           MOVE 40.0000      TO WS-QTY
           MOVE WS-QTY       TO TRN-QUANTITY
           MOVE 155.0000     TO WS-PRICE
           MOVE WS-PRICE     TO TRN-PRICE
           MOVE 6200.00      TO WS-AMOUNT
           MOVE WS-AMOUNT    TO TRN-AMOUNT
           WRITE TRANSACTION-RECORD

           PERFORM 5900-CLEAR-TRAN
           MOVE '000003'     TO TRN-SEQUENCE-NO
           MOVE 'PORT0002'   TO TRN-PORTFOLIO-ID
           MOVE 'MSFT      ' TO TRN-INVESTMENT-ID
           MOVE 'SL'         TO TRN-TYPE
           MOVE 999999.0000  TO WS-QTY
           MOVE WS-QTY       TO TRN-QUANTITY
           MOVE 400.0000     TO WS-PRICE
           MOVE WS-PRICE     TO TRN-PRICE
           MOVE 399999600.00 TO WS-AMOUNT
           MOVE WS-AMOUNT    TO TRN-AMOUNT
           WRITE TRANSACTION-RECORD

           PERFORM 5900-CLEAR-TRAN
           MOVE '000004'     TO TRN-SEQUENCE-NO
           MOVE 'PORT0001'   TO TRN-PORTFOLIO-ID
           MOVE 'CASHXFER  ' TO TRN-INVESTMENT-ID
           MOVE 'TR'         TO TRN-TYPE
           MOVE 10.0000      TO WS-QTY
           MOVE WS-QTY       TO TRN-QUANTITY
           MOVE 0.0000       TO WS-PRICE
           MOVE WS-PRICE     TO TRN-PRICE
           MOVE 0.00         TO WS-AMOUNT
           MOVE WS-AMOUNT    TO TRN-AMOUNT
           WRITE TRANSACTION-RECORD

           PERFORM 5900-CLEAR-TRAN
           MOVE '000005'     TO TRN-SEQUENCE-NO
           MOVE 'PORT0002'   TO TRN-PORTFOLIO-ID
           MOVE 'MGMTFEE   ' TO TRN-INVESTMENT-ID
           MOVE 'FE'         TO TRN-TYPE
           MOVE 1.0000       TO WS-QTY
           MOVE WS-QTY       TO TRN-QUANTITY
           MOVE 125.5000     TO WS-PRICE
           MOVE WS-PRICE     TO TRN-PRICE
           MOVE 125.50       TO WS-AMOUNT
           MOVE WS-AMOUNT    TO TRN-AMOUNT
           WRITE TRANSACTION-RECORD

           PERFORM 5900-CLEAR-TRAN
           MOVE '000006'     TO TRN-SEQUENCE-NO
           MOVE 'PORT0001'   TO TRN-PORTFOLIO-ID
           MOVE 'BADTYPE   ' TO TRN-INVESTMENT-ID
           MOVE 'ZZ'         TO TRN-TYPE
           MOVE 5.0000       TO WS-QTY
           MOVE WS-QTY       TO TRN-QUANTITY
           MOVE 10.0000      TO WS-PRICE
           MOVE WS-PRICE     TO TRN-PRICE
           MOVE 50.00        TO WS-AMOUNT
           MOVE WS-AMOUNT    TO TRN-AMOUNT
           WRITE TRANSACTION-RECORD

           PERFORM 5900-CLEAR-TRAN
           MOVE '000007'     TO TRN-SEQUENCE-NO
           MOVE 'PORT9997'   TO TRN-PORTFOLIO-ID
           MOVE 'AAPL      ' TO TRN-INVESTMENT-ID
           MOVE 'BU'         TO TRN-TYPE
           MOVE 5.0000       TO WS-QTY
           MOVE WS-QTY       TO TRN-QUANTITY
           MOVE 10.0000      TO WS-PRICE
           MOVE WS-PRICE     TO TRN-PRICE
           MOVE 50.00        TO WS-AMOUNT
           MOVE WS-AMOUNT    TO TRN-AMOUNT
           WRITE TRANSACTION-RECORD

           PERFORM 5900-CLEAR-TRAN
           MOVE '000008'     TO TRN-SEQUENCE-NO
           MOVE 'PORT0001'   TO TRN-PORTFOLIO-ID
           MOVE 'AAPL      ' TO TRN-INVESTMENT-ID
           MOVE 'BU'         TO TRN-TYPE
           MOVE 0.0000       TO WS-QTY
           MOVE WS-QTY       TO TRN-QUANTITY
           MOVE 10.0000      TO WS-PRICE
           MOVE WS-PRICE     TO TRN-PRICE
           MOVE 50.00        TO WS-AMOUNT
           MOVE WS-AMOUNT    TO TRN-AMOUNT
           WRITE TRANSACTION-RECORD

           PERFORM 5900-CLEAR-TRAN
           MOVE '000009'     TO TRN-SEQUENCE-NO
           MOVE SPACES       TO TRN-PORTFOLIO-ID
           MOVE 'AAPL      ' TO TRN-INVESTMENT-ID
           MOVE 'BU'         TO TRN-TYPE
           MOVE 5.0000       TO WS-QTY
           MOVE WS-QTY       TO TRN-QUANTITY
           MOVE 10.0000      TO WS-PRICE
           MOVE WS-PRICE     TO TRN-PRICE
           MOVE 50.00        TO WS-AMOUNT
           MOVE WS-AMOUNT    TO TRN-AMOUNT
           WRITE TRANSACTION-RECORD

           CLOSE TRAN-FILE
           .

       5900-CLEAR-TRAN.
           INITIALIZE TRANSACTION-RECORD
           MOVE '20240401'   TO TRN-DATE
           MOVE '120000'     TO TRN-TIME
           MOVE 'USD'        TO TRN-CURRENCY
           MOVE 'P'          TO TRN-STATUS
           MOVE '20240401T120000.000000000' TO TRN-PROCESS-DATE
           MOVE 'GOLDUSER'   TO TRN-PROCESS-USER
           MOVE SPACES       TO TRN-FILLER
           MOVE ZERO         TO WS-QTY
           MOVE ZERO         TO WS-PRICE
           MOVE ZERO         TO WS-AMOUNT
           .
