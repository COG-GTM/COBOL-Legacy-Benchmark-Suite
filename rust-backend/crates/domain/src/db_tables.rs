//! DB2 table structures.
//!
//! Translated from COBOL copybook `DBTBLS.cpy`.

use chrono::NaiveDateTime;
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use crate::error::{check_max_len, check_non_empty, ValidationErrors};

// ---------------------------------------------------------------------------
// DBTBLS.cpy — Position history table (POSHIST-RECORD)
// ---------------------------------------------------------------------------

/// Position history record stored in DB2.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PositionHistory {
    pub account_no: String,
    pub portfolio_id: String,
    pub transaction_date: String,
    pub transaction_time: String,
    pub transaction_type: String,
    pub security_id: String,
    pub quantity: Decimal,
    pub price: Decimal,
    pub amount: Decimal,
    pub fees: Decimal,
    pub total_amount: Decimal,
    pub cost_basis: Decimal,
    pub gain_loss: Decimal,
    pub process_date: String,
    pub process_time: String,
    pub program_id: String,
    pub user_id: String,
    pub audit_timestamp: Option<NaiveDateTime>,
}

impl Default for PositionHistory {
    fn default() -> Self {
        Self {
            account_no: String::new(),
            portfolio_id: String::new(),
            transaction_date: String::new(),
            transaction_time: String::new(),
            transaction_type: String::new(),
            security_id: String::new(),
            quantity: Decimal::ZERO,
            price: Decimal::ZERO,
            amount: Decimal::ZERO,
            fees: Decimal::ZERO,
            total_amount: Decimal::ZERO,
            cost_basis: Decimal::ZERO,
            gain_loss: Decimal::ZERO,
            process_date: String::new(),
            process_time: String::new(),
            program_id: String::new(),
            user_id: String::new(),
            audit_timestamp: None,
        }
    }
}

impl PositionHistory {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();

        check_non_empty(&mut errors, "account_no", &self.account_no);
        check_max_len(&mut errors, "account_no", &self.account_no, 10);

        check_non_empty(&mut errors, "portfolio_id", &self.portfolio_id);
        check_max_len(&mut errors, "portfolio_id", &self.portfolio_id, 8);

        check_max_len(&mut errors, "transaction_date", &self.transaction_date, 10);
        check_max_len(&mut errors, "transaction_time", &self.transaction_time, 8);
        check_max_len(&mut errors, "transaction_type", &self.transaction_type, 2);

        check_non_empty(&mut errors, "security_id", &self.security_id);
        check_max_len(&mut errors, "security_id", &self.security_id, 12);

        check_max_len(&mut errors, "process_date", &self.process_date, 10);
        check_max_len(&mut errors, "process_time", &self.process_time, 8);
        check_max_len(&mut errors, "program_id", &self.program_id, 8);
        check_max_len(&mut errors, "user_id", &self.user_id, 8);

        errors.into_result()
    }

    /// Net proceeds: amount minus fees.
    pub fn net_amount(&self) -> Decimal {
        self.amount - self.fees
    }
}

// ---------------------------------------------------------------------------
// DBTBLS.cpy — Error log type (EL-ERROR-TYPE, level-88)
// ---------------------------------------------------------------------------

/// Error log entry type: S=System, A=Application, D=Data.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum ErrorLogType {
    System,
    Application,
    Data,
}

impl ErrorLogType {
    pub fn code(&self) -> &'static str {
        match self {
            Self::System => "S",
            Self::Application => "A",
            Self::Data => "D",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "S" => Some(Self::System),
            "A" => Some(Self::Application),
            "D" => Some(Self::Data),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// DBTBLS.cpy — Error log severity (EL-ERROR-SEVERITY, level-88)
// ---------------------------------------------------------------------------

/// Error log severity: 1=Info, 2=Warning, 3=Error, 4=Severe.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum ErrorLogSeverity {
    Info,
    Warning,
    Error,
    Severe,
}

impl ErrorLogSeverity {
    pub fn value(&self) -> i16 {
        match self {
            Self::Info => 1,
            Self::Warning => 2,
            Self::Error => 3,
            Self::Severe => 4,
        }
    }

    pub fn from_value(v: i16) -> Option<Self> {
        match v {
            1 => Some(Self::Info),
            2 => Some(Self::Warning),
            3 => Some(Self::Error),
            4 => Some(Self::Severe),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// DBTBLS.cpy — Error log table (ERRLOG-RECORD)
// ---------------------------------------------------------------------------

/// Error log record stored in DB2.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorLog {
    pub error_timestamp: Option<NaiveDateTime>,
    pub program_id: String,
    pub error_type: ErrorLogType,
    pub error_severity: ErrorLogSeverity,
    pub error_code: String,
    pub error_message: String,
    pub process_date: String,
    pub process_time: String,
    pub user_id: String,
    pub additional_info: String,
}

impl Default for ErrorLog {
    fn default() -> Self {
        Self {
            error_timestamp: None,
            program_id: String::new(),
            error_type: ErrorLogType::System,
            error_severity: ErrorLogSeverity::Info,
            error_code: String::new(),
            error_message: String::new(),
            process_date: String::new(),
            process_time: String::new(),
            user_id: String::new(),
            additional_info: String::new(),
        }
    }
}

impl ErrorLog {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();

        check_max_len(&mut errors, "program_id", &self.program_id, 8);
        check_max_len(&mut errors, "error_code", &self.error_code, 8);
        check_max_len(&mut errors, "error_message", &self.error_message, 200);
        check_max_len(&mut errors, "process_date", &self.process_date, 10);
        check_max_len(&mut errors, "process_time", &self.process_time, 8);
        check_max_len(&mut errors, "user_id", &self.user_id, 8);
        check_max_len(&mut errors, "additional_info", &self.additional_info, 500);

        errors.into_result()
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use rust_decimal_macros::dec;

    // -- PositionHistory ----------------------------------------------------

    #[test]
    fn position_history_default_matches_cobol_init() {
        let ph = PositionHistory::default();
        assert!(ph.account_no.is_empty());
        assert!(ph.portfolio_id.is_empty());
        assert_eq!(ph.quantity, Decimal::ZERO);
        assert_eq!(ph.price, Decimal::ZERO);
        assert_eq!(ph.amount, Decimal::ZERO);
        assert_eq!(ph.fees, Decimal::ZERO);
        assert_eq!(ph.total_amount, Decimal::ZERO);
        assert_eq!(ph.cost_basis, Decimal::ZERO);
        assert_eq!(ph.gain_loss, Decimal::ZERO);
        assert!(ph.audit_timestamp.is_none());
    }

    #[test]
    fn position_history_serde_roundtrip() {
        let ph = PositionHistory {
            account_no: "ACCT000001".into(),
            portfolio_id: "PORT0001".into(),
            transaction_date: "2024-06-15".into(),
            transaction_time: "14:30:00".into(),
            transaction_type: "BU".into(),
            security_id: "AAPL00000001".into(),
            quantity: dec!(100.000),
            price: dec!(150.250),
            amount: dec!(15025.00),
            fees: dec!(9.99),
            total_amount: dec!(15034.99),
            cost_basis: dec!(15034.99),
            gain_loss: Decimal::ZERO,
            process_date: "2024-06-15".into(),
            process_time: "14:30:01".into(),
            program_id: "POSUPD00".into(),
            user_id: "TRADER01".into(),
            audit_timestamp: Some(
                chrono::NaiveDate::from_ymd_opt(2024, 6, 15)
                    .unwrap()
                    .and_hms_opt(14, 30, 1)
                    .unwrap(),
            ),
        };
        let json = serde_json::to_string(&ph).unwrap();
        let back: PositionHistory = serde_json::from_str(&json).unwrap();
        assert_eq!(ph, back);
    }

    #[test]
    fn position_history_net_amount() {
        let ph = PositionHistory {
            amount: dec!(15025.00),
            fees: dec!(9.99),
            ..PositionHistory::default()
        };
        assert_eq!(ph.net_amount(), dec!(15015.01));
    }

    #[test]
    fn position_history_validation_pass() {
        let ph = PositionHistory {
            account_no: "ACCT000001".into(),
            portfolio_id: "PORT0001".into(),
            security_id: "AAPL00000001".into(),
            ..PositionHistory::default()
        };
        assert!(ph.validate().is_ok());
    }

    #[test]
    fn position_history_validation_fail() {
        let ph = PositionHistory::default();
        let errs = ph.validate().unwrap_err();
        let fields: Vec<&str> = errs.errors().iter().map(|e| e.field.as_str()).collect();
        assert!(fields.contains(&"account_no"));
        assert!(fields.contains(&"portfolio_id"));
        assert!(fields.contains(&"security_id"));
    }

    // -- ErrorLogType -------------------------------------------------------

    #[test]
    fn error_log_type_roundtrip() {
        for elt in [
            ErrorLogType::System,
            ErrorLogType::Application,
            ErrorLogType::Data,
        ] {
            let json = serde_json::to_string(&elt).unwrap();
            let back: ErrorLogType = serde_json::from_str(&json).unwrap();
            assert_eq!(elt, back);
        }
    }

    #[test]
    fn error_log_type_codes() {
        assert_eq!(ErrorLogType::System.code(), "S");
        assert_eq!(ErrorLogType::Application.code(), "A");
        assert_eq!(ErrorLogType::Data.code(), "D");
        assert_eq!(
            ErrorLogType::from_code("A"),
            Some(ErrorLogType::Application)
        );
        assert_eq!(ErrorLogType::from_code("X"), None);
    }

    // -- ErrorLogSeverity ---------------------------------------------------

    #[test]
    fn error_log_severity_roundtrip() {
        for els in [
            ErrorLogSeverity::Info,
            ErrorLogSeverity::Warning,
            ErrorLogSeverity::Error,
            ErrorLogSeverity::Severe,
        ] {
            let json = serde_json::to_string(&els).unwrap();
            let back: ErrorLogSeverity = serde_json::from_str(&json).unwrap();
            assert_eq!(els, back);
        }
    }

    #[test]
    fn error_log_severity_ordering() {
        assert!(ErrorLogSeverity::Info < ErrorLogSeverity::Warning);
        assert!(ErrorLogSeverity::Warning < ErrorLogSeverity::Error);
        assert!(ErrorLogSeverity::Error < ErrorLogSeverity::Severe);
    }

    #[test]
    fn error_log_severity_values() {
        assert_eq!(ErrorLogSeverity::Info.value(), 1);
        assert_eq!(ErrorLogSeverity::Severe.value(), 4);
        assert_eq!(
            ErrorLogSeverity::from_value(3),
            Some(ErrorLogSeverity::Error)
        );
        assert_eq!(ErrorLogSeverity::from_value(99), None);
    }

    // -- ErrorLog -----------------------------------------------------------

    #[test]
    fn error_log_default_matches_cobol_init() {
        let el = ErrorLog::default();
        assert!(el.error_timestamp.is_none());
        assert!(el.program_id.is_empty());
        assert_eq!(el.error_type, ErrorLogType::System);
        assert_eq!(el.error_severity, ErrorLogSeverity::Info);
        assert!(el.error_code.is_empty());
        assert!(el.error_message.is_empty());
        assert!(el.user_id.is_empty());
        assert!(el.additional_info.is_empty());
    }

    #[test]
    fn error_log_serde_roundtrip() {
        let el = ErrorLog {
            error_timestamp: Some(
                chrono::NaiveDate::from_ymd_opt(2024, 6, 15)
                    .unwrap()
                    .and_hms_opt(14, 30, 0)
                    .unwrap(),
            ),
            program_id: "TRNVAL00".into(),
            error_type: ErrorLogType::Data,
            error_severity: ErrorLogSeverity::Error,
            error_code: "DV001234".into(),
            error_message: "Invalid transaction amount".into(),
            process_date: "2024-06-15".into(),
            process_time: "14:30:00".into(),
            user_id: "SYSTEM".into(),
            additional_info: "Amount exceeded maximum".into(),
        };
        let json = serde_json::to_string(&el).unwrap();
        let back: ErrorLog = serde_json::from_str(&json).unwrap();
        assert_eq!(el, back);
    }

    #[test]
    fn error_log_validation_pass() {
        let el = ErrorLog {
            program_id: "TRNVAL00".into(),
            error_code: "DV001234".into(),
            error_message: "Bad data".into(),
            user_id: "SYSTEM".into(),
            ..ErrorLog::default()
        };
        assert!(el.validate().is_ok());
    }

    #[test]
    fn error_log_validation_fail_too_long() {
        let el = ErrorLog {
            error_message: "X".repeat(201),
            additional_info: "X".repeat(501),
            ..ErrorLog::default()
        };
        let errs = el.validate().unwrap_err();
        assert!(errs.errors().iter().any(|e| e.field == "error_message"));
        assert!(errs.errors().iter().any(|e| e.field == "additional_info"));
    }
}
