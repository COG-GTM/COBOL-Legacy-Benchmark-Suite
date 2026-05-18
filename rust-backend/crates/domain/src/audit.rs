//! Audit trail record.
//!
//! Translated from COBOL copybook `AUDITLOG.cpy`.

use serde::{Deserialize, Serialize};

use crate::error::{check_max_len, check_non_empty, ValidationErrors};

// ---------------------------------------------------------------------------
// AUDITLOG.cpy — Audit type (AUD-TYPE, level-88)
// ---------------------------------------------------------------------------

/// Audit event type: TRAN=Transaction, USER=UserAction, SYST=SystemEvent.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum AuditType {
    Transaction,
    UserAction,
    SystemEvent,
}

impl AuditType {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Transaction => "TRAN",
            Self::UserAction => "USER",
            Self::SystemEvent => "SYST",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "TRAN" => Some(Self::Transaction),
            "USER" => Some(Self::UserAction),
            "SYST" => Some(Self::SystemEvent),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// AUDITLOG.cpy — Audit action (AUD-ACTION, level-88)
// ---------------------------------------------------------------------------

/// Audit action performed.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum AuditAction {
    Create,
    Update,
    Delete,
    Inquire,
    Login,
    Logout,
    Startup,
    Shutdown,
}

impl AuditAction {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Create => "CREATE",
            Self::Update => "UPDATE",
            Self::Delete => "DELETE",
            Self::Inquire => "INQUIRE",
            Self::Login => "LOGIN",
            Self::Logout => "LOGOUT",
            Self::Startup => "STARTUP",
            Self::Shutdown => "SHUTDOWN",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code.trim() {
            "CREATE" => Some(Self::Create),
            "UPDATE" => Some(Self::Update),
            "DELETE" => Some(Self::Delete),
            "INQUIRE" => Some(Self::Inquire),
            "LOGIN" => Some(Self::Login),
            "LOGOUT" => Some(Self::Logout),
            "STARTUP" => Some(Self::Startup),
            "SHUTDOWN" => Some(Self::Shutdown),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// AUDITLOG.cpy — Audit status (AUD-STATUS, level-88)
// ---------------------------------------------------------------------------

/// Outcome of the audited operation.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum AuditStatus {
    Success,
    Failure,
    Warning,
}

impl AuditStatus {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Success => "SUCC",
            Self::Failure => "FAIL",
            Self::Warning => "WARN",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "SUCC" => Some(Self::Success),
            "FAIL" => Some(Self::Failure),
            "WARN" => Some(Self::Warning),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// AUDITLOG.cpy — Audit record (AUDIT-RECORD)
// ---------------------------------------------------------------------------

/// Full audit trail record.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuditRecord {
    // -- AUD-HEADER --
    pub timestamp: String,
    pub system_id: String,
    pub user_id: String,
    pub program: String,
    pub terminal: String,
    // -- event classification --
    pub audit_type: AuditType,
    pub action: AuditAction,
    pub status: AuditStatus,
    // -- AUD-KEY-INFO --
    pub portfolio_id: String,
    pub account_no: String,
    // -- change images --
    pub before_image: String,
    pub after_image: String,
    pub message: String,
}

impl Default for AuditRecord {
    fn default() -> Self {
        Self {
            timestamp: String::new(),
            system_id: String::new(),
            user_id: String::new(),
            program: String::new(),
            terminal: String::new(),
            audit_type: AuditType::SystemEvent,
            action: AuditAction::Inquire,
            status: AuditStatus::Success,
            portfolio_id: String::new(),
            account_no: String::new(),
            before_image: String::new(),
            after_image: String::new(),
            message: String::new(),
        }
    }
}

impl AuditRecord {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();

        check_non_empty(&mut errors, "timestamp", &self.timestamp);
        check_max_len(&mut errors, "timestamp", &self.timestamp, 26);

        check_max_len(&mut errors, "system_id", &self.system_id, 8);
        check_non_empty(&mut errors, "user_id", &self.user_id);
        check_max_len(&mut errors, "user_id", &self.user_id, 8);
        check_max_len(&mut errors, "program", &self.program, 8);
        check_max_len(&mut errors, "terminal", &self.terminal, 8);

        check_max_len(&mut errors, "portfolio_id", &self.portfolio_id, 8);
        check_max_len(&mut errors, "account_no", &self.account_no, 10);

        check_max_len(&mut errors, "before_image", &self.before_image, 100);
        check_max_len(&mut errors, "after_image", &self.after_image, 100);
        check_max_len(&mut errors, "message", &self.message, 100);

        errors.into_result()
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    // -- AuditType ----------------------------------------------------------

    #[test]
    fn audit_type_roundtrip() {
        for at in [
            AuditType::Transaction,
            AuditType::UserAction,
            AuditType::SystemEvent,
        ] {
            let json = serde_json::to_string(&at).unwrap();
            let back: AuditType = serde_json::from_str(&json).unwrap();
            assert_eq!(at, back);
        }
    }

    #[test]
    fn audit_type_codes() {
        assert_eq!(AuditType::Transaction.code(), "TRAN");
        assert_eq!(AuditType::from_code("USER"), Some(AuditType::UserAction));
        assert_eq!(AuditType::from_code("XX"), None);
    }

    // -- AuditAction --------------------------------------------------------

    #[test]
    fn audit_action_roundtrip() {
        for aa in [
            AuditAction::Create,
            AuditAction::Update,
            AuditAction::Delete,
            AuditAction::Inquire,
            AuditAction::Login,
            AuditAction::Logout,
            AuditAction::Startup,
            AuditAction::Shutdown,
        ] {
            let json = serde_json::to_string(&aa).unwrap();
            let back: AuditAction = serde_json::from_str(&json).unwrap();
            assert_eq!(aa, back);
        }
    }

    #[test]
    fn audit_action_codes() {
        assert_eq!(AuditAction::Create.code(), "CREATE");
        assert_eq!(AuditAction::Shutdown.code(), "SHUTDOWN");
        assert_eq!(AuditAction::from_code("LOGIN   "), Some(AuditAction::Login));
        assert_eq!(AuditAction::from_code("NOPE"), None);
    }

    // -- AuditStatus --------------------------------------------------------

    #[test]
    fn audit_status_roundtrip() {
        for ast in [
            AuditStatus::Success,
            AuditStatus::Failure,
            AuditStatus::Warning,
        ] {
            let json = serde_json::to_string(&ast).unwrap();
            let back: AuditStatus = serde_json::from_str(&json).unwrap();
            assert_eq!(ast, back);
        }
    }

    #[test]
    fn audit_status_codes() {
        assert_eq!(AuditStatus::Failure.code(), "FAIL");
        assert_eq!(AuditStatus::from_code("WARN"), Some(AuditStatus::Warning));
        assert_eq!(AuditStatus::from_code("OK"), None);
    }

    // -- AuditRecord --------------------------------------------------------

    #[test]
    fn audit_record_default_matches_cobol_init() {
        let ar = AuditRecord::default();
        assert!(ar.timestamp.is_empty());
        assert!(ar.system_id.is_empty());
        assert!(ar.user_id.is_empty());
        assert!(ar.program.is_empty());
        assert!(ar.terminal.is_empty());
        assert!(ar.portfolio_id.is_empty());
        assert!(ar.account_no.is_empty());
        assert!(ar.before_image.is_empty());
        assert!(ar.after_image.is_empty());
        assert!(ar.message.is_empty());
    }

    #[test]
    fn audit_record_serde_roundtrip() {
        let ar = AuditRecord {
            timestamp: "2024-03-20-10.30.45.000000".into(),
            system_id: "SYS001".into(),
            user_id: "ADMIN".into(),
            program: "PORTMSTR".into(),
            terminal: "TERM001".into(),
            audit_type: AuditType::Transaction,
            action: AuditAction::Create,
            status: AuditStatus::Success,
            portfolio_id: "PORT0001".into(),
            account_no: "ACCT000001".into(),
            before_image: String::new(),
            after_image: "new portfolio created".into(),
            message: "Portfolio created successfully".into(),
        };
        let json = serde_json::to_string(&ar).unwrap();
        let back: AuditRecord = serde_json::from_str(&json).unwrap();
        assert_eq!(ar, back);
    }

    #[test]
    fn audit_record_validation_pass() {
        let ar = AuditRecord {
            timestamp: "2024-03-20-10.30.45.000000".into(),
            system_id: "SYS001".into(),
            user_id: "ADMIN".into(),
            program: "PORTMSTR".into(),
            terminal: "TERM001".into(),
            audit_type: AuditType::UserAction,
            action: AuditAction::Login,
            status: AuditStatus::Success,
            ..AuditRecord::default()
        };
        assert!(ar.validate().is_ok());
    }

    #[test]
    fn audit_record_validation_fail_empty_required() {
        let ar = AuditRecord::default();
        let errs = ar.validate().unwrap_err();
        let fields: Vec<&str> = errs.errors().iter().map(|e| e.field.as_str()).collect();
        assert!(fields.contains(&"timestamp"));
        assert!(fields.contains(&"user_id"));
    }

    #[test]
    fn audit_record_validation_fail_too_long() {
        let ar = AuditRecord {
            timestamp: "2024-03-20-10.30.45.000000".into(),
            user_id: "ADMIN".into(),
            before_image: "X".repeat(101),
            after_image: "X".repeat(101),
            message: "X".repeat(101),
            ..AuditRecord::default()
        };
        let errs = ar.validate().unwrap_err();
        assert!(errs.errors().len() >= 3);
    }
}
