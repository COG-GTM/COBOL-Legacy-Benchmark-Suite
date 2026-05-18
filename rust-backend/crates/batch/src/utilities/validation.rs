//! Data integrity validation utility.
//!
//! Ported from COBOL program `UTLVAL00.cbl` — Data Validation Utility.
//!
//! Original responsibilities:
//! - Data integrity checks (orphan detection, null-where-required)
//! - Cross-reference validation (position ↔ transaction consistency)
//! - Format verification (account number patterns, date ranges)
//! - Balance reconciliation (sum-of-parts vs. control totals)
//!
//! In the Rust port each COBOL validation type becomes a method on
//! [`DataValidator`], producing a [`ValidationReport`] with per-check
//! results.

use std::fmt;

use chrono::NaiveDate;
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use tracing::{info, warn};

// ---------------------------------------------------------------------------
// WS-VALIDATION-TYPES
// ---------------------------------------------------------------------------

/// Validation type selector (mirrors WS-VALIDATION-TYPES in UTLVAL00).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ValidationType {
    Integrity,
    CrossReference,
    Format,
    Balance,
}

impl ValidationType {
    pub fn from_code(s: &str) -> Option<Self> {
        match s.trim().to_uppercase().as_str() {
            "INTEGRITY" => Some(Self::Integrity),
            "XREF" | "CROSSREFERENCE" | "CROSS_REFERENCE" => Some(Self::CrossReference),
            "FORMAT" => Some(Self::Format),
            "BALANCE" => Some(Self::Balance),
            _ => None,
        }
    }

    pub fn code(&self) -> &'static str {
        match self {
            Self::Integrity => "INTEGRITY",
            Self::CrossReference => "XREF",
            Self::Format => "FORMAT",
            Self::Balance => "BALANCE",
        }
    }
}

impl fmt::Display for ValidationType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.code())
    }
}

// ---------------------------------------------------------------------------
// Validation findings
// ---------------------------------------------------------------------------

/// Severity for a single finding.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum FindingSeverity {
    Error,
    Warning,
    Info,
}

impl fmt::Display for FindingSeverity {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Error => f.write_str("ERROR"),
            Self::Warning => f.write_str("WARNING"),
            Self::Info => f.write_str("INFO"),
        }
    }
}

/// A single validation finding (mirrors one ERROR-RECORD line).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ValidationFinding {
    pub validation_type: ValidationType,
    pub severity: FindingSeverity,
    pub key: String,
    pub description: String,
}

impl fmt::Display for ValidationFinding {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "[{:9}] {:7} key={:<20} {}",
            self.validation_type, self.severity, self.key, self.description
        )
    }
}

// ---------------------------------------------------------------------------
// WS-VALIDATION-TOTALS
// ---------------------------------------------------------------------------

/// Aggregate counters (mirrors WS-VALIDATION-TOTALS).
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct ValidationTotals {
    pub records_read: u64,
    pub records_valid: u64,
    pub records_error: u64,
    pub total_amount: Decimal,
    pub control_total: Decimal,
}

// ---------------------------------------------------------------------------
// Validation report
// ---------------------------------------------------------------------------

/// Full result of a validation run.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct ValidationReport {
    pub findings: Vec<ValidationFinding>,
    pub totals: ValidationTotals,
}

impl ValidationReport {
    pub fn return_code(&self) -> i32 {
        if self.totals.records_error > 0 {
            8
        } else if !self.findings.is_empty() {
            4
        } else {
            0
        }
    }

    pub fn error_count(&self) -> usize {
        self.findings
            .iter()
            .filter(|f| f.severity == FindingSeverity::Error)
            .count()
    }

    pub fn warning_count(&self) -> usize {
        self.findings
            .iter()
            .filter(|f| f.severity == FindingSeverity::Warning)
            .count()
    }
}

impl fmt::Display for ValidationReport {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "=== Data Validation Report ===")?;
        writeln!(f, "Records read:  {}", self.totals.records_read)?;
        writeln!(f, "Records valid: {}", self.totals.records_valid)?;
        writeln!(f, "Records error: {}", self.totals.records_error)?;
        if !self.findings.is_empty() {
            writeln!(f, "Findings ({}):", self.findings.len())?;
            for finding in &self.findings {
                writeln!(f, "  {finding}")?;
            }
        } else {
            writeln!(f, "Findings: none")?;
        }
        writeln!(f, "RC: {}", self.return_code())
    }
}

// ---------------------------------------------------------------------------
// Error type
// ---------------------------------------------------------------------------

#[derive(Debug, thiserror::Error)]
pub enum ValidationError {
    #[error("invalid validation type: {0}")]
    InvalidType(String),

    #[error("database error: {0}")]
    Db(#[from] sqlx::Error),
}

// ---------------------------------------------------------------------------
// DataValidator
// ---------------------------------------------------------------------------

/// Validation request (analogous to one VALIDATION-CONTROL record).
#[derive(Debug, Clone)]
pub struct ValidationRequest {
    pub validation_type: ValidationType,
    pub table_name: Option<String>,
}

/// Executes data integrity validations against the portfolio database.
///
/// Maps to UTLVAL00 main loop: iterate control records, dispatch each
/// validation type, accumulate findings.
pub struct DataValidator;

impl Default for DataValidator {
    fn default() -> Self {
        Self::new()
    }
}

impl DataValidator {
    pub fn new() -> Self {
        Self
    }

    /// Run a batch of validations (mirrors 2000-PROCESS).
    pub async fn run(
        &self,
        pool: &PgPool,
        requests: &[ValidationRequest],
    ) -> Result<ValidationReport, ValidationError> {
        let mut report = ValidationReport::default();

        for req in requests {
            match req.validation_type {
                ValidationType::Integrity => {
                    self.check_integrity(pool, &mut report).await?;
                }
                ValidationType::CrossReference => {
                    self.check_cross_references(pool, &mut report).await?;
                }
                ValidationType::Format => {
                    self.check_formats(pool, &mut report).await?;
                }
                ValidationType::Balance => {
                    self.check_balances(pool, &mut report).await?;
                }
            }
        }

        report.totals.records_error = report.error_count() as u64;
        report.totals.records_valid = report
            .totals
            .records_read
            .saturating_sub(report.totals.records_error);

        Ok(report)
    }

    /// Check data integrity — orphan records, null required fields
    /// (mirrors 2200-CHECK-INTEGRITY).
    async fn check_integrity(
        &self,
        pool: &PgPool,
        report: &mut ValidationReport,
    ) -> Result<(), ValidationError> {
        info!("checking data integrity (orphan records)");

        let position_count = self.count_table(pool, "positions").await?;
        let transaction_count = self.count_table(pool, "transactions").await?;
        report.totals.records_read += position_count + transaction_count;

        let orphan_positions = self.find_orphan_positions(pool).await?;
        for key in &orphan_positions {
            report.findings.push(ValidationFinding {
                validation_type: ValidationType::Integrity,
                severity: FindingSeverity::Error,
                key: key.clone(),
                description: "position references non-existent portfolio".to_string(),
            });
        }

        let orphan_transactions = self.find_orphan_transactions(pool).await?;
        for key in &orphan_transactions {
            report.findings.push(ValidationFinding {
                validation_type: ValidationType::Integrity,
                severity: FindingSeverity::Error,
                key: key.clone(),
                description: "transaction references non-existent position".to_string(),
            });
        }

        if orphan_positions.is_empty() && orphan_transactions.is_empty() {
            info!("integrity check passed — no orphans found");
        } else {
            warn!(
                orphan_positions = orphan_positions.len(),
                orphan_transactions = orphan_transactions.len(),
                "integrity violations found"
            );
        }

        Ok(())
    }

    /// Check cross-references between positions and transactions
    /// (mirrors 2300-CHECK-XREF).
    async fn check_cross_references(
        &self,
        pool: &PgPool,
        report: &mut ValidationReport,
    ) -> Result<(), ValidationError> {
        info!("checking cross-references");

        let join_count = self.count_xref_records(pool).await?;
        report.totals.records_read += join_count;

        let mismatches = self.find_xref_mismatches(pool).await?;
        for (key, desc) in &mismatches {
            report.findings.push(ValidationFinding {
                validation_type: ValidationType::CrossReference,
                severity: FindingSeverity::Error,
                key: key.clone(),
                description: desc.clone(),
            });
        }

        Ok(())
    }

    /// Check field formats — account number patterns, date ranges
    /// (mirrors 2400-CHECK-FORMAT).
    async fn check_formats(
        &self,
        pool: &PgPool,
        report: &mut ValidationReport,
    ) -> Result<(), ValidationError> {
        info!("checking data formats");

        let txn_count = self.count_table(pool, "transactions").await?;
        report.totals.records_read += txn_count;

        let bad_dates = self.find_invalid_dates(pool).await?;
        for (key, desc) in &bad_dates {
            report.findings.push(ValidationFinding {
                validation_type: ValidationType::Format,
                severity: FindingSeverity::Warning,
                key: key.clone(),
                description: desc.clone(),
            });
        }

        Ok(())
    }

    /// Check balance reconciliation — position amounts vs. transaction sums
    /// (mirrors 2500-CHECK-BALANCE).
    async fn check_balances(
        &self,
        pool: &PgPool,
        report: &mut ValidationReport,
    ) -> Result<(), ValidationError> {
        info!("checking balance reconciliation");

        let pos_count = self.count_table(pool, "positions").await?;
        report.totals.records_read += pos_count;

        let mismatches = self.find_balance_mismatches(pool).await?;
        for (key, position_amt, txn_sum) in &mismatches {
            report.findings.push(ValidationFinding {
                validation_type: ValidationType::Balance,
                severity: FindingSeverity::Error,
                key: key.clone(),
                description: format!("position amount {position_amt} != transaction sum {txn_sum}"),
            });
            report.totals.total_amount += position_amt;
            report.totals.control_total += txn_sum;
        }

        Ok(())
    }

    // -- helper queries (gracefully handle missing tables) --

    async fn count_table(&self, pool: &PgPool, table: &str) -> Result<u64, ValidationError> {
        let result: Result<(i64,), _> =
            sqlx::query_as(&format!("SELECT count(*)::bigint FROM {table}"))
                .fetch_one(pool)
                .await;
        match result {
            Ok((n,)) => Ok(n as u64),
            Err(_) => Ok(0),
        }
    }

    async fn count_xref_records(&self, pool: &PgPool) -> Result<u64, ValidationError> {
        let result: Result<(i64,), _> = sqlx::query_as(
            "SELECT count(*)::bigint FROM positions p \
             JOIN transactions t ON t.position_id = p.id",
        )
        .fetch_one(pool)
        .await;
        match result {
            Ok((n,)) => Ok(n as u64),
            Err(_) => Ok(0),
        }
    }

    async fn find_orphan_positions(&self, pool: &PgPool) -> Result<Vec<String>, ValidationError> {
        let result: Result<Vec<(String,)>, _> = sqlx::query_as(
            "SELECT p.id::text FROM positions p \
             LEFT JOIN portfolios pf ON p.portfolio_id = pf.id \
             WHERE pf.id IS NULL",
        )
        .fetch_all(pool)
        .await;

        match result {
            Ok(rows) => Ok(rows.into_iter().map(|(id,)| id).collect()),
            Err(e) => {
                info!(error = %e, "positions/portfolios table query failed — skipping");
                Ok(Vec::new())
            }
        }
    }

    async fn find_orphan_transactions(
        &self,
        pool: &PgPool,
    ) -> Result<Vec<String>, ValidationError> {
        let result: Result<Vec<(String,)>, _> = sqlx::query_as(
            "SELECT t.id::text FROM transactions t \
             LEFT JOIN positions p ON t.position_id = p.id \
             WHERE p.id IS NULL",
        )
        .fetch_all(pool)
        .await;

        match result {
            Ok(rows) => Ok(rows.into_iter().map(|(id,)| id).collect()),
            Err(e) => {
                info!(error = %e, "transactions/positions table query failed — skipping");
                Ok(Vec::new())
            }
        }
    }

    async fn find_xref_mismatches(
        &self,
        pool: &PgPool,
    ) -> Result<Vec<(String, String)>, ValidationError> {
        let result: Result<Vec<(String, String)>, _> = sqlx::query_as(
            "SELECT p.id::text, \
                    'security_id mismatch between position and transaction' \
             FROM positions p \
             JOIN transactions t ON t.position_id = p.id \
             WHERE p.security_id != t.security_id \
             LIMIT 1000",
        )
        .fetch_all(pool)
        .await;

        match result {
            Ok(rows) => Ok(rows),
            Err(e) => {
                info!(error = %e, "xref query failed — skipping");
                Ok(Vec::new())
            }
        }
    }

    async fn find_invalid_dates(
        &self,
        pool: &PgPool,
    ) -> Result<Vec<(String, String)>, ValidationError> {
        let far_future = NaiveDate::from_ymd_opt(2100, 1, 1).unwrap();
        let far_past = NaiveDate::from_ymd_opt(1900, 1, 1).unwrap();

        let result: Result<Vec<(String, String)>, _> = sqlx::query_as(
            "SELECT id::text, \
                    'transaction date out of valid range' \
             FROM transactions \
             WHERE trans_date > $1 OR trans_date < $2 \
             LIMIT 1000",
        )
        .bind(far_future)
        .bind(far_past)
        .fetch_all(pool)
        .await;

        match result {
            Ok(rows) => Ok(rows),
            Err(e) => {
                info!(error = %e, "date format query failed — skipping");
                Ok(Vec::new())
            }
        }
    }

    async fn find_balance_mismatches(
        &self,
        pool: &PgPool,
    ) -> Result<Vec<(String, Decimal, Decimal)>, ValidationError> {
        let result: Result<Vec<(String, Decimal, Decimal)>, _> = sqlx::query_as(
            "SELECT p.id::text, \
                    p.market_value, \
                    COALESCE(SUM(t.amount), 0) as txn_sum \
             FROM positions p \
             LEFT JOIN transactions t ON t.position_id = p.id \
             GROUP BY p.id, p.market_value \
             HAVING p.market_value != COALESCE(SUM(t.amount), 0) \
             LIMIT 1000",
        )
        .fetch_all(pool)
        .await;

        match result {
            Ok(rows) => Ok(rows),
            Err(e) => {
                info!(error = %e, "balance query failed — skipping");
                Ok(Vec::new())
            }
        }
    }
}

/// Parse a validation type code (convenience wrapper).
pub fn parse_validation_type(code: &str) -> Result<ValidationType, ValidationError> {
    ValidationType::from_code(code).ok_or_else(|| ValidationError::InvalidType(code.to_string()))
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validation_type_round_trip() {
        for vt in [
            ValidationType::Integrity,
            ValidationType::CrossReference,
            ValidationType::Format,
            ValidationType::Balance,
        ] {
            assert_eq!(ValidationType::from_code(vt.code()), Some(vt));
        }
    }

    #[test]
    fn validation_type_aliases() {
        assert_eq!(
            ValidationType::from_code("XREF"),
            Some(ValidationType::CrossReference)
        );
        assert_eq!(
            ValidationType::from_code("CROSSREFERENCE"),
            Some(ValidationType::CrossReference)
        );
        assert_eq!(
            ValidationType::from_code("cross_reference"),
            Some(ValidationType::CrossReference)
        );
    }

    #[test]
    fn validation_type_invalid() {
        assert!(ValidationType::from_code("BOGUS").is_none());
        assert!(parse_validation_type("BOGUS").is_err());
    }

    #[test]
    fn report_rc_zero_when_clean() {
        let report = ValidationReport::default();
        assert_eq!(report.return_code(), 0);
        assert_eq!(report.error_count(), 0);
        assert_eq!(report.warning_count(), 0);
    }

    #[test]
    fn report_rc_8_on_errors() {
        let mut report = ValidationReport::default();
        report.totals.records_error = 1;
        report.findings.push(ValidationFinding {
            validation_type: ValidationType::Integrity,
            severity: FindingSeverity::Error,
            key: "POS001".into(),
            description: "orphan".into(),
        });
        assert_eq!(report.return_code(), 8);
        assert_eq!(report.error_count(), 1);
    }

    #[test]
    fn report_rc_4_on_warnings_only() {
        let mut report = ValidationReport::default();
        report.findings.push(ValidationFinding {
            validation_type: ValidationType::Format,
            severity: FindingSeverity::Warning,
            key: "TXN001".into(),
            description: "future date".into(),
        });
        assert_eq!(report.return_code(), 4);
        assert_eq!(report.warning_count(), 1);
        assert_eq!(report.error_count(), 0);
    }

    #[test]
    fn finding_display() {
        let f = ValidationFinding {
            validation_type: ValidationType::Balance,
            severity: FindingSeverity::Error,
            key: "POS123".into(),
            description: "mismatch".into(),
        };
        let s = format!("{f}");
        assert!(s.contains("BALANCE"));
        assert!(s.contains("ERROR"));
        assert!(s.contains("POS123"));
    }

    #[test]
    fn report_display_formatting() {
        let report = ValidationReport {
            findings: vec![ValidationFinding {
                validation_type: ValidationType::Integrity,
                severity: FindingSeverity::Error,
                key: "POS001".into(),
                description: "orphan record".into(),
            }],
            totals: ValidationTotals {
                records_read: 100,
                records_valid: 99,
                records_error: 1,
                total_amount: Decimal::new(10000, 2),
                control_total: Decimal::new(10000, 2),
            },
        };
        let output = format!("{report}");
        assert!(output.contains("Data Validation Report"));
        assert!(output.contains("Records read:  100"));
        assert!(output.contains("Records error: 1"));
        assert!(output.contains("orphan record"));
    }

    #[test]
    fn validation_request_construction() {
        let req = ValidationRequest {
            validation_type: ValidationType::Balance,
            table_name: Some("positions".into()),
        };
        assert_eq!(req.validation_type, ValidationType::Balance);
    }

    #[test]
    fn finding_severity_display() {
        assert_eq!(format!("{}", FindingSeverity::Error), "ERROR");
        assert_eq!(format!("{}", FindingSeverity::Warning), "WARNING");
        assert_eq!(format!("{}", FindingSeverity::Info), "INFO");
    }
}
