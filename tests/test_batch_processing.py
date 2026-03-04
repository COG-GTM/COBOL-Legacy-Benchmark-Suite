"""
Tests for batch processing rules from PRCSEQ00.cbl, HISTLD00.cbl, and BCHCTL00.cbl.

These tests encode the business rules for batch job sequencing,
dependency checking, DB2 commit thresholds, and duplicate handling.
"""

import pytest

from tests.business_rules.validators import (
    BatchProcess,
    check_dependency,
    validate_batch_sequence,
    check_batch_step_prerequisite,
    should_commit,
    handle_sqlcode,
    BATCH_JOB_SEQUENCE,
    COMMIT_THRESHOLD,
)


# =====================================================================
# Dependency Checking
# Reference: PRCSEQ00.cbl 2210-CHECK-DEP-STATUS
# Rule: A process cannot start if hard dependencies haven't completed;
#       if dependency completed but with RC > threshold, return error
# =====================================================================
class TestDependencyChecking:
    """Validate dependency checking rules from PRCSEQ00."""

    def test_hard_dependency_not_completed_fails(self):
        dep = BatchProcess(process_id="TRNVAL00", status="A", return_code=0)
        result = check_dependency(dep, is_hard=True, rc_threshold=4)
        assert result.valid is False
        assert "Hard dependency not completed" in result.error_message

    def test_hard_dependency_completed_within_threshold(self):
        dep = BatchProcess(process_id="TRNVAL00", status="D", return_code=0)
        result = check_dependency(dep, is_hard=True, rc_threshold=4)
        assert result.valid is True

    def test_hard_dependency_completed_at_threshold(self):
        dep = BatchProcess(process_id="TRNVAL00", status="D", return_code=4)
        result = check_dependency(dep, is_hard=True, rc_threshold=4)
        assert result.valid is True

    def test_hard_dependency_completed_exceeds_threshold(self):
        dep = BatchProcess(process_id="TRNVAL00", status="D", return_code=8)
        result = check_dependency(dep, is_hard=True, rc_threshold=4)
        assert result.valid is False
        assert "exceeds threshold" in result.error_message

    def test_soft_dependency_not_completed_passes(self):
        dep = BatchProcess(process_id="TRNVAL00", status="A", return_code=0)
        result = check_dependency(dep, is_hard=False, rc_threshold=4)
        assert result.valid is True

    def test_soft_dependency_completed_exceeds_threshold(self):
        dep = BatchProcess(process_id="TRNVAL00", status="D", return_code=8)
        result = check_dependency(dep, is_hard=False, rc_threshold=4)
        assert result.valid is False

    def test_dependency_in_error_state_is_not_done(self):
        dep = BatchProcess(process_id="TRNVAL00", status="E", return_code=12)
        result = check_dependency(dep, is_hard=True, rc_threshold=4)
        assert result.valid is False

    def test_dependency_in_ready_state_is_not_done(self):
        dep = BatchProcess(process_id="TRNVAL00", status="R", return_code=0)
        result = check_dependency(dep, is_hard=True, rc_threshold=4)
        assert result.valid is False


# =====================================================================
# Batch Job Flow Validation
# Rule: Expected sequence is TRNVAL00 -> POSUPD00 -> HISTLD00 -> Reports
#       Each requiring RC <= 4 from the previous step
# =====================================================================
class TestBatchJobFlow:
    """Validate the expected batch job sequence."""

    def test_correct_sequence(self):
        result = validate_batch_sequence(BATCH_JOB_SEQUENCE)
        assert result.valid is True

    def test_incorrect_sequence_order(self):
        wrong_order = ["POSUPD00", "TRNVAL00", "HISTLD00", "REPORTS"]
        result = validate_batch_sequence(wrong_order)
        assert result.valid is False

    def test_missing_step(self):
        missing = ["TRNVAL00", "POSUPD00", "HISTLD00"]
        result = validate_batch_sequence(missing)
        assert result.valid is False

    def test_extra_step(self):
        extra = ["TRNVAL00", "POSUPD00", "HISTLD00", "REPORTS", "EXTRA00"]
        result = validate_batch_sequence(extra)
        assert result.valid is False

    def test_empty_sequence(self):
        result = validate_batch_sequence([])
        assert result.valid is False


class TestBatchStepPrerequisite:
    """Each batch step requires RC <= 4 from the previous step."""

    def test_rc_zero_passes(self):
        result = check_batch_step_prerequisite(0)
        assert result.valid is True

    def test_rc_four_passes(self):
        result = check_batch_step_prerequisite(4)
        assert result.valid is True

    def test_rc_eight_fails(self):
        result = check_batch_step_prerequisite(8)
        assert result.valid is False
        assert "exceeds maximum" in result.error_message

    def test_rc_twelve_fails(self):
        result = check_batch_step_prerequisite(12)
        assert result.valid is False

    def test_custom_threshold(self):
        result = check_batch_step_prerequisite(8, max_rc=8)
        assert result.valid is True


# =====================================================================
# DB2 Commit Threshold
# Reference: HISTLD00.cbl 2300-CHECK-COMMIT
# Rule: Commit occurs every 1000 records
# =====================================================================
class TestDb2CommitThreshold:
    """Commit occurs every 1000 records."""

    def test_commit_at_threshold(self):
        assert should_commit(COMMIT_THRESHOLD) is True

    def test_no_commit_below_threshold(self):
        assert should_commit(999) is False

    def test_commit_at_multiple(self):
        assert should_commit(2000) is True

    def test_no_commit_at_zero(self):
        assert should_commit(0) is False

    def test_no_commit_at_one(self):
        assert should_commit(1) is False

    def test_commit_at_3000(self):
        assert should_commit(3000) is True

    def test_no_commit_at_1001(self):
        assert should_commit(1001) is False


# =====================================================================
# Duplicate Handling
# Reference: HISTLD00.cbl 2200-LOAD-TO-DB2
# Rule: SQLCODE -803 (duplicate key) is silently skipped, not an error
# =====================================================================
class TestDuplicateHandling:
    """SQLCODE -803 is silently skipped; other errors are real errors."""

    def test_sqlcode_zero_is_success(self):
        success, is_error = handle_sqlcode(0)
        assert success is True
        assert is_error is False

    def test_sqlcode_minus_803_is_skipped(self):
        success, is_error = handle_sqlcode(-803)
        assert success is False
        assert is_error is False  # silently skipped

    def test_other_sqlcode_is_error(self):
        success, is_error = handle_sqlcode(-911)
        assert success is False
        assert is_error is True

    def test_sqlcode_minus_one_is_error(self):
        success, is_error = handle_sqlcode(-1)
        assert success is False
        assert is_error is True

    def test_positive_sqlcode_is_error(self):
        success, is_error = handle_sqlcode(100)
        assert success is False
        assert is_error is True
