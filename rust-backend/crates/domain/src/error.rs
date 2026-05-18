//! Error handling and return code management.
//!
//! Translated from COBOL copybooks:
//! - `ERRHAND.cpy` — Standard error handling definitions
//! - `RTNCODE.cpy` — Return code management

use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};

// ---------------------------------------------------------------------------
// Validation helpers
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error)]
#[error("{field}: {message}")]
pub struct ValidationError {
    pub field: String,
    pub message: String,
}

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct ValidationErrors(Vec<ValidationError>);

impl ValidationErrors {
    pub fn new() -> Self {
        Self(Vec::new())
    }

    pub fn add(&mut self, field: impl Into<String>, message: impl Into<String>) {
        self.0.push(ValidationError {
            field: field.into(),
            message: message.into(),
        });
    }

    pub fn is_empty(&self) -> bool {
        self.0.is_empty()
    }

    pub fn errors(&self) -> &[ValidationError] {
        &self.0
    }

    pub fn into_result(self) -> Result<(), Self> {
        if self.is_empty() {
            Ok(())
        } else {
            Err(self)
        }
    }
}

impl std::fmt::Display for ValidationErrors {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        for (i, err) in self.0.iter().enumerate() {
            if i > 0 {
                write!(f, "; ")?;
            }
            write!(f, "{err}")?;
        }
        Ok(())
    }
}

impl std::error::Error for ValidationErrors {}

pub(crate) fn check_max_len(errors: &mut ValidationErrors, field: &str, value: &str, max: usize) {
    if value.len() > max {
        errors.add(
            field,
            format!("exceeds max length {max} (got {})", value.len()),
        );
    }
}

pub(crate) fn check_non_empty(errors: &mut ValidationErrors, field: &str, value: &str) {
    if value.is_empty() {
        errors.add(field, "must not be empty");
    }
}

// ---------------------------------------------------------------------------
// ERRHAND.cpy — Error categories (ERR-CATEGORIES)
// ---------------------------------------------------------------------------

/// Error category codes.
///
/// COBOL level-88 values: VS=Vsam, VL=Validation, PR=Processing, SY=System.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum ErrorCategory {
    Vsam,
    Validation,
    Processing,
    System,
}

impl ErrorCategory {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Vsam => "VS",
            Self::Validation => "VL",
            Self::Processing => "PR",
            Self::System => "SY",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "VS" => Some(Self::Vsam),
            "VL" => Some(Self::Validation),
            "PR" => Some(Self::Processing),
            "SY" => Some(Self::System),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// ERRHAND.cpy — Severity / return codes (ERR-RETURN-CODES)
// ---------------------------------------------------------------------------

/// Error severity levels.
///
/// Numeric values match COBOL: Success=0, Warning=4, Error=8, Severe=12, Terminal=16.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum ErrorSeverity {
    Success,
    Warning,
    Error,
    Severe,
    Terminal,
}

impl ErrorSeverity {
    pub fn code(&self) -> i16 {
        match self {
            Self::Success => 0,
            Self::Warning => 4,
            Self::Error => 8,
            Self::Severe => 12,
            Self::Terminal => 16,
        }
    }

    pub fn from_code(code: i16) -> Option<Self> {
        match code {
            0 => Some(Self::Success),
            4 => Some(Self::Warning),
            8 => Some(Self::Error),
            12 => Some(Self::Severe),
            16 => Some(Self::Terminal),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// ERRHAND.cpy — Error message structure (ERR-MESSAGE)
// ---------------------------------------------------------------------------

/// Error message record.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorMessage {
    pub date: String,
    pub time: String,
    pub program: String,
    pub category: ErrorCategory,
    pub code: String,
    pub severity: ErrorSeverity,
    pub text: String,
    pub details: String,
}

impl Default for ErrorMessage {
    fn default() -> Self {
        Self {
            date: String::new(),
            time: String::new(),
            program: String::new(),
            category: ErrorCategory::System,
            code: String::new(),
            severity: ErrorSeverity::Success,
            text: String::new(),
            details: String::new(),
        }
    }
}

impl ErrorMessage {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();
        check_max_len(&mut errors, "date", &self.date, 10);
        check_max_len(&mut errors, "time", &self.time, 8);
        check_max_len(&mut errors, "program", &self.program, 8);
        check_max_len(&mut errors, "code", &self.code, 4);
        check_max_len(&mut errors, "text", &self.text, 80);
        check_max_len(&mut errors, "details", &self.details, 256);
        errors.into_result()
    }
}

// ---------------------------------------------------------------------------
// ERRHAND.cpy — VSAM status codes (ERR-VSAM-STATUSES / ERR-VSAM-MSGS)
// ---------------------------------------------------------------------------

/// VSAM file status codes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum VsamStatus {
    Success,
    DuplicateKey,
    NotFound,
    EndOfFile,
}

impl VsamStatus {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Success => "00",
            Self::DuplicateKey => "22",
            Self::NotFound => "23",
            Self::EndOfFile => "10",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "00" => Some(Self::Success),
            "22" => Some(Self::DuplicateKey),
            "23" => Some(Self::NotFound),
            "10" => Some(Self::EndOfFile),
            _ => None,
        }
    }

    pub fn message(&self) -> &'static str {
        match self {
            Self::Success => "Operation successful",
            Self::DuplicateKey => "Duplicate record key",
            Self::NotFound => "Record not found",
            Self::EndOfFile => "End of file reached",
        }
    }
}

// ---------------------------------------------------------------------------
// RTNCODE.cpy — Return code request type (RC-REQUEST-TYPE)
// ---------------------------------------------------------------------------

/// Request type for return code operations.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum ReturnCodeRequestType {
    Initialize,
    SetCode,
    GetCode,
    LogCode,
    Analyze,
}

impl ReturnCodeRequestType {
    pub fn code(&self) -> char {
        match self {
            Self::Initialize => 'I',
            Self::SetCode => 'S',
            Self::GetCode => 'G',
            Self::LogCode => 'L',
            Self::Analyze => 'A',
        }
    }

    pub fn from_code(code: char) -> Option<Self> {
        match code {
            'I' => Some(Self::Initialize),
            'S' => Some(Self::SetCode),
            'G' => Some(Self::GetCode),
            'L' => Some(Self::LogCode),
            'A' => Some(Self::Analyze),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// RTNCODE.cpy — Return code status (RC-STATUS)
// ---------------------------------------------------------------------------

/// Status within the return code management area.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum ReturnCodeStatus {
    Success,
    Warning,
    Error,
    Severe,
}

impl ReturnCodeStatus {
    pub fn code(&self) -> char {
        match self {
            Self::Success => 'S',
            Self::Warning => 'W',
            Self::Error => 'E',
            Self::Severe => 'F',
        }
    }

    pub fn from_code(code: char) -> Option<Self> {
        match code {
            'S' => Some(Self::Success),
            'W' => Some(Self::Warning),
            'E' => Some(Self::Error),
            'F' => Some(Self::Severe),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// RTNCODE.cpy — Return code area (RETURN-CODE-AREA)
// ---------------------------------------------------------------------------

/// Return code management area.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ReturnCodeArea {
    pub request_type: ReturnCodeRequestType,
    pub program_id: String,
    pub current_code: i16,
    pub highest_code: i16,
    pub new_code: i16,
    pub status: ReturnCodeStatus,
    pub message: String,
    pub response_code: i32,
    pub start_time: Option<NaiveDateTime>,
    pub end_time: Option<NaiveDateTime>,
    pub total_codes: i32,
    pub max_code: i16,
    pub min_code: i16,
    pub return_value: i16,
    pub highest_return: i16,
    pub return_status: String,
}

impl Default for ReturnCodeArea {
    fn default() -> Self {
        Self {
            request_type: ReturnCodeRequestType::Initialize,
            program_id: String::new(),
            current_code: 0,
            highest_code: 0,
            new_code: 0,
            status: ReturnCodeStatus::Success,
            message: String::new(),
            response_code: 0,
            start_time: None,
            end_time: None,
            total_codes: 0,
            max_code: 0,
            min_code: 0,
            return_value: 0,
            highest_return: 0,
            return_status: String::new(),
        }
    }
}

impl ReturnCodeArea {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();
        check_max_len(&mut errors, "program_id", &self.program_id, 8);
        check_max_len(&mut errors, "message", &self.message, 80);
        check_max_len(&mut errors, "return_status", &self.return_status, 1);
        errors.into_result()
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    // -- ValidationErrors ---------------------------------------------------

    #[test]
    fn empty_validation_errors_is_ok() {
        let errors = ValidationErrors::new();
        assert!(errors.is_empty());
        assert!(errors.into_result().is_ok());
    }

    #[test]
    fn validation_errors_collects_issues() {
        let mut errors = ValidationErrors::new();
        errors.add("field1", "bad");
        errors.add("field2", "worse");
        assert_eq!(errors.errors().len(), 2);
        assert!(errors.into_result().is_err());
    }

    // -- ErrorCategory ------------------------------------------------------

    #[test]
    fn error_category_roundtrip() {
        for cat in [
            ErrorCategory::Vsam,
            ErrorCategory::Validation,
            ErrorCategory::Processing,
            ErrorCategory::System,
        ] {
            let json = serde_json::to_string(&cat).unwrap();
            let back: ErrorCategory = serde_json::from_str(&json).unwrap();
            assert_eq!(cat, back);
        }
    }

    #[test]
    fn error_category_codes() {
        assert_eq!(ErrorCategory::Vsam.code(), "VS");
        assert_eq!(
            ErrorCategory::from_code("VL"),
            Some(ErrorCategory::Validation)
        );
        assert_eq!(ErrorCategory::from_code("XX"), None);
    }

    // -- ErrorSeverity ------------------------------------------------------

    #[test]
    fn error_severity_roundtrip() {
        for sev in [
            ErrorSeverity::Success,
            ErrorSeverity::Warning,
            ErrorSeverity::Error,
            ErrorSeverity::Severe,
            ErrorSeverity::Terminal,
        ] {
            let json = serde_json::to_string(&sev).unwrap();
            let back: ErrorSeverity = serde_json::from_str(&json).unwrap();
            assert_eq!(sev, back);
        }
    }

    #[test]
    fn error_severity_codes() {
        assert_eq!(ErrorSeverity::Terminal.code(), 16);
        assert_eq!(ErrorSeverity::from_code(8), Some(ErrorSeverity::Error));
        assert_eq!(ErrorSeverity::from_code(99), None);
    }

    // -- ErrorMessage -------------------------------------------------------

    #[test]
    fn error_message_default_matches_cobol_init() {
        let msg = ErrorMessage::default();
        assert!(msg.date.is_empty());
        assert!(msg.time.is_empty());
        assert!(msg.program.is_empty());
        assert!(msg.code.is_empty());
        assert!(msg.text.is_empty());
        assert!(msg.details.is_empty());
        assert_eq!(msg.severity, ErrorSeverity::Success);
    }

    #[test]
    fn error_message_serde_roundtrip() {
        let msg = ErrorMessage {
            date: "2024-03-20".into(),
            time: "10:30:45".into(),
            program: "TRNVAL00".into(),
            category: ErrorCategory::Validation,
            code: "E001".into(),
            severity: ErrorSeverity::Error,
            text: "Invalid amount".into(),
            details: "Amount was negative".into(),
        };
        let json = serde_json::to_string(&msg).unwrap();
        let back: ErrorMessage = serde_json::from_str(&json).unwrap();
        assert_eq!(msg, back);
    }

    #[test]
    fn error_message_validation_pass() {
        let msg = ErrorMessage {
            date: "2024-03-20".into(),
            time: "10:30:45".into(),
            program: "TRNVAL00".into(),
            category: ErrorCategory::Validation,
            code: "E001".into(),
            severity: ErrorSeverity::Error,
            text: "Bad".into(),
            details: "Details".into(),
        };
        assert!(msg.validate().is_ok());
    }

    #[test]
    fn error_message_validation_fail_lengths() {
        let msg = ErrorMessage {
            program: "X".repeat(9),
            code: "X".repeat(5),
            ..ErrorMessage::default()
        };
        let errs = msg.validate().unwrap_err();
        assert_eq!(errs.errors().len(), 2);
    }

    // -- VsamStatus ---------------------------------------------------------

    #[test]
    fn vsam_status_roundtrip() {
        for vs in [
            VsamStatus::Success,
            VsamStatus::DuplicateKey,
            VsamStatus::NotFound,
            VsamStatus::EndOfFile,
        ] {
            let json = serde_json::to_string(&vs).unwrap();
            let back: VsamStatus = serde_json::from_str(&json).unwrap();
            assert_eq!(vs, back);
        }
    }

    #[test]
    fn vsam_status_codes_and_messages() {
        assert_eq!(VsamStatus::NotFound.code(), "23");
        assert_eq!(VsamStatus::NotFound.message(), "Record not found");
        assert_eq!(VsamStatus::from_code("22"), Some(VsamStatus::DuplicateKey));
        assert_eq!(VsamStatus::from_code("99"), None);
    }

    // -- ReturnCodeRequestType ----------------------------------------------

    #[test]
    fn return_code_request_type_roundtrip() {
        for rt in [
            ReturnCodeRequestType::Initialize,
            ReturnCodeRequestType::SetCode,
            ReturnCodeRequestType::GetCode,
            ReturnCodeRequestType::LogCode,
            ReturnCodeRequestType::Analyze,
        ] {
            let json = serde_json::to_string(&rt).unwrap();
            let back: ReturnCodeRequestType = serde_json::from_str(&json).unwrap();
            assert_eq!(rt, back);
        }
    }

    #[test]
    fn return_code_request_type_codes() {
        assert_eq!(ReturnCodeRequestType::Analyze.code(), 'A');
        assert_eq!(
            ReturnCodeRequestType::from_code('S'),
            Some(ReturnCodeRequestType::SetCode)
        );
        assert_eq!(ReturnCodeRequestType::from_code('Z'), None);
    }

    // -- ReturnCodeStatus ---------------------------------------------------

    #[test]
    fn return_code_status_roundtrip() {
        for st in [
            ReturnCodeStatus::Success,
            ReturnCodeStatus::Warning,
            ReturnCodeStatus::Error,
            ReturnCodeStatus::Severe,
        ] {
            let json = serde_json::to_string(&st).unwrap();
            let back: ReturnCodeStatus = serde_json::from_str(&json).unwrap();
            assert_eq!(st, back);
        }
    }

    #[test]
    fn return_code_status_codes() {
        assert_eq!(ReturnCodeStatus::Severe.code(), 'F');
        assert_eq!(
            ReturnCodeStatus::from_code('W'),
            Some(ReturnCodeStatus::Warning)
        );
        assert_eq!(ReturnCodeStatus::from_code('X'), None);
    }

    // -- ReturnCodeArea -----------------------------------------------------

    #[test]
    fn return_code_area_default_matches_cobol_init() {
        let rca = ReturnCodeArea::default();
        assert_eq!(rca.request_type, ReturnCodeRequestType::Initialize);
        assert!(rca.program_id.is_empty());
        assert_eq!(rca.current_code, 0);
        assert_eq!(rca.highest_code, 0);
        assert_eq!(rca.new_code, 0);
        assert_eq!(rca.status, ReturnCodeStatus::Success);
        assert!(rca.message.is_empty());
        assert_eq!(rca.response_code, 0);
        assert!(rca.start_time.is_none());
        assert!(rca.end_time.is_none());
        assert_eq!(rca.total_codes, 0);
        assert_eq!(rca.return_value, 0);
    }

    #[test]
    fn return_code_area_serde_roundtrip() {
        let rca = ReturnCodeArea {
            request_type: ReturnCodeRequestType::SetCode,
            program_id: "POSUPD00".into(),
            current_code: 4,
            highest_code: 8,
            new_code: 4,
            status: ReturnCodeStatus::Warning,
            message: "Minor issue".into(),
            response_code: 0,
            start_time: None,
            end_time: None,
            total_codes: 3,
            max_code: 8,
            min_code: 0,
            return_value: 4,
            highest_return: 8,
            return_status: "W".into(),
        };
        let json = serde_json::to_string(&rca).unwrap();
        let back: ReturnCodeArea = serde_json::from_str(&json).unwrap();
        assert_eq!(rca, back);
    }

    #[test]
    fn return_code_area_validation_fail() {
        let rca = ReturnCodeArea {
            program_id: "X".repeat(9),
            ..ReturnCodeArea::default()
        };
        let errs = rca.validate().unwrap_err();
        assert!(errs.errors().iter().any(|e| e.field == "program_id"));
    }
}
