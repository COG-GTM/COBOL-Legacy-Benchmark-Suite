//! Position record.
//!
//! Translated from COBOL copybook `POSREC.cpy`.

use chrono::{NaiveDate, NaiveDateTime};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use crate::error::{check_max_len, check_non_empty, ValidationErrors};

// ---------------------------------------------------------------------------
// POSREC.cpy — Position status (POS-STATUS, level-88)
// ---------------------------------------------------------------------------

/// Position lifecycle status: A=Active, C=Closed, P=Pending.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum PositionStatus {
    #[serde(rename = "A")]
    Active,
    #[serde(rename = "C")]
    Closed,
    #[serde(rename = "P")]
    Pending,
}

impl PositionStatus {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Active => "A",
            Self::Closed => "C",
            Self::Pending => "P",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "A" => Some(Self::Active),
            "C" => Some(Self::Closed),
            "P" => Some(Self::Pending),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// POSREC.cpy — Position record (POSITION-RECORD)
// ---------------------------------------------------------------------------

/// Position record — flattened from COBOL group structure.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PositionRecord {
    // -- POS-KEY --
    pub portfolio_id: String,
    pub date: Option<NaiveDate>,
    pub investment_id: String,
    // -- POS-DATA --
    pub quantity: Decimal,
    pub cost_basis: Decimal,
    pub market_value: Decimal,
    pub currency: String,
    pub status: PositionStatus,
    // -- POS-AUDIT --
    pub last_maintenance_date: Option<NaiveDateTime>,
    pub last_maintenance_user: String,
}

impl Default for PositionRecord {
    fn default() -> Self {
        Self {
            portfolio_id: String::new(),
            date: None,
            investment_id: String::new(),
            quantity: Decimal::ZERO,
            cost_basis: Decimal::ZERO,
            market_value: Decimal::ZERO,
            currency: String::new(),
            status: PositionStatus::Active,
            last_maintenance_date: None,
            last_maintenance_user: String::new(),
        }
    }
}

impl PositionRecord {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();

        check_non_empty(&mut errors, "portfolio_id", &self.portfolio_id);
        check_max_len(&mut errors, "portfolio_id", &self.portfolio_id, 8);

        if self.date.is_none() {
            errors.add("date", "must not be empty");
        }

        check_non_empty(&mut errors, "investment_id", &self.investment_id);
        check_max_len(&mut errors, "investment_id", &self.investment_id, 10);

        if self.status == PositionStatus::Active && self.quantity < Decimal::ZERO {
            errors.add("quantity", "must not be negative for active positions");
        }

        if self.cost_basis < Decimal::ZERO {
            errors.add("cost_basis", "must not be negative");
        }

        check_non_empty(&mut errors, "currency", &self.currency);
        check_max_len(&mut errors, "currency", &self.currency, 3);

        check_max_len(
            &mut errors,
            "last_maintenance_user",
            &self.last_maintenance_user,
            8,
        );

        errors.into_result()
    }

    /// Unrealised gain or loss: market value minus cost basis.
    pub fn unrealized_gain_loss(&self) -> Decimal {
        self.market_value - self.cost_basis
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use rust_decimal_macros::dec;

    // -- PositionStatus -----------------------------------------------------

    #[test]
    fn position_status_roundtrip() {
        for ps in [
            PositionStatus::Active,
            PositionStatus::Closed,
            PositionStatus::Pending,
        ] {
            let json = serde_json::to_string(&ps).unwrap();
            let back: PositionStatus = serde_json::from_str(&json).unwrap();
            assert_eq!(ps, back);
        }
    }

    #[test]
    fn position_status_codes() {
        assert_eq!(PositionStatus::Active.code(), "A");
        assert_eq!(PositionStatus::Closed.code(), "C");
        assert_eq!(PositionStatus::Pending.code(), "P");
        assert_eq!(PositionStatus::from_code("A"), Some(PositionStatus::Active));
        assert_eq!(PositionStatus::from_code("Z"), None);
    }

    // -- PositionRecord -----------------------------------------------------

    #[test]
    fn position_default_matches_cobol_init() {
        let p = PositionRecord::default();
        assert!(p.portfolio_id.is_empty());
        assert!(p.date.is_none());
        assert!(p.investment_id.is_empty());
        assert_eq!(p.quantity, Decimal::ZERO);
        assert_eq!(p.cost_basis, Decimal::ZERO);
        assert_eq!(p.market_value, Decimal::ZERO);
        assert!(p.currency.is_empty());
        assert_eq!(p.status, PositionStatus::Active);
        assert!(p.last_maintenance_date.is_none());
        assert!(p.last_maintenance_user.is_empty());
    }

    #[test]
    fn position_serde_roundtrip() {
        let p = PositionRecord {
            portfolio_id: "PORT0001".into(),
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            investment_id: "AAPL000001".into(),
            quantity: dec!(100.0000),
            cost_basis: dec!(15025.00),
            market_value: dec!(16500.00),
            currency: "USD".into(),
            status: PositionStatus::Active,
            last_maintenance_date: Some(
                NaiveDate::from_ymd_opt(2024, 6, 15)
                    .unwrap()
                    .and_hms_opt(14, 30, 0)
                    .unwrap(),
            ),
            last_maintenance_user: "SYSTEM".into(),
        };
        let json = serde_json::to_string(&p).unwrap();
        let back: PositionRecord = serde_json::from_str(&json).unwrap();
        assert_eq!(p, back);
    }

    #[test]
    fn position_unrealized_gain_loss() {
        let p = PositionRecord {
            cost_basis: dec!(15025.00),
            market_value: dec!(16500.00),
            ..PositionRecord::default()
        };
        assert_eq!(p.unrealized_gain_loss(), dec!(1475.00));
    }

    #[test]
    fn position_validation_pass() {
        let p = PositionRecord {
            portfolio_id: "PORT0001".into(),
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            investment_id: "AAPL000001".into(),
            quantity: dec!(100),
            cost_basis: dec!(15025.00),
            market_value: dec!(16500.00),
            currency: "USD".into(),
            status: PositionStatus::Active,
            ..PositionRecord::default()
        };
        assert!(p.validate().is_ok());
    }

    #[test]
    fn position_validation_fail_empty_fields() {
        let p = PositionRecord::default();
        let errs = p.validate().unwrap_err();
        let fields: Vec<&str> = errs.errors().iter().map(|e| e.field.as_str()).collect();
        assert!(fields.contains(&"portfolio_id"));
        assert!(fields.contains(&"date"));
        assert!(fields.contains(&"investment_id"));
        assert!(fields.contains(&"currency"));
    }

    #[test]
    fn position_validation_fail_negative_quantity_when_active() {
        let p = PositionRecord {
            portfolio_id: "PORT0001".into(),
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            investment_id: "AAPL000001".into(),
            quantity: dec!(-10),
            cost_basis: dec!(100),
            currency: "USD".into(),
            status: PositionStatus::Active,
            ..PositionRecord::default()
        };
        let errs = p.validate().unwrap_err();
        assert!(errs.errors().iter().any(|e| e.field == "quantity"));
    }

    #[test]
    fn position_validation_fail_negative_cost_basis() {
        let p = PositionRecord {
            portfolio_id: "PORT0001".into(),
            date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            investment_id: "AAPL000001".into(),
            quantity: dec!(100),
            cost_basis: dec!(-1),
            currency: "USD".into(),
            status: PositionStatus::Active,
            ..PositionRecord::default()
        };
        let errs = p.validate().unwrap_err();
        assert!(errs.errors().iter().any(|e| e.field == "cost_basis"));
    }
}
