//! Error processing utilities.
//!
//! Translated from COBOL program `ERRPROC.cbl` and copybook `ERRHAND.cpy`.
//! Provides structured error logging, severity categorization, and
//! COBOL-compatible return-code mapping.

use chrono::Utc;
use domain::{ErrorCategory, ErrorMessage, ErrorSeverity};
use tracing::{error, info, warn};

/// Maps a COBOL numeric return code to an [`ErrorSeverity`].
///
/// Values follow the COBOL ERR-RETURN-CODES definitions:
/// 0 = Success, 4 = Warning, 8 = Error, 12 = Severe, 16 = Terminal.
/// Codes that fall between defined levels are rounded up to the next severity.
pub fn severity_from_return_code(code: i16) -> ErrorSeverity {
    match code {
        ..=0 => ErrorSeverity::Success,
        1..=4 => ErrorSeverity::Warning,
        5..=8 => ErrorSeverity::Error,
        9..=12 => ErrorSeverity::Severe,
        _ => ErrorSeverity::Terminal,
    }
}

/// Classifies a two-character COBOL error category code into an
/// [`ErrorCategory`], defaulting to [`ErrorCategory::System`] for unknown
/// codes.
pub fn category_from_code(code: &str) -> ErrorCategory {
    ErrorCategory::from_code(code).unwrap_or(ErrorCategory::System)
}

// ---------------------------------------------------------------------------
// ErrorProcessor — port of ERRPROC.cbl
// ---------------------------------------------------------------------------

/// Structured error processor (port of COBOL `ERRPROC`).
///
/// Accepts error requests, formats them with a timestamp, emits structured
/// `tracing` events, and returns the severity as a return code.
#[derive(Debug, Clone)]
pub struct ErrorProcessor {
    program_id: String,
}

impl ErrorProcessor {
    pub fn new(program_id: impl Into<String>) -> Self {
        Self {
            program_id: program_id.into(),
        }
    }

    /// Build an [`ErrorMessage`] and emit it via `tracing`.
    ///
    /// Mirrors ERRPROC's 2000-PROCESS-ERROR paragraph: populates all fields,
    /// writes to the log, displays the error, and returns the severity code.
    pub fn process_error(&self, request: &ErrorRequest) -> i16 {
        let now = Utc::now();
        let msg = ErrorMessage {
            date: now.format("%Y-%m-%d").to_string(),
            time: now.format("%H:%M:%S").to_string(),
            program: self.program_id.clone(),
            category: category_from_code(&request.category),
            code: request.error_code.clone(),
            severity: severity_from_return_code(request.severity),
            text: request.error_text.clone(),
            details: request.error_details.clone(),
        };

        self.write_log(&msg);
        self.display_error(&msg);

        msg.severity.code()
    }

    /// Format an error message string for display or external logging.
    pub fn format_message(msg: &ErrorMessage) -> String {
        format!(
            "[{} {}] {} ({}/{}) severity={}: {} | {}",
            msg.date,
            msg.time,
            msg.program,
            msg.category.code(),
            msg.code,
            msg.severity.code(),
            msg.text,
            msg.details,
        )
    }

    // -- private helpers (mirror ERRPROC paragraphs) --------------------------

    /// 2100-WRITE-LOG: emit a structured `tracing` event.
    fn write_log(&self, msg: &ErrorMessage) {
        match msg.severity {
            ErrorSeverity::Success => {
                info!(
                    program = %msg.program,
                    category = %msg.category.code(),
                    code = %msg.code,
                    severity = msg.severity.code(),
                    text = %msg.text,
                    details = %msg.details,
                    "error_processor"
                );
            }
            ErrorSeverity::Warning => {
                warn!(
                    program = %msg.program,
                    category = %msg.category.code(),
                    code = %msg.code,
                    severity = msg.severity.code(),
                    text = %msg.text,
                    details = %msg.details,
                    "error_processor"
                );
            }
            _ => {
                error!(
                    program = %msg.program,
                    category = %msg.category.code(),
                    code = %msg.code,
                    severity = msg.severity.code(),
                    text = %msg.text,
                    details = %msg.details,
                    "error_processor"
                );
            }
        }
    }

    /// 2200-DISPLAY-ERROR: emit the COBOL-style display block via `tracing`.
    fn display_error(&self, msg: &ErrorMessage) {
        let formatted = Self::format_message(msg);
        match msg.severity {
            ErrorSeverity::Success => info!("{formatted}"),
            ErrorSeverity::Warning => warn!("{formatted}"),
            _ => error!("{formatted}"),
        }
    }
}

// ---------------------------------------------------------------------------
// ErrorRequest — mirrors LS-ERROR-REQUEST linkage
// ---------------------------------------------------------------------------

/// Incoming error request (maps to COBOL `LS-ERROR-REQUEST` linkage section).
#[derive(Debug, Clone)]
pub struct ErrorRequest {
    pub category: String,
    pub error_code: String,
    pub severity: i16,
    pub error_text: String,
    pub error_details: String,
}

impl Default for ErrorRequest {
    fn default() -> Self {
        Self {
            category: "SY".into(),
            error_code: String::new(),
            severity: 0,
            error_text: String::new(),
            error_details: String::new(),
        }
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn severity_mapping() {
        assert_eq!(severity_from_return_code(0), ErrorSeverity::Success);
        assert_eq!(severity_from_return_code(4), ErrorSeverity::Warning);
        assert_eq!(severity_from_return_code(8), ErrorSeverity::Error);
        assert_eq!(severity_from_return_code(12), ErrorSeverity::Severe);
        assert_eq!(severity_from_return_code(16), ErrorSeverity::Terminal);
    }

    #[test]
    fn severity_between_levels_rounds_up() {
        assert_eq!(severity_from_return_code(1), ErrorSeverity::Warning);
        assert_eq!(severity_from_return_code(5), ErrorSeverity::Error);
        assert_eq!(severity_from_return_code(9), ErrorSeverity::Severe);
        assert_eq!(severity_from_return_code(13), ErrorSeverity::Terminal);
    }

    #[test]
    fn negative_codes_map_to_success() {
        assert_eq!(severity_from_return_code(-1), ErrorSeverity::Success);
        assert_eq!(severity_from_return_code(-100), ErrorSeverity::Success);
    }

    #[test]
    fn category_known_codes() {
        assert_eq!(category_from_code("VS"), ErrorCategory::Vsam);
        assert_eq!(category_from_code("VL"), ErrorCategory::Validation);
        assert_eq!(category_from_code("PR"), ErrorCategory::Processing);
        assert_eq!(category_from_code("SY"), ErrorCategory::System);
    }

    #[test]
    fn category_unknown_defaults_to_system() {
        assert_eq!(category_from_code("XX"), ErrorCategory::System);
        assert_eq!(category_from_code(""), ErrorCategory::System);
    }

    #[test]
    fn process_error_returns_severity_code() {
        let proc = ErrorProcessor::new("TESTPGM");
        let req = ErrorRequest {
            category: "PR".into(),
            error_code: "E001".into(),
            severity: 8,
            error_text: "Test error".into(),
            error_details: "detail".into(),
        };
        let code = proc.process_error(&req);
        assert_eq!(code, ErrorSeverity::Error.code());
    }

    #[test]
    fn format_message_output() {
        let msg = ErrorMessage {
            date: "2024-03-20".into(),
            time: "10:30:45".into(),
            program: "TESTPGM".into(),
            category: ErrorCategory::Processing,
            code: "E001".into(),
            severity: ErrorSeverity::Error,
            text: "Something failed".into(),
            details: "extra info".into(),
        };
        let formatted = ErrorProcessor::format_message(&msg);
        assert!(formatted.contains("TESTPGM"));
        assert!(formatted.contains("PR"));
        assert!(formatted.contains("E001"));
        assert!(formatted.contains("Something failed"));
        assert!(formatted.contains("extra info"));
    }

    #[test]
    fn error_request_default() {
        let req = ErrorRequest::default();
        assert_eq!(req.category, "SY");
        assert_eq!(req.severity, 0);
        assert!(req.error_code.is_empty());
    }
}
