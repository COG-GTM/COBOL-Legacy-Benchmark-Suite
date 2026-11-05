       IDENTIFICATION DIVISION.
       PROGRAM-ID. SECMGR.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-ENV  PIC X(4).
       LINKAGE SECTION.
       01  REQ.
           05 RQ-TYPE     PIC X.
           05 RQ-USER     PIC X(8).
           05 RQ-RES      PIC X(8).
           05 RQ-ACC      PIC X(8).
           05 RQ-RC       PIC S9(8) COMP.
           05 RQ-ERR      PIC X(80).
       PROCEDURE DIVISION USING REQ.
           MOVE SPACES TO WS-ENV.
           EVALUATE RQ-TYPE
             WHEN 'V' ACCEPT WS-ENV FROM ENVIRONMENT 'SCN_V'
             WHEN 'A' ACCEPT WS-ENV FROM ENVIRONMENT 'SCN_A'
             WHEN 'L' ACCEPT WS-ENV FROM ENVIRONMENT 'SCN_L'
           END-EVALUATE.
           MOVE FUNCTION NUMVAL(WS-ENV) TO RQ-RC.
           IF RQ-RC NOT = 0
              EVALUATE RQ-TYPE
                WHEN 'V' MOVE 'User validation failed' TO RQ-ERR
                WHEN 'A' MOVE 'Access denied' TO RQ-ERR
                WHEN 'L' MOVE 'Audit logging failed' TO RQ-ERR
              END-EVALUATE
           END-IF.
           IF RQ-TYPE = 'L' AND RQ-RC = 0
              DISPLAY 'EVT AUDITLOG-ROW user=' FUNCTION TRIM(RQ-USER)
                      ' program=' FUNCTION TRIM(RQ-RES)
                      ' access=' FUNCTION TRIM(RQ-ACC)
           ELSE
              DISPLAY 'EVT SECMGR ' RQ-TYPE ' rc=' RQ-RC
           END-IF.
           GOBACK.
       END PROGRAM SECMGR.

       IDENTIFICATION DIVISION.
       PROGRAM-ID. INQPORT.
       DATA DIVISION.
       LINKAGE SECTION.
       01  CA.
           05 CA-FUNC  PIC X(4).
           05 CA-ACCT  PIC X(10).
       PROCEDURE DIVISION USING CA.
           DISPLAY 'EVT DATA-SENT POSMAP acct=' CA-ACCT.
           GOBACK.
       END PROGRAM INQPORT.

       IDENTIFICATION DIVISION.
       PROGRAM-ID. INQHIST.
       DATA DIVISION.
       LINKAGE SECTION.
       01  CA.
           05 CA-FUNC  PIC X(4).
           05 CA-ACCT  PIC X(10).
       PROCEDURE DIVISION USING CA.
           DISPLAY 'EVT DATA-SENT HISMAP acct=' CA-ACCT.
           GOBACK.
       END PROGRAM INQHIST.

       IDENTIFICATION DIVISION.
       PROGRAM-ID. ERRHNDL.
       DATA DIVISION.
       LINKAGE SECTION.
       01  EA  PIC X(200).
       PROCEDURE DIVISION USING EA.
           DISPLAY 'EVT ERROR-SCREEN'.
           GOBACK.
       END PROGRAM ERRHNDL.

       IDENTIFICATION DIVISION.
       PROGRAM-ID. RECVSTUB.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  CALLS   PIC 9 VALUE 0.
       LINKAGE SECTION.
       01  CA.
           05 CA-FUNC  PIC X(4).
           05 CA-ACCT  PIC X(10).
       PROCEDURE DIVISION USING CA.
           ADD 1 TO CALLS.
           IF CALLS = 1
              ACCEPT CA-FUNC FROM ENVIRONMENT 'SCN_FUNC'
              MOVE '0000012345' TO CA-ACCT
           ELSE
              MOVE 'EXIT' TO CA-FUNC
           END-IF.
           DISPLAY 'EVT RECEIVE func=' CA-FUNC.
           GOBACK.
       END PROGRAM RECVSTUB.

       IDENTIFICATION DIVISION.
       PROGRAM-ID. ASGNSTUB.
       DATA DIVISION.
       LINKAGE SECTION.
       01  U  PIC X(8).
       PROCEDURE DIVISION USING U.
           MOVE 'ALICE' TO U.
           GOBACK.
       END PROGRAM ASGNSTUB.

       IDENTIFICATION DIVISION.
       PROGRAM-ID. SENDSTUB.
       DATA DIVISION.
       LINKAGE SECTION.
       01  M  PIC X(6).
       PROCEDURE DIVISION USING M.
           DISPLAY 'EVT SEND ' M.
           GOBACK.
       END PROGRAM SENDSTUB.

       IDENTIFICATION DIVISION.
       PROGRAM-ID. RETSTUB.
       PROCEDURE DIVISION.
           DISPLAY 'EVT CICS-RETURN'.
           STOP RUN.
       END PROGRAM RETSTUB.

       IDENTIFICATION DIVISION.
       PROGRAM-ID. SENDTXT.
       DATA DIVISION.
       LINKAGE SECTION.
       01  T  PIC X(80).
       PROCEDURE DIVISION USING T.
           DISPLAY 'EVT SEND-TEXT "' FUNCTION TRIM(T) '"'.
           GOBACK.
       END PROGRAM SENDTXT.
