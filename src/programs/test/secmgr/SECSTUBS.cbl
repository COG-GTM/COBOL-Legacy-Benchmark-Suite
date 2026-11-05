       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSASGN.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  STUB-CICS-USER      PIC X(8) EXTERNAL.
       01  STUB-CICS-RESP      PIC S9(8) COMP EXTERNAL.
       LINKAGE SECTION.
       01  L-USER              PIC X(8).
       01  L-RESP              PIC S9(8) COMP.
       PROCEDURE DIVISION USING L-USER L-RESP.
           MOVE STUB-CICS-RESP TO L-RESP
           IF STUB-CICS-RESP = 0
              MOVE STUB-CICS-USER TO L-USER
           END-IF
           GOBACK.
       END PROGRAM CICSASGN.

       IDENTIFICATION DIVISION.
       PROGRAM-ID. SQLAUTH.
      * AUTHFILE contains exactly one grant: ADMIN / INQONLN / READ
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  STUB-SQL-QUERIED    PIC X(8) EXTERNAL.
       LINKAGE SECTION.
       01  L-USER              PIC X(8).
       01  L-RES               PIC X(8).
       01  L-ACC               PIC X(8).
       01  L-CNT               PIC S9(9) COMP-5.
       01  L-SQLCODE           PIC S9(9) COMP-5.
       PROCEDURE DIVISION USING L-USER L-RES L-ACC L-CNT L-SQLCODE.
           MOVE L-USER TO STUB-SQL-QUERIED
           MOVE 0 TO L-SQLCODE
           IF L-USER = 'ADMIN' AND L-RES = 'INQONLN'
              AND L-ACC = 'READ'
              MOVE 1 TO L-CNT
           ELSE
              MOVE 0 TO L-CNT
           END-IF
           GOBACK.
       END PROGRAM SQLAUTH.
