//! Transaction record.
//!
//! Translated from COBOL copybook `TRNREC.cpy`.

use chrono::{NaiveDate, NaiveDateTime, NaiveTime};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use crate::common::TransactionType;
use crate::error::{check_max_len, check_non_empty, ValidationErrors};

// ---------------------------------------------------------------------------
// TRNREC.cpy — Transaction status (TRN-STATUS, level-88)
// ---------------------------------------------------------------------------

/// Transaction processing status: P=Pending, D=Done, F=Failed, R=Reversed.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum TransactionStatus {
    #[serde(rename = "P")]
    Pending,
    #[serde(rename = "D")]
    Done,
    #[serde(rename = "F")]
    Failed,
    #[serde(rename = "R")]
    Reversed,
}

impl TransactionStatus {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Pending => "P",
            Self::Done => "D",
            Self::Failed => "F",
            Self::Reversed => "R",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "P" => Some(Self::Pending),
            "D" => Some(Self::Done),
            "F" => Some(Self::Failed),
            "R" => Some(Self::Reversed),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// TRNREC.cpy — Transaction record (TRANSACTION-RECORD)
// ---------------------------------------------------------------------------

/// Transaction record — flattened from COBOL group structure.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TransactionRecord {
    // -- TRN-KEY --
    pub date: Option<NaiveDate>,
    pub time: Option<NaiveTime>,
    pub portfolio_id: String,
    pub sequence_no: String,
    // -- TRN-DATA --
    pub investment_id: String,
    pub transaction_type: TransactionType,
    pub quantity: Decimal,
    pub price: Decimal,
    pub amount: Decimal,
    pub currency: String,
    pub status: TransactionStatus,
    // -- TRN-AUDIT --
    pub process_date: Option<NaiveDateTime>,
    pub process_user: String,
}

impl Default for TransactionRecord {
    fn default() -> Self {
        Self {
            date: None,
            time: None,
            portfolio_id: String::new(),
            sequence_no: String::new(),
            investment_id: String::new(),
            transaction_type: TransactionType::Buy,
            quantity: Decimal::ZERO,
            price: Decimal::ZERO,
            amount: Decimal::ZERO,
            currency: String::new(),
            status: TransactionStatus::Pending,
            process_date: None,
            process_user: String::new(),
        }
    }
}

impl TransactionRecord {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();

        if self.date.is_none() {
            errors.add("date", "must not be empty");
        }

        check_non_empty(&mut errors, "portfolio_id", &self.portfolio_id);
        check_max_len(&mut errors, "portfolio_id", &self.portfolio_id, 8);

        check_max_len(&mut errors, "sequence_no", &self.sequence_no, 6);

        check_non_empty(&mut errors, "investment_id", &self.investment_id);
        check_max_len(&mut errors, "investment_id", &self.investment_id, 10);

        if self.quantity == Decimal::ZERO && self.transaction_type != TransactionType::Fee {
            errors.add("quantity", "must be non-zero for non-fee transactions");
        }

        if self.price < Decimal::ZERO {
            errors.add("price", "must not be negative");
        }

        check_non_empty(&mut errors, "currency", &self.currency);
        check_max_len(&mut errors, "currency", &self.currency, 3);

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
    use rust_decimal_macros::dec;

    // -- TransactionStatus --------------------------------------------------

    #[test]
    fn transaction_status_roundtrip() {
        for ts in [
            TransactionStatus::Pending,
            TransactionStatus::Done,
            TransactionStatus::Failed,
            TransactionStatus::Reversed,
        ] {
            let json = serde_json::to_string(&ts).unwrap();
            let back: TransactionStatus = serde_json::from_str(&json).unwrap();
            assert_eq!(ts, back);
        }
    }

    #[test]
    fn transaction_status_codes() {
        assert_eq!(TransactionStatus::Pending.code(), "P");
        assert_eq!(TransactionStatus::Done.code(), "D");
        assert_eq!(
            TransactionStatus::from_code("R"),
            Some(TransactionStatus::Reversed)
        );
        assert_eq!(TransactionStatus::from_code("X"), None);
    }

    // -- TransactionRecord --------------------------------------------------

    #[test]
    fn transaction_default_matches_cobol_init() {
        let t = TransactionRecord::default();
        assert!(t.date.is_none());
        assert!(t.time.is_none());
        assert!(t.portfolio_id.is_empty());
        assert!(t.sequence_no.is_empty());
        assert!(t.investment_id.is_empty());
        assert_eq!(t.quantity, Decimal::ZERO);
        assert_eq!(t.price, Decimal::ZERO);
        assert_eq!(t.amount, Decimal::ZERO);
        assert!(t.currency.is_empty());
        assert_eq!(t.status, TransactionStatus::Pending);
        assert!(t.process_date.is_none());
        assert!(t.process_user.is_empty());
    }

    #[test]
    fn transaction_serde_roundtrip() {
        let t = TransactionRecord {
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            time: Some(NaiveTime::from_hms_opt(14, 30, 0).unwrap()),
            portfolio_id: "PORT0001".into(),
            sequence_no: "000001".into(),
            investment_id: "AAPL000001".into(),
            transaction_type: TransactionType::Buy,
            quantity: dec!(100.0000),
            price: dec!(150.2500),
            amount: dec!(15025.00),
            currency: "USD".into(),
            status: TransactionStatus::Done,
            process_date: Some(
                NaiveDate::from_ymd_opt(2024, 6, 15)
                    .unwrap()
                    .and_hms_opt(14, 30, 0)
                    .unwrap(),
            ),
            process_user: "TRADER01".into(),
        };
        let json = serde_json::to_string(&t).unwrap();
        let back: TransactionRecord = serde_json::from_str(&json).unwrap();
        assert_eq!(t, back);
    }

    #[test]
    fn transaction_validation_pass() {
        let t = TransactionRecord {
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            portfolio_id: "PORT0001".into(),
            sequence_no: "000001".into(),
            investment_id: "AAPL000001".into(),
            transaction_type: TransactionType::Buy,
            quantity: dec!(100),
            price: dec!(150.25),
            amount: dec!(15025.00),
            currency: "USD".into(),
            status: TransactionStatus::Pending,
            process_user: "TRADER01".into(),
            ..TransactionRecord::default()
        };
        assert!(t.validate().is_ok());
    }

    #[test]
    fn transaction_validation_fail_empty_fields() {
        let t = TransactionRecord::default();
        let errs = t.validate().unwrap_err();
        let fields: Vec<&str> = errs.errors().iter().map(|e| e.field.as_str()).collect();
        assert!(fields.contains(&"date"));
        assert!(fields.contains(&"portfolio_id"));
        assert!(fields.contains(&"investment_id"));
        assert!(fields.contains(&"currency"));
    }

    #[test]
    fn transaction_validation_zero_quantity_ok_for_fee() {
        let t = TransactionRecord {
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            portfolio_id: "PORT0001".into(),
            investment_id: "FEE0000001".into(),
            transaction_type: TransactionType::Fee,
            quantity: Decimal::ZERO,
            price: Decimal::ZERO,
            amount: dec!(25.00),
            currency: "USD".into(),
            ..TransactionRecord::default()
        };
        assert!(t.validate().is_ok());
    }

    #[test]
    fn transaction_validation_fail_negative_price() {
        let t = TransactionRecord {
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            portfolio_id: "PORT0001".into(),
            investment_id: "AAPL000001".into(),
            transaction_type: TransactionType::Buy,
            quantity: dec!(100),
            price: dec!(-1),
            currency: "USD".into(),
            ..TransactionRecord::default()
        };
        let errs = t.validate().unwrap_err();
        assert!(errs.errors().iter().any(|e| e.field == "price"));
    }
}
