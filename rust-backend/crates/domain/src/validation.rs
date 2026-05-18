//! Portfolio validation logic.
//!
//! Translated from COBOL program `PORTVALD.cbl` and copybook `PORTVAL.cpy`.
//!
//! The original COBOL subroutine validates four data elements — portfolio ID,
//! account number, investment type, and monetary amount — each with a distinct
//! return code. This module preserves the exact rules while expressing them as
//! idiomatic Rust with a typed error enum.

use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use crate::portfolio::PortfolioRecord;

// ---------------------------------------------------------------------------
// PORTVAL.cpy — Validation constants
// ---------------------------------------------------------------------------

/// Required prefix for portfolio IDs (`VAL-ID-PREFIX`).
const PORTFOLIO_ID_PREFIX: &str = "PORT";

/// Number of numeric digits following the prefix.
const PORTFOLIO_ID_SUFFIX_LEN: usize = 4;

/// Total length of a valid portfolio ID: 4 (prefix) + 4 (digits).
const PORTFOLIO_ID_LEN: usize = PORTFOLIO_ID_PREFIX.len() + PORTFOLIO_ID_SUFFIX_LEN;

/// Required length of an account number (`PIC X(10)` all-numeric).
const ACCOUNT_NUMBER_LEN: usize = 10;

/// Minimum allowed monetary amount (`VAL-MIN-AMOUNT`, S9(13)V99).
const MIN_AMOUNT: Decimal = Decimal::from_parts(2_764_472_319, 232_830, 0, true, 2);

/// Maximum allowed monetary amount (`VAL-MAX-AMOUNT`, S9(13)V99).
const MAX_AMOUNT: Decimal = Decimal::from_parts(2_764_472_319, 232_830, 0, false, 2);

/// Allowed investment type codes (COBOL level-88 values in `3000-VALIDATE-TYPE`).
const VALID_INVESTMENT_TYPES: &[&str] = &["STK", "BND", "MMF", "ETF"];

// ---------------------------------------------------------------------------
// PORTVAL.cpy — Validation return codes → Rust error enum
// ---------------------------------------------------------------------------

/// Validation error variants mirroring `VAL-RETURN-CODES` in `PORTVAL.cpy`.
///
/// | COBOL code | Variant              |
/// |------------|----------------------|
/// | 0          | *(Ok — no error)*    |
/// | 1          | `InvalidPortfolioId` |
/// | 2          | `InvalidAccount`     |
/// | 3          | `InvalidInvestmentType` |
/// | 4          | `InvalidAmount`      |
#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error, Serialize, Deserialize)]
pub enum PortfolioValidationError {
    #[error("Invalid Portfolio ID format: {reason}")]
    InvalidPortfolioId { reason: String },

    #[error("Invalid Account Number format: {reason}")]
    InvalidAccount { reason: String },

    #[error("Invalid Investment Type: {reason}")]
    InvalidInvestmentType { reason: String },

    #[error("Amount outside valid range: {reason}")]
    InvalidAmount { reason: String },
}

impl PortfolioValidationError {
    /// Return code matching the COBOL `VAL-RETURN-CODES`.
    pub fn cobol_return_code(&self) -> i16 {
        match self {
            Self::InvalidPortfolioId { .. } => 1,
            Self::InvalidAccount { .. } => 2,
            Self::InvalidInvestmentType { .. } => 3,
            Self::InvalidAmount { .. } => 4,
        }
    }
}

// ---------------------------------------------------------------------------
// 1000-VALIDATE-ID
// ---------------------------------------------------------------------------

/// Validate a portfolio ID per `PORTVALD.cbl` section `1000-VALIDATE-ID`.
///
/// Rules:
/// 1. Must be exactly 8 characters long.
/// 2. First 4 characters must be `"PORT"`.
/// 3. Last 4 characters must be ASCII digits.
pub fn validate_portfolio_id(id: &str) -> Result<(), PortfolioValidationError> {
    if id.len() != PORTFOLIO_ID_LEN {
        return Err(PortfolioValidationError::InvalidPortfolioId {
            reason: format!(
                "must be exactly {PORTFOLIO_ID_LEN} characters, got {}",
                id.len()
            ),
        });
    }

    if !id.starts_with(PORTFOLIO_ID_PREFIX) {
        return Err(PortfolioValidationError::InvalidPortfolioId {
            reason: format!("must start with '{PORTFOLIO_ID_PREFIX}'"),
        });
    }

    let suffix = &id[PORTFOLIO_ID_PREFIX.len()..];
    if !suffix.chars().all(|c| c.is_ascii_digit()) {
        return Err(PortfolioValidationError::InvalidPortfolioId {
            reason: "trailing 4 characters must be numeric digits".into(),
        });
    }

    Ok(())
}

// ---------------------------------------------------------------------------
// 2000-VALIDATE-ACCOUNT
// ---------------------------------------------------------------------------

/// Validate an account number per `PORTVALD.cbl` section `2000-VALIDATE-ACCOUNT`.
///
/// Rules:
/// 1. Must be exactly 10 characters long.
/// 2. All characters must be ASCII digits.
/// 3. Must not be all zeros.
pub fn validate_account(account: &str) -> Result<(), PortfolioValidationError> {
    if account.len() != ACCOUNT_NUMBER_LEN {
        return Err(PortfolioValidationError::InvalidAccount {
            reason: format!(
                "must be exactly {ACCOUNT_NUMBER_LEN} characters, got {}",
                account.len()
            ),
        });
    }

    if !account.chars().all(|c| c.is_ascii_digit()) {
        return Err(PortfolioValidationError::InvalidAccount {
            reason: "must contain only numeric digits".into(),
        });
    }

    if account.chars().all(|c| c == '0') {
        return Err(PortfolioValidationError::InvalidAccount {
            reason: "must not be all zeros".into(),
        });
    }

    Ok(())
}

// ---------------------------------------------------------------------------
// 3000-VALIDATE-TYPE
// ---------------------------------------------------------------------------

/// Validate an investment type code per `PORTVALD.cbl` section `3000-VALIDATE-TYPE`.
///
/// Allowed values: `STK`, `BND`, `MMF`, `ETF`.
pub fn validate_investment_type(inv_type: &str) -> Result<(), PortfolioValidationError> {
    if VALID_INVESTMENT_TYPES.contains(&inv_type) {
        Ok(())
    } else {
        Err(PortfolioValidationError::InvalidInvestmentType {
            reason: format!(
                "'{inv_type}' is not a recognised type; expected one of {:?}",
                VALID_INVESTMENT_TYPES
            ),
        })
    }
}

// ---------------------------------------------------------------------------
// 4000-VALIDATE-AMOUNT
// ---------------------------------------------------------------------------

/// Validate a monetary amount per `PORTVALD.cbl` section `4000-VALIDATE-AMOUNT`.
///
/// The amount must fall within the COBOL `S9(13)V99` range:
/// `[-9_999_999_999_999.99, 9_999_999_999_999.99]`.
pub fn validate_amount(amount: Decimal) -> Result<(), PortfolioValidationError> {
    if amount < MIN_AMOUNT || amount > MAX_AMOUNT {
        return Err(PortfolioValidationError::InvalidAmount {
            reason: format!("{amount} is outside the valid range [{MIN_AMOUNT}, {MAX_AMOUNT}]"),
        });
    }
    Ok(())
}

// ---------------------------------------------------------------------------
// Composite validation (not a direct COBOL paragraph — convenience wrapper)
// ---------------------------------------------------------------------------

/// Validate an entire `PortfolioRecord`, collecting all errors without
/// short-circuiting.
///
/// This extends the per-field validation from `PORTVALD.cbl` to operate on
/// the full `PORTFLIO.cpy` record layout so callers can surface every problem
/// in a single pass.
pub fn validate_portfolio(
    portfolio: &PortfolioRecord,
) -> Result<(), Vec<PortfolioValidationError>> {
    let mut errors = Vec::new();

    if let Err(e) = validate_portfolio_id(&portfolio.id) {
        errors.push(e);
    }
    if let Err(e) = validate_account(&portfolio.account_no) {
        errors.push(e);
    }
    if let Err(e) = validate_amount(portfolio.total_value) {
        errors.push(e);
    }
    if let Err(e) = validate_amount(portfolio.cash_balance) {
        errors.push(e);
    }

    if errors.is_empty() {
        Ok(())
    } else {
        Err(errors)
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use proptest::prelude::*;
    use rust_decimal_macros::dec;

    // =======================================================================
    // Unit tests — validate_portfolio_id
    // =======================================================================

    #[test]
    fn valid_portfolio_ids() {
        assert!(validate_portfolio_id("PORT0001").is_ok());
        assert!(validate_portfolio_id("PORT9999").is_ok());
        assert!(validate_portfolio_id("PORT0000").is_ok());
    }

    #[test]
    fn portfolio_id_wrong_prefix() {
        let err = validate_portfolio_id("XORT0001").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidPortfolioId { .. }
        ));
    }

    #[test]
    fn portfolio_id_too_short() {
        let err = validate_portfolio_id("PORT01").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidPortfolioId { .. }
        ));
    }

    #[test]
    fn portfolio_id_too_long() {
        let err = validate_portfolio_id("PORT00001").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidPortfolioId { .. }
        ));
    }

    #[test]
    fn portfolio_id_non_numeric_suffix() {
        let err = validate_portfolio_id("PORT00AB").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidPortfolioId { .. }
        ));
    }

    #[test]
    fn portfolio_id_empty() {
        let err = validate_portfolio_id("").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidPortfolioId { .. }
        ));
    }

    // =======================================================================
    // Unit tests — validate_account
    // =======================================================================

    #[test]
    fn valid_accounts() {
        assert!(validate_account("1234567890").is_ok());
        assert!(validate_account("0000000001").is_ok());
        assert!(validate_account("9999999999").is_ok());
    }

    #[test]
    fn account_all_zeros() {
        let err = validate_account("0000000000").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidAccount { .. }
        ));
    }

    #[test]
    fn account_too_short() {
        let err = validate_account("123456789").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidAccount { .. }
        ));
    }

    #[test]
    fn account_too_long() {
        let err = validate_account("12345678901").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidAccount { .. }
        ));
    }

    #[test]
    fn account_non_numeric() {
        let err = validate_account("12345678AB").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidAccount { .. }
        ));
    }

    #[test]
    fn account_empty() {
        let err = validate_account("").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidAccount { .. }
        ));
    }

    // =======================================================================
    // Unit tests — validate_investment_type
    // =======================================================================

    #[test]
    fn valid_investment_types() {
        for t in &["STK", "BND", "MMF", "ETF"] {
            assert!(
                validate_investment_type(t).is_ok(),
                "expected {t} to be valid"
            );
        }
    }

    #[test]
    fn investment_type_invalid_code() {
        let err = validate_investment_type("OPT").unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidInvestmentType { .. }
        ));
    }

    #[test]
    fn investment_type_empty() {
        assert!(validate_investment_type("").is_err());
    }

    #[test]
    fn investment_type_case_sensitive() {
        assert!(validate_investment_type("stk").is_err());
        assert!(validate_investment_type("Bnd").is_err());
    }

    // =======================================================================
    // Unit tests — validate_amount
    // =======================================================================

    #[test]
    fn valid_amounts() {
        assert!(validate_amount(dec!(0)).is_ok());
        assert!(validate_amount(dec!(100.50)).is_ok());
        assert!(validate_amount(dec!(-100.50)).is_ok());
        assert!(validate_amount(dec!(9999999999999.99)).is_ok());
        assert!(validate_amount(dec!(-9999999999999.99)).is_ok());
    }

    #[test]
    fn amount_exceeds_max() {
        let err = validate_amount(dec!(10000000000000.00)).unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidAmount { .. }
        ));
    }

    #[test]
    fn amount_below_min() {
        let err = validate_amount(dec!(-10000000000000.00)).unwrap_err();
        assert!(matches!(
            err,
            PortfolioValidationError::InvalidAmount { .. }
        ));
    }

    // =======================================================================
    // Unit tests — validate_portfolio (composite)
    // =======================================================================

    #[test]
    fn valid_portfolio_record() {
        let p = PortfolioRecord {
            id: "PORT0001".into(),
            account_no: "1234567890".into(),
            total_value: dec!(1000.00),
            cash_balance: dec!(500.00),
            ..Default::default()
        };
        assert!(validate_portfolio(&p).is_ok());
    }

    #[test]
    fn portfolio_collects_all_errors() {
        let p = PortfolioRecord {
            id: "BAD".into(),
            account_no: "NOTNUM".into(),
            total_value: dec!(99999999999999.99),
            cash_balance: dec!(-99999999999999.99),
            ..Default::default()
        };
        let errs = validate_portfolio(&p).unwrap_err();
        assert_eq!(errs.len(), 4, "expected 4 errors, got: {errs:?}");
    }

    // =======================================================================
    // Unit tests — cobol_return_code
    // =======================================================================

    #[test]
    fn return_codes_match_cobol() {
        assert_eq!(
            PortfolioValidationError::InvalidPortfolioId {
                reason: String::new()
            }
            .cobol_return_code(),
            1
        );
        assert_eq!(
            PortfolioValidationError::InvalidAccount {
                reason: String::new()
            }
            .cobol_return_code(),
            2
        );
        assert_eq!(
            PortfolioValidationError::InvalidInvestmentType {
                reason: String::new()
            }
            .cobol_return_code(),
            3
        );
        assert_eq!(
            PortfolioValidationError::InvalidAmount {
                reason: String::new()
            }
            .cobol_return_code(),
            4
        );
    }

    // =======================================================================
    // Property-based tests (proptest)
    // =======================================================================

    /// Strategy producing a valid portfolio ID: "PORT" + 4 ASCII digits.
    fn valid_portfolio_id_strategy() -> impl Strategy<Value = String> {
        "[0-9]{4}".prop_map(|digits| format!("PORT{digits}"))
    }

    /// Strategy producing a valid account number: exactly 10 ASCII digits, not
    /// all zeros.
    fn valid_account_strategy() -> impl Strategy<Value = String> {
        "[0-9]{10}".prop_filter("must not be all zeros", |s| s.chars().any(|c| c != '0'))
    }

    /// Strategy producing a valid investment type code.
    fn valid_investment_type_strategy() -> impl Strategy<Value = &'static str> {
        prop_oneof![Just("STK"), Just("BND"), Just("MMF"), Just("ETF"),]
    }

    /// Strategy producing a valid monetary amount within COBOL S9(13)V99 range.
    fn valid_amount_strategy() -> impl Strategy<Value = Decimal> {
        (-999_999_999_999_999i64..=999_999_999_999_999i64).prop_map(|cents| Decimal::new(cents, 2))
    }

    proptest! {
        // -- Portfolio ID ---------------------------------------------------

        #[test]
        fn prop_valid_id_always_passes(id in valid_portfolio_id_strategy()) {
            prop_assert!(validate_portfolio_id(&id).is_ok());
        }

        #[test]
        fn prop_arbitrary_string_never_panics(s in ".*") {
            let _ = validate_portfolio_id(&s);
        }

        #[test]
        fn prop_wrong_prefix_always_fails(
            prefix in "[A-Z]{4}".prop_filter("not PORT", |p| p != "PORT"),
            digits in "[0-9]{4}"
        ) {
            let id = format!("{prefix}{digits}");
            let err = validate_portfolio_id(&id).unwrap_err();
            let is_invalid_id = matches!(err, PortfolioValidationError::InvalidPortfolioId { .. });
            prop_assert!(is_invalid_id);
        }

        #[test]
        fn prop_wrong_length_always_fails(
            extra in "[0-9]{1,8}"
        ) {
            let id = format!("PORT{extra}");
            if id.len() != PORTFOLIO_ID_LEN {
                let err = validate_portfolio_id(&id).unwrap_err();
                let is_invalid_id = matches!(err, PortfolioValidationError::InvalidPortfolioId { .. });
                prop_assert!(is_invalid_id);
            }
        }

        // -- Account --------------------------------------------------------

        #[test]
        fn prop_valid_account_always_passes(acct in valid_account_strategy()) {
            prop_assert!(validate_account(&acct).is_ok());
        }

        #[test]
        fn prop_arbitrary_string_account_never_panics(s in ".*") {
            let _ = validate_account(&s);
        }

        #[test]
        fn prop_non_numeric_account_fails(
            prefix in "[0-9]{5}",
            bad in "[a-zA-Z]{1}",
            suffix in "[0-9]{4}"
        ) {
            let acct = format!("{prefix}{bad}{suffix}");
            let err = validate_account(&acct).unwrap_err();
            let is_invalid_acct = matches!(err, PortfolioValidationError::InvalidAccount { .. });
            prop_assert!(is_invalid_acct);
        }

        // -- Investment type ------------------------------------------------

        #[test]
        fn prop_valid_type_always_passes(t in valid_investment_type_strategy()) {
            prop_assert!(validate_investment_type(t).is_ok());
        }

        #[test]
        fn prop_arbitrary_string_type_never_panics(s in ".*") {
            let _ = validate_investment_type(&s);
        }

        #[test]
        fn prop_random_three_letter_type_fails(
            s in "[A-Z]{3}".prop_filter(
                "not a valid type",
                |s| !VALID_INVESTMENT_TYPES.contains(&s.as_str())
            )
        ) {
            let err = validate_investment_type(&s).unwrap_err();
            let is_invalid_type = matches!(err, PortfolioValidationError::InvalidInvestmentType { .. });
            prop_assert!(is_invalid_type);
        }

        // -- Amount ---------------------------------------------------------

        #[test]
        fn prop_valid_amount_always_passes(amt in valid_amount_strategy()) {
            prop_assert!(validate_amount(amt).is_ok());
        }

        #[test]
        fn prop_amount_above_max_fails(
            extra in 1i64..=999_999_999_999i64
        ) {
            let amt = MAX_AMOUNT + Decimal::new(extra, 2);
            let err = validate_amount(amt).unwrap_err();
            let is_invalid_amt = matches!(err, PortfolioValidationError::InvalidAmount { .. });
            prop_assert!(is_invalid_amt);
        }

        #[test]
        fn prop_amount_below_min_fails(
            extra in 1i64..=999_999_999_999i64
        ) {
            let amt = MIN_AMOUNT - Decimal::new(extra, 2);
            let err = validate_amount(amt).unwrap_err();
            let is_invalid_amt = matches!(err, PortfolioValidationError::InvalidAmount { .. });
            prop_assert!(is_invalid_amt);
        }

        // -- Composite portfolio validation ---------------------------------

        #[test]
        fn prop_valid_portfolio_always_passes(
            id in valid_portfolio_id_strategy(),
            acct in valid_account_strategy(),
            total in valid_amount_strategy(),
            cash in valid_amount_strategy(),
        ) {
            let p = PortfolioRecord {
                id,
                account_no: acct,
                total_value: total,
                cash_balance: cash,
                ..Default::default()
            };
            prop_assert!(validate_portfolio(&p).is_ok());
        }
    }
}
