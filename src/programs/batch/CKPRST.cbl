       IDENTIFICATION DIVISION.
       PROGRAM-ID. CKPRST.
       
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT CHECKPOINT-FILE
           ASSIGN TO CKPTFILE
           ORGANIZATION IS INDEXED
           ACCESS MODE IS DYNAMIC
           RECORD KEY IS CKR-KEY
           FILE STATUS IS WS-FILE-STATUS.
           
       DATA DIVISION.
       FILE SECTION.
       FD  CHECKPOINT-FILE.
       01  CHECKPOINT-FILE-RECORD.
           05  CKR-KEY.
               10  CKR-PROGRAM-ID      PIC X(8).
               10  CKR-RUN-DATE        PIC X(8).
           05  CKR-DATA                PIC X(400).
       
       WORKING-STORAGE SECTION.
       01  WS-FILE-STATUS             PIC X(2).
       01  WS-ENTRY-POINT             PIC X(4).
           88  ENTRY-POINT-INIT       VALUE 'INIT'.
           88  ENTRY-POINT-TAKE       VALUE 'TAKE'.
           88  ENTRY-POINT-COMMIT     VALUE 'CMIT'.
           88  ENTRY-POINT-RESTART    VALUE 'RSTR'.
       
       LINKAGE SECTION.
       COPY CKPRST.
       COPY RETHND.
       
       PROCEDURE DIVISION USING CHECKPOINT-CONTROL
                              RETURN-HANDLING.
           
           EVALUATE TRUE
               WHEN ENTRY-POINT-INIT
                   PERFORM PROC-INIT
               WHEN ENTRY-POINT-TAKE
                   PERFORM PROC-TAKE-CHECKPOINT
               WHEN ENTRY-POINT-COMMIT
                   PERFORM PROC-COMMIT-CHECKPOINT
               WHEN ENTRY-POINT-RESTART
                   PERFORM PROC-RESTART
           END-EVALUATE
           
           GOBACK
           .
      
       PROC-INIT.
           CONTINUE.
       
       PROC-TAKE-CHECKPOINT.
           CONTINUE.
       
       PROC-COMMIT-CHECKPOINT.
           CONTINUE.
       
       PROC-RESTART.
           CONTINUE.
