       IDENTIFICATION DIVISION.
       PROGRAM-ID. TSTSECMG.
      *****************************************************************
      * SECMGR Regression Test                                        *
      *                                                               *
      * Calls the stub-translated SECMGR with each request type and   *
      * checks the response code and which user ID was used as the    *
      * AUTHFILE key. AUTHFILE stub holds one grant:                  *
      *   ADMIN / INQONLN / READ                                      *
      * Exits with RETURN-CODE 1 if any case fails.                   *
      *****************************************************************
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  STUB-CICS-USER      PIC X(8) EXTERNAL.
       01  STUB-CICS-RESP      PIC S9(8) COMP EXTERNAL.
       01  STUB-SQL-QUERIED    PIC X(8) EXTERNAL.
       01  REQ.
           05 SEC-REQUEST-TYPE     PIC X.
           05 SEC-USER-ID          PIC X(8).
           05 SEC-RESOURCE-NAME    PIC X(8).
           05 SEC-ACCESS-TYPE      PIC X(8).
           05 SEC-RESPONSE-CODE    PIC S9(8) COMP.
           05 SEC-ERROR-INFO       PIC X(80).
       01  W-LABEL             PIC X(36).
       01  W-SIGNON            PIC X(8).
       01  W-RESP              PIC S9(8) COMP.
       01  W-TYPE              PIC X.
       01  W-CLAIM             PIC X(8).
       01  W-EXP-RC            PIC S9(8) COMP.
       01  W-EXP-KEY           PIC X(8).
       01  W-RC                PIC -9(3).
       01  W-RESULT            PIC X(4).
       01  W-FAILURES          PIC 9(3) VALUE 0.
       PROCEDURE DIVISION.
           MOVE 'A' TO W-TYPE
           MOVE 0 TO W-RESP

           MOVE 'S1 A: ALICE claims ADMIN' TO W-LABEL
           MOVE 'ALICE' TO W-SIGNON  MOVE 'ADMIN' TO W-CLAIM
           MOVE 8 TO W-EXP-RC        MOVE '-' TO W-EXP-KEY
           PERFORM RUN-CASE

           MOVE 'S2 A: ADMIN as self, granted' TO W-LABEL
           MOVE 'ADMIN' TO W-SIGNON  MOVE 'ADMIN' TO W-CLAIM
           MOVE 0 TO W-EXP-RC        MOVE 'ADMIN' TO W-EXP-KEY
           PERFORM RUN-CASE

           MOVE 'S3 A: ALICE as self, no grant' TO W-LABEL
           MOVE 'ALICE' TO W-SIGNON  MOVE 'ALICE' TO W-CLAIM
           MOVE 8 TO W-EXP-RC        MOVE 'ALICE' TO W-EXP-KEY
           PERFORM RUN-CASE

           MOVE 'S4 A: ADMIN claims ALICE' TO W-LABEL
           MOVE 'ADMIN' TO W-SIGNON  MOVE 'ALICE' TO W-CLAIM
           MOVE 8 TO W-EXP-RC        MOVE '-' TO W-EXP-KEY
           PERFORM RUN-CASE

           MOVE 'S5 A: ASSIGN fails' TO W-LABEL
           MOVE 'ALICE' TO W-SIGNON  MOVE 'ADMIN' TO W-CLAIM
           MOVE 16 TO W-RESP
           MOVE 12 TO W-EXP-RC       MOVE '-' TO W-EXP-KEY
           PERFORM RUN-CASE
           MOVE 0 TO W-RESP

           MOVE 'V' TO W-TYPE
           MOVE 'S6 V: ADMIN as self' TO W-LABEL
           MOVE 'ADMIN' TO W-SIGNON  MOVE 'ADMIN' TO W-CLAIM
           MOVE 0 TO W-EXP-RC        MOVE '-' TO W-EXP-KEY
           PERFORM RUN-CASE

           MOVE 'S7 V: ALICE claims ADMIN' TO W-LABEL
           MOVE 'ALICE' TO W-SIGNON  MOVE 'ADMIN' TO W-CLAIM
           MOVE 8 TO W-EXP-RC        MOVE '-' TO W-EXP-KEY
           PERFORM RUN-CASE

           MOVE 'L' TO W-TYPE
           MOVE 'S8 L: audit as ADMIN' TO W-LABEL
           MOVE 'ADMIN' TO W-SIGNON  MOVE 'ADMIN' TO W-CLAIM
           MOVE 0 TO W-EXP-RC        MOVE '-' TO W-EXP-KEY
           PERFORM RUN-CASE

           IF W-FAILURES > 0
              DISPLAY 'TSTSECMG: ' W-FAILURES ' FAILURE(S)'
              MOVE 1 TO RETURN-CODE
           ELSE
              DISPLAY 'TSTSECMG: ALL CASES PASSED'
              MOVE 0 TO RETURN-CODE
           END-IF
           STOP RUN.

       RUN-CASE.
           MOVE W-SIGNON TO STUB-CICS-USER
           MOVE W-RESP TO STUB-CICS-RESP
           MOVE '-' TO STUB-SQL-QUERIED
           INITIALIZE REQ
           MOVE W-TYPE TO SEC-REQUEST-TYPE
           MOVE W-CLAIM TO SEC-USER-ID
           MOVE 'INQONLN' TO SEC-RESOURCE-NAME
           MOVE 'READ' TO SEC-ACCESS-TYPE
           MOVE -1 TO SEC-RESPONSE-CODE
           CALL 'SECMGR' USING REQ
           CANCEL 'SECMGR'
           IF SEC-RESPONSE-CODE = W-EXP-RC
              AND STUB-SQL-QUERIED = W-EXP-KEY
              MOVE 'PASS' TO W-RESULT
           ELSE
              MOVE 'FAIL' TO W-RESULT
              ADD 1 TO W-FAILURES
           END-IF
           MOVE SEC-RESPONSE-CODE TO W-RC
           DISPLAY W-RESULT ' ' W-LABEL ' rc=' W-RC
                   ' authfile-key=' STUB-SQL-QUERIED.
