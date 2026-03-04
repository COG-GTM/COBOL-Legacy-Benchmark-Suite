"""
Business rule validators extracted from COBOL source programs.

These functions encode the validation and processing logic found in the
Investment Portfolio Management System COBOL programs (PORTVALD.cbl,
PORTMSTR.cbl, PORTTRAN.cbl, SECMGR.cbl, PRCSEQ00.cbl, HISTLD00.cbl,
RPTPOS00.cbl, AUDPROC.cbl).

They serve as an executable specification of the expected behavior and can
be used as acceptance criteria when translating the COBOL to a modern language.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple


# ---------------------------------------------------------------------------
# Constants (mirroring COBOL copybook values)
# ---------------------------------------------------------------------------
VALID_INVESTMENT_TYPES = {"STK", "BND", "MMF", "ETF"}
VALID_TRANSACTION_TYPES = {"BU", "SL", "TR", "FE"}
VALID_PORTFOLIO_STATUSES = {"A", "I", "C"}
VALID_CLIENT_TYPES = {"I", "C", "T"}
VALID_TRANSACTION_STATUSES = {"P", "D", "F", "R"}

AMOUNT_MIN = -9999999999999.99
AMOUNT_MAX = 9999999999999.99

ERROR_THRESHOLD = 100
COMMIT_THRESHOLD = 1000

AUDIT_ACTION_MAP = {
    "BU": "CREATE",
    "SL": "DELETE",
    "TR": "UPDATE",
    "FE": "UPDATE",
}

# Batch job expected sequence
BATCH_JOB_SEQUENCE = ["TRNVAL00", "POSUPD00", "HISTLD00", "REPORTS"]


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------
@dataclass
class Portfolio:
    portfolio_id: str = ""
    account_number: str = ""
    name: str = ""
    status: str = ""
    client_type: str = ""
    investment_type: str = ""
    total_units: float = 0.0
    total_cost: float = 0.0


@dataclass
class Transaction:
    transaction_type: str = ""
    portfolio_id: str = ""
    quantity: float = 0.0
    price: float = 0.0
    amount: float = 0.0
    status: str = "P"


@dataclass
class ValidationResult:
    valid: bool
    error_message: str = ""


@dataclass
class ProcessingResult:
    success: bool
    error_message: str = ""
    portfolio: Optional[Portfolio] = None


@dataclass
class AuditRecord:
    timestamp: str = ""
    program: str = ""
    user_id: str = ""
    transaction_type: str = ""
    action: str = ""
    status: str = ""
    portfolio_id: str = ""
    account_number: str = ""
    before_image: str = ""
    message: str = ""


@dataclass
class BatchProcess:
    process_id: str = ""
    status: str = "R"  # R=Ready, A=Active, D=Done, E=Error
    return_code: int = 0
    dependencies: List[str] = field(default_factory=list)
    dependency_types: Dict[str, str] = field(default_factory=dict)  # dep_id -> "hard"/"soft"
    dependency_rc_thresholds: Dict[str, int] = field(default_factory=dict)


@dataclass
class SecurityRequest:
    user_id: str = ""
    session_user_id: str = ""
    resource_name: str = ""
    access_type: str = ""


# ---------------------------------------------------------------------------
# Portfolio Validation (from PORTVALD.cbl and PORTMSTR.cbl)
# ---------------------------------------------------------------------------
def validate_portfolio_id(portfolio_id: str) -> ValidationResult:
    """
    Portfolio ID must start with 'PORT' followed by exactly 4 numeric digits.
    Reference: PORTVALD.cbl 1000-VALIDATE-ID, PORTMSTR.cbl 2100-VALIDATE-PORTFOLIO
    """
    if len(portfolio_id) != 8:
        return ValidationResult(False, "Invalid Portfolio ID format")
    if portfolio_id[:4] != "PORT":
        return ValidationResult(False, "Invalid Portfolio ID format")
    if not portfolio_id[4:8].isdigit():
        return ValidationResult(False, "Invalid Portfolio ID format")
    return ValidationResult(True)


def validate_account_number(account_number: str) -> ValidationResult:
    """
    Account number must be exactly 10 numeric digits and cannot be all zeros.
    Reference: PORTVALD.cbl 2000-VALIDATE-ACCOUNT
    """
    if len(account_number) != 10:
        return ValidationResult(False, "Invalid Account Number")
    if not account_number.isdigit():
        return ValidationResult(False, "Invalid Account Number")
    if account_number == "0000000000":
        return ValidationResult(False, "Invalid Account Number")
    return ValidationResult(True)


def validate_investment_type(investment_type: str) -> ValidationResult:
    """
    Investment type must be one of STK, BND, MMF, ETF (case-sensitive).
    Reference: PORTVALD.cbl 3000-VALIDATE-TYPE
    """
    if investment_type not in VALID_INVESTMENT_TYPES:
        return ValidationResult(False, "Invalid Investment Type")
    return ValidationResult(True)


def validate_portfolio_name(name: str) -> ValidationResult:
    """
    Portfolio name is required and cannot be blank or all spaces.
    Reference: PORTMSTR.cbl 2100-VALIDATE-PORTFOLIO
    """
    if not name or name.strip() == "":
        return ValidationResult(False, "Portfolio Name is required")
    return ValidationResult(True)


def validate_portfolio_status(status: str) -> ValidationResult:
    """
    Portfolio status must be A (Active), I (Inactive), or C (Closed).
    Reference: PORTMSTR.cbl WS-VALID-STATUS 88-level values
    """
    if status not in VALID_PORTFOLIO_STATUSES:
        return ValidationResult(False, "Invalid Portfolio Status")
    return ValidationResult(True)


def validate_client_type(client_type: str) -> ValidationResult:
    """
    Client type must be I (Individual), C (Corporate), or T (Trust).
    """
    if client_type not in VALID_CLIENT_TYPES:
        return ValidationResult(False, "Invalid Client Type")
    return ValidationResult(True)


def validate_amount_range(amount: float) -> ValidationResult:
    """
    Amount must be between -9999999999999.99 and +9999999999999.99.
    Reference: PORTVALD.cbl 4000-VALIDATE-AMOUNT
    """
    if amount < AMOUNT_MIN or amount > AMOUNT_MAX:
        return ValidationResult(False, "Amount out of valid range")
    return ValidationResult(True)


# ---------------------------------------------------------------------------
# Transaction Validation (from PORTTRAN.cbl)
# ---------------------------------------------------------------------------
def validate_transaction_type(transaction_type: str) -> ValidationResult:
    """
    Transaction type must be BU, SL, TR, or FE (case-sensitive).
    Reference: PORTTRAN.cbl 2120-CHECK-TRANSACTION-TYPE
    """
    if transaction_type not in VALID_TRANSACTION_TYPES:
        return ValidationResult(
            False, f"Invalid Transaction Type: {transaction_type}"
        )
    return ValidationResult(True)


def validate_transaction_portfolio_id(
    portfolio_id: str, master_file: Dict[str, Portfolio]
) -> ValidationResult:
    """
    Portfolio ID is required (not blank) and must exist in the master file.
    Reference: PORTTRAN.cbl 2110-CHECK-PORTFOLIO
    """
    if not portfolio_id or portfolio_id.strip() == "":
        return ValidationResult(False, "Portfolio ID is required")
    if portfolio_id not in master_file:
        return ValidationResult(
            False, f"Invalid Portfolio ID: {portfolio_id}"
        )
    return ValidationResult(True)


def validate_transaction_quantity(quantity: float) -> ValidationResult:
    """
    Quantity must be greater than zero for all transaction types.
    Reference: PORTTRAN.cbl 2130-CHECK-AMOUNTS
    """
    if quantity <= 0:
        return ValidationResult(False, "Quantity must be greater than zero")
    return ValidationResult(True)


def validate_transaction_price(
    price: float, transaction_type: str
) -> ValidationResult:
    """
    Price must be > 0 for all types except TR (transfers).
    Reference: PORTTRAN.cbl 2130-CHECK-AMOUNTS
    """
    if price <= 0 and transaction_type != "TR":
        return ValidationResult(False, "Price must be greater than zero")
    return ValidationResult(True)


def validate_transaction_amount(
    amount: float, transaction_type: str
) -> ValidationResult:
    """
    Amount must be > 0 for all types except TR (transfers).
    Reference: PORTTRAN.cbl 2130-CHECK-AMOUNTS
    """
    if amount <= 0 and transaction_type != "TR":
        return ValidationResult(False, "Amount must be greater than zero")
    return ValidationResult(True)


def validate_transaction_status(status: str) -> ValidationResult:
    """
    Transaction status must be P (Pending), D (Done), F (Failed), or R (Reversed).
    """
    if status not in VALID_TRANSACTION_STATUSES:
        return ValidationResult(False, "Invalid Transaction Status")
    return ValidationResult(True)


# ---------------------------------------------------------------------------
# Transaction Processing (from PORTTRAN.cbl)
# ---------------------------------------------------------------------------
def process_buy(
    portfolio: Portfolio, transaction: Transaction
) -> ProcessingResult:
    """
    Buy: add quantity to total units, add amount to total cost.
    Reference: PORTTRAN.cbl 2210-PROCESS-BUY
    """
    updated = Portfolio(
        portfolio_id=portfolio.portfolio_id,
        account_number=portfolio.account_number,
        name=portfolio.name,
        status=portfolio.status,
        client_type=portfolio.client_type,
        investment_type=portfolio.investment_type,
        total_units=portfolio.total_units + transaction.quantity,
        total_cost=portfolio.total_cost + transaction.amount,
    )
    return ProcessingResult(success=True, portfolio=updated)


def process_sell(
    portfolio: Portfolio, transaction: Transaction
) -> ProcessingResult:
    """
    Sell: subtract quantity from total units; reject if insufficient units.
    Reference: PORTTRAN.cbl 2220-PROCESS-SELL
    """
    if portfolio.total_units < transaction.quantity:
        return ProcessingResult(
            success=False, error_message="Insufficient units for sale"
        )
    updated = Portfolio(
        portfolio_id=portfolio.portfolio_id,
        account_number=portfolio.account_number,
        name=portfolio.name,
        status=portfolio.status,
        client_type=portfolio.client_type,
        investment_type=portfolio.investment_type,
        total_units=portfolio.total_units - transaction.quantity,
        total_cost=portfolio.total_cost - transaction.amount,
    )
    return ProcessingResult(success=True, portfolio=updated)


def process_fee(
    portfolio: Portfolio, transaction: Transaction
) -> ProcessingResult:
    """
    Fee: subtract fee amount from total cost; units remain unchanged.
    Reference: PORTTRAN.cbl 2240-PROCESS-FEE
    """
    updated = Portfolio(
        portfolio_id=portfolio.portfolio_id,
        account_number=portfolio.account_number,
        name=portfolio.name,
        status=portfolio.status,
        client_type=portfolio.client_type,
        investment_type=portfolio.investment_type,
        total_units=portfolio.total_units,
        total_cost=portfolio.total_cost - transaction.amount,
    )
    return ProcessingResult(success=True, portfolio=updated)


def process_transfer(
    portfolio: Portfolio, transaction: Transaction
) -> ProcessingResult:
    """
    Transfer: always returns error 'Transfer processing not implemented'.
    Reference: PORTTRAN.cbl 2230-PROCESS-TRANSFER
    """
    return ProcessingResult(
        success=False,
        error_message="Transfer processing not implemented",
    )


def should_halt_processing(error_count: int) -> bool:
    """
    Processing halts when error count exceeds 100.
    Reference: PORTTRAN.cbl 0000-MAIN (WS-ERROR-COUNT > 100)
    """
    return error_count > ERROR_THRESHOLD


# ---------------------------------------------------------------------------
# Batch Processing (from PRCSEQ00.cbl, HISTLD00.cbl, BCHCTL00.cbl)
# ---------------------------------------------------------------------------
def check_dependency(
    dependency: BatchProcess,
    is_hard: bool,
    rc_threshold: int,
) -> ValidationResult:
    """
    A process cannot start if hard dependencies haven't completed.
    If dependency completed but RC > threshold, return error.
    Reference: PRCSEQ00.cbl 2210-CHECK-DEP-STATUS
    """
    if dependency.status != "D":
        if is_hard:
            return ValidationResult(False, "Hard dependency not completed")
        return ValidationResult(True)
    if dependency.return_code > rc_threshold:
        return ValidationResult(
            False,
            f"Dependency RC {dependency.return_code} exceeds threshold {rc_threshold}",
        )
    return ValidationResult(True)


def validate_batch_sequence(
    sequence: List[str],
) -> ValidationResult:
    """
    Validate expected batch job sequence: TRNVAL00 -> POSUPD00 -> HISTLD00 -> REPORTS.
    Each step requires RC <= 4 from the previous step.
    """
    if sequence != BATCH_JOB_SEQUENCE:
        return ValidationResult(
            False,
            f"Invalid batch sequence. Expected {BATCH_JOB_SEQUENCE}, got {sequence}",
        )
    return ValidationResult(True)


def check_batch_step_prerequisite(
    previous_rc: int, max_rc: int = 4
) -> ValidationResult:
    """
    Each batch step requires that the previous step completed with RC <= max_rc.
    """
    if previous_rc > max_rc:
        return ValidationResult(
            False,
            f"Previous step RC {previous_rc} exceeds maximum allowed {max_rc}",
        )
    return ValidationResult(True)


def should_commit(records_processed: int, threshold: int = COMMIT_THRESHOLD) -> bool:
    """
    DB2 commit occurs every `threshold` records (default 1000).
    Reference: HISTLD00.cbl 2300-CHECK-COMMIT
    """
    return records_processed > 0 and records_processed % threshold == 0


def handle_sqlcode(sqlcode: int) -> Tuple[bool, bool]:
    """
    Handle SQLCODE from DB2 insert.
    Returns (success, is_error).
    SQLCODE 0 = success, -803 = duplicate (silently skipped), other = error.
    Reference: HISTLD00.cbl 2200-LOAD-TO-DB2
    """
    if sqlcode == 0:
        return True, False
    if sqlcode == -803:
        return False, False  # duplicate, silently skipped
    return False, True  # real error


# ---------------------------------------------------------------------------
# Security (from SECMGR.cbl)
# ---------------------------------------------------------------------------
def validate_user(
    request_user_id: str, session_user_id: str
) -> Tuple[int, str]:
    """
    User ID must match the CICS session user; mismatch returns RC=8.
    Reference: SECMGR.cbl P100-VALIDATE-USER
    """
    if request_user_id == session_user_id:
        return 0, ""
    return 8, "User validation failed"


def check_authorization(
    user_id: str,
    resource_name: str,
    access_type: str,
    auth_entries: List[Dict[str, str]],
) -> Tuple[int, str]:
    """
    User must have a matching entry in AUTHFILE for the resource and access type.
    No match returns RC=8 'Access denied'.
    Reference: SECMGR.cbl P200-CHECK-AUTH
    """
    for entry in auth_entries:
        if (
            entry.get("user_id") == user_id
            and entry.get("resource") == resource_name
            and entry.get("access_type") == access_type
        ):
            return 0, ""
    return 8, "Access denied"


def run_security_pipeline(
    request_user_id: str,
    session_user_id: str,
    resource_name: str,
    access_type: str,
    auth_entries: List[Dict[str, str]],
    audit_success: bool = True,
) -> Tuple[int, str, List[str]]:
    """
    Three-phase security: Validate -> Authorize -> Log.
    Failure at any phase stops processing.
    Reference: SECMGR.cbl main EVALUATE block
    Returns (rc, error_message, completed_phases).
    """
    completed_phases: List[str] = []

    # Phase 1: Validate
    rc, msg = validate_user(request_user_id, session_user_id)
    if rc != 0:
        return rc, msg, completed_phases
    completed_phases.append("validate")

    # Phase 2: Authorize
    rc, msg = check_authorization(
        request_user_id, resource_name, access_type, auth_entries
    )
    if rc != 0:
        return rc, msg, completed_phases
    completed_phases.append("authorize")

    # Phase 3: Log (audit)
    if audit_success:
        completed_phases.append("log")
        return 0, "", completed_phases
    return 12, "Audit logging failed", completed_phases


# ---------------------------------------------------------------------------
# Reporting (from RPTPOS00.cbl)
# ---------------------------------------------------------------------------
def calculate_position_change_pct(
    current_value: float, previous_value: float
) -> Optional[float]:
    """
    Position change percentage = (current - previous) / previous * 100.
    Returns None if previous_value is zero (division by zero guard).
    Reference: RPTPOS00.cbl 2110-FORMAT-POSITION
    """
    if previous_value == 0:
        return None
    return (current_value - previous_value) / previous_value * 100


# ---------------------------------------------------------------------------
# Audit Trail (from PORTTRAN.cbl 2300-UPDATE-AUDIT-TRAIL)
# ---------------------------------------------------------------------------
def map_transaction_to_audit_action(transaction_type: str) -> str:
    """
    Map transaction type to audit action.
    BU -> CREATE, SL -> DELETE, TR -> UPDATE, FE -> UPDATE.
    Reference: PORTTRAN.cbl 2300-UPDATE-AUDIT-TRAIL
    """
    return AUDIT_ACTION_MAP.get(transaction_type, "")


def map_file_status_to_audit_status(file_status: str) -> str:
    """
    File status '00' maps to 'SUCC', anything else maps to 'FAIL'.
    Reference: PORTTRAN.cbl 2300-UPDATE-AUDIT-TRAIL
    """
    if file_status == "00":
        return "SUCC"
    return "FAIL"


def build_audit_record(
    timestamp: str,
    program: str,
    user_id: str,
    transaction_type: str,
    portfolio_id: str,
    account_number: str,
    before_image: str,
    amount: float,
    units: float,
) -> AuditRecord:
    """
    Build an audit record with all required fields.
    Reference: PORTTRAN.cbl 2300-UPDATE-AUDIT-TRAIL
    """
    action = map_transaction_to_audit_action(transaction_type)
    message = f"Transaction: {transaction_type} Amount: {amount} Units: {units}"
    return AuditRecord(
        timestamp=timestamp,
        program=program,
        user_id=user_id,
        transaction_type=transaction_type,
        action=action,
        status="",  # set later based on file status
        portfolio_id=portfolio_id,
        account_number=account_number,
        before_image=before_image,
        message=message,
    )


def handle_audit_write_failure(audproc_rc: int) -> bool:
    """
    If AUDPROC returns non-zero, an error routine is invoked.
    Returns True if error routine should be invoked.
    Reference: PORTTRAN.cbl 2310-WRITE-AUDIT-RECORD
    """
    return audproc_rc != 0
