"""
Audit trail logging for the Investment Portfolio Management System.

Migrated from COBOL sources:
  - src/programs/common/AUDPROC.cbl   (Audit trail processing subroutine)
  - src/copybook/common/AUDITLOG.cpy  (Audit trail record definitions)

Key COBOL patterns preserved:
  - AUDPROC.cbl:  0000-MAIN -> 1000-INITIALIZE -> 2000-PROCESS-AUDIT
                  -> 3000-TERMINATE
  - AUDPROC.cbl:  LINKAGE SECTION fields (LS-AUDIT-REQUEST) define the
                  audit record interface
  - AUDITLOG.cpy: AUDIT-RECORD layout with header, type, action, status,
                  key-info, before/after images, and message
  - AUDITLOG.cpy: Level-88 condition values for type, action, and status
"""

from __future__ import annotations

import functools
import inspect
import logging
from contextlib import contextmanager
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Callable, Generator, TypeVar

from .constants import (
    AuditAction,
    AuditStatus,
    AuditType,
    DEFAULT_SYSTEM_ID,
    MAX_AUDIT_MESSAGE_LENGTH,
)

logger = logging.getLogger("portfolio.audit")

F = TypeVar("F", bound=Callable[..., Any])


# ============================================================
# Audit Record — mirrors AUDITLOG.cpy AUDIT-RECORD
# ============================================================

@dataclass
class AuditRecord:
    """A single audit trail entry.

    Field mapping from AUDITLOG.cpy::

        AUD-HEADER
            AUD-TIMESTAMP     -> timestamp
            AUD-SYSTEM-ID     -> system_id
            AUD-USER-ID       -> user_id
            AUD-PROGRAM       -> program
            AUD-TERMINAL      -> terminal_id
        AUD-TYPE              -> audit_type
        AUD-ACTION            -> action
        AUD-STATUS            -> status
        AUD-KEY-INFO
            AUD-PORTFOLIO-ID  -> entity_id
            AUD-ACCOUNT-NO    -> account_no
        AUD-BEFORE-IMAGE      -> old_value
        AUD-AFTER-IMAGE       -> new_value
        AUD-MESSAGE           -> message
    """

    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    system_id: str = DEFAULT_SYSTEM_ID
    user_id: str = ""
    program: str = ""
    terminal_id: str = ""
    audit_type: AuditType = AuditType.USER_ACTION
    action: AuditAction = AuditAction.READ
    status: AuditStatus = AuditStatus.SUCCESS
    entity_type: str = ""
    entity_id: str = ""
    account_no: str = ""
    old_value: str = ""
    new_value: str = ""
    message: str = ""
    details: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        """Serialize the audit record to a dictionary."""
        return {
            "timestamp": self.timestamp.isoformat(),
            "system_id": self.system_id,
            "user_id": self.user_id,
            "program": self.program,
            "terminal_id": self.terminal_id,
            "audit_type": self.audit_type.value,
            "action": self.action.value,
            "status": self.status.value,
            "entity_type": self.entity_type,
            "entity_id": self.entity_id,
            "account_no": self.account_no,
            "old_value": self.old_value,
            "new_value": self.new_value,
            "message": self.message[:MAX_AUDIT_MESSAGE_LENGTH],
            "details": self.details,
        }


# ============================================================
# AuditLogger — main audit processing class
# ============================================================

class AuditLogger:
    """Audit trail logger replacing AUDPROC.cbl.

    Preserves the COBOL subroutine flow::

        0000-MAIN
            PERFORM 1000-INITIALIZE      -> __init__ / _initialize
            PERFORM 2000-PROCESS-AUDIT   -> log_action / log_login / etc.
            PERFORM 3000-TERMINATE       -> (handled by context manager)

    The audit logger writes records via the Python logging subsystem.
    In production, a logging handler can route audit records to a database
    (mirroring how ERRHNDL.cbl inserts into DB2) or to a file (mirroring
    how AUDPROC.cbl writes to AUDIT-FILE).

    Usage::

        audit = AuditLogger(system_id="PORTMGMT")

        # Direct logging
        audit.log_action(
            user="JDOE",
            action=AuditAction.UPDATE,
            entity_type="PORTFOLIO",
            entity_id="PORT0001",
            old_value="100.00",
            new_value="150.00",
        )

        # As context manager
        with audit.track("JDOE", AuditAction.UPDATE, "PORTFOLIO", "PORT0001"):
            ...  # operation

        # As decorator
        @audit.audited(action=AuditAction.CREATE, entity_type="PORTFOLIO")
        def create_portfolio(user: str, portfolio_id: str, ...):
            ...
    """

    def __init__(
        self,
        system_id: str = DEFAULT_SYSTEM_ID,
        program: str = "",
        terminal_id: str = "",
    ) -> None:
        self._system_id = system_id
        self._program = program
        self._terminal_id = terminal_id

    # ----------------------------------------------------------
    # Core audit methods (mirrors AUDPROC.cbl 2000-PROCESS-AUDIT)
    # ----------------------------------------------------------

    def log_action(
        self,
        user: str,
        action: AuditAction,
        entity_type: str,
        entity_id: str,
        old_value: str = "",
        new_value: str = "",
        details: str = "",
        status: AuditStatus = AuditStatus.SUCCESS,
        account_no: str = "",
    ) -> AuditRecord:
        """Record an audit trail entry for a data action.

        Mirrors AUDPROC.cbl 2000-PROCESS-AUDIT which populates the
        AUDIT-RECORD fields from the LS-AUDIT-REQUEST linkage section::

            MOVE WS-FORMATTED-TIME  TO AUD-TIMESTAMP
            MOVE LS-SYSTEM-INFO     TO AUD-HEADER
            MOVE LS-TYPE            TO AUD-TYPE
            MOVE LS-ACTION          TO AUD-ACTION
            MOVE LS-STATUS          TO AUD-STATUS
            MOVE LS-KEY-INFO        TO AUD-KEY-INFO
            MOVE LS-BEFORE-IMAGE    TO AUD-BEFORE-IMAGE
            MOVE LS-AFTER-IMAGE     TO AUD-AFTER-IMAGE
            MOVE LS-MESSAGE         TO AUD-MESSAGE

        Args:
            user: User performing the action (maps to AUD-USER-ID).
            action: The audit action (maps to AUD-ACTION).
            entity_type: Type of entity affected (e.g. "PORTFOLIO").
            entity_id: Identifier of affected entity (maps to AUD-PORTFOLIO-ID).
            old_value: Before-image of the data (maps to AUD-BEFORE-IMAGE).
            new_value: After-image of the data (maps to AUD-AFTER-IMAGE).
            details: Free-text message (maps to AUD-MESSAGE).
            status: Outcome status (maps to AUD-STATUS).
            account_no: Account number (maps to AUD-ACCOUNT-NO).

        Returns:
            The :class:`AuditRecord` that was logged.
        """
        record = AuditRecord(
            system_id=self._system_id,
            user_id=user,
            program=self._program,
            terminal_id=self._terminal_id,
            audit_type=AuditType.USER_ACTION,
            action=action,
            status=status,
            entity_type=entity_type,
            entity_id=entity_id,
            account_no=account_no,
            old_value=old_value,
            new_value=new_value,
            message=details,
        )
        self._write_audit(record)
        return record

    def log_login(
        self,
        user: str,
        success: bool,
        ip_address: str = "",
        details: str = "",
    ) -> AuditRecord:
        """Record an authentication event.

        Maps to AUDITLOG.cpy AUD-ACTION level-88 values:
          AUD-LOGIN  = 'LOGIN   '
          AUD-LOGOUT = 'LOGOUT  '

        Args:
            user: User attempting authentication (maps to AUD-USER-ID).
            success: Whether authentication succeeded.
            ip_address: Client IP address (stored in terminal_id field,
                replacing the COBOL AUD-TERMINAL concept).
            details: Additional information.

        Returns:
            The :class:`AuditRecord` that was logged.
        """
        record = AuditRecord(
            system_id=self._system_id,
            user_id=user,
            program=self._program,
            terminal_id=ip_address or self._terminal_id,
            audit_type=AuditType.USER_ACTION,
            action=AuditAction.LOGIN,
            status=AuditStatus.SUCCESS if success else AuditStatus.FAILURE,
            entity_type="SESSION",
            entity_id=user,
            message=details,
        )
        self._write_audit(record)
        return record

    def log_batch_event(
        self,
        batch_id: str,
        step: str,
        status: AuditStatus = AuditStatus.SUCCESS,
        details: str = "",
    ) -> AuditRecord:
        """Record a batch processing event.

        Maps to AUDITLOG.cpy AUD-TYPE level-88:
          AUD-SYSTEM-EVENT = 'SYST'
        And AUD-ACTION level-88:
          AUD-STARTUP  = 'STARTUP '
          AUD-SHUTDOWN = 'SHUTDOWN'

        Args:
            batch_id: Identifier of the batch job.
            step: Current processing step or phase name.
            status: Outcome status.
            details: Additional information.

        Returns:
            The :class:`AuditRecord` that was logged.
        """
        # Determine action from step name
        step_upper = step.upper()
        if step_upper in ("START", "STARTUP", "BEGIN"):
            action = AuditAction.BATCH_START
        elif step_upper in ("END", "SHUTDOWN", "COMPLETE"):
            action = AuditAction.BATCH_END
        else:
            # Mid-process steps (PROCESS, CHECKPOINT, etc.) use CREATE
            # as a neutral action to avoid misrepresenting as STARTUP
            action = AuditAction.CREATE

        record = AuditRecord(
            system_id=self._system_id,
            user_id="BATCH",
            program=self._program,
            terminal_id=self._terminal_id,
            audit_type=AuditType.SYSTEM_EVENT,
            action=action,
            status=status,
            entity_type="BATCH",
            entity_id=batch_id,
            message=f"[{step}] {details}".strip(),
            details={"batch_id": batch_id, "step": step},
        )
        self._write_audit(record)
        return record

    # ----------------------------------------------------------
    # Context Manager — automatic audit trail on operations
    # ----------------------------------------------------------

    @contextmanager
    def track(
        self,
        user: str,
        action: AuditAction,
        entity_type: str,
        entity_id: str,
        account_no: str = "",
    ) -> Generator[AuditRecord, None, None]:
        """Context manager for automatic audit trail.

        Logs the action on entry and updates the status on exit. If an
        exception occurs the status is set to FAILURE.

        Mirrors the COBOL pattern of:
          1000-INITIALIZE  (entry)
          2000-PROCESS-AUDIT (body + write)
          3000-TERMINATE   (exit / close)

        Usage::

            with audit.track("JDOE", AuditAction.UPDATE, "PORTFOLIO", "P001") as rec:
                # perform operation ...
                rec.old_value = "100.00"
                rec.new_value = "150.00"
                rec.message = "Updated position"

        Args:
            user: User performing the action.
            action: The audit action.
            entity_type: Type of entity affected.
            entity_id: Identifier of affected entity.
            account_no: Optional account number.

        Yields:
            An :class:`AuditRecord` that can be enriched during the operation.
        """
        record = AuditRecord(
            system_id=self._system_id,
            user_id=user,
            program=self._program,
            terminal_id=self._terminal_id,
            audit_type=AuditType.USER_ACTION,
            action=action,
            status=AuditStatus.SUCCESS,
            entity_type=entity_type,
            entity_id=entity_id,
            account_no=account_no,
        )
        try:
            yield record
        except Exception:
            record.status = AuditStatus.FAILURE
            self._write_audit(record)
            raise
        else:
            self._write_audit(record)

    # ----------------------------------------------------------
    # Decorator — automatic audit trail on service methods
    # ----------------------------------------------------------

    def audited(
        self,
        action: AuditAction,
        entity_type: str,
        user_param: str = "user",
        entity_id_param: str = "portfolio_id",
    ) -> Callable[[F], F]:
        """Decorator for automatic audit trail on service methods.

        The decorated function's keyword arguments are inspected to extract
        ``user`` and ``entity_id`` values. Override the parameter names
        with *user_param* and *entity_id_param*.

        Usage::

            @audit.audited(action=AuditAction.CREATE, entity_type="PORTFOLIO")
            def create_portfolio(user: str, portfolio_id: str, data: dict) -> ...:
                ...

        Args:
            action: The audit action to record.
            entity_type: Type of entity being affected.
            user_param: Name of the keyword argument containing the user ID.
            entity_id_param: Name of the keyword argument containing the
                entity identifier.

        Returns:
            A decorator that wraps the function with audit logging.
        """
        def decorator(func: F) -> F:
            sig = inspect.signature(func)

            @functools.wraps(func)
            def wrapper(*args: Any, **kwargs: Any) -> Any:
                # Bind positional + keyword args so we can look up by name
                bound = sig.bind(*args, **kwargs)
                bound.apply_defaults()
                all_args = bound.arguments

                user = all_args.get(user_param, "UNKNOWN")
                entity_id = all_args.get(entity_id_param, "")

                record = AuditRecord(
                    system_id=self._system_id,
                    user_id=str(user),
                    program=self._program or func.__module__,
                    terminal_id=self._terminal_id,
                    audit_type=AuditType.USER_ACTION,
                    action=action,
                    entity_type=entity_type,
                    entity_id=str(entity_id),
                )
                try:
                    result = func(*args, **kwargs)
                except Exception:
                    record.status = AuditStatus.FAILURE
                    self._write_audit(record)
                    raise
                else:
                    record.status = AuditStatus.SUCCESS
                    self._write_audit(record)
                    return result

            return wrapper  # type: ignore[return-value]

        return decorator

    # ----------------------------------------------------------
    # Private — write audit record
    # ----------------------------------------------------------

    def _write_audit(self, record: AuditRecord) -> None:
        """Write the audit record via the logging subsystem.

        Mirrors AUDPROC.cbl:
            WRITE AUDIT-RECORD
            IF WS-FILE-STATUS NOT = '00'
                DISPLAY 'Error writing audit record: ' WS-FILE-STATUS
                MOVE 8 TO LS-RETURN-CODE

        In production, a database-backed logging handler would persist
        this to the audit table (similar to ERRHNDL.cbl's DB2 INSERT).
        """
        data = record.to_dict()
        # Prefix reserved LogRecord attribute names to avoid KeyError
        # from logging.Logger.makeRecord (e.g. "message" is reserved).
        safe_extra = {
            (f"audit_{k}" if k in ("message", "name", "args", "levelname") else k): v
            for k, v in data.items()
        }
        logger.info(
            "AUDIT: [%s] %s %s %s by %s",
            record.action.value,
            record.entity_type,
            record.entity_id,
            record.status.value,
            record.user_id,
            extra=safe_extra,
        )
