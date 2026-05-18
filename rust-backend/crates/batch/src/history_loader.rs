//! History loading ETL pipeline.
//!
//! Ported from COBOL program `HISTLD00.cbl`.
//!
//! Reads transaction-history records from an input source, inserts them
//! into the position-history table (DB2 `POSHIST`), and performs periodic
//! commit / checkpoint operations to allow restart after failure.

use std::fmt;

use chrono::{NaiveDate, NaiveDateTime, NaiveTime, Utc};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};
use tracing::info;

// ---------------------------------------------------------------------------
// Input record — mirrors COBOL HISTREC.cpy / TH-* fields
// ---------------------------------------------------------------------------

/// A transaction-history input record (mirrors COBOL TH-* fields).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HistoryInputRecord {
    pub account_no: String,
    pub portfolio_id: String,
    pub trans_date: NaiveDate,
    pub trans_time: NaiveTime,
    pub trans_type: String,
    pub security_id: String,
    pub quantity: Decimal,
    pub price: Decimal,
    pub amount: Decimal,
    pub fees: Decimal,
    pub total_amount: Decimal,
    pub cost_basis: Decimal,
    pub gain_loss: Decimal,
}

// ---------------------------------------------------------------------------
// Output record — mirrors COBOL POSHIST-RECORD / PH-* fields
// ---------------------------------------------------------------------------

/// A position-history output record (mirrors COBOL PH-* fields).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PositionHistoryRecord {
    pub account_no: String,
    pub portfolio_id: String,
    pub trans_date: NaiveDate,
    pub trans_time: NaiveTime,
    pub trans_type: String,
    pub security_id: String,
    pub quantity: Decimal,
    pub price: Decimal,
    pub amount: Decimal,
    pub fees: Decimal,
    pub total_amount: Decimal,
    pub cost_basis: Decimal,
    pub gain_loss: Decimal,
}

impl From<&HistoryInputRecord> for PositionHistoryRecord {
    fn from(input: &HistoryInputRecord) -> Self {
        Self {
            account_no: input.account_no.clone(),
            portfolio_id: input.portfolio_id.clone(),
            trans_date: input.trans_date,
            trans_time: input.trans_time,
            trans_type: input.trans_type.clone(),
            security_id: input.security_id.clone(),
            quantity: input.quantity,
            price: input.price,
            amount: input.amount,
            fees: input.fees,
            total_amount: input.total_amount,
            cost_basis: input.cost_basis,
            gain_loss: input.gain_loss,
        }
    }
}

// ---------------------------------------------------------------------------
// Checkpoint — mirrors COBOL CKPRST.cpy
// ---------------------------------------------------------------------------

/// Checkpoint state persisted between commits (mirrors COBOL checkpoint
/// fields in BCHCTL record and CKPRST copybook).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Checkpoint {
    pub program_id: String,
    pub run_date: NaiveDate,
    pub status: CheckpointStatus,
    pub records_read: u64,
    pub records_written: u64,
    pub records_error: u64,
    pub restart_count: u16,
    pub last_key: String,
    pub last_time: Option<NaiveDateTime>,
    pub phase: CheckpointPhase,
    pub commit_freq: u32,
    pub max_errors: u32,
    pub max_restarts: u16,
}

/// Checkpoint status (mirrors CK-STATUS level-88 values).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum CheckpointStatus {
    Initial,
    Active,
    Complete,
    Failed,
    Restarted,
}

impl CheckpointStatus {
    pub fn code(&self) -> char {
        match self {
            Self::Initial => 'I',
            Self::Active => 'A',
            Self::Complete => 'C',
            Self::Failed => 'F',
            Self::Restarted => 'R',
        }
    }

    pub fn from_code(c: char) -> Option<Self> {
        match c {
            'I' => Some(Self::Initial),
            'A' => Some(Self::Active),
            'C' => Some(Self::Complete),
            'F' => Some(Self::Failed),
            'R' => Some(Self::Restarted),
            _ => None,
        }
    }
}

/// Processing phase (mirrors CK-PHASE level-88 values).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum CheckpointPhase {
    Init,
    Read,
    Process,
    Update,
    Terminate,
}

impl CheckpointPhase {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Init => "00",
            Self::Read => "10",
            Self::Process => "20",
            Self::Update => "30",
            Self::Terminate => "40",
        }
    }
}

impl Default for Checkpoint {
    fn default() -> Self {
        Self {
            program_id: "HISTLD00".into(),
            run_date: Utc::now().date_naive(),
            status: CheckpointStatus::Initial,
            records_read: 0,
            records_written: 0,
            records_error: 0,
            restart_count: 0,
            last_key: String::new(),
            last_time: None,
            phase: CheckpointPhase::Init,
            commit_freq: 1000, // WS-COMMIT-THRESHOLD
            max_errors: 100,   // error threshold from HISTLD00
            max_restarts: 3,   // CK-MAX-RESTARTS
        }
    }
}

// ---------------------------------------------------------------------------
// Load result
// ---------------------------------------------------------------------------

/// Outcome of a single record load attempt.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LoadOutcome {
    Inserted,
    Duplicate,
    Error,
}

/// Statistics produced by the ETL pipeline (mirrors COBOL 3400-DISPLAY-STATS).
#[derive(Debug, Clone, Default)]
pub struct LoadStats {
    pub records_read: u64,
    pub records_written: u64,
    pub records_duplicate: u64,
    pub error_count: u64,
    pub commits: u64,
}

impl fmt::Display for LoadStats {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "HISTLD00 Processing Statistics:")?;
        writeln!(f, "  Records Read:    {}", self.records_read)?;
        writeln!(f, "  Records Written: {}", self.records_written)?;
        writeln!(f, "  Duplicates:      {}", self.records_duplicate)?;
        writeln!(f, "  Errors:          {}", self.error_count)?;
        writeln!(f, "  Commits:         {}", self.commits)?;
        Ok(())
    }
}

// ---------------------------------------------------------------------------
// HistoryLoader — port of HISTLD00
// ---------------------------------------------------------------------------

/// Errors produced by [`HistoryLoader`].
#[derive(Debug, Clone, thiserror::Error)]
pub enum HistoryLoadError {
    #[error("max errors ({max}) exceeded — aborting")]
    MaxErrorsExceeded { max: u32 },

    #[error("checkpoint not found for restart")]
    CheckpointNotFound,

    #[error("load error: {0}")]
    Other(String),
}

/// ETL pipeline for loading transaction history into the position-history
/// table (port of HISTLD00).
///
/// Operates in-memory for testability. Callers provide input records and a
/// sink callback; the loader handles chunked commits and checkpoint updates.
#[derive(Debug)]
pub struct HistoryLoader {
    checkpoint: Checkpoint,
    stats: LoadStats,
    commit_count: u32,
    output: Vec<PositionHistoryRecord>,
}

impl Default for HistoryLoader {
    fn default() -> Self {
        Self::new()
    }
}

impl HistoryLoader {
    pub fn new() -> Self {
        Self {
            checkpoint: Checkpoint::default(),
            stats: LoadStats::default(),
            commit_count: 0,
            output: Vec::new(),
        }
    }

    pub fn with_checkpoint(mut self, checkpoint: Checkpoint) -> Self {
        self.checkpoint = checkpoint;
        self
    }

    pub fn with_commit_frequency(mut self, freq: u32) -> Self {
        self.checkpoint.commit_freq = freq;
        self
    }

    pub fn with_max_errors(mut self, max: u32) -> Self {
        self.checkpoint.max_errors = max;
        self
    }

    /// Run the full ETL pipeline (mirrors 0000-MAIN).
    ///
    /// `records` is the input stream of history records.
    /// `load_fn` is called for each record to simulate the DB2 INSERT;
    /// it returns a [`LoadOutcome`].
    pub fn run<F>(
        &mut self,
        records: &[HistoryInputRecord],
        mut load_fn: F,
    ) -> Result<LoadStats, HistoryLoadError>
    where
        F: FnMut(&PositionHistoryRecord) -> LoadOutcome,
    {
        // 1000-INITIALIZE — preserve Restarted status for skip logic.
        if self.checkpoint.status != CheckpointStatus::Restarted {
            self.checkpoint.status = CheckpointStatus::Active;
        }
        self.checkpoint.phase = CheckpointPhase::Read;

        // 2000-PROCESS loop
        for input in records {
            // Check restart position: skip records we already processed.
            if self.checkpoint.status == CheckpointStatus::Restarted
                && !self.checkpoint.last_key.is_empty()
            {
                let key = Self::record_key(input);
                if key <= self.checkpoint.last_key {
                    continue;
                }
                self.checkpoint.status = CheckpointStatus::Active;
            }

            self.stats.records_read += 1;
            self.checkpoint.records_read = self.stats.records_read;

            // 2200-LOAD-TO-DB2
            let output_rec = PositionHistoryRecord::from(input);
            let outcome = load_fn(&output_rec);

            match outcome {
                LoadOutcome::Inserted => {
                    self.stats.records_written += 1;
                    self.checkpoint.records_written = self.stats.records_written;
                    self.output.push(output_rec);
                }
                LoadOutcome::Duplicate => {
                    self.stats.records_duplicate += 1;
                }
                LoadOutcome::Error => {
                    self.stats.error_count += 1;
                    self.checkpoint.records_error = self.stats.error_count;
                }
            }

            // 2300-CHECK-COMMIT
            self.commit_count += 1;
            if self.commit_count >= self.checkpoint.commit_freq {
                self.do_commit(input);
            }

            // Error threshold (mirrors WS-ERROR-COUNT > 100 guard)
            if self.stats.error_count > u64::from(self.checkpoint.max_errors) {
                return Err(HistoryLoadError::MaxErrorsExceeded {
                    max: self.checkpoint.max_errors,
                });
            }
        }

        // 3000-TERMINATE: final commit
        self.do_final_commit();

        Ok(self.stats.clone())
    }

    /// Retrieve the current checkpoint (for persistence / restart).
    pub fn checkpoint(&self) -> &Checkpoint {
        &self.checkpoint
    }

    /// Retrieve all output records produced during the run.
    pub fn output(&self) -> &[PositionHistoryRecord] {
        &self.output
    }

    /// Restart from a previous checkpoint.
    pub fn restart_from(checkpoint: Checkpoint) -> Self {
        let mut loader = Self::new();
        loader.checkpoint = checkpoint;
        loader.checkpoint.status = CheckpointStatus::Restarted;
        loader.checkpoint.restart_count += 1;
        loader.stats.records_read = loader.checkpoint.records_read;
        loader.stats.records_written = loader.checkpoint.records_written;
        loader.stats.error_count = loader.checkpoint.records_error;
        loader
    }

    // -- private helpers (mirror HISTLD00 paragraphs) -------------------------

    /// 2310-UPDATE-CHECKPOINT + COMMIT WORK
    fn do_commit(&mut self, last_record: &HistoryInputRecord) {
        self.commit_count = 0;
        self.stats.commits += 1;
        self.checkpoint.last_key = Self::record_key(last_record);
        self.checkpoint.last_time = Some(Utc::now().naive_utc());
        self.checkpoint.phase = CheckpointPhase::Update;
        info!(
            records_read = self.stats.records_read,
            records_written = self.stats.records_written,
            "checkpoint committed"
        );
        self.checkpoint.phase = CheckpointPhase::Read;
    }

    /// Build a composite key that uniquely identifies a record's position
    /// in the input stream. Includes date, time, and security to avoid
    /// collisions when multiple transactions share the same account/portfolio.
    fn record_key(rec: &HistoryInputRecord) -> String {
        format!(
            "{}:{}:{}:{}:{}",
            rec.account_no, rec.portfolio_id, rec.trans_date, rec.trans_time, rec.security_id
        )
    }

    /// 3100-FINAL-COMMIT
    fn do_final_commit(&mut self) {
        self.stats.commits += 1;
        self.checkpoint.status = CheckpointStatus::Complete;
        self.checkpoint.phase = CheckpointPhase::Terminate;
        self.checkpoint.last_time = Some(Utc::now().naive_utc());
        info!("{}", self.stats);
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use rust_decimal_macros::dec;

    fn make_input(account: &str, portfolio: &str) -> HistoryInputRecord {
        HistoryInputRecord {
            account_no: account.into(),
            portfolio_id: portfolio.into(),
            trans_date: NaiveDate::from_ymd_opt(2024, 1, 15).unwrap(),
            trans_time: NaiveTime::from_hms_opt(10, 30, 0).unwrap(),
            trans_type: "BUY".into(),
            security_id: "AAPL000001".into(),
            quantity: dec!(100),
            price: dec!(150.00),
            amount: dec!(15000.00),
            fees: dec!(9.95),
            total_amount: dec!(15009.95),
            cost_basis: dec!(15009.95),
            gain_loss: dec!(0),
        }
    }

    #[test]
    fn basic_load_all_inserted() {
        let records: Vec<_> = (0..5)
            .map(|i| make_input(&format!("ACC{i:04}"), &format!("PF{i:04}")))
            .collect();

        let mut loader = HistoryLoader::new().with_commit_frequency(3);
        let stats = loader.run(&records, |_| LoadOutcome::Inserted).unwrap();

        assert_eq!(stats.records_read, 5);
        assert_eq!(stats.records_written, 5);
        assert_eq!(stats.error_count, 0);
        assert!(stats.commits >= 2); // at least one mid-stream + final
    }

    #[test]
    fn duplicates_not_counted_as_written() {
        let records = vec![make_input("ACC1", "PF1"), make_input("ACC2", "PF2")];
        let mut loader = HistoryLoader::new();
        let stats = loader.run(&records, |_| LoadOutcome::Duplicate).unwrap();

        assert_eq!(stats.records_read, 2);
        assert_eq!(stats.records_written, 0);
        assert_eq!(stats.records_duplicate, 2);
    }

    #[test]
    fn error_threshold_triggers_abort() {
        let records: Vec<_> = (0..200)
            .map(|i| make_input(&format!("ACC{i:04}"), "PF0001"))
            .collect();

        let mut loader = HistoryLoader::new().with_max_errors(10);
        let err = loader.run(&records, |_| LoadOutcome::Error).unwrap_err();

        assert!(matches!(err, HistoryLoadError::MaxErrorsExceeded { .. }));
    }

    #[test]
    fn checkpoint_records_progress() {
        let records: Vec<_> = (0..10)
            .map(|i| make_input(&format!("ACC{i:04}"), "PF0001"))
            .collect();

        let mut loader = HistoryLoader::new().with_commit_frequency(4);
        loader.run(&records, |_| LoadOutcome::Inserted).unwrap();

        let ckpt = loader.checkpoint();
        assert_eq!(ckpt.status, CheckpointStatus::Complete);
        assert_eq!(ckpt.records_read, 10);
        assert_eq!(ckpt.records_written, 10);
    }

    #[test]
    fn restart_skips_processed_records() {
        let records: Vec<_> = (0..10)
            .map(|i| make_input(&format!("ACC{i:04}"), "PF0001"))
            .collect();

        // Simulate: first run processed 5 records then failed.
        let ckpt = Checkpoint {
            records_read: 5,
            records_written: 5,
            last_key: "ACC0004:PF0001:2024-01-15:10:30:00:AAPL000001".into(),
            ..Checkpoint::default()
        };

        let mut loader = HistoryLoader::restart_from(ckpt);
        let stats = loader.run(&records, |_| LoadOutcome::Inserted).unwrap();

        // Should have re-read all 10 but only written the remaining 5.
        assert_eq!(stats.records_written, 10); // 5 prior + 5 new
    }

    #[test]
    fn checkpoint_status_roundtrip() {
        for s in [
            CheckpointStatus::Initial,
            CheckpointStatus::Active,
            CheckpointStatus::Complete,
            CheckpointStatus::Failed,
            CheckpointStatus::Restarted,
        ] {
            assert_eq!(CheckpointStatus::from_code(s.code()), Some(s));
        }
    }

    #[test]
    fn position_history_from_input() {
        let input = make_input("ACC001", "PF001");
        let output = PositionHistoryRecord::from(&input);
        assert_eq!(output.account_no, "ACC001");
        assert_eq!(output.portfolio_id, "PF001");
        assert_eq!(output.quantity, dec!(100));
    }

    #[test]
    fn output_accumulates() {
        let records = vec![make_input("A1", "P1"), make_input("A2", "P2")];
        let mut loader = HistoryLoader::new();
        loader.run(&records, |_| LoadOutcome::Inserted).unwrap();
        assert_eq!(loader.output().len(), 2);
    }
}
