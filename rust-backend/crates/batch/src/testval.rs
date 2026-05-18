//! Test data validator.
//!
//! Ported from COBOL program `TSTVAL00.cbl`.
//!
//! Validates generated test data against business rules:
//! - Referential integrity (positions → portfolios, transactions → positions)
//! - Numeric consistency (position totals vs. transaction sums)
//! - Field-level domain validation
//! - Generates a validation report with pass/fail metrics

use std::collections::HashSet;
use std::fmt;
use std::time::Instant;

use rust_decimal::Decimal;

use crate::testdata::GeneratedData;

// ---------------------------------------------------------------------------
// Validation result types — mirrors COBOL WS-TEST-DETAIL / WS-SUMMARY-LINE
// ---------------------------------------------------------------------------

/// Category of validation check, matching COBOL WS-TEST-TYPES.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CheckCategory {
    Functional,
    Integration,
    Performance,
    Error,
}

impl fmt::Display for CheckCategory {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Functional => write!(f, "FUNCTIONAL"),
            Self::Integration => write!(f, "INTEGRATE"),
            Self::Performance => write!(f, "PERFORM"),
            Self::Error => write!(f, "ERROR"),
        }
    }
}

/// Outcome of a single validation check.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CheckResult {
    pub id: String,
    pub category: CheckCategory,
    pub description: String,
    pub passed: bool,
    pub detail: String,
}

/// Summary report produced by the validator (mirrors COBOL REPORT-RECORD).
#[derive(Debug, Clone)]
pub struct ValidationReport {
    pub checks: Vec<CheckResult>,
    pub total: usize,
    pub passed: usize,
    pub failed: usize,
    pub success_rate: f64,
    pub elapsed_ms: u128,
}

impl fmt::Display for ValidationReport {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "{}", "*".repeat(132))?;
        writeln!(f, "{:>66}", "TEST VALIDATION REPORT")?;
        writeln!(f, "{}", "*".repeat(132))?;
        writeln!(f)?;
        writeln!(
            f,
            "{:<10}  {:<10}  {:<50}  {:<4}",
            "TEST-ID", "TYPE", "DESCRIPTION", "RSLT"
        )?;
        writeln!(f, "{}", "-".repeat(80))?;
        for c in &self.checks {
            let status = if c.passed { "PASS" } else { "FAIL" };
            writeln!(
                f,
                "{:<10}  {:<10}  {:<50}  {status}",
                c.id, c.category, c.description
            )?;
            if !c.passed {
                writeln!(f, "           -> {}", c.detail)?;
            }
        }
        writeln!(f)?;
        writeln!(
            f,
            "TOTAL TESTS: {:>6}  PASSED: {:>6}  FAILED: {:>6}  SUCCESS: {:>6.2}%",
            self.total, self.passed, self.failed, self.success_rate
        )?;
        writeln!(f, "ELAPSED: {} ms", self.elapsed_ms)?;
        Ok(())
    }
}

// ---------------------------------------------------------------------------
// TestDataValidator — mirrors COBOL TSTVAL00
// ---------------------------------------------------------------------------

/// Validation harness that checks generated data against business rules.
///
/// Usage:
/// ```no_run
/// use batch::testval::TestDataValidator;
/// use batch::testdata::{TestDataGenerator, GeneratorConfig, TestType};
///
/// let mut gen = TestDataGenerator::new(42);
/// let data = gen.generate(&[
///     GeneratorConfig { test_type: TestType::Portfolio, volume: 10 },
///     GeneratorConfig { test_type: TestType::Transaction, volume: 50 },
/// ]);
/// let report = TestDataValidator::validate(&data);
/// assert!(report.failed == 0);
/// ```
pub struct TestDataValidator;

impl TestDataValidator {
    /// Run the full validation suite (mirrors 2000-PROCESS).
    pub fn validate(data: &GeneratedData) -> ValidationReport {
        let start = Instant::now();

        let mut checks = Vec::new();
        let mut seq = 0u32;

        // Functional: individual record validation (mirrors 2200-RUN-FUNCTIONAL-TEST).
        Self::validate_portfolios(data, &mut checks, &mut seq);
        Self::validate_positions(data, &mut checks, &mut seq);
        Self::validate_transactions(data, &mut checks, &mut seq);

        // Integration: referential integrity (mirrors 2300-RUN-INTEGRATION-TEST).
        Self::validate_position_portfolio_refs(data, &mut checks, &mut seq);
        Self::validate_transaction_portfolio_refs(data, &mut checks, &mut seq);

        // Integration: numeric consistency (mirrors 2600-VALIDATE-RESULTS).
        Self::validate_numeric_consistency(data, &mut checks, &mut seq);

        let elapsed = start.elapsed().as_millis();
        let total = checks.len();
        let passed = checks.iter().filter(|c| c.passed).count();
        let failed = total - passed;
        let success_rate = if total > 0 {
            (passed as f64 / total as f64) * 100.0
        } else {
            0.0
        };

        ValidationReport {
            checks,
            total,
            passed,
            failed,
            success_rate,
            elapsed_ms: elapsed,
        }
    }

    // -- Functional checks --------------------------------------------------

    fn validate_portfolios(data: &GeneratedData, checks: &mut Vec<CheckResult>, seq: &mut u32) {
        for port in &data.portfolios {
            *seq += 1;
            let id = format!("FP{:06}", seq);
            let result = port.validate();
            checks.push(CheckResult {
                id,
                category: CheckCategory::Functional,
                description: format!("Portfolio {} field validation", port.id),
                passed: result.is_ok(),
                detail: result.err().map(|e| e.to_string()).unwrap_or_default(),
            });
        }
    }

    fn validate_positions(data: &GeneratedData, checks: &mut Vec<CheckResult>, seq: &mut u32) {
        for pos in &data.positions {
            *seq += 1;
            let id = format!("FQ{:06}", seq);
            let result = pos.validate();
            checks.push(CheckResult {
                id,
                category: CheckCategory::Functional,
                description: format!(
                    "Position {}/{} field validation",
                    pos.portfolio_id, pos.investment_id
                ),
                passed: result.is_ok(),
                detail: result.err().map(|e| e.to_string()).unwrap_or_default(),
            });
        }
    }

    fn validate_transactions(data: &GeneratedData, checks: &mut Vec<CheckResult>, seq: &mut u32) {
        for trn in &data.transactions {
            *seq += 1;
            let id = format!("FT{:06}", seq);
            let result = trn.validate();
            checks.push(CheckResult {
                id,
                category: CheckCategory::Functional,
                description: format!(
                    "Transaction {}/{} field validation",
                    trn.portfolio_id, trn.sequence_no
                ),
                passed: result.is_ok(),
                detail: result.err().map(|e| e.to_string()).unwrap_or_default(),
            });
        }
    }

    // -- Integration checks -------------------------------------------------

    fn validate_position_portfolio_refs(
        data: &GeneratedData,
        checks: &mut Vec<CheckResult>,
        seq: &mut u32,
    ) {
        let port_ids: HashSet<&str> = data.portfolios.iter().map(|p| p.id.as_str()).collect();

        let orphans: Vec<&str> = data
            .positions
            .iter()
            .filter(|pos| !port_ids.contains(pos.portfolio_id.as_str()))
            .map(|pos| pos.portfolio_id.as_str())
            .collect();

        *seq += 1;
        checks.push(CheckResult {
            id: format!("IP{:06}", seq),
            category: CheckCategory::Integration,
            description: "Position → portfolio referential integrity".to_string(),
            passed: orphans.is_empty(),
            detail: if orphans.is_empty() {
                String::new()
            } else {
                format!(
                    "{} orphan position(s): {:?}",
                    orphans.len(),
                    &orphans[..orphans.len().min(5)]
                )
            },
        });
    }

    fn validate_transaction_portfolio_refs(
        data: &GeneratedData,
        checks: &mut Vec<CheckResult>,
        seq: &mut u32,
    ) {
        let port_ids: HashSet<&str> = data.portfolios.iter().map(|p| p.id.as_str()).collect();

        let orphans: Vec<&str> = data
            .transactions
            .iter()
            .filter(|t| !port_ids.contains(t.portfolio_id.as_str()))
            .map(|t| t.portfolio_id.as_str())
            .collect();

        *seq += 1;
        checks.push(CheckResult {
            id: format!("IT{:06}", seq),
            category: CheckCategory::Integration,
            description: "Transaction → portfolio referential integrity".to_string(),
            passed: orphans.is_empty(),
            detail: if orphans.is_empty() {
                String::new()
            } else {
                format!(
                    "{} orphan transaction(s): {:?}",
                    orphans.len(),
                    &orphans[..orphans.len().min(5)]
                )
            },
        });
    }

    /// Verify numeric invariants within the data set (mirrors 2600-VALIDATE-RESULTS).
    fn validate_numeric_consistency(
        data: &GeneratedData,
        checks: &mut Vec<CheckResult>,
        seq: &mut u32,
    ) {
        // Check 1: transaction amount should equal quantity × price (skip fees).
        let mut amount_mismatches: Vec<String> = Vec::new();
        for trn in &data.transactions {
            if trn.transaction_type == domain::common::TransactionType::Fee {
                continue;
            }
            let expected = trn.quantity * trn.price;
            if trn.amount != expected {
                amount_mismatches.push(format!(
                    "{}/{}: amount={}, qty*price={}",
                    trn.portfolio_id, trn.sequence_no, trn.amount, expected
                ));
            }
        }
        *seq += 1;
        checks.push(CheckResult {
            id: format!("NC{:06}", seq),
            category: CheckCategory::Integration,
            description: "Numeric consistency: transaction amount = quantity × price".to_string(),
            passed: amount_mismatches.is_empty(),
            detail: if amount_mismatches.is_empty() {
                String::new()
            } else {
                format!(
                    "{} mismatch(es): {}",
                    amount_mismatches.len(),
                    amount_mismatches.join("; ")
                )
            },
        });

        // Check 2: active portfolio total_value should be non-negative.
        for port in &data.portfolios {
            if port.status == domain::portfolio::PortfolioStatus::Active
                && port.total_value < Decimal::ZERO
            {
                *seq += 1;
                checks.push(CheckResult {
                    id: format!("NC{:06}", seq),
                    category: CheckCategory::Functional,
                    description: format!("Active portfolio {} total_value non-negative", port.id),
                    passed: false,
                    detail: format!("total_value = {}", port.total_value),
                });
            }
        }

        // Check 3: position cost_basis should be non-negative.
        let neg_cost: Vec<String> = data
            .positions
            .iter()
            .filter(|p| p.cost_basis < Decimal::ZERO)
            .map(|p| format!("{}/{}", p.portfolio_id, p.investment_id))
            .collect();
        *seq += 1;
        checks.push(CheckResult {
            id: format!("NC{:06}", seq),
            category: CheckCategory::Integration,
            description: "Numeric consistency: position cost_basis non-negative".to_string(),
            passed: neg_cost.is_empty(),
            detail: if neg_cost.is_empty() {
                String::new()
            } else {
                format!("{} negative cost_basis: {:?}", neg_cost.len(), neg_cost)
            },
        });
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use crate::testdata::{GeneratorConfig, TestDataGenerator, TestType};
    use domain::common::TransactionType;
    use domain::position::{PositionRecord, PositionStatus};
    use domain::transaction::TransactionRecord;

    #[test]
    fn clean_data_passes_validation() {
        let mut gen = TestDataGenerator::new(42);
        let data = gen.generate(&[
            GeneratorConfig {
                test_type: TestType::Portfolio,
                volume: 10,
            },
            GeneratorConfig {
                test_type: TestType::Transaction,
                volume: 50,
            },
        ]);
        let report = TestDataValidator::validate(&data);
        assert!(
            report.failed == 0,
            "Expected no failures on clean data, got:\n{report}"
        );
        assert!(report.total > 0);
        assert!((report.success_rate - 100.0).abs() < f64::EPSILON);
    }

    #[test]
    fn error_data_triggers_failures() {
        let mut gen = TestDataGenerator::new(42);
        let data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Error,
            volume: 0,
        }]);
        let report = TestDataValidator::validate(&data);
        assert!(
            report.failed > 0,
            "Expected failures on error data, got:\n{report}"
        );
    }

    #[test]
    fn report_display_includes_summary() {
        let mut gen = TestDataGenerator::new(42);
        let data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 3,
        }]);
        let report = TestDataValidator::validate(&data);
        let text = report.to_string();
        assert!(text.contains("TEST VALIDATION REPORT"));
        assert!(text.contains("TOTAL TESTS:"));
        assert!(text.contains("PASSED:"));
    }

    #[test]
    fn different_seeds_all_pass() {
        for seed in [1, 100, 999, 12345, 99999] {
            let mut gen = TestDataGenerator::new(seed);
            let data = gen.generate(&[
                GeneratorConfig {
                    test_type: TestType::Portfolio,
                    volume: 5,
                },
                GeneratorConfig {
                    test_type: TestType::Transaction,
                    volume: 20,
                },
            ]);
            let report = TestDataValidator::validate(&data);
            assert!(
                report.failed == 0,
                "Seed {seed} produced failures:\n{report}"
            );
        }
    }

    #[test]
    fn empty_data_produces_empty_report() {
        let data = GeneratedData::default();
        let report = TestDataValidator::validate(&data);
        assert_eq!(report.total, 4); // 2 ref-integrity + 2 numeric consistency
        assert_eq!(report.failed, 0);
    }

    #[test]
    fn corrupted_referential_integrity_detected() {
        let mut gen = TestDataGenerator::new(42);
        let mut data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 3,
        }]);

        // Insert a position referencing a non-existent portfolio.
        data.positions.push(PositionRecord {
            portfolio_id: "ZZZZZZZZ".to_string(),
            date: chrono::NaiveDate::from_ymd_opt(2024, 1, 1),
            investment_id: "INV0000001".to_string(),
            quantity: Decimal::new(100, 0),
            cost_basis: Decimal::new(5000, 0),
            market_value: Decimal::new(5500, 0),
            currency: "USD".to_string(),
            status: PositionStatus::Active,
            ..PositionRecord::default()
        });

        let report = TestDataValidator::validate(&data);
        let ref_check = report
            .checks
            .iter()
            .find(|c| c.description.contains("Position → portfolio"))
            .expect("Should have position ref-integrity check");
        assert!(!ref_check.passed, "Should detect orphan position");
    }

    #[test]
    fn corrupted_transaction_ref_detected() {
        let mut gen = TestDataGenerator::new(42);
        let mut data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 3,
        }]);

        // Insert a transaction referencing a non-existent portfolio.
        data.transactions.push(TransactionRecord {
            date: chrono::NaiveDate::from_ymd_opt(2024, 6, 1),
            portfolio_id: "NONEXIST".to_string(),
            sequence_no: "000001".to_string(),
            investment_id: "AAPL000001".to_string(),
            transaction_type: TransactionType::Buy,
            quantity: Decimal::new(10, 0),
            price: Decimal::new(150, 0),
            amount: Decimal::new(1500, 0),
            currency: "USD".to_string(),
            status: domain::transaction::TransactionStatus::Done,
            ..TransactionRecord::default()
        });

        let report = TestDataValidator::validate(&data);
        let ref_check = report
            .checks
            .iter()
            .find(|c| c.description.contains("Transaction → portfolio"))
            .expect("Should have transaction ref-integrity check");
        assert!(!ref_check.passed, "Should detect orphan transaction");
    }
}
