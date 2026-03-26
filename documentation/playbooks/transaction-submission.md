# Playbook: Transaction Submission Form

## Introduction

This playbook covers the implementation of a Transaction Submission page for the modernized Portfolio Management System web UI. In the legacy COBOL system, transactions are submitted via a sequential flat file (TRANFILE) and processed through a nightly batch pipeline: TRNVAL00 (validation) → POSUPD00 (position update) → HISTLD00 (history load to DB2). There is no online transaction entry — users currently depend on back-office file preparation. This playbook creates a frontend-only transaction entry form with client-side validation that mirrors the legacy TRNVAL00 rules, a confirmation step, and a status tracking view. The backend integration will be added later.

Reference files for pipeline context:
- `src/copybook/batch/PRCSEQ.cpy` (lines 68-71) — batch sequence definition
- `documentation/technical/data-dictionary.md` (lines 274-276) — pipeline documentation

## Business Value

- **Self-service transaction entry**: Eliminates the dependency on back-office teams to prepare flat files, reducing turnaround from next-day batch to near-real-time submission.
- **Shift-left validation**: Client-side validation catches errors (invalid account, bad transaction type, negative quantities) before submission, reducing the reject rate that currently shows up only after the nightly TRNVAL00 run.
- **Audit trail readiness**: The form captures all fields from the legacy TRANSACTION-RECORD copybook (TRNREC.cpy), ensuring no data loss when the backend is wired up.
- **User confidence**: A confirmation step and status tracking view give users visibility into their submissions, replacing the opaque batch process where status was only visible via the next day's error report.

## Scope

**In scope:**
- Transaction entry form (BUY, SELL, TRANSFER, FEE)
- Client-side validation mirroring TRNVAL00 rules
- Confirmation dialog before submission
- Transaction status tracking page (mock data)
- Mock JSON data layer matching TRNREC.cpy structure

**Out of scope:**
- Backend API integration
- Actual batch pipeline execution
- Position balance checks (requires live VSAM/DB2 data)
- Authentication/authorization (covered by a separate playbook)

## Technical Context

### Legacy Data Structures

Reference: `src/copybook/common/TRNREC.cpy` (lines 6-31) — the core Transaction Record structure.

Key field mappings from COBOL to form inputs:

| COBOL Field | Type | Form Field | Notes |
|---|---|---|---|
| TRN-TYPE (PIC X(02)) | Dropdown | Transaction Type | BU=Buy, SL=Sell, TR=Transfer, FE=Fee |
| TRN-PORTFOLIO-ID (PIC X(08)) | Text input | Portfolio ID | 8-char alphanumeric |
| TRN-INVESTMENT-ID (PIC X(10)) | Text input | Security/Fund ID | 10-char alphanumeric |
| TRN-QUANTITY (PIC S9(11)V9(4)) | Number input | Quantity | Signed, 4 decimal places |
| TRN-PRICE (PIC S9(11)V9(4)) | Number input | Price | Signed, 4 decimal places |
| TRN-AMOUNT (PIC S9(13)V9(2)) | Calculated | Amount | Auto-calculated: Qty × Price |
| TRN-CURRENCY (PIC X(03)) | Dropdown | Currency | 3-char ISO code |
| TRN-STATUS (PIC X(01)) | Badge | Status | P=Pending, D=Done, F=Failed, R=Reversed |

### Validation Rules (from TRNVAL00)

Reference: `documentation/technical/data-dictionary.md` (lines 237-252) — Section 5.1 Transaction Validation:
1. Account Number must be numeric and exist in customer master
2. Fund ID must exist in fund master
3. Transaction Date must not be future date
4. Share Quantity must not be zero for BUY/SELL
5. Amount must be non-zero for FEE
6. Price must be greater than zero for BUY/SELL

Reference: `src/copybook/common/PORTVAL.cpy` (lines 11-29) — Validation Return Codes:
- +0 VAL-SUCCESS
- +1 VAL-INVALID-ID (Invalid Portfolio ID format)
- +2 VAL-INVALID-ACCT (Invalid Account Number format)
- +3 VAL-INVALID-TYPE (Invalid Investment Type)
- +4 VAL-INVALID-AMT (Amount outside valid range)

Reference: `documentation/technical/data-dictionary.md` (lines 255-262) — Error Codes:
- E001: Invalid Account Number (Reject)
- E002: Invalid Fund ID (Reject)
- E003: Invalid Transaction Type (Reject)
- E004: Insufficient Position Balance (Reject)
- W001: Zero Dollar Transaction (Warning - Process)
- W002: Duplicate Transaction ID (Warning - Log)

### Batch Pipeline Context

Reference: `documentation/technical/data-dictionary.md` (lines 296-305):
- TRNVAL00 → POSUPD00 → HISTLD00 → RPTGEN00 (daily 1800-2000)
- Transaction statuses map: Pending (P) → Done (D) or Failed (F), with Reversed (R) for post-processing

## Implementation Steps

### Step 1: Transaction Form Component
Create a transaction entry form with conditional fields by type:
- Common fields: Transaction Type dropdown, Account Number (9-digit numeric), Portfolio ID (8-char), Transaction Date (date picker, default today), Currency dropdown (default USD)
- BUY/SELL: Security/Fund ID (10-char), Quantity (positive, 4 decimals), Price (positive, 4 decimals), Amount (auto-calculated, read-only)
- TRANSFER: Source Account, Destination Account, Security/Fund ID, Quantity
- FEE: Amount (required, non-zero), Description

### Step 2: Client-Side Validation
Implement validation on blur and on submit:
- Account format: /^\d{9}$/ and >= 100000000 → E001
- Portfolio ID: /^[A-Z0-9]{8}$/ → VAL-INVALID-ID
- Security/Fund ID: /^[A-Z0-9]{10}$/ → E002
- Transaction type: must be BU/SL/TR/FE → E003
- Quantity > 0 for BUY/SELL
- Price > 0 for BUY/SELL
- Amount != 0 for FEE
- Date <= today
- Zero dollar warning (W001) allows submission

### Step 3: Confirmation Dialog
After validation passes, show confirmation modal with read-only summary of all fields, calculated amount, generated Transaction ID (YYYYMMDD + 4-digit sequence), Confirm & Submit / Edit buttons.

### Step 4: Transaction Status Tracking Page
Table with columns: Transaction ID, Date, Type, Account, Fund ID, Quantity, Price, Amount, Status. Status badges: Pending (yellow/P), Validated (blue), Processed (green/D), Rejected (red/F), Reversed (gray/R). Features: filter by status/date/account, sort all columns, click for detail, pagination (10 per page).

### Step 5: Mock Data Layer
Create mock JSON fixtures matching TRNREC.cpy structure. Seed with ~20 mock transactions across all types and statuses.

### Step 6: Navigation Integration
Add "Submit Transaction" to main nav. Add "Transaction Status" as sub-nav/tab. Wire confirmation success to redirect to status tracking with new transaction highlighted.

## Acceptance Criteria

1. User can select a transaction type and see only relevant fields for that type
2. All client-side validation rules fire on blur and on submit with inline error messages matching legacy error codes
3. Zero-dollar transactions show warning (W001) but allow submission
4. Confirmation dialog shows complete read-only summary before final submission
5. Submitted transactions appear in status tracking table with Pending status
6. Status tracking table supports filtering by status, sorting by all columns, and pagination
7. TRANSFER type shows both source and destination account fields
8. FEE type requires non-zero amount and hides quantity/price fields
9. Amount is auto-calculated (Qty × Price) for BUY/SELL and is read-only
10. Transaction Date defaults to today and rejects future dates
11. All mock data structures match the TRNREC.cpy field definitions
