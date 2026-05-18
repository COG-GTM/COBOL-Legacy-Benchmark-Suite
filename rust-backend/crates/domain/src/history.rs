//! History record.
//!
//! Translated from COBOL copybook `HISTREC.cpy`.

use chrono::{NaiveDate, NaiveDateTime, NaiveTime};
use serde::{Deserialize, Serialize};

use crate::error::{check_max_len, check_non_empty, ValidationErrors};

// ---------------------------------------------------------------------------
// HISTREC.cpy — Record type (HIST-RECORD-TYPE, level-88)
// ---------------------------------------------------------------------------

/// History record type: PT=Portfolio, PS=Position, TR=Transaction.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum HistoryRecordType {
    Portfolio,
    Position,
    Transaction,
}

impl HistoryRecordType {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Portfolio => "PT",
            Self::Position => "PS",
            Self::Transaction => "TR",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "PT" => Some(Self::Portfolio),
            "PS" => Some(Self::Position),
            "TR" => Some(Self::Transaction),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// HISTREC.cpy — Action code (HIST-ACTION-CODE, level-88)
// ---------------------------------------------------------------------------

/// History action: A=Add, C=Change, D=Delete.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum HistoryAction {
    Add,
    Change,
    Delete,
}

impl HistoryAction {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Add => "A",
            Self::Change => "C",
            Self::Delete => "D",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "A" => Some(Self::Add),
            "C" => Some(Self::Change),
            "D" => Some(Self::Delete),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// HISTREC.cpy — History record (HISTORY-RECORD)
// ---------------------------------------------------------------------------

/// History record capturing before/after images of data changes.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryRecord {
    // -- HIST-KEY --
    pub portfolio_id: String,
    pub date: Option<NaiveDate>,
    pub time: Option<NaiveTime>,
    pub sequence_no: String,
    // -- HIST-DATA --
    pub record_type: HistoryRecordType,
    pub action_code: HistoryAction,
    pub before_image: String,
    pub after_image: String,
    pub reason_code: String,
    // -- HIST-AUDIT --
    pub process_date: Option<NaiveDateTime>,
    pub process_user: String,
}

impl Default for HistoryRecord {
    fn default() -> Self {
        Self {
            portfolio_id: String::new(),
            date: None,
            time: None,
            sequence_no: String::new(),
            record_type: HistoryRecordType::Portfolio,
            action_code: HistoryAction::Add,
            before_image: String::new(),
            after_image: String::new(),
            reason_code: String::new(),
            process_date: None,
            process_user: String::new(),
        }
    }
}

impl HistoryRecord {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();

        check_non_empty(&mut errors, "portfolio_id", &self.portfolio_id);
        check_max_len(&mut errors, "portfolio_id", &self.portfolio_id, 8);

        if self.date.is_none() {
            errors.add("date", "must not be empty");
        }

        check_max_len(&mut errors, "sequence_no", &self.sequence_no, 4);

        check_max_len(&mut errors, "before_image", &self.before_image, 400);
        check_max_len(&mut errors, "after_image", &self.after_image, 400);
        check_max_len(&mut errors, "reason_code", &self.reason_code, 4);

        check_max_len(&mut errors, "process_user", &self.process_user, 8);

        errors.into_result()
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    // -- HistoryRecordType --------------------------------------------------

    #[test]
    fn history_record_type_roundtrip() {
        for rt in [
            HistoryRecordType::Portfolio,
            HistoryRecordType::Position,
            HistoryRecordType::Transaction,
        ] {
            let json = serde_json::to_string(&rt).unwrap();
            let back: HistoryRecordType = serde_json::from_str(&json).unwrap();
            assert_eq!(rt, back);
        }
    }

    #[test]
    fn history_record_type_codes() {
        assert_eq!(HistoryRecordType::Portfolio.code(), "PT");
        assert_eq!(HistoryRecordType::Position.code(), "PS");
        assert_eq!(HistoryRecordType::Transaction.code(), "TR");
        assert_eq!(
            HistoryRecordType::from_code("PS"),
            Some(HistoryRecordType::Position)
        );
        assert_eq!(HistoryRecordType::from_code("XX"), None);
    }

    // -- HistoryAction ------------------------------------------------------

    #[test]
    fn history_action_roundtrip() {
        for ha in [
            HistoryAction::Add,
            HistoryAction::Change,
            HistoryAction::Delete,
        ] {
            let json = serde_json::to_string(&ha).unwrap();
            let back: HistoryAction = serde_json::from_str(&json).unwrap();
            assert_eq!(ha, back);
        }
    }

    #[test]
    fn history_action_codes() {
        assert_eq!(HistoryAction::Add.code(), "A");
        assert_eq!(HistoryAction::Change.code(), "C");
        assert_eq!(HistoryAction::Delete.code(), "D");
        assert_eq!(HistoryAction::from_code("D"), Some(HistoryAction::Delete));
        assert_eq!(HistoryAction::from_code("X"), None);
    }

    // -- HistoryRecord ------------------------------------------------------

    #[test]
    fn history_record_default_matches_cobol_init() {
        let hr = HistoryRecord::default();
        assert!(hr.portfolio_id.is_empty());
        assert!(hr.date.is_none());
        assert!(hr.time.is_none());
        assert!(hr.sequence_no.is_empty());
        assert!(hr.before_image.is_empty());
        assert!(hr.after_image.is_empty());
        assert!(hr.reason_code.is_empty());
        assert!(hr.process_date.is_none());
        assert!(hr.process_user.is_empty());
    }

    #[test]
    fn history_record_serde_roundtrip() {
        let hr = HistoryRecord {
            portfolio_id: "PORT0001".into(),
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            time: Some(NaiveTime::from_hms_opt(14, 30, 0).unwrap()),
            sequence_no: "0001".into(),
            record_type: HistoryRecordType::Transaction,
            action_code: HistoryAction::Add,
            before_image: String::new(),
            after_image: "new transaction record".into(),
            reason_code: "INIT".into(),
            process_date: Some(
                NaiveDate::from_ymd_opt(2024, 6, 15)
                    .unwrap()
                    .and_hms_opt(14, 30, 0)
                    .unwrap(),
            ),
            process_user: "SYSTEM".into(),
        };
        let json = serde_json::to_string(&hr).unwrap();
        let back: HistoryRecord = serde_json::from_str(&json).unwrap();
        assert_eq!(hr, back);
    }

    #[test]
    fn history_record_validation_pass() {
        let hr = HistoryRecord {
            portfolio_id: "PORT0001".into(),
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            sequence_no: "0001".into(),
            record_type: HistoryRecordType::Portfolio,
            action_code: HistoryAction::Change,
            reason_code: "MANT".into(),
            process_user: "ADMIN".into(),
            ..HistoryRecord::default()
        };
        assert!(hr.validate().is_ok());
    }

    #[test]
    fn history_record_validation_fail_empty_fields() {
        let hr = HistoryRecord::default();
        let errs = hr.validate().unwrap_err();
        let fields: Vec<&str> = errs.errors().iter().map(|e| e.field.as_str()).collect();
        assert!(fields.contains(&"portfolio_id"));
        assert!(fields.contains(&"date"));
    }

    #[test]
    fn history_record_validation_fail_image_too_long() {
        let hr = HistoryRecord {
            portfolio_id: "PORT0001".into(),
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            before_image: "X".repeat(401),
            after_image: "X".repeat(401),
            ..HistoryRecord::default()
        };
        let errs = hr.validate().unwrap_err();
        assert!(errs.errors().iter().any(|e| e.field == "before_image"));
        assert!(errs.errors().iter().any(|e| e.field == "after_image"));
    }
}
