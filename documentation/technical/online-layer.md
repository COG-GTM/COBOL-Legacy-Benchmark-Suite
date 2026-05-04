# Online (CICS) Layer — Technical Documentation

> **System**: COBOL Legacy Benchmark Suite — Investment Portfolio Management  
> **Layer**: Online Transaction Processing (CICS/OLTP)  
> **Transaction ID**: `PINQ`  
> **CSD Group**: `PORTGRP`

---

## Table of Contents

1. [Overview](#1-overview)
2. [CICS Transaction Flow](#2-cics-transaction-flow)
3. [DB2 Connection & Recovery Flow](#3-db2-connection--recovery-flow)
4. [BMS Screen Layouts (INQSET.bms)](#4-bms-screen-layouts-inqsetbms)
5. [CICS Resource Definitions (PORTDFN.csd)](#5-cics-resource-definitions-portdfncsd)
6. [Program Reference](#6-program-reference)
   - 6.1 [INQONLN — Portfolio Online Inquiry Main Handler](#61-inqonln--portfolio-online-inquiry-main-handler)
   - 6.2 [INQPORT — Portfolio Position Inquiry](#62-inqport--portfolio-position-inquiry)
   - 6.3 [INQHIST — Transaction History Inquiry](#63-inqhist--transaction-history-inquiry)
   - 6.4 [DB2ONLN — Online DB2 Connection Manager](#64-db2onln--online-db2-connection-manager)
   - 6.5 [DB2RECV — DB2 Recovery Manager](#65-db2recv--db2-recovery-manager)
   - 6.6 [CURSMGR — Cursor Manager](#66-cursmgr--cursor-manager)
   - 6.7 [ERRHNDL — Centralized Error Handler](#67-errhndl--centralized-error-handler)
   - 6.8 [SECMGR — Security Manager](#68-secmgr--security-manager)
7. [Copybook Dependencies](#7-copybook-dependencies)
8. [DB2 Tables & SQL Operations](#8-db2-tables--sql-operations)
9. [Error Handling & Recovery Patterns](#9-error-handling--recovery-patterns)
10. [Security Model](#10-security-model)

---

## 1. Overview

The Online Layer provides real-time inquiry capabilities for the Investment Portfolio Management System through CICS (Customer Information Control System) terminal interactions. Users initiate the **PINQ** transaction from a 3270 terminal, which invokes the main handler program **INQONLN**. From there, the system routes requests to specialized sub-programs for portfolio position lookups, transaction history retrieval, security validation, and error handling.

### Key Characteristics

| Attribute              | Value                                          |
|------------------------|-------------------------------------------------|
| Transaction ID         | `PINQ`                                          |
| Entry Program          | `INQONLN`                                       |
| CICS Profile           | `DFHCICST`                                      |
| BMS Mapset             | `INQSET`                                        |
| DB2 Subsystem          | `POSMVP`                                        |
| DB2 Plan               | `PORTPLAN`                                      |
| CSD Group              | `PORTGRP`                                       |
| VSAM File              | `POSFILE` (`PORTFOLIO.POSITION.VSAM`)           |
| Number of Programs     | 8                                               |
| Number of BMS Maps     | 4 (`MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP`)      |

---

## 2. CICS Transaction Flow

The following diagram illustrates the program call chain initiated by the `PINQ` transaction:

```
 3270 Terminal
      |
      | PINQ transaction
      v
 +----------+      SEND MAP('INQMNU')       +-----------+
 | INQONLN  |------------------------------>| MENMAP    |  Main Menu Screen
 | (Main    |      RECEIVE MAP('INQMAP')     | (INQSET)  |
 | Handler) |<------------------------------+-----------+
 |          |
 |          |--- EVALUATE WS-COMMAREA-FUNCTION:
 |          |
 |          |--- 'MENU' --> P200-DISPLAY-MENU
 |          |                  SEND MAP('INQMNU') / INQSET
 |          |
 |          |--- 'INQP' --> P300-PORTFOLIO-INQUIRY
 |          |                  |
 |          |                  | EXEC CICS LINK PROGRAM('INQPORT')
 |          |                  v
 |          |              +----------+
 |          |              | INQPORT  |--- READ FILE('POSFILE') [VSAM]
 |          |              | (Position|--- SEND MAP('POSMAP') / INQSET
 |          |              |  Inquiry)|
 |          |              +----------+
 |          |
 |          |--- 'INQH' --> P400-HISTORY-INQUIRY
 |          |                  |
 |          |                  | EXEC CICS LINK PROGRAM('INQHIST')
 |          |                  v
 |          |              +----------+     LINK      +----------+
 |          |              | INQHIST  |-------------->| DB2ONLN  |
 |          |              | (History |     LINK      | (Connect)|
 |          |              |  Inquiry)|<-------+      +----------+
 |          |              |          |        |           |
 |          |              |          | LINK   |   on failure
 |          |              |          |------->|           v
 |          |              |          |        |      +----------+
 |          |              |          | CURSMGR|      | DB2RECV  |
 |          |              |          | (D/O/F/C)     | (Recovery|
 |          |              |          |               +----------+
 |          |              |          |--- SEND MAP('HISMAP') / INQSET
 |          |              +----------+
 |          |
 |          |--- 'EXIT' --> SET SESSION-TERMINATED
 |          |
 |          |--- OTHER --> P900-ERROR-ROUTINE
 |          |                  |
 |          |                  | EXEC CICS LINK PROGRAM('ERRHNDL')
 |          |                  v
 |          |              +----------+
 |          |              | ERRHNDL  |--- INSERT INTO ERRLOG [DB2]
 |          |              | (Error   |--- Format message
 |          |              |  Handler)|--- Determine action (ABEND/CONTINUE/RETURN)
 |          |              +----------+
 |          |
 |          |--- P050-SECURITY-CHECK (runs after every request)
 |          |       |
 |          |       | EXEC CICS ASSIGN USERID(...)
 |          |       | EXEC CICS LINK PROGRAM('SECMGR') [Validate: 'V']
 |          |       | EXEC CICS LINK PROGRAM('SECMGR') [Authorize: 'A']
 |          |       | EXEC CICS LINK PROGRAM('SECMGR') [Audit: 'L']
 |          |       v
 |          |   +----------+
 |          |   | SECMGR   |--- CICS ASSIGN USERID/TERMID/TRANSID
 |          |   | (Security|--- SELECT FROM AUTHFILE [DB2]
 |          |   |  Manager)|--- INSERT INTO AUDITLOG [DB2]
 |          |   +----------+
 |          |
 +----------+
      |
      | EXEC CICS RETURN
      v
   Session End
```

### Request Processing Lifecycle

1. **RECEIVE MAP** — INQONLN receives terminal input from `INQMAP` / `INQSET`
2. **Route** — EVALUATE dispatches to the appropriate function (`MENU`, `INQP`, `INQH`, `EXIT`)
3. **Execute** — Sub-program is LINKed with the COMMAREA
4. **Security** — P050-SECURITY-CHECK validates, authorizes, and audits the user via SECMGR
5. **Error** — If security fails or an error occurs, ERRHNDL is invoked
6. **Loop** — Process repeats until the user selects `EXIT`
7. **RETURN** — CICS RETURN terminates the transaction

---

## 3. DB2 Connection & Recovery Flow

The DB2 infrastructure programs work together to provide reliable database access with automatic recovery:

```
 +----------+                    +----------+                    +----------+
 | Caller   |   'C' Connect     | DB2ONLN  |   SQL CONNECT      |   DB2    |
 | (e.g.    |------------------>| (Conn    |------------------->| POSMVP   |
 | INQHIST) |   DB2-REQUEST     | Manager) |   TO POSMVP        | Subsystem|
 |          |<------------------|          |<-------------------|          |
 |          |   Token/Error     |          |   SQLCODE           |          |
 +----------+                    +----+-----+                    +----------+
      |                               |
      | On failure                    | Pool tracking:
      | (DB2-RESPONSE-CODE != 0)      |   WS-ACTIVE-CONNECTIONS
      v                               |   WS-MAX-CONNECTIONS (100)
 +----------+                         |
 | DB2RECV  |                         |
 | (Recovery|   'C' Connection Recovery:
 | Manager) |     Retry up to 3 times
 |          |     CICS DELAY between retries (2s interval)
 |          |     Re-LINK to DB2ONLN on each attempt
 |          |
 |          |   'T' Transaction Recovery:
 |          |     SQL ROLLBACK
 |          |
 |          |   'R' Cursor Recovery:
 |          |     LINK ERRHNDL for logging
 |          |     Set RETRY or FAILED status
 +----+-----+
      |
      | On cursor operations
      v
 +----------+
 | CURSMGR  |   'D' Declare  -->  SQL DECLARE ... CURSOR FOR
 | (Cursor  |   'O' Open     -->  SQL OPEN :CURS-NAME
 | Manager) |   'F' Fetch    -->  SQL FETCH :CURS-NAME INTO ...
 |          |   'C' Close    -->  SQL CLOSE :CURS-NAME
 |          |
 |          |   Array fetch support (up to 20 rows)
 |          |   Performance stats (fetch count, rows, time)
 +----------+
```

### Connection Lifecycle

1. **Connect** (`'C'`): Caller sends a connect request to DB2ONLN. If pool capacity allows (< 100 active), a `SQL CONNECT TO POSMVP` is issued. A connection token (timestamp + connection count) is returned.
2. **On Failure**: Caller invokes DB2RECV with `'C'` (connection recovery). DB2RECV retries up to 3 times with a 2-second `CICS DELAY` interval between attempts.
3. **Disconnect** (`'D'`): `SQL DISCONNECT` is issued and the active connection count is decremented.
4. **Status** (`'S'`): `SELECT CURRENT SERVER` verifies DB2 connectivity. Returns active connection count.

### Cursor Lifecycle (via CURSMGR)

1. **Declare** (`'D'`): Registers a dynamic SQL statement as a named cursor. Configures array fetch size (1 or 20 rows).
2. **Open** (`'O'`): Opens the declared cursor. Resets fetch counters.
3. **Fetch** (`'F'`): Fetches data into `CURS-DATA-AREA` (up to 3000 bytes). Supports array fetching for performance.
4. **Close** (`'C'`): Closes the cursor and releases resources.

---

## 4. BMS Screen Layouts (INQSET.bms)

The mapset `INQSET` is defined with `TYPE=MAP, MODE=INOUT, LANG=COBOL, STORAGE=AUTO` and contains four maps. All maps use a standard 24x80 terminal size with color and highlight attributes.

### 4.1 MENMAP — Main Menu

```
+------------------------------------------------------------------------------+
| Row  1: "Portfolio Management System"                          (PROT,BRT)    |
|                                                                              |
| Row  3: "Select Option:"                                      (PROT)        |
|                                                                              |
| Row  5:   "1. Portfolio Position Inquiry"                      (PROT)        |
| Row  6:   "2. Transaction History"                             (PROT)        |
| Row  7:   "3. Exit"                                            (PROT)        |
|                                                                              |
| Row  9:   [OPTION] (1 char, UNPROT, NUM, IC — cursor here)                  |
|                                                                              |
| Row 23: [ERRMSG] (78 chars, PROT, BRT, RED)                                 |
+------------------------------------------------------------------------------+
```

- **OPTION** field: Single-character numeric input with initial cursor (IC) placement.
- **ERRMSG** field: Error message display area in bright red.

### 4.2 POSMAP — Portfolio Position Inquiry

```
+------------------------------------------------------------------------------+
| Row  1: "Portfolio Position Inquiry"                           (PROT,BRT)    |
|                                                                              |
| Row  3: "Account:" [ACCTIN] (10 chars, UNPROT, IC)                          |
|                                                                              |
| Row  5: "Fund ID:"  [FUNDOUT] (6 chars, TURQUOISE)                          |
|         "Fund Name:" [NAMEOUT] (30 chars, TURQUOISE)                         |
|                                                                              |
| Row  7: "Units:"     [UNITOUT] (15 chars, TURQUOISE)                        |
|                                                                              |
| Row  9: "Cost Basis:"   [COSTOUT] (15 chars, TURQUOISE)                     |
|                                                                              |
| Row 11: "Market Value:" [VALOUT]  (15 chars, TURQUOISE)                     |
|                                                                              |
| Row 22: "PF3=Exit  PF7=Previous  PF8=Next"                    (PROT)        |
| Row 23: [POSMSG] (78 chars, PROT, BRT, RED)                                 |
+------------------------------------------------------------------------------+
```

- **ACCTIN**: Account number entry field (unprotected with initial cursor).
- Output fields display fund ID, name, unit count, cost basis, and market value in turquoise.
- PF3/PF7/PF8 function key hints in row 22.

### 4.3 HISMAP — Transaction History Inquiry

```
+------------------------------------------------------------------------------+
| Row  1: "Transaction History Inquiry"                          (PROT,BRT)    |
|                                                                              |
| Row  3: "Account:" [HISAIN] (10 chars, UNPROT, IC)                          |
|                                                                              |
| Row  5: Date       Type  Units      Price      Amount         (PROT,BRT)    |
| Row  7: [ROW1]  (65 chars, TURQUOISE)                                       |
| Row  8: [ROW2]  (65 chars, TURQUOISE)                                       |
| Row  9: [ROW3]  (65 chars, TURQUOISE)                                       |
| Row 10: [ROW4]  (65 chars, TURQUOISE)                                       |
| Row 11: [ROW5]  (65 chars, TURQUOISE)                                       |
| Row 12: [ROW6]  (65 chars, TURQUOISE)                                       |
| Row 13: [ROW7]  (65 chars, TURQUOISE)                                       |
| Row 14: [ROW8]  (65 chars, TURQUOISE)                                       |
| Row 15: [ROW9]  (65 chars, TURQUOISE)                                       |
| Row 16: [ROW10] (65 chars, TURQUOISE)                                       |
|                                                                              |
| Row 22: "PF3=Exit  PF7=Previous  PF8=Next"                    (PROT)        |
| Row 23: [HISMSG] (78 chars, PROT, BRT, RED)                                 |
+------------------------------------------------------------------------------+
```

- **HISAIN**: Account number entry field.
- 10 data rows (ROW1–ROW10) for transaction history display.
- Column headers: Date (pos 1), Type (pos 12), Units (pos 17), Price (pos 28), Amount (pos 39).

### 4.4 ERRMAP — Error Message Display

```
+------------------------------------------------------------------------------+
| Row  1: "System Error"                                         (PROT,BRT)    |
|                                                                              |
| Row  3: "Error Code:" [ERRCOUT] (8 chars, RED)                              |
|                                                                              |
| Row  5: "Details:"    [ERRDOUT] (65 chars, RED)                              |
|                                                                              |
| Row 22: "Press ENTER to continue"                              (PROT)        |
+------------------------------------------------------------------------------+
```

- Displays error code and details in red.
- User presses ENTER to acknowledge and return.

---

## 5. CICS Resource Definitions (PORTDFN.csd)

All resources are defined in CSD group **PORTGRP** and installed via list **PORTLST**.

### 5.1 Transaction Definition

| Resource | Type        | Program  | Profile    | Status  |
|----------|-------------|----------|------------|---------|
| `PINQ`   | TRANSACTION | INQONLN  | DFHCICST   | ENABLED |

### 5.2 Program Definitions

| Program  | Language | Data Location | Exec Key | Resident |
|----------|----------|---------------|----------|----------|
| INQONLN  | COBOL    | ANY           | USER     | NO       |
| INQPORT  | COBOL    | ANY           | USER     | NO       |
| INQHIST  | COBOL    | ANY           | USER     | NO       |
| DB2ONLN  | COBOL    | ANY           | USER     | NO       |
| CURSMGR  | COBOL    | ANY           | USER     | NO       |
| DB2RECV  | COBOL    | ANY           | USER     | NO       |
| SECMGR   | COBOL    | ANY           | USER     | NO       |

> **Note**: ERRHNDL is not explicitly defined in `PORTDFN.csd` but is referenced via `EXEC CICS LINK` from multiple programs. It would typically be defined in a shared system group.

### 5.3 Mapset Definition

| Resource | Type   | Resident |
|----------|--------|----------|
| INQSET   | MAPSET | NO       |

### 5.4 File Definition

| Resource | Type | DSName                      | Access          | Record Size | Strings |
|----------|------|-----------------------------|-----------------|-------------|---------|
| POSFILE  | FILE | PORTFOLIO.POSITION.VSAM     | READ, BROWSE, ADD | 200         | 10      |

### 5.5 DB2 Definitions

| Resource  | Type     | Plan     | Auth Type | Priority | Protect Num |
|-----------|----------|----------|-----------|----------|-------------|
| PORTDB2   | DB2ENTRY | PORTPLAN | USERID    | HIGH     | 5           |
| PINQ      | DB2TRAN  | (Entry: PORTDB2) | —  | —        | —           |

---

## 6. Program Reference

### 6.1 INQONLN — Portfolio Online Inquiry Main Handler

**Source**: `src/programs/online/INQONLN.cbl`

#### Purpose & Business Function

INQONLN is the entry-point program for the `PINQ` transaction. It manages the terminal session lifecycle: receiving user input from BMS maps, routing to the appropriate inquiry sub-program, enforcing security checks, and handling errors. It loops until the user selects EXIT.

#### CICS Commands Used

| Command                    | Context                                     |
|----------------------------|---------------------------------------------|
| `HANDLE CONDITION`         | Routes ERROR, PGMIDERR, NOTFND to P900      |
| `RECEIVE MAP('INQMAP')`   | Reads terminal input into WS-COMMAREA       |
| `SEND MAP('INQMNU')`      | Displays the main menu (MENMAP)             |
| `LINK PROGRAM('INQPORT')` | Delegates to portfolio position inquiry      |
| `LINK PROGRAM('INQHIST')` | Delegates to transaction history inquiry     |
| `LINK PROGRAM('ERRHNDL')` | Invokes centralized error handler            |
| `LINK PROGRAM('SECMGR')`  | Invokes security validation (V), authorization (A), and audit (L) |
| `ASSIGN USERID`            | Retrieves the CICS user ID for security      |
| `ABEND ABCODE('IERR')`    | Abends the transaction on fatal errors       |
| `RETURN`                   | Returns control to CICS                     |

#### BMS Map References

| Map Used          | Operation     | Purpose               |
|-------------------|---------------|------------------------|
| `INQMAP` / INQSET | RECEIVE MAP   | Read terminal input    |
| `INQMNU` / INQSET | SEND MAP      | Display main menu      |

#### Copybook Dependencies

| Copybook | Section         | Purpose                         |
|----------|-----------------|----------------------------------|
| `INQCOM` | WS + LINKAGE    | COMMAREA structure               |
| `ERRHND` | WORKING-STORAGE | Error handling data area         |

#### COMMAREA Structure

Uses `INQCOM` copybook (passed to all sub-programs):

```
01  INQCOM-AREA.
    05 INQCOM-FUNCTION         PIC X(4).       -- MENU/INQP/INQH/EXIT
    05 INQCOM-ACCOUNT-NO       PIC X(10).      -- Account number
    05 INQCOM-RESPONSE-CODE    PIC S9(8) COMP. -- Response code
    05 INQCOM-ERROR-MSG        PIC X(80).      -- Error message
```

#### Program Call Chain

```
INQONLN --> INQPORT  (via LINK, COMMAREA)
        --> INQHIST  (via LINK, COMMAREA)
        --> ERRHNDL  (via LINK, WS-ERROR-AREA)
        --> SECMGR   (via LINK, WS-SECURITY-REQUEST — 3 calls: V, A, L)
```

#### Security Flow (P050-SECURITY-CHECK)

1. **Validate** (`'V'`): Retrieves CICS USERID via ASSIGN, sends to SECMGR for identity validation
2. **Authorize** (`'A'`): If validation passes, checks authorization for resource `INQONLN` with access type `READ`
3. **Audit** (`'L'`): If authorization passes, logs the access event

If any step fails (`SEC-RESPONSE-CODE != 0`), the error message is propagated and P900-ERROR-ROUTINE is invoked, followed by `EXEC CICS RETURN`.

#### Error Handling

- P900-ERROR-ROUTINE populates `ERR-PROGRAM` (`'INQONLN'`), `ERR-PARAGRAPH`, CICS EIBRESP/EIBRESP2, and sets severity to WARNING.
- LINKs to ERRHNDL for logging and action determination.
- If ERRHNDL returns `ERR-ABEND`, issues `EXEC CICS ABEND ABCODE('IERR')`.

---

### 6.2 INQPORT — Portfolio Position Inquiry

**Source**: `src/programs/online/INQPORT.cbl`

#### Purpose & Business Function

Retrieves and displays the current portfolio position for a given account. Reads position data from the VSAM file `POSFILE` and formats it for display on the `POSMAP` BMS map.

#### CICS Commands Used

| Command                     | Context                                      |
|-----------------------------|----------------------------------------------|
| `HANDLE CONDITION`          | Routes ERROR to P999, NOTFND to P900         |
| `READ FILE('POSFILE')`      | Reads VSAM position record by account key    |
| `SEND MAP('POSMAP')`        | Displays position data on the terminal       |
| `RETURN`                    | Returns control to caller (INQONLN)          |

#### BMS Map References

| Map Used          | Operation  | Purpose                     |
|-------------------|------------|------------------------------|
| `POSMAP` / INQSET | SEND MAP   | Display portfolio position   |

#### DB2 Interactions

- Includes `SQLPOS` via `EXEC SQL INCLUDE SQLPOS` for DB2 position structure (available for cross-reference but primary data access is via VSAM).

#### VSAM Access

- **File**: `POSFILE` (`PORTFOLIO.POSITION.VSAM`)
- **Operation**: `READ` by `RIDFLD` (account number from COMMAREA)
- **Record Layout**: Uses `POSREC` copybook

#### Copybook Dependencies

| Copybook | Section         | Purpose                              |
|----------|-----------------|---------------------------------------|
| `INQCOM` | WS + LINKAGE    | COMMAREA structure                    |
| `POSREC` | WORKING-STORAGE | VSAM position record layout           |
| `SQLPOS` | WORKING-STORAGE | DB2 position structure (SQL INCLUDE)  |

#### Data Flow

1. Receives COMMAREA from INQONLN with account number
2. Moves account number to `POSITION-ACCOUNT` of `WS-POSITION-RECORD`
3. Reads VSAM file POSFILE using the account as the key
4. If found: sends `POSMAP` with position data (fund ID, name, units, cost basis, market value)
5. If not found: sets error message `"Position not found for account"` in COMMAREA and returns

#### Error Handling

- **P900-NOT-FOUND**: Sets `INQCOM-ERROR-MSG` to `"Position not found for account"` and returns the COMMAREA to the caller.
- **P999-ERROR-ROUTINE**: Sets `INQCOM-ERROR-MSG` to `"Error accessing position data"` with the response code and returns.

---

### 6.3 INQHIST — Transaction History Inquiry

**Source**: `src/programs/online/INQHIST.cbl`

#### Purpose & Business Function

Retrieves transaction history from the DB2 `POSHIST` table for a given account and displays up to 10 rows on the `HISMAP` BMS map. Manages the full DB2 lifecycle: connection (via DB2ONLN), cursor management (via CURSMGR), and recovery (via DB2RECV).

#### CICS Commands Used

| Command                     | Context                                      |
|-----------------------------|----------------------------------------------|
| `HANDLE CONDITION`          | Routes ERROR to P999                         |
| `LINK PROGRAM('DB2ONLN')`  | Establishes DB2 connection                   |
| `LINK PROGRAM('DB2RECV')`  | Invokes connection recovery on failure       |
| `LINK PROGRAM('CURSMGR')`  | Manages cursor lifecycle (D, O, F, C)        |
| `SEND MAP('HISMAP')`       | Displays transaction history                 |
| `RETURN`                    | Returns control to caller (INQONLN)          |

#### BMS Map References

| Map Used          | Operation  | Purpose                          |
|-------------------|------------|-----------------------------------|
| `HISMAP` / INQSET | SEND MAP   | Display transaction history rows |

#### DB2 Interactions

| Operation         | Table/Object | Details                                 |
|-------------------|--------------|-----------------------------------------|
| SQL INCLUDE SQLCA | —            | DB2 communication area                  |
| SELECT (via cursor)| `POSHIST`   | `TRANS_DATE, TRANS_TYPE, TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC` |

#### Copybook Dependencies

| Copybook | Section         | Purpose                         |
|----------|-----------------|----------------------------------|
| `INQCOM` | WS + LINKAGE    | COMMAREA structure               |

#### COMMAREA & Working Storage Structures

**WS-HISTORY-TABLE** (10-row array):
```
05 WS-HISTORY-ENTRY OCCURS 10 TIMES.
   10 WS-TRANS-DATE    PIC X(10).
   10 WS-TRANS-TYPE    PIC X(4).
   10 WS-TRANS-UNITS   PIC S9(9)V99 COMP-3.
   10 WS-TRANS-PRICE   PIC S9(9)V99 COMP-3.
   10 WS-TRANS-AMOUNT  PIC S9(9)V99 COMP-3.
```

**WS-DB2-REQUEST** (passed to DB2ONLN):
```
05 DB2-REQUEST-TYPE        PIC X.           -- 'C' Connect
05 DB2-RESPONSE-CODE       PIC S9(8) COMP.
05 DB2-CONNECTION-TOKEN    PIC X(16).
05 DB2-ERROR-INFO.
   10 DB2-SQLCODE          PIC S9(9) COMP.
   10 DB2-ERROR-MSG        PIC X(80).
```

**WS-CURSOR-REQUEST** (passed to CURSMGR):
```
05 CURS-REQUEST-TYPE     PIC X.             -- D/O/F/C
05 CURS-NAME             PIC X(18).         -- 'HISTORY_CURSOR'
05 CURS-STMT             PIC X(240).        -- SQL statement
05 CURS-ARRAY-FETCH      PIC X.            -- 'Y' for array fetch
05 CURS-RESPONSE-CODE    PIC S9(8) COMP.
05 CURS-DATA-AREA        PIC X(3000).
05 CURS-DATA-LENGTH      PIC S9(4) COMP.
```

#### Program Call Chain

```
INQHIST --> DB2ONLN  ('C' connect)
        --> DB2RECV  ('C' connection recovery, on DB2ONLN failure)
        --> CURSMGR  ('D' declare, 'O' open, 'F' fetch, 'C' close)
```

#### Data Flow

1. **P100-INIT-PROGRAM**: Moves DFHCOMMAREA to WS, sets up error handling, calls P150-DB2-CONNECT
2. **P150-DB2-CONNECT**: LINKs to DB2ONLN with `'C'` request. On failure, LINKs to DB2RECV for recovery. Retries connection on `RECV-SUCCESS`. On `RECV-FAILED`, propagates error and invokes P999.
3. **P200-GET-HISTORY**: Builds SELECT statement for POSHIST, calls CURSMGR to Declare (`'D'`), Open (`'O'`), Fetch (`'F'`), then Close (`'C'`). Array fetch is enabled (`CURS-ARRAY-FETCH = 'Y'`).
4. **P250-FETCH-HISTORY**: Fetches data via CURSMGR (`'F'`) and moves `CURS-DATA-AREA` into `WS-HISTORY-TABLE`.
5. **P300-FORMAT-DISPLAY**: Sends `HISMAP` with history data.

#### Recovery Flow

If DB2ONLN connect fails:
1. INQHIST sets `RECV-REQUEST-TYPE = 'C'`, `RECV-PROGRAM = 'INQHIST'`, `RECV-SQLCODE` from the failed connection
2. LINKs to DB2RECV
3. DB2RECV attempts up to 3 reconnections via DB2ONLN with 2-second delays
4. On success: INQHIST re-enters P150-DB2-CONNECT
5. On failure: Error message propagated to COMMAREA, P999 invoked

---

### 6.4 DB2ONLN — Online DB2 Connection Manager

**Source**: `src/programs/online/DB2ONLN.cbl`

#### Purpose & Business Function

Manages the DB2 connection pool for all online programs. Provides connect, disconnect, and status-check operations against the `POSMVP` DB2 subsystem with a maximum pool size of 100 connections.

#### CICS Commands Used

| Command   | Context                          |
|-----------|----------------------------------|
| `RETURN`  | Returns control to caller        |

#### DB2 Interactions

| Operation                    | Details                                    |
|------------------------------|--------------------------------------------|
| `SQL CONNECT TO POSMVP`     | Establishes a new DB2 connection           |
| `SQL DISCONNECT`             | Releases an active DB2 connection          |
| `SQL SELECT CURRENT SERVER`  | Verifies DB2 connectivity (status check)   |
| `SQL INCLUDE SQLCA`          | DB2 communication area                     |

#### Copybook Dependencies

| Copybook | Section         | Purpose                   |
|----------|-----------------|----------------------------|
| `ERRHND` | WORKING-STORAGE | Error handling data area   |

#### COMMAREA Structure (Linkage Section)

```
01  DB2-REQUEST-AREA.
    05 DB2-REQUEST-TYPE        PIC X.
       88 DB2-CONNECT              VALUE 'C'.
       88 DB2-DISCONNECT           VALUE 'D'.
       88 DB2-STATUS               VALUE 'S'.
    05 DB2-RESPONSE-CODE       PIC S9(8) COMP.
    05 DB2-CONNECTION-TOKEN    PIC X(16).
    05 DB2-ERROR-INFO.
       10 DB2-SQLCODE          PIC S9(9) COMP.
       10 DB2-ERROR-MSG        PIC X(80).
```

#### Connection Pool Statistics

```
01  WS-POOL-STATS.
    05 WS-TOTAL-CONNECTIONS    PIC S9(8) COMP VALUE 0.
    05 WS-ACTIVE-CONNECTIONS   PIC S9(8) COMP VALUE 0.
    05 WS-AVAILABLE-CONNECTIONS PIC S9(8) COMP VALUE 0.
    05 WS-MAX-CONNECTIONS      PIC S9(8) COMP VALUE 100.
```

#### Operations

| Request | Paragraph                  | Behavior                                           |
|---------|----------------------------|----------------------------------------------------|
| `'C'`   | P100-PROCESS-CONNECT       | If pool < max: issues `SQL CONNECT TO POSMVP`, generates token, increments active count. Otherwise returns "Maximum connections reached" with RC=-1. |
| `'D'`   | P200-PROCESS-DISCONNECT    | Issues `SQL DISCONNECT`, decrements active count.  |
| `'S'`   | P300-CHECK-STATUS          | Issues `SELECT CURRENT SERVER` to verify DB2 health. Returns active connection count. |

#### Token Generation (P120-GENERATE-TOKEN)

Connection tokens are generated by concatenating `FUNCTION CURRENT-DATE` with the active connection count, providing a unique session identifier for each connection.

---

### 6.5 DB2RECV — DB2 Recovery Manager

**Source**: `src/programs/online/DB2RECV.cbl`

#### Purpose & Business Function

Provides recovery services for DB2 failures in the online layer. Handles three types of recovery: connection failures (with retry logic), transaction rollbacks, and cursor errors. Acts as the resilience layer between the application programs and DB2.

#### CICS Commands Used

| Command                     | Context                                    |
|-----------------------------|--------------------------------------------|
| `LINK PROGRAM('DB2ONLN')`  | Re-attempts DB2 connection during recovery |
| `LINK PROGRAM('ERRHNDL')`  | Logs cursor recovery errors                |
| `DELAY INTERVAL(2)`        | Wait between connection retry attempts     |
| `RETURN`                    | Returns control to caller                  |

#### DB2 Interactions

| Operation      | Details                                  |
|----------------|------------------------------------------|
| `SQL ROLLBACK` | Rolls back the current transaction       |
| `SQL INCLUDE SQLCA` | DB2 communication area              |

#### Copybook Dependencies

| Copybook | Section         | Purpose                           |
|----------|-----------------|-------------------------------------|
| `ERRHND` | WORKING-STORAGE | Error handling data area            |
| `DB2REQ` | WORKING-STORAGE | DB2 request structure for LINK calls |

#### COMMAREA Structure (Linkage Section)

```
01  RECOVERY-REQUEST-AREA.
    05 RECV-REQUEST-TYPE     PIC X.
       88 RECV-CONNECTION         VALUE 'C'.  -- Connection recovery
       88 RECV-TRANSACTION        VALUE 'T'.  -- Transaction rollback
       88 RECV-CURSOR             VALUE 'R'.  -- Cursor recovery
    05 RECV-RESPONSE-CODE    PIC S9(8) COMP.
    05 RECV-SQLCODE          PIC S9(9) COMP.
    05 RECV-ERROR-INFO.
       10 RECV-PROGRAM       PIC X(8).
       10 RECV-CURSOR        PIC X(18).
       10 RECV-MESSAGE       PIC X(80).
    05 RECV-STATUS           PIC X.
       88 RECV-SUCCESS            VALUE 'S'.
       88 RECV-FAILED             VALUE 'F'.
       88 RECV-RETRY              VALUE 'R'.
```

#### Recovery Operations

| Request | Paragraph                  | Strategy                                                              |
|---------|----------------------------|-----------------------------------------------------------------------|
| `'C'`   | P100-RECOVER-CONNECTION    | Retries DB2ONLN connect up to 3 times with 2-second `CICS DELAY`. Sets `RECV-SUCCESS` on reconnect, `RECV-FAILED` after max retries. |
| `'T'`   | P200-RECOVER-TRANSACTION   | Issues `SQL ROLLBACK`. Returns `RECV-SUCCESS` if SQLCODE=0, `RECV-FAILED` otherwise. |
| `'R'`   | P300-RECOVER-CURSOR        | Logs the error via ERRHNDL (populates program name, cursor name, SQLCODE). Sets `RECV-RETRY` if ERRHNDL says continue, `RECV-FAILED` otherwise. |

#### Retry Configuration

```
01  WS-RECOVERY-STATS.
    05 WS-RETRY-COUNT        PIC S9(4) COMP VALUE 0.
    05 WS-MAX-RETRIES        PIC S9(4) COMP VALUE 3.
    05 WS-RETRY-INTERVAL     PIC S9(8) COMP VALUE 2.  -- seconds
    05 WS-LAST-ERROR         PIC S9(9) COMP VALUE 0.
```

---

### 6.6 CURSMGR — Cursor Manager

**Source**: `src/programs/online/CURSMGR.cbl`

#### Purpose & Business Function

Provides centralized cursor lifecycle management for all online DB2 operations. Encapsulates cursor declaration, opening, fetching (with array fetch optimization), and closing. Tracks performance statistics (fetch count, rows fetched, fetch time).

#### CICS Commands Used

| Command  | Context                   |
|----------|---------------------------|
| `RETURN` | Returns control to caller |

#### DB2 Interactions

| Operation                           | Details                                          |
|-------------------------------------|--------------------------------------------------|
| `SQL DECLARE :CURS-NAME CURSOR FOR` | Declares a named cursor from a dynamic statement |
| `SQL OPEN :CURS-NAME`              | Opens the declared cursor                        |
| `SQL FETCH :CURS-NAME INTO`        | Fetches data (single row or array)               |
| `SQL CLOSE :CURS-NAME`             | Closes the cursor                                |
| `SQL INCLUDE SQLCA`                | DB2 communication area                           |

#### COMMAREA Structure (Linkage Section)

```
01  CURSOR-REQUEST-AREA.
    05 CURS-REQUEST-TYPE     PIC X.
       88 CURS-DECLARE           VALUE 'D'.
       88 CURS-OPEN              VALUE 'O'.
       88 CURS-FETCH             VALUE 'F'.
       88 CURS-CLOSE             VALUE 'C'.
    05 CURS-NAME             PIC X(18).         -- Cursor name
    05 CURS-STMT             PIC X(240).        -- SQL statement
    05 CURS-ARRAY-FETCH      PIC X.
       88 USE-ARRAY-FETCH         VALUE 'Y'.
       88 NO-ARRAY-FETCH          VALUE 'N'.
    05 CURS-RESPONSE-CODE    PIC S9(8) COMP.
    05 CURS-DATA-AREA        PIC X(3000).       -- Result data buffer
    05 CURS-DATA-LENGTH      PIC S9(4) COMP.
```

#### Operations

| Request | Paragraph            | Behavior                                                                 |
|---------|----------------------|--------------------------------------------------------------------------|
| `'D'`   | P100-DECLARE-CURSOR  | Sets array size (20 if array fetch, 1 otherwise). Declares cursor.       |
| `'O'`   | P200-OPEN-CURSOR     | Resets fetch counters. Opens cursor.                                     |
| `'F'`   | P300-FETCH-DATA      | Fetches rows into `CURS-DATA-AREA`. Updates stats.                       |
| `'C'`   | P400-CLOSE-CURSOR    | Closes cursor, records total stats.                                      |

#### Performance Tracking

```
01  WS-CURSOR-STATS.
    05 WS-FETCH-COUNT         PIC S9(8) COMP VALUE 0.
    05 WS-ROWS-FETCHED        PIC S9(8) COMP VALUE 0.
    05 WS-FETCH-TIME          PIC S9(8) COMP VALUE 0.
```

#### Array Fetch Configuration

- **Max array size**: 20 rows (`WS-MAX-ROWS`)
- **Data buffer**: 3000 bytes (`CURS-DATA-AREA`)
- When `CURS-ARRAY-FETCH = 'Y'`, fetches up to 20 rows per call for improved performance

---

### 6.7 ERRHNDL — Centralized Error Handler

**Source**: `src/programs/online/ERRHNDL.cbl`

#### Purpose & Business Function

Provides centralized error processing for all online programs. Initializes error context, logs errors to the DB2 `ERRLOG` table, formats human-readable error messages with trace IDs, and determines the recovery action (continue, return, or abend).

#### CICS Commands Used

| Command  | Context                   |
|----------|---------------------------|
| `RETURN` | Returns control to caller |

#### DB2 Interactions

| Operation                  | Table    | Details                                              |
|----------------------------|----------|------------------------------------------------------|
| `INSERT INTO ERRLOG`       | `ERRLOG` | Logs: timestamp, program, paragraph, SQLCODE, CICS RESP, severity, message, trace ID |
| `SQL INCLUDE SQLCA`        | —        | DB2 communication area                               |

#### Copybook Dependencies

| Copybook | Section         | Purpose                   |
|----------|-----------------|----------------------------|
| `ERRHND` | WS + LINKAGE    | Error handling data area   |

#### COMMAREA Structure (via ERRHND copybook)

```
01  ERROR-HANDLING.
    05 ERR-PROGRAM          PIC X(8).       -- Originating program
    05 ERR-PARAGRAPH        PIC X(30).      -- Originating paragraph
    05 ERR-SQLCODE          PIC S9(9) COMP. -- DB2 SQL code
    05 ERR-CICS-RESP        PIC S9(8) COMP. -- CICS EIBRESP
    05 ERR-CICS-RESP2       PIC S9(8) COMP. -- CICS EIBRESP2
    05 ERR-SEVERITY         PIC X.
       88 ERR-FATAL              VALUE 'F'.
       88 ERR-WARNING            VALUE 'W'.
       88 ERR-INFO               VALUE 'I'.
    05 ERR-MESSAGE          PIC X(80).      -- Error message text
    05 ERR-ACTION           PIC X.
       88 ERR-RETURN            VALUE 'R'.  -- Return to caller
       88 ERR-CONTINUE          VALUE 'C'.  -- Continue processing
       88 ERR-ABEND             VALUE 'A'.  -- Abend the transaction
    05 ERR-TRACE.
       10 ERR-TRACE-ID      PIC X(16).     -- Unique trace identifier
       10 ERR-TIMESTAMP     PIC X(26).     -- Error timestamp
```

#### Processing Flow

1. **P100-INIT-ERROR-HANDLER**: Copies DFHCOMMAREA to working storage. Generates timestamp via `FUNCTION CURRENT-DATE`. If `ERR-TRACE-ID` is blank, generates a random trace ID via `FUNCTION RANDOM`.
2. **P200-LOG-ERROR**: Inserts a full error record into the `ERRLOG` DB2 table. If the insert itself fails, marks the error as FATAL.
3. **P300-FORMAT-MESSAGE**: Builds a formatted message: `"Error in <program> - <message> (<trace-id>)"`.
4. **P400-DETERMINE-ACTION**: Sets the action flag based on severity:
   - `ERR-FATAL` → `ERR-ABEND` (caller should ABEND)
   - `ERR-WARNING` → `ERR-CONTINUE` (processing can continue)
   - `ERR-INFO` → `ERR-CONTINUE` (informational, continue)
   - `OTHER` → `ERR-RETURN` (return to caller)
5. Copies the updated error area back to DFHCOMMAREA.

---

### 6.8 SECMGR — Security Manager

**Source**: `src/programs/online/SECMGR.cbl`

#### Purpose & Business Function

Provides security services for the online layer: user identity validation against CICS credentials, resource-level authorization via DB2 `AUTHFILE` table lookups, and audit trail logging to the DB2 `AUDITLOG` table.

#### CICS Commands Used

| Command                                        | Context                                    |
|------------------------------------------------|--------------------------------------------|
| `ASSIGN USERID(WS-USER-ID)`                   | Retrieves the CICS signed-on user ID       |
| `ASSIGN TERMID(WS-TERMINAL-ID)`               | Retrieves the terminal ID (for audit)      |
| `ASSIGN TRANSID(WS-TRANSACTION-ID)`           | Retrieves the transaction ID (for audit)   |
| `RETURN`                                        | Returns control to caller                  |

#### DB2 Interactions

| Operation                   | Table      | Details                                                |
|-----------------------------|------------|--------------------------------------------------------|
| `SELECT COUNT(*) FROM AUTHFILE` | `AUTHFILE` | Checks authorization: `WHERE USER_ID = ? AND RESOURCE = ? AND ACCESS_TYPE = ?` |
| `INSERT INTO AUDITLOG`     | `AUDITLOG` | Logs: timestamp, user ID, terminal ID, transaction ID, program, access type |
| `SQL INCLUDE SQLCA`        | —          | DB2 communication area                                 |

#### Copybook Dependencies

| Copybook | Section         | Purpose                   |
|----------|-----------------|----------------------------|
| `ERRHND` | WORKING-STORAGE | Error handling data area   |

#### COMMAREA Structure (Linkage Section)

```
01  SECURITY-REQUEST-AREA.
    05 SEC-REQUEST-TYPE     PIC X.
       88 SEC-VALIDATE           VALUE 'V'.  -- Validate user identity
       88 SEC-AUTHORIZE          VALUE 'A'.  -- Check authorization
       88 SEC-AUDIT              VALUE 'L'.  -- Log access
    05 SEC-USER-ID          PIC X(8).        -- User to validate
    05 SEC-RESOURCE-NAME    PIC X(8).        -- Resource being accessed
    05 SEC-ACCESS-TYPE      PIC X(8).        -- Type of access (e.g. READ)
    05 SEC-RESPONSE-CODE    PIC S9(8) COMP.  -- 0=success, 8=denied, 12=error
    05 SEC-ERROR-INFO       PIC X(80).       -- Error description
```

#### Operations

| Request | Paragraph          | Behavior                                                              |
|---------|--------------------|-----------------------------------------------------------------------|
| `'V'`   | P100-VALIDATE-USER | `CICS ASSIGN USERID` to get the actual CICS user. Compares with `SEC-USER-ID`. RC=0 on match, RC=8 on mismatch, RC=12 if ASSIGN fails. |
| `'A'`   | P200-CHECK-AUTH    | `SELECT COUNT(*) FROM AUTHFILE` where user, resource, and access type match. RC=0 if count > 0, RC=8 if denied, RC=12 on SQL error. |
| `'L'`   | P300-LOG-ACCESS    | `CICS ASSIGN` to get user/terminal/transaction IDs. `INSERT INTO AUDITLOG`. RC=0 on success, RC=12 on failure. |

#### Response Code Convention

| Code | Meaning                                |
|------|----------------------------------------|
| 0    | Success                                |
| 8    | Access denied / validation failed      |
| 12   | System error (CICS or DB2 failure)     |

---

## 7. Copybook Dependencies

### Online Copybooks (`src/copybook/online/`)

| Copybook   | Used By                               | Purpose                                       |
|------------|---------------------------------------|------------------------------------------------|
| `INQCOM`   | INQONLN, INQPORT, INQHIST            | Online inquiry COMMAREA structure              |
| `ERRHND`   | INQONLN, DB2ONLN, DB2RECV, ERRHNDL, SECMGR | Error handling data area               |
| `DB2REQ`   | DB2RECV                               | DB2 request structure for LINK calls to DB2ONLN |

### Common Copybooks (`src/copybook/common/`)

| Copybook   | Used By   | Purpose                                   |
|------------|-----------|-------------------------------------------|
| `POSREC`   | INQPORT   | VSAM position record layout               |

### DB2 Includes

| Include    | Used By                           | Purpose                   |
|------------|-----------------------------------|---------------------------|
| `SQLCA`    | INQHIST, DB2ONLN, DB2RECV, CURSMGR, ERRHNDL, SECMGR | DB2 SQL communication area |
| `SQLPOS`   | INQPORT                          | DB2 position data structure |

---

## 8. DB2 Tables & SQL Operations

### Tables Used by the Online Layer

| Table              | Source DDL                               | Programs           | Operations              |
|--------------------|------------------------------------------|--------------------|--------------------------|
| `POSHIST`          | `src/database/db2/POSHIST.sql`           | INQHIST (via CURSMGR) | SELECT (cursor-based)  |
| `ERRLOG`           | `src/database/db2/ERRLOG.sql`            | ERRHNDL            | INSERT                   |
| `AUTHFILE`         | (referenced in SECMGR)                   | SECMGR             | SELECT COUNT(*)          |
| `AUDITLOG`         | (referenced in SECMGR)                   | SECMGR             | INSERT                   |

### POSHIST Table Schema

```sql
CREATE TABLE POSHIST (
    ACCOUNT_NO        CHAR(8)         NOT NULL,
    PORTFOLIO_ID      CHAR(10)        NOT NULL,
    TRANS_DATE        DATE            NOT NULL,
    TRANS_TIME        TIME            NOT NULL,
    TRANS_TYPE        CHAR(2)         NOT NULL,     -- BU=Buy, SL=Sell, TR=Transfer
    SECURITY_ID       CHAR(12)        NOT NULL,
    QUANTITY          DECIMAL(15,3)   NOT NULL,
    PRICE             DECIMAL(15,3)   NOT NULL,
    AMOUNT            DECIMAL(15,2)   NOT NULL,
    FEES              DECIMAL(15,2)   NOT NULL WITH DEFAULT 0,
    TOTAL_AMOUNT      DECIMAL(15,2)   NOT NULL,
    COST_BASIS        DECIMAL(15,2)   NOT NULL,
    GAIN_LOSS         DECIMAL(15,2)   NOT NULL,
    PROCESS_DATE      DATE            NOT NULL,
    PROCESS_TIME      TIME            NOT NULL,
    PROGRAM_ID        CHAR(8)         NOT NULL,
    USER_ID           CHAR(8)         NOT NULL,
    AUDIT_TIMESTAMP   TIMESTAMP       NOT NULL WITH DEFAULT
);
-- Partitioned by TRANS_DATE (quarterly)
-- PK: (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
```

### ERRLOG Table Schema

```sql
CREATE TABLE ERRLOG (
    ERROR_TIMESTAMP   TIMESTAMP       NOT NULL,
    PROGRAM_ID        CHAR(8)         NOT NULL,
    ERROR_TYPE        CHAR(1)         NOT NULL,     -- S=System, A=Application, D=Data
    ERROR_SEVERITY    INTEGER         NOT NULL,     -- 1=Info, 2=Warning, 3=Error, 4=Severe
    ERROR_CODE        CHAR(8)         NOT NULL,
    ERROR_MESSAGE     VARCHAR(200)    NOT NULL,
    PROCESS_DATE      DATE            NOT NULL,
    PROCESS_TIME      TIME            NOT NULL,
    USER_ID           CHAR(8)         NOT NULL,
    ADDITIONAL_INFO   VARCHAR(500)
);
-- PK: (ERROR_TIMESTAMP, PROGRAM_ID)
```

### DB2 Plan Configuration

```sql
BIND PLAN PORTPLAN
     PKLIST(*.PORTPKG.*)
     ACTION(REPLACE)
     RETAIN
     VALIDATE(RUN)
     ISOLATION(CS)         -- Cursor Stability
     ACQUIRE(USE)
     RELEASE(COMMIT)
     EXPLAIN(YES);
```

- **Isolation**: Cursor Stability (CS) — locks only the current row during cursor operations
- **Acquire/Release**: USE/COMMIT — acquires locks when needed, releases on COMMIT
- **Plan**: `PORTPLAN` bound to DB2ENTRY `PORTDB2` with USERID-based authentication

---

## 9. Error Handling & Recovery Patterns

### Error Classification

| Severity  | Flag Value | Action Taken    | Example                                |
|-----------|------------|-----------------|----------------------------------------|
| Fatal     | `'F'`      | ABEND (`IERR`)  | ERRLOG insert failure                  |
| Warning   | `'W'`      | Continue        | VSAM record not found, bad user input  |
| Info      | `'I'`      | Continue        | Informational logging                  |
| Other     | —          | Return          | Unclassified errors                    |

### Error Flow Pattern

All programs follow a consistent pattern for error handling:

1. **Caller** populates `ERR-PROGRAM`, `ERR-PARAGRAPH`, `ERR-SQLCODE`/`ERR-CICS-RESP`, and severity
2. **Caller** LINKs to ERRHNDL with the error area as COMMAREA
3. **ERRHNDL** logs to DB2 `ERRLOG`, formats the message, determines the action
4. **Caller** checks `ERR-ACTION`:
   - `ERR-ABEND` → `EXEC CICS ABEND ABCODE('IERR')`
   - `ERR-CONTINUE` → Continue processing with error message
   - `ERR-RETURN` → Return control to the calling program

### DB2 Recovery Pattern

```
Connection Failure                    Transaction Failure
     |                                      |
     v                                      v
  DB2RECV 'C'                           DB2RECV 'T'
     |                                      |
     +-- Retry 1 --> DB2ONLN 'C'        SQL ROLLBACK
     |       |                              |
     |  (2s delay)                     Success/Fail
     |       |
     +-- Retry 2 --> DB2ONLN 'C'
     |       |
     |  (2s delay)
     |       |
     +-- Retry 3 --> DB2ONLN 'C'
     |
     +-- Max retries --> RECV-FAILED

Cursor Failure
     |
     v
  DB2RECV 'R'
     |
     +-- Log via ERRHNDL
     |
     +-- ERR-CONTINUE? --> RECV-RETRY
     +-- ERR-ABEND?    --> RECV-FAILED
```

### Trace ID Pattern

Every error is tagged with a unique trace ID (`ERR-TRACE-ID`, 16 characters) generated via `FUNCTION RANDOM`. This trace ID is:
- Included in the formatted error message
- Stored in the ERRLOG DB2 table
- Available for cross-referencing errors across programs within a single request chain

---

## 10. Security Model

### Three-Phase Security Check

INQONLN enforces security on every request through a three-phase check in `P050-SECURITY-CHECK`:

```
Phase 1: VALIDATE ('V')                Phase 2: AUTHORIZE ('A')              Phase 3: AUDIT ('L')
   |                                       |                                     |
   | CICS ASSIGN USERID                    | SELECT COUNT(*)                     | CICS ASSIGN USERID,
   | Compare SEC-USER-ID                   | FROM AUTHFILE                       |   TERMID, TRANSID
   | with actual CICS user                 | WHERE USER_ID = ?                   | INSERT INTO AUDITLOG
   |                                       |   AND RESOURCE = ?                  |
   v                                       |   AND ACCESS_TYPE = ?               v
RC=0: Match                                v                                 RC=0: Logged
RC=8: Mismatch                          RC=0: Authorized (count > 0)         RC=12: Log failed
RC=12: ASSIGN failed                    RC=8: Denied (count = 0)
                                        RC=12: SQL error
```

### Authorization Model

- **AUTHFILE** table stores access control entries with columns: `USER_ID`, `RESOURCE`, `ACCESS_TYPE`
- Authorization is checked at the program level (e.g., resource=`'INQONLN'`, access=`'READ'`)
- The model supports fine-grained access control per user, resource, and access type

### Audit Trail

- **AUDITLOG** table records every validated access with: timestamp, user ID, terminal ID, transaction ID, program name, and access type
- Captures CICS session context (terminal and transaction IDs) for complete traceability
- Audit logging occurs only after successful validation and authorization

### Security Failure Handling

If any security phase fails:
1. `SEC-ERROR-INFO` is copied to `WS-ERROR-MESSAGE`
2. `P900-ERROR-ROUTINE` is invoked (which LINKs to ERRHNDL)
3. `EXEC CICS RETURN` terminates the request

This ensures that unauthorized or unvalidated requests cannot proceed to access portfolio data.
