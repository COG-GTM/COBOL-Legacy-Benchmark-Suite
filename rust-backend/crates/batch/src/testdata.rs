//! Test data generator.
//!
//! Ported from COBOL program `TSTGEN00.cbl`.
//!
//! Generates deterministic test data for system testing:
//! - Portfolio test data
//! - Transaction test scenarios
//! - Position records
//! - Error condition data
//! - Performance test volumes

use chrono::{NaiveDate, NaiveDateTime, NaiveTime};
use rust_decimal::Decimal;

use domain::common::TransactionType;
use domain::portfolio::{ClientType, PortfolioRecord, PortfolioStatus};
use domain::position::{PositionRecord, PositionStatus};
use domain::transaction::{TransactionRecord, TransactionStatus};

// ---------------------------------------------------------------------------
// Configuration — mirrors COBOL CONFIG-RECORD / CFG-TEST-TYPE
// ---------------------------------------------------------------------------

/// Test-data generation type, matching COBOL WS-TEST-TYPES.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TestType {
    Portfolio,
    Transaction,
    Error,
    Volume,
}

/// Configuration for a single generation run (mirrors CONFIG-RECORD).
#[derive(Debug, Clone)]
pub struct GeneratorConfig {
    pub test_type: TestType,
    pub volume: u32,
}

// ---------------------------------------------------------------------------
// Seeded PRNG — mirrors COBOL WS-RANDOM-VALUES / 1200-INIT-RANDOM
// ---------------------------------------------------------------------------

/// Simple linear-congruential PRNG for deterministic generation.
///
/// The COBOL program reads a seed from the RANDOM-SEED file and uses it for
/// all random-value derivation. We replicate that with a portable LCG so that
/// identical seeds produce identical outputs across platforms.
#[derive(Debug, Clone)]
struct Rng {
    state: u64,
}

impl Rng {
    fn new(seed: u64) -> Self {
        Self {
            state: if seed == 0 { 1 } else { seed },
        }
    }

    /// Advance state and return the next pseudo-random `u64`.
    fn next_u64(&mut self) -> u64 {
        // LCG parameters from Numerical Recipes.
        self.state = self
            .state
            .wrapping_mul(6_364_136_223_846_793_005)
            .wrapping_add(1);
        self.state
    }

    /// Return a value in `[0, bound)`.
    fn next_bounded(&mut self, bound: u64) -> u64 {
        if bound == 0 {
            return 0;
        }
        self.next_u64() % bound
    }

    /// Return a `Decimal` in `[0, max)` with two fractional digits.
    fn next_decimal(&mut self, max: u64) -> Decimal {
        let whole = self.next_bounded(max);
        let frac = self.next_bounded(100);
        Decimal::new((whole * 100 + frac) as i64, 2)
    }

    /// Return a `Decimal` in `[0, max)` with four fractional digits.
    fn next_decimal4(&mut self, max: u64) -> Decimal {
        let whole = self.next_bounded(max);
        let frac = self.next_bounded(10_000);
        Decimal::new((whole * 10_000 + frac) as i64, 4)
    }
}

// ---------------------------------------------------------------------------
// Look-up tables — mimic the COBOL EVALUATE branches in 2210/2310
// ---------------------------------------------------------------------------

const CLIENT_TYPES: [ClientType; 3] = [
    ClientType::Individual,
    ClientType::Corporate,
    ClientType::Trust,
];

const PORTFOLIO_STATUSES: [PortfolioStatus; 3] = [
    PortfolioStatus::Active,
    PortfolioStatus::Closed,
    PortfolioStatus::Suspended,
];

const TRANSACTION_TYPES: [TransactionType; 4] = [
    TransactionType::Buy,
    TransactionType::Sell,
    TransactionType::Transfer,
    TransactionType::Fee,
];

const TRANSACTION_STATUSES: [TransactionStatus; 4] = [
    TransactionStatus::Pending,
    TransactionStatus::Done,
    TransactionStatus::Failed,
    TransactionStatus::Reversed,
];

const POSITION_STATUSES: [PositionStatus; 3] = [
    PositionStatus::Active,
    PositionStatus::Closed,
    PositionStatus::Pending,
];

const CURRENCIES: [&str; 5] = ["USD", "EUR", "GBP", "JPY", "CAD"];

const CLIENT_NAMES: [&str; 10] = [
    "SMITH INVESTMENTS LLC",
    "JONES FAMILY TRUST",
    "ACME CORPORATION",
    "GLOBAL PARTNERS INC",
    "PACIFIC RIM FUND",
    "MIDWEST HOLDINGS",
    "ATLANTIC VENTURES",
    "SUMMIT CAPITAL GRP",
    "VALLEY ENTERPRISES",
    "HERITAGE FUND MGMT",
];

const INVESTMENT_IDS: [&str; 8] = [
    "AAPL000001",
    "MSFT000002",
    "GOOG000003",
    "AMZN000004",
    "TSLA000005",
    "JPM0000006",
    "BAC0000007",
    "WFC0000008",
];

// ---------------------------------------------------------------------------
// TestDataGenerator — mirrors COBOL TSTGEN00
// ---------------------------------------------------------------------------

/// Generated test data set.
#[derive(Debug, Clone, Default)]
pub struct GeneratedData {
    pub portfolios: Vec<PortfolioRecord>,
    pub positions: Vec<PositionRecord>,
    pub transactions: Vec<TransactionRecord>,
    pub records_written: u64,
    pub error_count: u64,
}

/// Deterministic test data generator ported from TSTGEN00.cbl.
///
/// Usage:
/// ```
/// use batch::testdata::{TestDataGenerator, GeneratorConfig, TestType};
/// let mut gen = TestDataGenerator::new(42);
/// let data = gen.generate(&[
///     GeneratorConfig { test_type: TestType::Portfolio, volume: 10 },
///     GeneratorConfig { test_type: TestType::Transaction, volume: 50 },
/// ]);
/// assert_eq!(data.portfolios.len(), 10);
/// ```
pub struct TestDataGenerator {
    rng: Rng,
}

impl TestDataGenerator {
    /// Create a generator with the given seed (mirrors 1200-INIT-RANDOM).
    pub fn new(seed: u64) -> Self {
        Self {
            rng: Rng::new(seed),
        }
    }

    /// Run all configs and return the combined data set (mirrors 2000-PROCESS).
    pub fn generate(&mut self, configs: &[GeneratorConfig]) -> GeneratedData {
        let mut data = GeneratedData::default();
        for cfg in configs {
            match cfg.test_type {
                TestType::Portfolio => self.gen_portfolios(cfg.volume, &mut data),
                TestType::Transaction => self.gen_transactions(cfg.volume, &mut data),
                TestType::Error => self.gen_error_data(&mut data),
                TestType::Volume => self.gen_volume_data(&mut data),
            }
        }
        data
    }

    /// Generate `count` portfolio records with associated positions
    /// (mirrors 2200-GEN-PORTFOLIO / 2210-GEN-PORT-DATA).
    fn gen_portfolios(&mut self, count: u32, data: &mut GeneratedData) {
        for _i in 0..count {
            let port = self.gen_one_portfolio(data.portfolios.len() as u32);
            let port_id = port.id.clone();

            // Generate 1-5 positions per portfolio.
            let pos_count = (self.rng.next_bounded(5) + 1) as u32;
            for j in 0..pos_count {
                let pos = self.gen_one_position(&port_id, j);
                data.positions.push(pos);
            }

            data.portfolios.push(port);
            data.records_written += 1;
        }
    }

    fn gen_one_portfolio(&mut self, index: u32) -> PortfolioRecord {
        let id = format!("PT{:06}", index + 1);
        let acct = format!("AC{:08}", self.rng.next_bounded(99_999_999));
        let name_idx = self.rng.next_bounded(CLIENT_NAMES.len() as u64) as usize;
        let ct_idx = self.rng.next_bounded(CLIENT_TYPES.len() as u64) as usize;
        let ps_idx = self.rng.next_bounded(PORTFOLIO_STATUSES.len() as u64) as usize;

        let base_year = 2020 + (self.rng.next_bounded(5) as i32);
        let month = (self.rng.next_bounded(12) + 1) as u32;
        let day = (self.rng.next_bounded(28) + 1) as u32;
        let create_date = NaiveDate::from_ymd_opt(base_year, month, day);
        let maint_date = create_date.map(|d| {
            let offset = self.rng.next_bounded(365) as i64;
            d + chrono::Duration::days(offset)
        });

        let total_value = self.rng.next_decimal(10_000_000);
        let cash_pct = self.rng.next_bounded(30) + 1; // 1-30 %
        let cash_balance =
            ((total_value * Decimal::new(cash_pct as i64, 0)) / Decimal::new(100, 0)).round_dp(2);

        PortfolioRecord {
            id,
            account_no: acct,
            client_name: CLIENT_NAMES[name_idx].to_string(),
            client_type: CLIENT_TYPES[ct_idx],
            create_date,
            last_maintenance_date: maint_date,
            status: PORTFOLIO_STATUSES[ps_idx],
            total_value,
            cash_balance,
            last_user: format!("USR{:05}", self.rng.next_bounded(99_999)),
            last_transaction_date: maint_date,
        }
    }

    fn gen_one_position(&mut self, portfolio_id: &str, index: u32) -> PositionRecord {
        let inv_idx = ((self.rng.next_bounded(INVESTMENT_IDS.len() as u64) as u32 + index)
            as usize)
            % INVESTMENT_IDS.len();
        let cur_idx = self.rng.next_bounded(CURRENCIES.len() as u64) as usize;
        let ps_idx = self.rng.next_bounded(POSITION_STATUSES.len() as u64) as usize;

        let base_year = 2022 + (self.rng.next_bounded(3) as i32);
        let month = (self.rng.next_bounded(12) + 1) as u32;
        let day = (self.rng.next_bounded(28) + 1) as u32;
        let pos_date = NaiveDate::from_ymd_opt(base_year, month, day);

        let quantity = self.rng.next_decimal4(100_000);
        let cost_basis = self.rng.next_decimal(5_000_000);
        let gain_pct = self.rng.next_bounded(40) as i64 - 10; // -10% to +29%
        let market_value = (cost_basis
            + (cost_basis * Decimal::new(gain_pct, 0)) / Decimal::new(100, 0))
        .round_dp(2);

        let maint_dt = pos_date.map(|d| {
            let offset = self.rng.next_bounded(180) as i64;
            let nd = d + chrono::Duration::days(offset);
            NaiveDateTime::new(
                nd,
                NaiveTime::from_hms_opt(
                    self.rng.next_bounded(24) as u32,
                    self.rng.next_bounded(60) as u32,
                    self.rng.next_bounded(60) as u32,
                )
                .unwrap_or_default(),
            )
        });

        PositionRecord {
            portfolio_id: portfolio_id.to_string(),
            date: pos_date,
            investment_id: INVESTMENT_IDS[inv_idx].to_string(),
            quantity,
            cost_basis,
            market_value,
            currency: CURRENCIES[cur_idx].to_string(),
            status: POSITION_STATUSES[ps_idx],
            last_maintenance_date: maint_dt,
            last_maintenance_user: format!("USR{:05}", self.rng.next_bounded(99_999)),
        }
    }

    /// Generate `count` transaction records (mirrors 2300-GEN-TRANSACTION / 2310).
    ///
    /// When no portfolios exist yet, stub portfolios are created so that
    /// referential integrity holds for transaction-only generation runs.
    fn gen_transactions(&mut self, count: u32, data: &mut GeneratedData) {
        // If no portfolios exist and we actually need some, create stubs.
        if count > 0 && data.portfolios.is_empty() {
            self.gen_portfolios(10, data);
        }
        for i in 0..count {
            let port_id = {
                let idx = self.rng.next_bounded(data.portfolios.len() as u64) as usize;
                data.portfolios[idx].id.clone()
            };

            let inv_idx = self.rng.next_bounded(INVESTMENT_IDS.len() as u64) as usize;
            let tt_idx = self.rng.next_bounded(TRANSACTION_TYPES.len() as u64) as usize;
            let ts_idx = self.rng.next_bounded(TRANSACTION_STATUSES.len() as u64) as usize;
            let cur_idx = self.rng.next_bounded(CURRENCIES.len() as u64) as usize;

            let base_year = 2023 + (self.rng.next_bounded(3) as i32);
            let month = (self.rng.next_bounded(12) + 1) as u32;
            let day = (self.rng.next_bounded(28) + 1) as u32;
            let trn_date = NaiveDate::from_ymd_opt(base_year, month, day);
            let trn_time = NaiveTime::from_hms_opt(
                self.rng.next_bounded(24) as u32,
                self.rng.next_bounded(60) as u32,
                self.rng.next_bounded(60) as u32,
            );

            let quantity = self.rng.next_decimal(10_000);
            let price = self.rng.next_decimal(10_000);
            let tt = TRANSACTION_TYPES[tt_idx];
            let amount = if tt == TransactionType::Fee {
                self.rng.next_decimal(10_000)
            } else {
                quantity * price
            };

            let process_dt = trn_date.map(|d| {
                NaiveDateTime::new(
                    d,
                    NaiveTime::from_hms_opt(
                        self.rng.next_bounded(24) as u32,
                        self.rng.next_bounded(60) as u32,
                        self.rng.next_bounded(60) as u32,
                    )
                    .unwrap_or_default(),
                )
            });

            let trn = TransactionRecord {
                date: trn_date,
                time: trn_time,
                portfolio_id: port_id,
                sequence_no: format!("{:06}", i + 1),
                investment_id: INVESTMENT_IDS[inv_idx].to_string(),
                transaction_type: tt,
                quantity,
                price,
                amount,
                currency: CURRENCIES[cur_idx].to_string(),
                status: TRANSACTION_STATUSES[ts_idx],
                process_date: process_dt,
                process_user: format!("USR{:05}", self.rng.next_bounded(99_999)),
            };

            data.transactions.push(trn);
            data.records_written += 1;
        }
    }

    /// Generate intentionally broken records for error-path testing
    /// (mirrors 2400-GEN-ERROR-DATA / 2410 / 2420).
    fn gen_error_data(&mut self, data: &mut GeneratedData) {
        // Portfolio with missing required fields.
        data.portfolios.push(PortfolioRecord {
            id: String::new(),
            account_no: String::new(),
            client_name: String::new(),
            ..PortfolioRecord::default()
        });

        // Portfolio with over-length fields.
        data.portfolios.push(PortfolioRecord {
            id: "TOOLONGID".to_string(),
            account_no: "ACCTEXCEEDSLIMIT".to_string(),
            client_name: "A".repeat(31),
            ..PortfolioRecord::default()
        });

        // Transaction with negative price.
        data.transactions.push(TransactionRecord {
            date: NaiveDate::from_ymd_opt(2024, 1, 1),
            portfolio_id: "PT000001".to_string(),
            investment_id: "BAD0000001".to_string(),
            transaction_type: TransactionType::Buy,
            quantity: Decimal::ZERO,
            price: Decimal::new(-100, 0),
            amount: Decimal::ZERO,
            currency: "USD".to_string(),
            status: TransactionStatus::Pending,
            ..TransactionRecord::default()
        });

        // Transaction with empty required fields.
        data.transactions.push(TransactionRecord::default());

        // Position with negative quantity on active status.
        data.positions.push(PositionRecord {
            portfolio_id: "PT000001".to_string(),
            date: NaiveDate::from_ymd_opt(2024, 6, 15),
            investment_id: "NEG0000001".to_string(),
            quantity: Decimal::new(-500, 0),
            cost_basis: Decimal::new(10_000, 0),
            market_value: Decimal::new(9_000, 0),
            currency: "USD".to_string(),
            status: PositionStatus::Active,
            ..PositionRecord::default()
        });

        data.error_count += 5;
    }

    /// Generate high-volume data for performance testing
    /// (mirrors 2500-GEN-VOLUME-DATA / 2510 / 2520).
    fn gen_volume_data(&mut self, data: &mut GeneratedData) {
        self.gen_portfolios(1_000, data);
        self.gen_transactions(5_000, data);
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn deterministic_with_same_seed() {
        let mut g1 = TestDataGenerator::new(42);
        let d1 = g1.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 5,
        }]);

        let mut g2 = TestDataGenerator::new(42);
        let d2 = g2.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 5,
        }]);

        assert_eq!(d1.portfolios.len(), d2.portfolios.len());
        for (a, b) in d1.portfolios.iter().zip(d2.portfolios.iter()) {
            assert_eq!(a, b);
        }
        for (a, b) in d1.positions.iter().zip(d2.positions.iter()) {
            assert_eq!(a, b);
        }
    }

    #[test]
    fn different_seeds_differ() {
        let mut g1 = TestDataGenerator::new(1);
        let d1 = g1.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 3,
        }]);

        let mut g2 = TestDataGenerator::new(999);
        let d2 = g2.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 3,
        }]);

        // Extremely unlikely to be identical with different seeds.
        assert_ne!(d1.portfolios, d2.portfolios);
    }

    #[test]
    fn zero_volume_produces_nothing() {
        let mut gen = TestDataGenerator::new(7);
        let data = gen.generate(&[
            GeneratorConfig {
                test_type: TestType::Portfolio,
                volume: 0,
            },
            GeneratorConfig {
                test_type: TestType::Transaction,
                volume: 0,
            },
        ]);
        assert!(data.portfolios.is_empty());
        assert!(data.transactions.is_empty());
        assert!(data.positions.is_empty());
    }

    #[test]
    fn portfolio_generation_respects_volume() {
        let mut gen = TestDataGenerator::new(100);
        let data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 20,
        }]);
        assert_eq!(data.portfolios.len(), 20);
        assert!(!data.positions.is_empty());
    }

    #[test]
    fn transaction_generation_respects_volume() {
        let mut gen = TestDataGenerator::new(200);
        let data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Transaction,
            volume: 50,
        }]);
        assert_eq!(data.transactions.len(), 50);
    }

    #[test]
    fn error_data_contains_bad_records() {
        let mut gen = TestDataGenerator::new(300);
        let data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Error,
            volume: 0,
        }]);
        assert!(data.error_count > 0);
        // At least one portfolio should fail validation.
        let bad = data
            .portfolios
            .iter()
            .filter(|p| p.validate().is_err())
            .count();
        assert!(bad > 0, "Expected some invalid portfolios");
    }

    #[test]
    fn volume_data_produces_large_set() {
        let mut gen = TestDataGenerator::new(500);
        let data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Volume,
            volume: 0,
        }]);
        assert!(data.portfolios.len() >= 1_000);
        assert!(data.transactions.len() >= 5_000);
    }

    #[test]
    fn generated_portfolios_have_valid_fields() {
        let mut gen = TestDataGenerator::new(42);
        let data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 10,
        }]);
        for p in &data.portfolios {
            assert!(p.id.len() <= 8);
            assert!(p.account_no.len() <= 10);
            assert!(p.client_name.len() <= 30);
            assert!(p.create_date.is_some());
            assert!(
                p.validate().is_ok(),
                "Portfolio {:?} failed validation",
                p.id
            );
        }
    }

    #[test]
    fn generated_transactions_reference_existing_portfolios() {
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
        let port_ids: std::collections::HashSet<&str> =
            data.portfolios.iter().map(|p| p.id.as_str()).collect();
        for trn in &data.transactions {
            assert!(
                port_ids.contains(trn.portfolio_id.as_str()),
                "Transaction references unknown portfolio {}",
                trn.portfolio_id
            );
        }
    }

    #[test]
    fn positions_reference_their_portfolio() {
        let mut gen = TestDataGenerator::new(42);
        let data = gen.generate(&[GeneratorConfig {
            test_type: TestType::Portfolio,
            volume: 5,
        }]);
        let port_ids: std::collections::HashSet<&str> =
            data.portfolios.iter().map(|p| p.id.as_str()).collect();
        for pos in &data.positions {
            assert!(
                port_ids.contains(pos.portfolio_id.as_str()),
                "Position references unknown portfolio {}",
                pos.portfolio_id
            );
        }
    }
}
