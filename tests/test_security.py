"""
Tests for security rules from SECMGR.cbl.

These tests encode the business rules for user validation, authorization
checking, and the three-phase security pipeline.
"""

import pytest

from tests.business_rules.validators import (
    validate_user,
    check_authorization,
    run_security_pipeline,
)


# =====================================================================
# User Validation
# Reference: SECMGR.cbl P100-VALIDATE-USER
# Rule: User ID must match the CICS session user; mismatch returns RC=8
# =====================================================================
class TestUserValidation:
    """User ID must match the CICS session user."""

    def test_matching_user_returns_rc_zero(self):
        rc, msg = validate_user("USER0001", "USER0001")
        assert rc == 0
        assert msg == ""

    def test_mismatched_user_returns_rc_eight(self):
        rc, msg = validate_user("USER0001", "USER0002")
        assert rc == 8
        assert msg == "User validation failed"

    def test_empty_user_id_mismatch(self):
        rc, msg = validate_user("", "USER0001")
        assert rc == 8

    def test_both_empty_match(self):
        rc, msg = validate_user("", "")
        assert rc == 0

    def test_case_sensitive(self):
        rc, msg = validate_user("user0001", "USER0001")
        assert rc == 8


# =====================================================================
# Authorization Check
# Reference: SECMGR.cbl P200-CHECK-AUTH
# Rule: User must have matching entry in AUTHFILE for the resource and
#       access type; no match returns RC=8 "Access denied"
# =====================================================================
class TestAuthorizationCheck:
    """User must have a matching auth entry for resource and access type."""

    @pytest.fixture
    def auth_entries(self):
        return [
            {
                "user_id": "USER0001",
                "resource": "PORTFILE",
                "access_type": "READ",
            },
            {
                "user_id": "USER0001",
                "resource": "PORTFILE",
                "access_type": "WRITE",
            },
            {
                "user_id": "USER0002",
                "resource": "TRANFILE",
                "access_type": "READ",
            },
        ]

    def test_authorized_access(self, auth_entries):
        rc, msg = check_authorization("USER0001", "PORTFILE", "READ", auth_entries)
        assert rc == 0
        assert msg == ""

    def test_unauthorized_resource(self, auth_entries):
        rc, msg = check_authorization("USER0001", "TRANFILE", "READ", auth_entries)
        assert rc == 8
        assert msg == "Access denied"

    def test_unauthorized_access_type(self, auth_entries):
        rc, msg = check_authorization("USER0002", "TRANFILE", "WRITE", auth_entries)
        assert rc == 8
        assert msg == "Access denied"

    def test_unknown_user(self, auth_entries):
        rc, msg = check_authorization("UNKNOWN", "PORTFILE", "READ", auth_entries)
        assert rc == 8
        assert msg == "Access denied"

    def test_empty_auth_entries(self):
        rc, msg = check_authorization("USER0001", "PORTFILE", "READ", [])
        assert rc == 8
        assert msg == "Access denied"


# =====================================================================
# Three-Phase Security Pipeline
# Reference: SECMGR.cbl main EVALUATE block
# Rule: Validate -> Authorize -> Log; failure at any phase stops processing
# =====================================================================
class TestSecurityPipeline:
    """Three-phase security: Validate -> Authorize -> Log."""

    @pytest.fixture
    def auth_entries(self):
        return [
            {
                "user_id": "USER0001",
                "resource": "PORTFILE",
                "access_type": "READ",
            },
        ]

    def test_all_phases_succeed(self, auth_entries):
        rc, msg, phases = run_security_pipeline(
            request_user_id="USER0001",
            session_user_id="USER0001",
            resource_name="PORTFILE",
            access_type="READ",
            auth_entries=auth_entries,
            audit_success=True,
        )
        assert rc == 0
        assert msg == ""
        assert phases == ["validate", "authorize", "log"]

    def test_validation_failure_stops_pipeline(self, auth_entries):
        rc, msg, phases = run_security_pipeline(
            request_user_id="USER0001",
            session_user_id="WRONG",
            resource_name="PORTFILE",
            access_type="READ",
            auth_entries=auth_entries,
        )
        assert rc == 8
        assert "validation failed" in msg.lower()
        assert phases == []
        assert "authorize" not in phases
        assert "log" not in phases

    def test_authorization_failure_stops_pipeline(self, auth_entries):
        rc, msg, phases = run_security_pipeline(
            request_user_id="USER0001",
            session_user_id="USER0001",
            resource_name="UNKNOWN",
            access_type="READ",
            auth_entries=auth_entries,
        )
        assert rc == 8
        assert msg == "Access denied"
        assert phases == ["validate"]
        assert "authorize" not in phases
        assert "log" not in phases

    def test_audit_failure_stops_after_authorize(self, auth_entries):
        rc, msg, phases = run_security_pipeline(
            request_user_id="USER0001",
            session_user_id="USER0001",
            resource_name="PORTFILE",
            access_type="READ",
            auth_entries=auth_entries,
            audit_success=False,
        )
        assert rc == 12
        assert "Audit logging failed" in msg
        assert phases == ["validate", "authorize"]
        assert "log" not in phases
