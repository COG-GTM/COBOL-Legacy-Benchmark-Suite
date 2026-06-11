# Migration Prompt: `RPTSTA00` (System Statistics Report Generator) — COBOL → Java

> **How to use this document.** This is a self-contained prompt. Hand it (in full) to a
> developer or an AI coding assistant. Everything required to perform the migration —
> the complete COBOL source, the copybook layouts, the functional behavior, the data
> mappings, and the acceptance criteria — is embedded below. You should not need to open
> any other file in the repository to complete the task.

---

## 1. Objective

**Translate the following COBOL batch program, `RPTSTA00` (System Statistics Report
Generator), into Java, preserving its exact functional behavior.**

`RPTSTA00` is a z/OS batch program that reads two indexed (VSAM KSDS) statistics files,
aggregates performance metrics from them, and writes a fixed-width, 132-column
sequential report file. Your Java program must read the same logical inputs, perform the
same aggregation and calculations, emit a byte-equivalent fixed-width report, and return
the same process exit codes (`0` on success, `12` on failure).

Keep the implementation **simple and framework-free** (no Spring). Target **Java 17+**;
records and text blocks are encouraged. Include **unit tests**. Preserve the batch-job
semantics: open inputs, read all records sequentially, aggregate, write the report, close,
and exit with a return code.

---

## 2. Full COBOL Source Listing

The complete source of `RPTSTA00` and the four referenced copybooks is reproduced inline
below so this prompt is fully self-contained.

> **Read this before you start (source caveats).** This program is part of a COBOL
> *benchmark* suite and the on-disk sources are partially synthetic. Three things will
> not "just compile," and you must account for them when translating:
>
> 1. **`COPY DB2STAT` does not resolve to a file-record copybook.** The only `DB2STAT`
>    artifact in the repository is a *full standalone program*
>    (`src/programs/common/DB2STAT.cbl`, a DB2 statistics collector), **not** a
>    copybook that defines the `DB2-STATS` indexed-file record. Consequently the record
>    layout for the `DB2STATS` VSAM file (including the `STAT-KEY` record key and the
>    `END-OF-DB2-STATS` end-of-file condition named in `RPTSTA00`) is **not defined in
>    the available source.** The `DB2STAT.cbl` program *is* embedded below because the
>    task asked for it, and its `WS-STATS-RECORD` working-storage layout is the best
>    available description of what a DB2-statistics record contains — use it to model the
>    `Db2StatRecord` Java type.
> 2. **`BCHCTL` defines `BATCH-CONTROL-RECORD` keyed by `BCT-KEY`**, but `RPTSTA00`'s
>    `SELECT` for `BATCH-STATS` names the record key `BCH-KEY` and the EOF condition
>    `END-OF-BATCH-STATS`. Treat `BCT-KEY` as the batch record's key and model the batch
>    record from the `BATCH-CONTROL-RECORD` layout below.
> 3. **Several detail paragraphs are referenced but not present** in `RPTSTA00`
>    (`2110-ACCUMULATE-DB2-STATS`, `2210-ACCUMULATE-BATCH-STATS`,
>    `2310-CALC-DB2-METRICS`, `2320-CALC-BATCH-METRICS`, `2410-WRITE-DB2-SECTION`,
>    `2420-WRITE-BATCH-SECTION`, `2430-WRITE-TREND-ANALYSIS`). Their *intent* is clear
>    from the surrounding working-storage and report layouts (accumulate per-record
>    counters, compute averages/success-rate, format detail lines). Implement them in
>    Java to match that intent, and document any assumption you make.
>
> In short: preserve the **observable structure and behavior** described in Sections 3–10.
> Where the COBOL is incomplete, fill the gap with the straightforward implementation
> implied by the data definitions, and call out each assumption in code comments and the
> PR description.

### 2.1 `src/programs/batch/RPTSTA00.cbl` — main program

```cobol
       IDENTIFICATION DIVISION.
       PROGRAM-ID. RPTSTA00.
       AUTHOR. CLAUDE.
       DATE-WRITTEN. 2024-04-09.
      *****************************************************************
      * System Statistics Report Generator                             *
      *                                                               *
      * Generates system performance and statistics report including:  *
      * - Processing statistics                                       *
      * - Performance metrics                                         *
      * - Resource utilization                                        *
      * - Trend analysis                                             *
      *****************************************************************
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT DB2-STATS ASSIGN TO DB2STATS
               ORGANIZATION IS INDEXED
               ACCESS MODE IS SEQUENTIAL
               RECORD KEY IS STAT-KEY
               FILE STATUS IS WS-DB2-STATUS.

           SELECT BATCH-STATS ASSIGN TO BCHSTATS
               ORGANIZATION IS INDEXED
               ACCESS MODE IS SEQUENTIAL
               RECORD KEY IS BCH-KEY
               FILE STATUS IS WS-BCH-STATUS.

           SELECT REPORT-FILE ASSIGN TO RPTFILE
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-REPORT-STATUS.

       DATA DIVISION.
       FILE SECTION.
           COPY DB2STAT.
           COPY BCHCTL.

       FD  REPORT-FILE
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  REPORT-RECORD             PIC X(132).

       WORKING-STORAGE SECTION.
           COPY RTNCODE.
           COPY ERRHAND.

       01  WS-FILE-STATUS.
           05  WS-DB2-STATUS         PIC XX.
           05  WS-BCH-STATUS         PIC XX.
           05  WS-REPORT-STATUS      PIC XX.

       01  WS-REPORT-HEADERS.
           05  WS-HEADER1.
               10  FILLER            PIC X(132) VALUE ALL '*'.
           05  WS-HEADER2.
               10  FILLER            PIC X(35) VALUE SPACES.
               10  FILLER            PIC X(62)
                   VALUE 'SYSTEM STATISTICS AND PERFORMANCE REPORT'.
               10  FILLER            PIC X(35) VALUE SPACES.
           05  WS-HEADER3.
               10  FILLER            PIC X(15) VALUE 'REPORT DATE:'.
               10  WS-REPORT-DATE    PIC X(10).
               10  FILLER            PIC X(107) VALUE SPACES.

       01  WS-PERFORMANCE-METRICS.
           05  WS-DB2-METRICS.
               10  WS-DB2-CALLS         PIC 9(9).
               10  WS-DB2-ELAPSED       PIC 9(9)V99.
               10  WS-DB2-CPU           PIC 9(9)V99.
               10  WS-DB2-WAIT          PIC 9(9)V99.
           05  WS-BATCH-METRICS.
               10  WS-BATCH-JOBS        PIC 9(9).
               10  WS-BATCH-SUCCESS     PIC 9(9).
               10  WS-BATCH-FAILED      PIC 9(9).
               10  WS-BATCH-ELAPSED     PIC 9(9)V99.

       01  WS-DETAIL-LINES.
           05  WS-DB2-DETAIL.
               10  FILLER               PIC X(20) VALUE 'DB2 CALLS:'.
               10  WS-DB2-CALLS-OUT     PIC ZZZ,ZZZ,ZZ9.
               10  FILLER               PIC X(20) VALUE SPACES.
               10  FILLER               PIC X(20) VALUE 'AVG RESPONSE:'.
               10  WS-DB2-AVG-RESP      PIC ZZ,ZZ9.999.
               10  FILLER               PIC X(40) VALUE SPACES.

           05  WS-BATCH-DETAIL.
               10  FILLER               PIC X(20) VALUE 'BATCH JOBS:'.
               10  WS-BATCH-TOTAL       PIC ZZZ,ZZ9.
               10  FILLER               PIC X(10) VALUE SPACES.
               10  FILLER               PIC X(20) VALUE 'SUCCESS RATE:'.
               10  WS-SUCCESS-RATE      PIC ZZ9.99.
               10  FILLER               PIC X(05) VALUE '%'.
               10  FILLER               PIC X(40) VALUE SPACES.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-PROCESS-REPORT
           PERFORM 3000-CLEANUP
           GOBACK.

       1000-INITIALIZE.
           PERFORM 1100-OPEN-FILES
           PERFORM 1200-WRITE-HEADERS
           PERFORM 1300-INIT-ACCUMULATORS.

       1100-OPEN-FILES.
           OPEN INPUT DB2-STATS
           IF WS-DB2-STATUS NOT = '00'
               MOVE 'ERROR OPENING DB2 STATS'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF

           OPEN INPUT BATCH-STATS
           IF WS-BCH-STATUS NOT = '00'
               MOVE 'ERROR OPENING BATCH STATS'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF

           OPEN OUTPUT REPORT-FILE
           IF WS-REPORT-STATUS NOT = '00'
               MOVE 'ERROR OPENING REPORT FILE'
                 TO WS-ERROR-MESSAGE
               PERFORM 9999-ERROR-HANDLER
           END-IF.

       1200-WRITE-HEADERS.
           ACCEPT WS-REPORT-DATE FROM DATE
           WRITE REPORT-RECORD FROM WS-HEADER1
           WRITE REPORT-RECORD FROM WS-HEADER2
           WRITE REPORT-RECORD FROM WS-HEADER3.

       1300-INIT-ACCUMULATORS.
           INITIALIZE WS-PERFORMANCE-METRICS.

       2000-PROCESS-REPORT.
           PERFORM 2100-PROCESS-DB2-STATS
           PERFORM 2200-PROCESS-BATCH-STATS
           PERFORM 2300-CALCULATE-METRICS
           PERFORM 2400-WRITE-REPORT.

       2100-PROCESS-DB2-STATS.
           READ DB2-STATS
               AT END SET END-OF-DB2-STATS TO TRUE
           END-READ

           PERFORM UNTIL END-OF-DB2-STATS
               PERFORM 2110-ACCUMULATE-DB2-STATS
               READ DB2-STATS
                   AT END SET END-OF-DB2-STATS TO TRUE
               END-READ
           END-PERFORM.

       2200-PROCESS-BATCH-STATS.
           READ BATCH-STATS
               AT END SET END-OF-BATCH-STATS TO TRUE
           END-READ

           PERFORM UNTIL END-OF-BATCH-STATS
               PERFORM 2210-ACCUMULATE-BATCH-STATS
               READ BATCH-STATS
                   AT END SET END-OF-BATCH-STATS TO TRUE
               END-READ
           END-PERFORM.

       2300-CALCULATE-METRICS.
           PERFORM 2310-CALC-DB2-METRICS
           PERFORM 2320-CALC-BATCH-METRICS.

       2400-WRITE-REPORT.
           PERFORM 2410-WRITE-DB2-SECTION
           PERFORM 2420-WRITE-BATCH-SECTION
           PERFORM 2430-WRITE-TREND-ANALYSIS.

       3000-CLEANUP.
           CLOSE DB2-STATS
                BATCH-STATS
                REPORT-FILE.

       9999-ERROR-HANDLER.
           DISPLAY WS-ERROR-MESSAGE
           MOVE 12 TO RETURN-CODE
           GOBACK.
```

### 2.2 Copybook `DB2STAT` — embedded as `src/programs/common/DB2STAT.cbl`

> As noted above, `DB2STAT` in this repository is a **full DB2 statistics-collector
> program**, not a file-record copybook. It is embedded in full because the task requested
> it. For the Java migration, the relevant structure is its `WS-STATS-RECORD`
> working-storage layout (lines reproduced below), which describes the per-program DB2
> statistics that a `DB2STATS` record logically carries. The DB2/SQL/CICS plumbing in this
> program is **out of scope** for the `RPTSTA00` migration (`RPTSTA00` itself contains no
> SQL).

```cobol
       *================================================================*
      * Program Name: DB2STAT
      * Description: DB2 Statistics Collector
      * Version: 1.0
      * Date: 2024
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DB2STAT.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL BEGIN DECLARE SECTION END-EXEC.
           01  WS-STATS-RECORD.
               05  WS-PROGRAM-ID      PIC X(8).
               05  WS-START-TIME      PIC X(26).
               05  WS-END-TIME        PIC X(26).
               05  WS-ROWS-READ       PIC S9(9) COMP.
               05  WS-ROWS-INSERTED   PIC S9(9) COMP.
               05  WS-ROWS-UPDATED    PIC S9(9) COMP.
               05  WS-ROWS-DELETED    PIC S9(9) COMP.
               05  WS-COMMITS         PIC S9(9) COMP.
               05  WS-ROLLBACKS       PIC S9(9) COMP.
               05  WS-CPU-TIME        PIC S9(9)V99 COMP-3.
               05  WS-ELAPSED-TIME    PIC S9(9)V99 COMP-3.
           EXEC SQL END DECLARE SECTION END-EXEC.

           COPY SQLCA.
           COPY DBPROC.
           COPY ERRHAND.

       01  WS-CURRENT-TIMESTAMP    PIC X(26).
       01  WS-START-TIMESTAMP      PIC X(26).
       01  WS-FORMATTED-TIME       PIC ZZ,ZZ9.99.

       LINKAGE SECTION.
       01  LS-STAT-REQUEST.
           05  LS-FUNCTION         PIC X(4).
               88  FUNC-INIT         VALUE 'INIT'.
               88  FUNC-UPDT         VALUE 'UPDT'.
               88  FUNC-TERM         VALUE 'TERM'.
               88  FUNC-DISP         VALUE 'DISP'.
           05  LS-PROGRAM-ID       PIC X(8).
           05  LS-STAT-DATA.
               10  LS-ROWS-READ    PIC S9(9) COMP.
               10  LS-ROWS-INSRT   PIC S9(9) COMP.
               10  LS-ROWS-UPDT    PIC S9(9) COMP.
               10  LS-ROWS-DELT    PIC S9(9) COMP.
               10  LS-COMMITS      PIC S9(9) COMP.
               10  LS-ROLLBACKS    PIC S9(9) COMP.
           05  LS-RETURN-CODE      PIC S9(4) COMP.

       PROCEDURE DIVISION USING LS-STAT-REQUEST.
       0000-MAIN.
           EVALUATE TRUE
               WHEN FUNC-INIT
                   PERFORM 1000-INITIALIZE
               WHEN FUNC-UPDT
                   PERFORM 2000-UPDATE-STATS
               WHEN FUNC-TERM
                   PERFORM 3000-TERMINATE
               WHEN FUNC-DISP
                   PERFORM 4000-DISPLAY-STATS
               WHEN OTHER
                   MOVE 'Invalid function code' TO ERR-TEXT
                   PERFORM 9000-ERROR-ROUTINE
           END-EVALUATE

           GOBACK
           .

       1000-INITIALIZE.
           INITIALIZE WS-STATS-RECORD
           MOVE LS-PROGRAM-ID TO WS-PROGRAM-ID

           ACCEPT WS-CURRENT-TIMESTAMP FROM TIME STAMP
           MOVE WS-CURRENT-TIMESTAMP TO WS-START-TIME
           MOVE WS-CURRENT-TIMESTAMP TO WS-START-TIMESTAMP

           PERFORM 1100-CREATE-STATS-TABLE
           PERFORM 1200-INSERT-INITIAL
           .

       1100-CREATE-STATS-TABLE.
           EXEC SQL
               DECLARE GLOBAL TEMPORARY TABLE SESSION.DBSTATS
               (PROGRAM_ID      CHAR(8)      NOT NULL,
                START_TIME     TIMESTAMP    NOT NULL,
                END_TIME      TIMESTAMP,
                ROWS_READ     INTEGER      NOT NULL,
                ROWS_INSERTED INTEGER      NOT NULL,
                ROWS_UPDATED  INTEGER      NOT NULL,
                ROWS_DELETED  INTEGER      NOT NULL,
                COMMITS       INTEGER      NOT NULL,
                ROLLBACKS     INTEGER      NOT NULL,
                CPU_TIME      DECIMAL(11,2),
                ELAPSED_TIME  DECIMAL(11,2))
               ON COMMIT PRESERVE ROWS
           END-EXEC

           IF SQLCODE NOT = 0 AND SQLCODE NOT = -601
               MOVE 'Error creating stats table' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .

       1200-INSERT-INITIAL.
           EXEC SQL
               INSERT INTO SESSION.DBSTATS
               (PROGRAM_ID, START_TIME, ROWS_READ,
                ROWS_INSERTED, ROWS_UPDATED, ROWS_DELETED,
                COMMITS, ROLLBACKS)
               VALUES
               (:WS-PROGRAM-ID, CURRENT TIMESTAMP,
                0, 0, 0, 0, 0, 0)
           END-EXEC

           IF SQLCODE = 0
               MOVE 0 TO LS-RETURN-CODE
           ELSE
               MOVE 'Error initializing stats' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .

       2000-UPDATE-STATS.
           MOVE LS-ROWS-READ  TO WS-ROWS-READ
           MOVE LS-ROWS-INSRT TO WS-ROWS-INSERTED
           MOVE LS-ROWS-UPDT  TO WS-ROWS-UPDATED
           MOVE LS-ROWS-DELT  TO WS-ROWS-DELETED
           MOVE LS-COMMITS    TO WS-COMMITS
           MOVE LS-ROLLBACKS  TO WS-ROLLBACKS

           EXEC SQL
               UPDATE SESSION.DBSTATS
               SET ROWS_READ = :WS-ROWS-READ,
                   ROWS_INSERTED = :WS-ROWS-INSERTED,
                   ROWS_UPDATED = :WS-ROWS-UPDATED,
                   ROWS_DELETED = :WS-ROWS-DELETED,
                   COMMITS = :WS-COMMITS,
                   ROLLBACKS = :WS-ROLLBACKS
               WHERE PROGRAM_ID = :WS-PROGRAM-ID
           END-EXEC

           IF SQLCODE = 0
               MOVE 0 TO LS-RETURN-CODE
           ELSE
               MOVE 'Error updating stats' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .

       3000-TERMINATE.
           ACCEPT WS-CURRENT-TIMESTAMP FROM TIME STAMP
           MOVE WS-CURRENT-TIMESTAMP TO WS-END-TIME

           PERFORM 3100-CALC-TIMES

           EXEC SQL
               UPDATE SESSION.DBSTATS
               SET END_TIME = :WS-END-TIME,
                   CPU_TIME = :WS-CPU-TIME,
                   ELAPSED_TIME = :WS-ELAPSED-TIME
               WHERE PROGRAM_ID = :WS-PROGRAM-ID
           END-EXEC

           IF SQLCODE = 0
               MOVE 0 TO LS-RETURN-CODE
               PERFORM 4000-DISPLAY-STATS
           ELSE
               MOVE 'Error finalizing stats' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .

       3100-CALC-TIMES.
           COMPUTE WS-ELAPSED-TIME = FUNCTION
               NUMVAL(WS-END-TIME(1:15)) -
               NUMVAL(WS-START-TIMESTAMP(1:15))

           MOVE WS-ELAPSED-TIME TO WS-CPU-TIME
           MULTIPLY 0.65 BY WS-CPU-TIME
           .

       4000-DISPLAY-STATS.
           EXEC SQL
               SELECT ROWS_READ, ROWS_INSERTED,
                      ROWS_UPDATED, ROWS_DELETED,
                      COMMITS, ROLLBACKS,
                      CPU_TIME, ELAPSED_TIME
               INTO :WS-STATS-RECORD
               FROM SESSION.DBSTATS
               WHERE PROGRAM_ID = :WS-PROGRAM-ID
           END-EXEC

           IF SQLCODE = 0
               DISPLAY 'DB2 Statistics for ' WS-PROGRAM-ID
               DISPLAY '  Records Read:    ' WS-ROWS-READ
               DISPLAY '  Records Inserted: ' WS-ROWS-INSERTED
               DISPLAY '  Records Updated:  ' WS-ROWS-UPDATED
               DISPLAY '  Records Deleted:  ' WS-ROWS-DELETED
               DISPLAY '  Commits:          ' WS-COMMITS
               DISPLAY '  Rollbacks:        ' WS-ROLLBACKS

               MOVE WS-CPU-TIME TO WS-FORMATTED-TIME
               DISPLAY '  CPU Time:         '
                       WS-FORMATTED-TIME ' seconds'

               MOVE WS-ELAPSED-TIME TO WS-FORMATTED-TIME
               DISPLAY '  Elapsed Time:     '
                       WS-FORMATTED-TIME ' seconds'

               MOVE 0 TO LS-RETURN-CODE
           ELSE
               MOVE 'Error retrieving stats' TO ERR-TEXT
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .

       9000-ERROR-ROUTINE.
           MOVE 'DB2STAT' TO ERR-PROGRAM
           MOVE 12 TO LS-RETURN-CODE
           CALL 'ERRPROC' USING ERR-MESSAGE
           .
```

### 2.3 Copybook `BCHCTL` — `src/copybook/batch/BCHCTL.cpy`

```cobol
      *================================================================*
      * BATCH CONTROL FILE RECORD DEFINITION
      * Version: 1.0
      * Date: 2024
      *
      * Purpose: Job-level control and process sequencing.
      * Works with: CKPRST.cpy for program-level checkpointing
      *================================================================*
       01  BATCH-CONTROL-RECORD.
           05  BCT-KEY.
               10  BCT-JOB-NAME      PIC X(8).
               10  BCT-PROCESS-DATE  PIC X(8).
               10  BCT-SEQUENCE-NO   PIC 9(4).
           05  BCT-DATA.
               10  BCT-STATUS        PIC X(1).
                   88  BCT-STATUS-READY    VALUE 'R'.
                   88  BCT-STATUS-ACTIVE   VALUE 'A'.
                   88  BCT-STATUS-WAITING  VALUE 'W'.
                   88  BCT-STATUS-DONE     VALUE 'D'.
                   88  BCT-STATUS-ERROR    VALUE 'E'.
               10  BCT-PROCESS-CONTROL.
                   15  BCT-STEP-NAME    PIC X(8).
                   15  BCT-PROGRAM-NAME PIC X(8).
                   15  BCT-START-TIME   PIC X(8).
                   15  BCT-END-TIME     PIC X(8).
               10  BCT-DEPENDENCIES.
                   15  BCT-PREREQ-COUNT PIC 9(2) COMP.
                   15  BCT-PREREQ-JOBS  OCCURS 10 TIMES.
                       20  BCT-PREREQ-NAME  PIC X(8).
                       20  BCT-PREREQ-SEQ   PIC 9(4).
                       20  BCT-PREREQ-RC    PIC S9(4) COMP.
               10  BCT-RETURN-INFO.
                   15  BCT-RETURN-CODE  PIC S9(4) COMP.
                   15  BCT-ERROR-DESC   PIC X(80).
           05  BCT-STATISTICS.
               10  BCT-RESTART-COUNT  PIC 9(2) COMP.
               10  BCT-ATTEMPT-TS     PIC X(26).
               10  BCT-COMPLETE-TS    PIC X(26).
           05  BCT-FILLER            PIC X(50).
      *================================================================*
      * This control file manages job-level sequencing and dependencies,
      * while CKPRST handles program-level checkpointing.
      *
      * Example usage:
      * 1. Job scheduler creates BCT record with READY status
      * 2. Job step checks prerequisites using BCT-PREREQ-JOBS
      * 3. Program uses CKPRST for checkpointing during execution
      * 4. Job completion updates BCT status and return info
      *================================================================*
```

### 2.4 Copybook `RTNCODE` — `src/copybook/common/RTNCODE.cpy`

```cobol
      ******************************************************************
      * Return Code Management Copybook                                 *
      ******************************************************************
       01  RETURN-CODE-AREA.
           05 RC-REQUEST-TYPE        PIC X.
              88 RC-INITIALIZE           VALUE 'I'.
              88 RC-SET-CODE             VALUE 'S'.
              88 RC-GET-CODE             VALUE 'G'.
              88 RC-LOG-CODE             VALUE 'L'.
              88 RC-ANALYZE              VALUE 'A'.
           05 RC-PROGRAM-ID         PIC X(8).
           05 RC-CODES-AREA.
              10 RC-CURRENT-CODE    PIC S9(4) COMP.
              10 RC-HIGHEST-CODE    PIC S9(4) COMP.
              10 RC-NEW-CODE        PIC S9(4) COMP.
              10 RC-STATUS          PIC X.
                 88 RC-STATUS-SUCCESS    VALUE 'S'.
                 88 RC-STATUS-WARNING    VALUE 'W'.
                 88 RC-STATUS-ERROR      VALUE 'E'.
                 88 RC-STATUS-SEVERE     VALUE 'F'.
           05 RC-MESSAGE           PIC X(80).
           05 RC-RESPONSE-CODE     PIC S9(8) COMP.
           05 RC-ANALYSIS-DATA.
              10 RC-START-TIME     PIC X(26).
              10 RC-END-TIME       PIC X(26).
              10 RC-TOTAL-CODES    PIC S9(8) COMP.
              10 RC-MAX-CODE       PIC S9(4) COMP.
              10 RC-MIN-CODE       PIC S9(4) COMP.
           05 RC-RETURN-DATA.
              10 RC-RETURN-VALUE   PIC S9(4) COMP.
              10 RC-HIGHEST-RETURN PIC S9(4) COMP.
              10 RC-RETURN-STATUS  PIC X.
```

### 2.5 Copybook `ERRHAND` — `src/copybook/common/ERRHAND.cpy`

```cobol
      *================================================================*
      * Copybook Name: ERRHAND
      * Description: Standard Error Handling Definitions
      * Author: [Author name]
      * Date Written: 2024-03-20
      *================================================================*

      *----------------------------------------------------------------*
      * Error Categories
      *----------------------------------------------------------------*
       01  ERR-CATEGORIES.
           05  ERR-CAT-VSAM        PIC X(2) VALUE 'VS'.
           05  ERR-CAT-VALID       PIC X(2) VALUE 'VL'.
           05  ERR-CAT-PROC        PIC X(2) VALUE 'PR'.
           05  ERR-CAT-SYSTEM      PIC X(2) VALUE 'SY'.

      *----------------------------------------------------------------*
      * Standard Return Codes
      *----------------------------------------------------------------*
       01  ERR-RETURN-CODES.
           05  ERR-SUCCESS         PIC S9(4) COMP VALUE +0.
           05  ERR-WARNING         PIC S9(4) COMP VALUE +4.
           05  ERR-ERROR           PIC S9(4) COMP VALUE +8.
           05  ERR-SEVERE          PIC S9(4) COMP VALUE +12.
           05  ERR-TERMINAL        PIC S9(4) COMP VALUE +16.

      *----------------------------------------------------------------*
      * Error Message Structure
      *----------------------------------------------------------------*
       01  ERR-MESSAGE.
           05  ERR-TIMESTAMP.
               10  ERR-DATE        PIC X(10).
               10  ERR-TIME        PIC X(8).
           05  ERR-PROGRAM         PIC X(8).
           05  ERR-CATEGORY        PIC X(2).
           05  ERR-CODE            PIC X(4).
           05  ERR-SEVERITY        PIC S9(4) COMP.
           05  ERR-TEXT            PIC X(80).
           05  ERR-DETAILS         PIC X(256).

      *----------------------------------------------------------------*
      * VSAM Status Handling
      *----------------------------------------------------------------*
       01  ERR-VSAM-STATUSES.
           05  ERR-VSAM-SUCCESS    PIC X(2) VALUE '00'.
           05  ERR-VSAM-DUPKEY     PIC X(2) VALUE '22'.
           05  ERR-VSAM-NOTFND     PIC X(2) VALUE '23'.
           05  ERR-VSAM-EOF        PIC X(2) VALUE '10'.

       01  ERR-VSAM-MSGS.
           05  ERR-VSAM-22         PIC X(80) VALUE
               'Duplicate record key'.
           05  ERR-VSAM-23         PIC X(80) VALUE
               'Record not found'.
           05  ERR-OTHER           PIC X(80) VALUE
               'Unexpected VSAM error'.
```

---

## 3. Functional Summary

`RPTSTA00` is a self-contained batch report generator. It:

- **Reads two indexed (VSAM KSDS) input files** sequentially from first to last record:
  - `DB2-STATS` (DDname `DB2STATS`), keyed by `STAT-KEY`, containing DB2 activity
    statistics per program (rows read/inserted/updated/deleted, commits, rollbacks,
    CPU time, elapsed time).
  - `BATCH-STATS` (DDname `BCHSTATS`), keyed by `BCT-KEY`, containing batch job control /
    status records (job name, status, return codes, timings).
- **Writes one sequential output file** (DDname `RPTFILE`), a fixed-length report with a
  record length of **132 characters** (`RECORDING MODE IS F`).
- **Aggregates** the input records into performance metrics (DB2 call counts and timing
  averages; batch job totals and success rate), then **formats and writes** a header
  block plus DB2, batch, and trend-analysis detail sections.
- **Has no DB2 SQL, no CICS, and no inter-program `CALL`s** of its own. (The separately
  embedded `DB2STAT.cbl` *does* contain SQL, but that is a different program and is not
  invoked by `RPTSTA00`.)
- **Handles errors inline**: any non-`'00'` file status on open triggers
  `9999-ERROR-HANDLER`, which displays a message, sets the process return code to **12**,
  and terminates (`GOBACK`).

Control flow at a glance:

```
0000-MAIN
  └─ 1000-INITIALIZE
  │     ├─ 1100-OPEN-FILES        (open 2 inputs + 1 output; RC=12 on any open error)
  │     ├─ 1200-WRITE-HEADERS     (date + 3 header lines)
  │     └─ 1300-INIT-ACCUMULATORS (zero all metrics)
  ├─ 2000-PROCESS-REPORT
  │     ├─ 2100-PROCESS-DB2-STATS    (read-until-EOF loop, accumulate)
  │     ├─ 2200-PROCESS-BATCH-STATS  (read-until-EOF loop, accumulate)
  │     ├─ 2300-CALCULATE-METRICS    (derive averages, success rate)
  │     └─ 2400-WRITE-REPORT         (DB2 section, batch section, trend analysis)
  └─ 3000-CLEANUP                 (close all three files)
```

---

## 4. Data Structures

Map each COBOL structure to a Java type as described below. **Use `BigDecimal` for all
fixed-point decimal (`V99`, `COMP-3`) values — never `double`/`float`** — to preserve
COBOL fixed-point arithmetic and rounding semantics.

### General PIC → Java mapping reference

| COBOL PIC clause | Meaning | Java type |
|---|---|---|
| `PIC X(n)` | Fixed-length alphanumeric | `String` (length `n`, space-padded) |
| `PIC 9(n)` | Unsigned integer, `n` digits | `int` (n≤9) / `long` (n≤18) |
| `PIC S9(n) COMP` | Signed binary halfword/fullword | `int` (n≤9) / `long` |
| `PIC 9(n)V99` | Unsigned fixed-point, 2 decimals | `BigDecimal` (scale 2) |
| `PIC S9(n)V99 COMP-3` | Signed packed-decimal, 2 decimals | `BigDecimal` (scale 2) |
| `PIC 9(n) COMP` | Unsigned binary | `int` / `long` |
| Edited (`ZZZ,ZZ9`, `ZZ9.99`, `%`) | Display formatting masks | Render via `String.format` / `DecimalFormat` (see §6) |

### 4.1 `DB2STAT` → `Db2StatRecord` (record / POJO)

Model from `WS-STATS-RECORD` in `DB2STAT.cbl`. This is the per-program DB2 statistics
record that `RPTSTA00` aggregates over the `DB2STATS` file.

| COBOL field | PIC | Java field | Java type |
|---|---|---|---|
| `WS-PROGRAM-ID` | `X(8)` | `programId` | `String` |
| `WS-START-TIME` | `X(26)` | `startTime` | `String` (timestamp text) |
| `WS-END-TIME` | `X(26)` | `endTime` | `String` |
| `WS-ROWS-READ` | `S9(9) COMP` | `rowsRead` | `int` |
| `WS-ROWS-INSERTED` | `S9(9) COMP` | `rowsInserted` | `int` |
| `WS-ROWS-UPDATED` | `S9(9) COMP` | `rowsUpdated` | `int` |
| `WS-ROWS-DELETED` | `S9(9) COMP` | `rowsDeleted` | `int` |
| `WS-COMMITS` | `S9(9) COMP` | `commits` | `int` |
| `WS-ROLLBACKS` | `S9(9) COMP` | `rollbacks` | `int` |
| `WS-CPU-TIME` | `S9(9)V99 COMP-3` | `cpuTime` | `BigDecimal` (scale 2) |
| `WS-ELAPSED-TIME` | `S9(9)V99 COMP-3` | `elapsedTime` | `BigDecimal` (scale 2) |

> The `STAT-KEY` named in `RPTSTA00`'s `SELECT` is the record key for the `DB2STATS` file.
> It is **not defined** in the available source; the most reasonable choice is
> `programId` (`WS-PROGRAM-ID`), since that is the per-program identity of a statistics
> record. Treat `programId` as the key unless you have a layout that says otherwise, and
> document the assumption.

Suggested Java:

```java
public record Db2StatRecord(
        String programId,
        String startTime,
        String endTime,
        int rowsRead,
        int rowsInserted,
        int rowsUpdated,
        int rowsDeleted,
        int commits,
        int rollbacks,
        BigDecimal cpuTime,
        BigDecimal elapsedTime) {
    public String key() { return programId; }   // RECORD KEY IS STAT-KEY
}
```

### 4.2 `BCHCTL` → `BatchStatRecord` (record / POJO)

Model from `BATCH-CONTROL-RECORD`. The composite `BCT-KEY` (job name + process date +
sequence number) is the record key (`BCH-KEY` in `RPTSTA00`'s `SELECT`).

| COBOL field | PIC | Java field | Java type | Notes |
|---|---|---|---|---|
| `BCT-JOB-NAME` | `X(8)` | `jobName` | `String` | part of key |
| `BCT-PROCESS-DATE` | `X(8)` | `processDate` | `String` | part of key |
| `BCT-SEQUENCE-NO` | `9(4)` | `sequenceNo` | `int` | part of key |
| `BCT-STATUS` | `X(1)` | `status` | `char`/`String` | `R/A/W/D/E` (see 88s) |
| `BCT-STEP-NAME` | `X(8)` | `stepName` | `String` | |
| `BCT-PROGRAM-NAME` | `X(8)` | `programName` | `String` | |
| `BCT-START-TIME` | `X(8)` | `startTime` | `String` | |
| `BCT-END-TIME` | `X(8)` | `endTime` | `String` | |
| `BCT-PREREQ-COUNT` | `9(2) COMP` | `prereqCount` | `int` | |
| `BCT-PREREQ-JOBS` (OCCURS 10) | group | `prereqs` | `List<Prereq>` (≤10) | array of 10 |
| ‣ `BCT-PREREQ-NAME` | `X(8)` | `name` | `String` | |
| ‣ `BCT-PREREQ-SEQ` | `9(4)` | `seq` | `int` | |
| ‣ `BCT-PREREQ-RC` | `S9(4) COMP` | `returnCode` | `int` | |
| `BCT-RETURN-CODE` | `S9(4) COMP` | `returnCode` | `int` | |
| `BCT-ERROR-DESC` | `X(80)` | `errorDesc` | `String` | |
| `BCT-RESTART-COUNT` | `9(2) COMP` | `restartCount` | `int` | |
| `BCT-ATTEMPT-TS` | `X(26)` | `attemptTs` | `String` | |
| `BCT-COMPLETE-TS` | `X(26)` | `completeTs` | `String` | |
| `BCT-FILLER` | `X(50)` | (omit) | — | reserved padding |

Define an enum or constants for `BCT-STATUS`:
`R`=READY, `A`=ACTIVE, `W`=WAITING, `D`=DONE, `E`=ERROR. For the report's success/failure
counting (see §6), treat `D` (DONE) as success and `E` (ERROR) as failure; you may also
factor in `BCT-RETURN-CODE` (0 = success). Document whichever rule you implement.

### 4.3 `RTNCODE` → `ReturnCodeArea` (DTO) and process exit code

`RETURN-CODE-AREA` models a return-code management area. In `RPTSTA00` the only
observable use is the final process return code (`MOVE 12 TO RETURN-CODE`). Represent the
process-level return code as an `int` exit status (see §7). If you want fidelity to the
copybook, model it as a DTO:

| COBOL field | PIC | Java field | Java type |
|---|---|---|---|
| `RC-REQUEST-TYPE` (+88s `I/S/G/L/A`) | `X` | `requestType` | `char` / enum |
| `RC-PROGRAM-ID` | `X(8)` | `programId` | `String` |
| `RC-CURRENT-CODE` | `S9(4) COMP` | `currentCode` | `int` |
| `RC-HIGHEST-CODE` | `S9(4) COMP` | `highestCode` | `int` |
| `RC-NEW-CODE` | `S9(4) COMP` | `newCode` | `int` |
| `RC-STATUS` (+88s `S/W/E/F`) | `X` | `status` | `char` / enum |
| `RC-MESSAGE` | `X(80)` | `message` | `String` |
| `RC-RESPONSE-CODE` | `S9(8) COMP` | `responseCode` | `int` |
| `RC-START-TIME` / `RC-END-TIME` | `X(26)` | `startTime`/`endTime` | `String` |
| `RC-TOTAL-CODES` | `S9(8) COMP` | `totalCodes` | `int` |
| `RC-MAX-CODE` / `RC-MIN-CODE` | `S9(4) COMP` | `maxCode`/`minCode` | `int` |
| `RC-RETURN-VALUE` / `RC-HIGHEST-RETURN` | `S9(4) COMP` | `returnValue`/`highestReturn` | `int` |
| `RC-RETURN-STATUS` | `X` | `returnStatus` | `char` |

### 4.4 `ERRHAND` → `ErrorMessage` DTO + constants

Map the constant groups to Java constants/enums and the `ERR-MESSAGE` structure to a DTO.

```java
public final class ErrorCodes {
    public static final int SUCCESS  = 0;
    public static final int WARNING  = 4;
    public static final int ERROR    = 8;
    public static final int SEVERE   = 12;   // RPTSTA00's failure return code
    public static final int TERMINAL = 16;

    public static final String CAT_VSAM   = "VS";
    public static final String CAT_VALID  = "VL";
    public static final String CAT_PROC   = "PR";
    public static final String CAT_SYSTEM = "SY";

    public static final String VSAM_OK     = "00";
    public static final String VSAM_DUPKEY = "22";
    public static final String VSAM_NOTFND = "23";
    public static final String VSAM_EOF    = "10";
}
```

| `ERR-MESSAGE` field | PIC | Java field | Java type |
|---|---|---|---|
| `ERR-DATE` | `X(10)` | `date` | `String` |
| `ERR-TIME` | `X(8)` | `time` | `String` |
| `ERR-PROGRAM` | `X(8)` | `program` | `String` |
| `ERR-CATEGORY` | `X(2)` | `category` | `String` |
| `ERR-CODE` | `X(4)` | `code` | `String` |
| `ERR-SEVERITY` | `S9(4) COMP` | `severity` | `int` |
| `ERR-TEXT` | `X(80)` | `text` | `String` |
| `ERR-DETAILS` | `X(256)` | `details` | `String` |

---

## 5. I/O Mapping (VSAM indexed reads)

`RPTSTA00` reads both inputs `ACCESS MODE IS SEQUENTIAL` over an `ORGANIZATION IS INDEXED`
(KSDS) file — i.e., a full sequential scan in ascending key order. Reproduce this in Java
behind an interface so the backing store is swappable.

**Define a `StatsDataSource` abstraction** with concrete implementations:

```java
public interface StatsDataSource {
    /** Records returned in ascending key order (mirrors VSAM sequential read of a KSDS). */
    Iterable<Db2StatRecord> readDb2Stats();
    Iterable<BatchStatRecord> readBatchStats();
}
```

Provide at least one concrete implementation, and design for the others:

1. **Flat-file / CSV (recommended default for tests).** Read keyed records from
   delimited or fixed-width files that mirror the copybook layout. Sort by key on read so
   the sequence matches a KSDS scan. This is the simplest way to get byte-identical
   report output for the acceptance tests.
2. **JDBC / repository.** Back each file with a table (`DB2STATS`, `BCHSTATS`) and read
   via `ORDER BY <key>`. Keep the SQL inside the data-source implementation so the report
   logic is storage-agnostic.
3. **Keep the design flexible via the `StatsDataSource` interface** so callers (the
   report service) never know whether data came from a file, a database, or a test fixture.

Map the COBOL `FILE STATUS` checks to exceptions/return values in the data source: an
open/read failure should surface as an exception that the caller turns into return code
`12` (see §7), matching `WS-DB2-STATUS NOT = '00'` → `9999-ERROR-HANDLER`.

---

## 6. Report Output (fixed-width, 132 columns)

The output is a sequential file of fixed-length **132-byte** records. Every line you emit
**must be exactly 132 characters** (space-padded on the right), matching `PIC X(132)` and
`RECORDING MODE IS F`. Reproduce each COBOL layout field-for-field.

### Header block (written by `1200-WRITE-HEADERS`, in order)

1. **HEADER1** — 132 asterisks (`PIC X(132) VALUE ALL '*'`).
2. **HEADER2** — 35 spaces + the literal `SYSTEM STATISTICS AND PERFORMANCE REPORT` left-
   justified in a 62-char field (space-padded to 62) + 35 spaces. (Note: 35+62+35 = 132.
   The literal is 40 chars, so the 62-char field is the title followed by 22 spaces.)
3. **HEADER3** — `REPORT DATE:` in a 15-char field + the report date in a 10-char field +
   107 spaces. The date comes from `ACCEPT WS-REPORT-DATE FROM DATE` (COBOL `DATE` =
   `YYMMDD`, 6 digits) moved into a `PIC X(10)` field (so it occupies the first 6 bytes,
   remaining 4 are spaces). For test determinism, make the date injectable (e.g., a
   `Clock` or an explicit value) rather than reading the system clock directly.

### Detail lines (formatting masks for the body sections)

The `WS-DETAIL-LINES` group defines the edited output masks. Reproduce the numeric edit
masks exactly:

- **DB2 detail (`WS-DB2-DETAIL`):**
  - `'DB2 CALLS:'` in `X(20)` + `WS-DB2-CALLS-OUT` `PIC ZZZ,ZZZ,ZZ9` (11 chars, comma-
    grouped, leading-zero-suppressed) + 20 spaces + `'AVG RESPONSE:'` in `X(20)` +
    `WS-DB2-AVG-RESP` `PIC ZZ,ZZ9.999` (10 chars, 3 decimals) + 40 spaces.
- **Batch detail (`WS-BATCH-DETAIL`):**
  - `'BATCH JOBS:'` in `X(20)` + `WS-BATCH-TOTAL` `PIC ZZZ,ZZ9` (7 chars) + 10 spaces +
    `'SUCCESS RATE:'` in `X(20)` + `WS-SUCCESS-RATE` `PIC ZZ9.99` (6 chars) + `'%'` in
    `X(05)` + 40 spaces.

> **Edit-mask semantics.** `Z` = leading-zero suppression (blank for suppressed leading
> zeros), `9` = forced digit, `,` = inserted comma (suppressed if to the left of all
> significant digits), `.` = decimal point. Implement these precisely — a naive
> `String.format("%,d", ...)` will not reproduce COBOL zero-suppression/space behavior in
> every case. Prefer a small dedicated formatter (or `DecimalFormat` with patterns like
> `",##0"`, then left-pad to the field width with spaces) and unit-test it against the
> COBOL masks.

### Derived metrics (computed in `2300-CALCULATE-METRICS`)

The `23xx` paragraphs are not in the source, but the working-storage makes their intent
clear. Implement:

- **DB2 calls** (`WS-DB2-CALLS`): total number of DB2 stat records (or a sum of activity
  counts — pick the interpretation consistent with your `2110` accumulation and document
  it).
- **DB2 average response** (`WS-DB2-AVG-RESP`): `WS-DB2-ELAPSED / WS-DB2-CALLS`
  (guard against divide-by-zero → 0), as `BigDecimal` rounded to 3 decimals for the
  `ZZ,ZZ9.999` mask.
- **Batch jobs total** (`WS-BATCH-JOBS` → `WS-BATCH-TOTAL`): count of batch records.
- **Success rate** (`WS-SUCCESS-RATE`): `WS-BATCH-SUCCESS / WS-BATCH-JOBS * 100`
  (guard divide-by-zero → 0), `BigDecimal` rounded to 2 decimals for `ZZ9.99`.

### Output mechanics in Java

Write with a `BufferedWriter` (or `Writer`) using a `ReportFileWriter` helper that
guarantees each line is padded/truncated to exactly 132 chars before writing the line
terminator. Use a small report-builder method per section (`String.format` / a field
formatter), e.g.:

```java
static String fixed(String s, int width) {
    if (s.length() >= width) return s.substring(0, width);
    return s + " ".repeat(width - s.length());
}
void writeLine(String content) throws IOException {
    writer.write(fixed(content, 132));
    writer.newLine();
}
```

---

## 7. Error Handling

Map `9999-ERROR-HANDLER` (`DISPLAY` message → `MOVE 12 TO RETURN-CODE` → `GOBACK`) to Java
conventions:

- **Log** the error message via **SLF4J** (`logger.error(message)`), mirroring the COBOL
  `DISPLAY`.
- **Signal failure** by either throwing a dedicated checked/unchecked exception
  (e.g., `StatisticsReportException`) caught at the top level, or by returning an exit
  code. The top-level entry point must ultimately cause the process to exit with **12**
  on any failure (use `System.exit(12)` from `main`, or return `12` from `run()`).
- **Structured try/catch:** wrap file open/read/write in try/catch. On `IOException`
  (or a data-source open failure analogous to `FILE STATUS NOT = '00'`), log a message
  that matches the COBOL intent (`"ERROR OPENING DB2 STATS"`, `"ERROR OPENING BATCH
  STATS"`, `"ERROR OPENING REPORT FILE"`) and propagate to the top-level handler.
- Use the constants from `ERRHAND` (`ErrorCodes.SEVERE == 12`) so the failure code is
  named, not magic.

```java
public int run() {
    try {
        initialize();        // open + headers + zero accumulators
        processReport();     // read, accumulate, calculate, write
        cleanup();           // close files
        return 0;            // success
    } catch (StatisticsReportException | IOException e) {
        log.error(e.getMessage(), e);
        return ErrorCodes.SEVERE;   // 12
    }
}

public static void main(String[] args) {
    System.exit(new RptSta00Application(/* deps */).run());
}
```

---

## 8. Control Flow

Translate the PERFORM structure into methods on a single class (preserve the paragraph
names as method names for traceability), with a `run()`/`main` entry point.

| COBOL paragraph | Java method | Responsibility |
|---|---|---|
| `0000-MAIN` | `run()` | orchestrates initialize → process → cleanup, returns exit code |
| `1000-INITIALIZE` | `initialize()` | open files, write headers, zero accumulators |
| `1100-OPEN-FILES` | `openFiles()` | open 2 inputs + 1 output; failure → RC 12 |
| `1200-WRITE-HEADERS` | `writeHeaders()` | report date + 3 header lines |
| `1300-INIT-ACCUMULATORS` | `initAccumulators()` | zero all metric fields |
| `2000-PROCESS-REPORT` | `processReport()` | run the four sub-steps below |
| `2100-PROCESS-DB2-STATS` | `processDb2Stats()` | sequential read-until-EOF loop |
| `2110-ACCUMULATE-DB2-STATS` | `accumulateDb2Stats(rec)` | add one DB2 record to totals |
| `2200-PROCESS-BATCH-STATS` | `processBatchStats()` | sequential read-until-EOF loop |
| `2210-ACCUMULATE-BATCH-STATS` | `accumulateBatchStats(rec)` | add one batch record; count success/fail |
| `2300-CALCULATE-METRICS` | `calculateMetrics()` | derive averages + success rate |
| `2310-CALC-DB2-METRICS` | `calcDb2Metrics()` | DB2 averages |
| `2320-CALC-BATCH-METRICS` | `calcBatchMetrics()` | batch totals / success rate |
| `2400-WRITE-REPORT` | `writeReport()` | write the three body sections |
| `2410-WRITE-DB2-SECTION` | `writeDb2Section()` | DB2 detail line(s) |
| `2420-WRITE-BATCH-SECTION` | `writeBatchSection()` | batch detail line(s) |
| `2430-WRITE-TREND-ANALYSIS` | `writeTrendAnalysis()` | trend section (see note) |
| `3000-CLEANUP` | `cleanup()` | close all files |
| `9999-ERROR-HANDLER` | `errorHandler(msg)` | log + signal RC 12 (see §7) |

> The `2110/2210/2310/2320/2410/2420/2430` paragraphs are referenced but absent from the
> source. Implement them per the intent described in §6 (accumulate counters, compute
> averages/success-rate, emit the corresponding detail lines). `2430-WRITE-TREND-ANALYSIS`
> has no defined layout in the source — implement a minimal, clearly-labeled trend section
> (e.g., echo the computed DB2 and batch aggregates) and document the assumption, or emit
> nothing beyond a section header if you prefer strict fidelity. State your choice.

The read loops must preserve the COBOL **priming-read pattern**: read one record, then
loop `while not EOF { accumulate; read next; }`.

---

## 9. Suggested Java Project Structure

```
src/main/java/com/legacy/reports/
    RptSta00Application.java          // main / run(): control flow (0000-MAIN ... 3000-CLEANUP)
    model/
        Db2StatRecord.java            // DB2STAT  -> §4.1
        BatchStatRecord.java          // BCHCTL   -> §4.2
        ReturnCodeArea.java           // RTNCODE  -> §4.3 (optional fidelity DTO)
        ErrorMessage.java             // ERRHAND  -> §4.4
    error/
        ErrorCodes.java               // ERRHAND constants (SEVERE=12, VSAM statuses, ...)
        StatisticsReportException.java
    service/
        StatisticsReportService.java  // 2000/2300 aggregation + metric calculations
    io/
        StatsDataSource.java          // interface (§5)
        CsvStatsDataReader.java       // flat-file/CSV implementation
        JdbcStatsDataReader.java      // optional JDBC implementation
        ReportFileWriter.java         // fixed-width 132-col writer (§6)
        ReportFormatter.java          // COBOL edit-mask formatting (ZZ,ZZ9.999 etc.)

src/test/java/com/legacy/reports/
    ReportFormatterTest.java          // edit-mask correctness
    StatisticsReportServiceTest.java  // aggregation + success-rate + divide-by-zero
    RptSta00ApplicationTest.java      // end-to-end: fixtures -> 132-col report + exit code
    resources/
        sample-db2stats.csv
        sample-bchstats.csv
        expected-report.txt           // golden 132-col output
```

(You may flatten packages if you prefer; keep model / io / service / error separation.)

---

## 10. Acceptance Criteria

1. **Functional equivalence of output.** Given the same input data (the same `DB2STATS`
   and `BCHSTATS` records), the Java program produces a report file that is **byte-
   identical** to the COBOL output where the COBOL behavior is fully specified — every
   line exactly **132 characters**, headers exactly as in §6, and numeric fields formatted
   per the COBOL edit masks. Where the COBOL is under-specified (the missing `2110`–`2430`
   paragraphs / trend section), output must be **functionally equivalent** to the
   documented intent and must remain stable/deterministic.
2. **Identical exit codes.** Return **`0`** on success and **`12`** on any failure
   (file open/read/write error), matching `9999-ERROR-HANDLER`.
3. **Sequential, full scan.** All records of both inputs are read in ascending key order
   and aggregated; the priming-read loop structure is preserved.
4. **No floating point for money/time math.** Decimal/`COMP-3` values use `BigDecimal`.
5. **Deterministic date handling.** The report date is injectable so tests are stable.
6. **Tests pass.** Unit tests cover the edit-mask formatter, the metric calculations
   (including divide-by-zero guards), and an end-to-end fixture-to-golden-file comparison.

---

## 11. Constraints and Notes

- **No Spring** (or any heavyweight framework). Plain Java + a logging facade (SLF4J) and
  a test framework (JUnit 5) is sufficient. A simple `pom.xml`/`build.gradle` is fine.
- **Java 17+**. Use `record`s for the immutable data models and **text blocks** where they
  improve readability. Other modern features (streams, `switch` expressions) are welcome.
- **Include unit tests** (JUnit 5 recommended), as described in §9–§10.
- **Preserve batch-job semantics:** open inputs and output, read every record
  sequentially, aggregate, write the fixed-width report, close everything, and exit with
  the appropriate return code. No interactive I/O, no CICS, no embedded SQL in the
  translated `RPTSTA00` itself.
- **Fidelity vs. completeness:** the source is partially synthetic (see the caveats in §2).
  Implement the fully-specified behavior exactly; for the gaps, implement the
  straightforward behavior implied by the data definitions and **explicitly document every
  assumption** (in code comments and the PR description) so a reviewer can verify your
  interpretation against a real layout if one becomes available.
- **Traceability:** keep COBOL paragraph names as Java method names (camelCased) and
  reference the originating copybook in each model class's doc comment.
```
