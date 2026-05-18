//! Portfolio master record.
//!
//! Translated from COBOL copybook `PORTFLIO.cpy`.

use chrono::NaiveDate;
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use crate::error::{check_max_len, check_non_empty, ValidationErrors};

// ---------------------------------------------------------------------------
// PORTFLIO.cpy — Client type (PORT-CLIENT-TYPE, level-88)
// ---------------------------------------------------------------------------

/// Portfolio client type: I=Individual, C=Corporate, T=Trust.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ClientType {
    #[serde(rename = "I")]
    Individual,
    #[serde(rename = "C")]
    Corporate,
    #[serde(rename = "T")]
    Trust,
}

impl ClientType {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Individual => "I",
            Self::Corporate => "C",
            Self::Trust => "T",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "I" => Some(Self::Individual),
            "C" => Some(Self::Corporate),
            "T" => Some(Self::Trust),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// PORTFLIO.cpy — Portfolio status (PORT-STATUS, level-88)
// ---------------------------------------------------------------------------

/// Portfolio lifecycle status: A=Active, C=Closed, S=Suspended.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum PortfolioStatus {
    #[serde(rename = "A")]
    Active,
    #[serde(rename = "C")]
    Closed,
    #[serde(rename = "S")]
    Suspended,
}

impl PortfolioStatus {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Active => "A",
            Self::Closed => "C",
            Self::Suspended => "S",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "A" => Some(Self::Active),
            "C" => Some(Self::Closed),
            "S" => Some(Self::Suspended),
            _ => None,
        }
    }
}

// ---------------------------------------------------------------------------
// PORTFLIO.cpy — Portfolio record (PORT-RECORD)
// ---------------------------------------------------------------------------

/// Portfolio master record — flattened from COBOL group structure.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PortfolioRecord {
    // -- PORT-KEY --
    pub id: String,
    pub account_no: String,
    // -- PORT-CLIENT-INFO --
    pub client_name: String,
    pub client_type: ClientType,
    // -- PORT-PORTFOLIO-INFO --
    pub create_date: Option<NaiveDate>,
    pub last_maintenance_date: Option<NaiveDate>,
    pub status: PortfolioStatus,
    // -- PORT-FINANCIAL-INFO --
    pub total_value: Decimal,
    pub cash_balance: Decimal,
    // -- PORT-AUDIT-INFO --
    pub last_user: String,
    pub last_transaction_date: Option<NaiveDate>,
}

impl Default for PortfolioRecord {
    fn default() -> Self {
        Self {
            id: String::new(),
            account_no: String::new(),
            client_name: String::new(),
            client_type: ClientType::Individual,
            create_date: None,
            last_maintenance_date: None,
            status: PortfolioStatus::Active,
            total_value: Decimal::ZERO,
            cash_balance: Decimal::ZERO,
            last_user: String::new(),
            last_transaction_date: None,
        }
    }
}

impl PortfolioRecord {
    pub fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();

        check_non_empty(&mut errors, "id", &self.id);
        check_max_len(&mut errors, "id", &self.id, 8);

        check_non_empty(&mut errors, "account_no", &self.account_no);
        check_max_len(&mut errors, "account_no", &self.account_no, 10);

        check_non_empty(&mut errors, "client_name", &self.client_name);
        check_max_len(&mut errors, "client_name", &self.client_name, 30);

        check_max_len(&mut errors, "last_user", &self.last_user, 8);

        if self.create_date.is_none() {
            errors.add("create_date", "must not be empty");
        }

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

    // -- ClientType ---------------------------------------------------------

    #[test]
    fn client_type_roundtrip() {
        for ct in [
            ClientType::Individual,
            ClientType::Corporate,
            ClientType::Trust,
        ] {
            let json = serde_json::to_string(&ct).unwrap();
            let back: ClientType = serde_json::from_str(&json).unwrap();
            assert_eq!(ct, back);
        }
    }

    #[test]
    fn client_type_codes() {
        assert_eq!(ClientType::Individual.code(), "I");
        assert_eq!(ClientType::Corporate.code(), "C");
        assert_eq!(ClientType::Trust.code(), "T");
        assert_eq!(ClientType::from_code("T"), Some(ClientType::Trust));
        assert_eq!(ClientType::from_code("X"), None);
    }

    // -- PortfolioStatus ----------------------------------------------------

    #[test]
    fn portfolio_status_roundtrip() {
        for ps in [
            PortfolioStatus::Active,
            PortfolioStatus::Closed,
            PortfolioStatus::Suspended,
        ] {
            let json = serde_json::to_string(&ps).unwrap();
            let back: PortfolioStatus = serde_json::from_str(&json).unwrap();
            assert_eq!(ps, back);
        }
    }

    #[test]
    fn portfolio_status_codes() {
        assert_eq!(PortfolioStatus::Active.code(), "A");
        assert_eq!(
            PortfolioStatus::from_code("S"),
            Some(PortfolioStatus::Suspended)
        );
        assert_eq!(PortfolioStatus::from_code("Z"), None);
    }

    // -- PortfolioRecord ----------------------------------------------------

    #[test]
    fn portfolio_default_matches_cobol_init() {
        let p = PortfolioRecord::default();
        assert!(p.id.is_empty());
        assert!(p.account_no.is_empty());
        assert!(p.client_name.is_empty());
        assert_eq!(p.total_value, Decimal::ZERO);
        assert_eq!(p.cash_balance, Decimal::ZERO);
        assert!(p.create_date.is_none());
        assert!(p.last_maintenance_date.is_none());
        assert!(p.last_user.is_empty());
        assert!(p.last_transaction_date.is_none());
    }

    #[test]
    fn portfolio_serde_roundtrip() {
        let p = PortfolioRecord {
            id: "PORT0001".into(),
            account_no: "ACCT000001".into(),
            client_name: "John Doe".into(),
            client_type: ClientType::Individual,
            create_date: Some(NaiveDate::from_ymd_opt(2024, 3, 20).unwrap()),
            last_maintenance_date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
            status: PortfolioStatus::Active,
            total_value: dec!(1234567890123.45),
            cash_balance: dec!(50000.00),
            last_user: "ADMIN".into(),
            last_transaction_date: Some(NaiveDate::from_ymd_opt(2024, 6, 15).unwrap()),
        };
        let json = serde_json::to_string(&p).unwrap();
        let back: PortfolioRecord = serde_json::from_str(&json).unwrap();
        assert_eq!(p, back);
    }

    #[test]
    fn portfolio_validation_pass() {
        let p = PortfolioRecord {
            id: "PORT0001".into(),
            account_no: "ACCT000001".into(),
            client_name: "Jane Smith".into(),
            client_type: ClientType::Corporate,
            create_date: Some(NaiveDate::from_ymd_opt(2024, 1, 1).unwrap()),
            status: PortfolioStatus::Active,
            total_value: dec!(100000.00),
            cash_balance: dec!(10000.00),
            last_user: "ADMIN".into(),
            ..PortfolioRecord::default()
        };
        assert!(p.validate().is_ok());
    }

    #[test]
    fn portfolio_validation_fail_empty_fields() {
        let p = PortfolioRecord::default();
        let errs = p.validate().unwrap_err();
        let fields: Vec<&str> = errs.errors().iter().map(|e| e.field.as_str()).collect();
        assert!(fields.contains(&"id"));
        assert!(fields.contains(&"account_no"));
        assert!(fields.contains(&"client_name"));
        assert!(fields.contains(&"create_date"));
    }

    #[test]
    fn portfolio_validation_fail_field_too_long() {
        let p = PortfolioRecord {
            id: "X".repeat(9),
            account_no: "X".repeat(11),
            client_name: "X".repeat(31),
            create_date: Some(NaiveDate::from_ymd_opt(2024, 1, 1).unwrap()),
            ..PortfolioRecord::default()
        };
        let errs = p.validate().unwrap_err();
        assert!(errs.errors().len() >= 3);
    }
}
