//! Common definitions and constants.
//!
//! Translated from COBOL copybook `COMMON.cpy`.

use chrono::{NaiveDate, NaiveTime};
use serde::{Deserialize, Serialize};

use crate::error::{check_max_len, check_non_empty, ValidationErrors};

// ---------------------------------------------------------------------------
// COMMON.cpy — Return codes (RETURN-CODES)
// ---------------------------------------------------------------------------

/// Standard return code values.
///
/// Matches COBOL numeric values: Success=0, Warning=4, Error=8, Severe=12, Critical=16.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum ReturnCode {
    Success,
    Warning,
    Error,
    Severe,
    Critical,
}

impl ReturnCode {
    pub fn value(&self) -> i16 {
        match self {
            Self::Success => 0,
            Self::Warning => 4,
            Self::Error => 8,
            Self::Severe => 12,
            Self::Critical => 16,
        }
    }

    pub fn from_value(v: i16) -> Option<Self> {
        match v {
            0 => Some(Self::Success),
            4 => Some(Self::Warning),
            8 => Some(Self::Error),
            12 => Some(Self::Severe),
            16 => Some(Self::Critical),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// COMMON.cpy — Status codes (STATUS-CODES)
// ---------------------------------------------------------------------------

/// General status codes used across the system.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum StatusCode {
    Active,
    Closed,
    Pending,
    Suspended,
    Failed,
    Reversed,
}

impl StatusCode {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Active => "A",
            Self::Closed => "C",
            Self::Pending => "P",
            Self::Suspended => "S",
            Self::Failed => "F",
            Self::Reversed => "R",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "A" => Some(Self::Active),
            "C" => Some(Self::Closed),
            "P" => Some(Self::Pending),
            "S" => Some(Self::Suspended),
            "F" => Some(Self::Failed),
            "R" => Some(Self::Reversed),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// COMMON.cpy — Transaction types (TRANSACTION-TYPES)
// ---------------------------------------------------------------------------

/// Transaction type codes: BU=Buy, SL=Sell, TR=Transfer, FE=Fee.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum TransactionType {
    Buy,
    Sell,
    Transfer,
    Fee,
}

impl TransactionType {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Buy => "BU",
            Self::Sell => "SL",
            Self::Transfer => "TR",
            Self::Fee => "FE",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "BU" => Some(Self::Buy),
            "SL" => Some(Self::Sell),
            "TR" => Some(Self::Transfer),
            "FE" => Some(Self::Fee),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// COMMON.cpy — Currency codes (CURRENCY-CODES)
// ---------------------------------------------------------------------------

/// Supported currency codes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "UPPERCASE")]
pub enum CurrencyCode {
    Usd,
    Eur,
    Gbp,
    Jpy,
    Cad,
}

impl CurrencyCode {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Usd => "USD",
            Self::Eur => "EUR",
            Self::Gbp => "GBP",
            Self::Jpy => "JPY",
            Self::Cad => "CAD",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "USD" => Some(Self::Usd),
            "EUR" => Some(Self::Eur),
            "GBP" => Some(Self::Gbp),
            "JPY" => Some(Self::Jpy),
            "CAD" => Some(Self::Cad),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// COMMON.cpy — Date/time structure (COMMON-DATETIME)
// ---------------------------------------------------------------------------

/// Broken-out date and time fields matching COBOL COMMON-DATETIME.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommonDateTime {
    pub date: Option<NaiveDate>,
    pub time: Option<NaiveTime>,
}

// ---------------------------------------------------------------------------
// COMMON.cpy — Error handling (ERROR-HANDLING)
// ---------------------------------------------------------------------------

/// Lightweight error info structure from COMMON.cpy ERROR-HANDLING.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorInfo {
    pub error_code: String,
    pub error_module: String,
    pub error_routine: String,
    pub error_message: String,
}

impl ErrorInfo {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();
        check_max_len(&mut errors, "error_code", &self.error_code, 4);
        check_max_len(&mut errors, "error_module", &self.error_module, 8);
        check_max_len(&mut errors, "error_routine", &self.error_routine, 8);
        check_max_len(&mut errors, "error_message", &self.error_message, 80);
        errors.into_result()
    }
}

// ---------------------------------------------------------------------------
// COMMON.cpy — Audit fields (AUDIT-FIELDS)
// ---------------------------------------------------------------------------

/// Common audit trail fields attached to operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuditFields {
    pub timestamp: String,
    pub user: String,
    pub terminal: String,
    pub program: String,
}

impl AuditFields {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();
        check_max_len(&mut errors, "timestamp", &self.timestamp, 26);
        check_max_len(&mut errors, "user", &self.user, 8);
        check_non_empty(&mut errors, "user", &self.user);
        check_max_len(&mut errors, "terminal", &self.terminal, 8);
        check_max_len(&mut errors, "program", &self.program, 8);
        errors.into_result()
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    // -- ReturnCode ---------------------------------------------------------

    #[test]
    fn return_code_ordering() {
        assert!(ReturnCode::Success < ReturnCode::Warning);
        assert!(ReturnCode::Warning < ReturnCode::Error);
        assert!(ReturnCode::Error < ReturnCode::Severe);
        assert!(ReturnCode::Severe < ReturnCode::Critical);
    }

    #[test]
    fn return_code_values() {
        assert_eq!(ReturnCode::Success.value(), 0);
        assert_eq!(ReturnCode::Critical.value(), 16);
        assert_eq!(ReturnCode::from_value(8), Some(ReturnCode::Error));
        assert_eq!(ReturnCode::from_value(99), None);
    }

    #[test]
    fn return_code_serde_roundtrip() {
        for rc in [
            ReturnCode::Success,
            ReturnCode::Warning,
            ReturnCode::Error,
            ReturnCode::Severe,
            ReturnCode::Critical,
        ] {
            let json = serde_json::to_string(&rc).unwrap();
            let back: ReturnCode = serde_json::from_str(&json).unwrap();
            assert_eq!(rc, back);
        }
    }

    // -- StatusCode ---------------------------------------------------------

    #[test]
    fn status_code_roundtrip() {
        for sc in [
            StatusCode::Active,
            StatusCode::Closed,
            StatusCode::Pending,
            StatusCode::Suspended,
            StatusCode::Failed,
            StatusCode::Reversed,
        ] {
            let json = serde_json::to_string(&sc).unwrap();
            let back: StatusCode = serde_json::from_str(&json).unwrap();
            assert_eq!(sc, back);
        }
    }

    #[test]
    fn status_code_cobol_codes() {
        assert_eq!(StatusCode::Active.code(), "A");
        assert_eq!(StatusCode::from_code("S"), Some(StatusCode::Suspended));
        assert_eq!(StatusCode::from_code("Z"), None);
    }

    // -- TransactionType ----------------------------------------------------

    #[test]
    fn transaction_type_roundtrip() {
        for tt in [
            TransactionType::Buy,
            TransactionType::Sell,
            TransactionType::Transfer,
            TransactionType::Fee,
        ] {
            let json = serde_json::to_string(&tt).unwrap();
            let back: TransactionType = serde_json::from_str(&json).unwrap();
            assert_eq!(tt, back);
        }
    }

    #[test]
    fn transaction_type_codes() {
        assert_eq!(TransactionType::Buy.code(), "BU");
        assert_eq!(TransactionType::from_code("FE"), Some(TransactionType::Fee));
        assert_eq!(TransactionType::from_code("XX"), None);
    }

    // -- CurrencyCode -------------------------------------------------------

    #[test]
    fn currency_code_roundtrip() {
        for cc in [
            CurrencyCode::Usd,
            CurrencyCode::Eur,
            CurrencyCode::Gbp,
            CurrencyCode::Jpy,
            CurrencyCode::Cad,
        ] {
            let json = serde_json::to_string(&cc).unwrap();
            let back: CurrencyCode = serde_json::from_str(&json).unwrap();
            assert_eq!(cc, back);
        }
    }

    #[test]
    fn currency_code_values() {
        assert_eq!(CurrencyCode::Usd.code(), "USD");
        assert_eq!(CurrencyCode::from_code("JPY"), Some(CurrencyCode::Jpy));
        assert_eq!(CurrencyCode::from_code("CHF"), None);
    }

    // -- CommonDateTime -----------------------------------------------------

    #[test]
    fn common_datetime_default() {
        let dt = CommonDateTime::default();
        assert!(dt.date.is_none());
        assert!(dt.time.is_none());
    }

    #[test]
    fn common_datetime_serde_roundtrip() {
        let dt = CommonDateTime {
            date: Some(NaiveDate::from_ymd_opt(2024, 3, 20).unwrap()),
            time: Some(NaiveTime::from_hms_opt(10, 30, 45).unwrap()),
        };
        let json = serde_json::to_string(&dt).unwrap();
        let back: CommonDateTime = serde_json::from_str(&json).unwrap();
        assert_eq!(dt, back);
    }

    // -- ErrorInfo ----------------------------------------------------------

    #[test]
    fn error_info_default_matches_cobol_init() {
        let ei = ErrorInfo::default();
        assert!(ei.error_code.is_empty());
        assert!(ei.error_module.is_empty());
        assert!(ei.error_routine.is_empty());
        assert!(ei.error_message.is_empty());
    }

    #[test]
    fn error_info_serde_roundtrip() {
        let ei = ErrorInfo {
            error_code: "E001".into(),
            error_module: "TRNVAL00".into(),
            error_routine: "VALIDATE".into(),
            error_message: "Invalid amount".into(),
        };
        let json = serde_json::to_string(&ei).unwrap();
        let back: ErrorInfo = serde_json::from_str(&json).unwrap();
        assert_eq!(ei, back);
    }

    #[test]
    fn error_info_validation_fail() {
        let ei = ErrorInfo {
            error_code: "X".repeat(5),
            error_module: "X".repeat(9),
            ..ErrorInfo::default()
        };
        let errs = ei.validate().unwrap_err();
        assert_eq!(errs.errors().len(), 2);
    }

    // -- AuditFields --------------------------------------------------------

    #[test]
    fn audit_fields_default_matches_cobol_init() {
        let af = AuditFields::default();
        assert!(af.timestamp.is_empty());
        assert!(af.user.is_empty());
        assert!(af.terminal.is_empty());
        assert!(af.program.is_empty());
    }

    #[test]
    fn audit_fields_serde_roundtrip() {
        let af = AuditFields {
            timestamp: "2024-03-20-10.30.45.000000".into(),
            user: "ADMIN".into(),
            terminal: "TERM001".into(),
            program: "PORTMSTR".into(),
        };
        let json = serde_json::to_string(&af).unwrap();
        let back: AuditFields = serde_json::from_str(&json).unwrap();
        assert_eq!(af, back);
    }

    #[test]
    fn audit_fields_validation_pass() {
        let af = AuditFields {
            timestamp: "2024-03-20-10.30.45.000000".into(),
            user: "ADMIN".into(),
            terminal: "TERM001".into(),
            program: "PORTMSTR".into(),
        };
        assert!(af.validate().is_ok());
    }

    #[test]
    fn audit_fields_validation_fail_empty_user() {
        let af = AuditFields::default();
        let errs = af.validate().unwrap_err();
        assert!(errs.errors().iter().any(|e| e.field == "user"));
    }
}
