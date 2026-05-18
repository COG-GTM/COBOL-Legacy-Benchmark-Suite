//! Audit trail processing utilities.
//!
//! Translated from COBOL program `AUDPROC.cbl` and copybook `AUDITLOG.cpy`.
//! Records audit events for all CRUD operations with user tracking,
//! timestamps, and before/after change images.

use chrono::Utc;
use domain::{AuditAction, AuditRecord, AuditStatus, AuditType};
use serde::{Deserialize, Serialize};
use tracing::{info, warn};

// ---------------------------------------------------------------------------
// AuditProcessor — port of AUDPROC.cbl
// ---------------------------------------------------------------------------

/// Audit trail processor (port of COBOL `AUDPROC`).
///
/// Accepts audit requests, stamps them with the current time, and returns
/// a fully populated [`AuditRecord`] ready for persistence.
#[derive(Debug, Clone)]
pub struct AuditProcessor {
    system_id: String,
    program: String,
}

impl AuditProcessor {
    pub fn new(system_id: impl Into<String>, program: impl Into<String>) -> Self {
        Self {
            system_id: system_id.into(),
            program: program.into(),
        }
    }

    /// Process an audit request and return the populated [`AuditRecord`].
    ///
    /// Mirrors AUDPROC's 2000-PROCESS-AUDIT paragraph: initialises the record,
    /// fills all fields from the request and system context, then logs the
    /// event.  Returns `Ok(AuditRecord)` on success or `Err(AuditError)` if
    /// the request fails validation.
    pub fn process_audit(&self, request: &AuditRequest) -> Result<AuditRecord, AuditError> {
        let now = Utc::now();
        let timestamp = now.format("%Y-%m-%d-%H.%M.%S.%6f").to_string();

        let audit_type = AuditType::from_code(&request.audit_type)
            .ok_or_else(|| AuditError::InvalidType(request.audit_type.clone()))?;
        let action = AuditAction::from_code(&request.action)
            .ok_or_else(|| AuditError::InvalidAction(request.action.clone()))?;
        let status = AuditStatus::from_code(&request.status)
            .ok_or_else(|| AuditError::InvalidStatus(request.status.clone()))?;

        let record = AuditRecord {
            timestamp,
            system_id: self.system_id.clone(),
            user_id: request.user_id.clone(),
            program: self.program.clone(),
            terminal: request.terminal.clone(),
            audit_type,
            action,
            status,
            portfolio_id: request.portfolio_id.clone(),
            account_no: request.account_no.clone(),
            before_image: request.before_image.clone(),
            after_image: request.after_image.clone(),
            message: request.message.clone(),
        };

        self.log_audit(&record);

        Ok(record)
    }

    /// Create a quick success audit record for a given action.
    pub fn record_success(
        &self,
        user_id: &str,
        action: AuditAction,
        portfolio_id: &str,
        account_no: &str,
        message: &str,
    ) -> AuditRecord {
        let now = Utc::now();
        let record = AuditRecord {
            timestamp: now.format("%Y-%m-%d-%H.%M.%S.%6f").to_string(),
            system_id: self.system_id.clone(),
            user_id: user_id.into(),
            program: self.program.clone(),
            terminal: String::new(),
            audit_type: AuditType::Transaction,
            action,
            status: AuditStatus::Success,
            portfolio_id: portfolio_id.into(),
            account_no: account_no.into(),
            before_image: String::new(),
            after_image: String::new(),
            message: message.into(),
        };
        self.log_audit(&record);
        record
    }

    /// Create an audit record that captures before/after change images.
    pub fn record_change(&self, change: &ChangeRecord<'_>) -> AuditRecord {
        let now = Utc::now();
        let record = AuditRecord {
            timestamp: now.format("%Y-%m-%d-%H.%M.%S.%6f").to_string(),
            system_id: self.system_id.clone(),
            user_id: change.user_id.into(),
            program: self.program.clone(),
            terminal: String::new(),
            audit_type: AuditType::Transaction,
            action: change.action,
            status: AuditStatus::Success,
            portfolio_id: change.portfolio_id.into(),
            account_no: change.account_no.into(),
            before_image: change.before.into(),
            after_image: change.after.into(),
            message: change.message.into(),
        };
        self.log_audit(&record);
        record
    }

    fn log_audit(&self, record: &AuditRecord) {
        match record.status {
            AuditStatus::Success => {
                info!(
                    system_id = %record.system_id,
                    user_id = %record.user_id,
                    program = %record.program,
                    audit_type = %record.audit_type.code(),
                    action = %record.action.code(),
                    status = %record.status.code(),
                    portfolio_id = %record.portfolio_id,
                    account_no = %record.account_no,
                    message = %record.message,
                    "audit_event"
                );
            }
            AuditStatus::Warning | AuditStatus::Failure => {
                warn!(
                    system_id = %record.system_id,
                    user_id = %record.user_id,
                    program = %record.program,
                    audit_type = %record.audit_type.code(),
                    action = %record.action.code(),
                    status = %record.status.code(),
                    portfolio_id = %record.portfolio_id,
                    account_no = %record.account_no,
                    message = %record.message,
                    "audit_event"
                );
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ChangeRecord — parameter struct for record_change
// ---------------------------------------------------------------------------

/// Parameters for [`AuditProcessor::record_change`].
#[derive(Debug, Clone)]
pub struct ChangeRecord<'a> {
    pub user_id: &'a str,
    pub action: AuditAction,
    pub portfolio_id: &'a str,
    pub account_no: &'a str,
    pub before: &'a str,
    pub after: &'a str,
    pub message: &'a str,
}

// ---------------------------------------------------------------------------
// AuditRequest — mirrors LS-AUDIT-REQUEST linkage
// ---------------------------------------------------------------------------

/// Incoming audit request (maps to COBOL `LS-AUDIT-REQUEST` linkage section).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuditRequest {
    pub user_id: String,
    pub terminal: String,
    pub audit_type: String,
    pub action: String,
    pub status: String,
    pub portfolio_id: String,
    pub account_no: String,
    pub before_image: String,
    pub after_image: String,
    pub message: String,
}

impl Default for AuditRequest {
    fn default() -> Self {
        Self {
            user_id: String::new(),
            terminal: String::new(),
            audit_type: "SYST".into(),
            action: "INQUIRE".into(),
            status: "SUCC".into(),
            portfolio_id: String::new(),
            account_no: String::new(),
            before_image: String::new(),
            after_image: String::new(),
            message: String::new(),
        }
    }
}

// ---------------------------------------------------------------------------
// AuditError
// ---------------------------------------------------------------------------

/// Errors that can occur during audit processing.
#[derive(Debug, Clone, thiserror::Error)]
pub enum AuditError {
    #[error("invalid audit type code: {0}")]
    InvalidType(String),
    #[error("invalid audit action code: {0}")]
    InvalidAction(String),
    #[error("invalid audit status code: {0}")]
    InvalidStatus(String),
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    fn make_processor() -> AuditProcessor {
        AuditProcessor::new("SYS001", "TESTPGM")
    }

    #[test]
    fn process_audit_success() {
        let proc = make_processor();
        let req = AuditRequest {
            user_id: "ADMIN".into(),
            terminal: "TERM001".into(),
            audit_type: "TRAN".into(),
            action: "CREATE".into(),
            status: "SUCC".into(),
            portfolio_id: "PORT0001".into(),
            account_no: "ACCT000001".into(),
            before_image: String::new(),
            after_image: "new portfolio".into(),
            message: "Portfolio created".into(),
        };
        let record = proc.process_audit(&req).unwrap();
        assert_eq!(record.system_id, "SYS001");
        assert_eq!(record.program, "TESTPGM");
        assert_eq!(record.user_id, "ADMIN");
        assert_eq!(record.audit_type, AuditType::Transaction);
        assert_eq!(record.action, AuditAction::Create);
        assert_eq!(record.status, AuditStatus::Success);
        assert_eq!(record.portfolio_id, "PORT0001");
        assert!(!record.timestamp.is_empty());
    }

    #[test]
    fn process_audit_invalid_type() {
        let proc = make_processor();
        let req = AuditRequest {
            audit_type: "XXXX".into(),
            ..AuditRequest::default()
        };
        let err = proc.process_audit(&req).unwrap_err();
        assert!(matches!(err, AuditError::InvalidType(_)));
    }

    #[test]
    fn process_audit_invalid_action() {
        let proc = make_processor();
        let req = AuditRequest {
            action: "NOPE".into(),
            ..AuditRequest::default()
        };
        let err = proc.process_audit(&req).unwrap_err();
        assert!(matches!(err, AuditError::InvalidAction(_)));
    }

    #[test]
    fn process_audit_invalid_status() {
        let proc = make_processor();
        let req = AuditRequest {
            status: "BAD".into(),
            ..AuditRequest::default()
        };
        let err = proc.process_audit(&req).unwrap_err();
        assert!(matches!(err, AuditError::InvalidStatus(_)));
    }

    #[test]
    fn record_success_helper() {
        let proc = make_processor();
        let record = proc.record_success(
            "USER01",
            AuditAction::Update,
            "PORT0001",
            "ACCT000001",
            "Updated position",
        );
        assert_eq!(record.action, AuditAction::Update);
        assert_eq!(record.status, AuditStatus::Success);
        assert_eq!(record.user_id, "USER01");
    }

    #[test]
    fn record_change_captures_images() {
        let proc = make_processor();
        let record = proc.record_change(&ChangeRecord {
            user_id: "USER01",
            action: AuditAction::Update,
            portfolio_id: "PORT0001",
            account_no: "ACCT000001",
            before: "old_value",
            after: "new_value",
            message: "Changed field",
        });
        assert_eq!(record.before_image, "old_value");
        assert_eq!(record.after_image, "new_value");
    }

    #[test]
    fn audit_request_default() {
        let req = AuditRequest::default();
        assert_eq!(req.audit_type, "SYST");
        assert_eq!(req.action, "INQUIRE");
        assert_eq!(req.status, "SUCC");
    }

    #[test]
    fn audit_record_serialization_roundtrip() {
        let proc = make_processor();
        let record = proc.record_success(
            "USER01",
            AuditAction::Create,
            "PORT0001",
            "ACCT000001",
            "test",
        );
        let json = serde_json::to_string(&record).unwrap();
        let back: AuditRecord = serde_json::from_str(&json).unwrap();
        assert_eq!(record, back);
    }
}
