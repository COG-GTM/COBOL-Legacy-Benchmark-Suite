//! Batch control processor.
//!
//! Ported from COBOL program `BCHCTL00.cbl` and copybooks `BCHCTL.cpy` /
//! `BCHCON.cpy`.
//!
//! Manages batch-job lifecycle: initialisation, prerequisite checking,
//! status updates, checkpoint/commit, and termination.

use std::collections::HashMap;
use std::fmt;

use chrono::{NaiveDate, Utc};
use serde::{Deserialize, Serialize};

// ---------------------------------------------------------------------------
// BCHCON.cpy — Batch control constants
// ---------------------------------------------------------------------------

/// Process status values (mirrors BCT-STAT-VALUES).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum JobStatus {
    Ready,
    Active,
    Waiting,
    Done,
    Error,
}

impl JobStatus {
    pub fn code(&self) -> char {
        match self {
            Self::Ready => 'R',
            Self::Active => 'A',
            Self::Waiting => 'W',
            Self::Done => 'D',
            Self::Error => 'E',
        }
    }

    pub fn from_code(c: char) -> Option<Self> {
        match c {
            'R' => Some(Self::Ready),
            'A' => Some(Self::Active),
            'W' => Some(Self::Waiting),
            'D' => Some(Self::Done),
            'E' => Some(Self::Error),
            _ => None,
        }
    }

    pub fn is_terminal(self) -> bool {
        matches!(self, Self::Done | Self::Error)
    }
}

impl fmt::Display for JobStatus {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.code())
    }
}

/// Return-code thresholds (mirrors BCT-RC-THRESHOLDS).
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub struct ReturnCode(pub i16);

impl ReturnCode {
    pub const SUCCESS: Self = Self(0);
    pub const WARNING: Self = Self(4);
    pub const ERROR: Self = Self(8);
    pub const SEVERE: Self = Self(12);
    pub const CRITICAL: Self = Self(16);
}

impl fmt::Display for ReturnCode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

/// Process type (mirrors BCT-PROC-TYPES).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProcessType {
    Initial,
    Update,
    Report,
    Cleanup,
}

impl ProcessType {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Initial => "INI",
            Self::Update => "UPD",
            Self::Report => "RPT",
            Self::Cleanup => "CLN",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "INI" => Some(Self::Initial),
            "UPD" => Some(Self::Update),
            "RPT" => Some(Self::Report),
            "CLN" => Some(Self::Cleanup),
            _ => None,
        }
    }
}

/// Dependency type (mirrors BCT-DEP-TYPES).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum DependencyType {
    Required,
    Optional,
    Exclusive,
}

impl DependencyType {
    pub fn code(&self) -> char {
        match self {
            Self::Required => 'R',
            Self::Optional => 'O',
            Self::Exclusive => 'X',
        }
    }
}

// ---------------------------------------------------------------------------
// BCHCTL.cpy — Batch control record
// ---------------------------------------------------------------------------

/// A prerequisite entry for a batch job (mirrors BCT-PREREQ-JOBS).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Prerequisite {
    pub job_name: String,
    pub sequence_no: u16,
    pub max_return_code: i16,
}

/// Batch control record (mirrors BATCH-CONTROL-RECORD).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BatchControlRecord {
    // -- BCT-KEY --
    pub job_name: String,
    pub process_date: NaiveDate,
    pub sequence_no: u16,
    // -- BCT-DATA --
    pub status: JobStatus,
    pub step_name: String,
    pub program_name: String,
    pub start_time: Option<String>,
    pub end_time: Option<String>,
    // -- BCT-DEPENDENCIES --
    pub prerequisites: Vec<Prerequisite>,
    // -- BCT-RETURN-INFO --
    pub return_code: i16,
    pub error_desc: String,
    // -- BCT-STATISTICS --
    pub restart_count: u16,
    pub attempt_ts: Option<String>,
    pub complete_ts: Option<String>,
}

impl BatchControlRecord {
    pub fn new(job_name: impl Into<String>, process_date: NaiveDate, sequence_no: u16) -> Self {
        Self {
            job_name: job_name.into(),
            process_date,
            sequence_no,
            status: JobStatus::Ready,
            step_name: String::new(),
            program_name: String::new(),
            start_time: None,
            end_time: None,
            prerequisites: Vec::new(),
            return_code: 0,
            error_desc: String::new(),
            restart_count: 0,
            attempt_ts: None,
            complete_ts: None,
        }
    }
}

// ---------------------------------------------------------------------------
// BatchController — port of BCHCTL00
// ---------------------------------------------------------------------------

/// Errors produced by [`BatchController`].
#[derive(Debug, Clone, thiserror::Error)]
pub enum BatchControlError {
    #[error("job not found: {0}")]
    JobNotFound(String),

    #[error("invalid function: {0}")]
    InvalidFunction(String),

    #[error("prerequisites not satisfied for job {0}")]
    PrerequisitesNotMet(String),

    #[error("invalid state transition: {from:?} -> {to:?} for job {job}")]
    InvalidTransition {
        job: String,
        from: JobStatus,
        to: JobStatus,
    },

    #[error("max restarts ({max}) exceeded for job {job}")]
    MaxRestartsExceeded { job: String, max: u16 },

    #[error("batch control error: {0}")]
    Other(String),
}

/// In-memory batch controller (port of BCHCTL00).
///
/// Manages a table of [`BatchControlRecord`]s keyed by `(job_name, process_date)`.
/// Provides the four COBOL entry-points: INIT, CHECK, UPDATE, TERMINATE.
#[derive(Debug, Clone)]
pub struct BatchController {
    records: HashMap<String, BatchControlRecord>,
    max_restarts: u16,
}

impl Default for BatchController {
    fn default() -> Self {
        Self::new()
    }
}

impl BatchController {
    pub fn new() -> Self {
        Self {
            records: HashMap::new(),
            max_restarts: 3, // BCT-MAX-RESTARTS
        }
    }

    pub fn with_max_restarts(mut self, max: u16) -> Self {
        self.max_restarts = max;
        self
    }

    /// Register a control record (mirrors 1000-PROCESS-INITIALIZE).
    pub fn register_job(&mut self, record: BatchControlRecord) {
        let key = Self::make_key(&record.job_name, &record.process_date);
        self.records.insert(key, record);
    }

    /// Initialise a job — set it to Active with a start timestamp
    /// (mirrors FUNC-INIT → 1000-PROCESS-INITIALIZE).
    pub fn init_job(
        &mut self,
        job_name: &str,
        process_date: &NaiveDate,
    ) -> Result<ReturnCode, BatchControlError> {
        let key = Self::make_key(job_name, process_date);
        let rec = self
            .records
            .get_mut(&key)
            .ok_or_else(|| BatchControlError::JobNotFound(job_name.to_string()))?;

        // 1300-VALIDATE-PROCESS: must be Ready or previously failed (for restart).
        match rec.status {
            JobStatus::Ready => {}
            JobStatus::Error => {
                if rec.restart_count >= self.max_restarts {
                    return Err(BatchControlError::MaxRestartsExceeded {
                        job: job_name.to_string(),
                        max: self.max_restarts,
                    });
                }
                rec.restart_count += 1;
            }
            other => {
                return Err(BatchControlError::InvalidTransition {
                    job: job_name.to_string(),
                    from: other,
                    to: JobStatus::Active,
                });
            }
        }

        // 1400-UPDATE-START-STATUS
        rec.status = JobStatus::Active;
        let now = Utc::now();
        rec.start_time = Some(now.format("%H:%M:%S").to_string());
        rec.attempt_ts = Some(now.to_rfc3339());
        rec.return_code = 0;
        rec.error_desc.clear();

        Ok(ReturnCode::SUCCESS)
    }

    /// Check whether all prerequisites for a job are satisfied
    /// (mirrors FUNC-CHEK → 2000-CHECK-PREREQUISITES).
    pub fn check_prerequisites(
        &self,
        job_name: &str,
        process_date: &NaiveDate,
    ) -> Result<ReturnCode, BatchControlError> {
        let key = Self::make_key(job_name, process_date);
        let rec = self
            .records
            .get(&key)
            .ok_or_else(|| BatchControlError::JobNotFound(job_name.to_string()))?;

        // 2200-CHECK-DEPENDENCIES
        for prereq in &rec.prerequisites {
            let dep_key = Self::make_key(&prereq.job_name, process_date);
            match self.records.get(&dep_key) {
                Some(dep_rec) => {
                    if dep_rec.status != JobStatus::Done {
                        return Ok(ReturnCode::WARNING);
                    }
                    if dep_rec.return_code > prereq.max_return_code {
                        return Ok(ReturnCode::ERROR);
                    }
                }
                None => return Ok(ReturnCode::WARNING),
            }
        }

        Ok(ReturnCode::SUCCESS)
    }

    /// Update the status of a running job
    /// (mirrors FUNC-UPDT → 3000-UPDATE-STATUS).
    pub fn update_status(
        &mut self,
        job_name: &str,
        process_date: &NaiveDate,
        new_status: JobStatus,
        return_code: i16,
        error_desc: Option<&str>,
    ) -> Result<ReturnCode, BatchControlError> {
        let key = Self::make_key(job_name, process_date);
        let rec = self
            .records
            .get_mut(&key)
            .ok_or_else(|| BatchControlError::JobNotFound(job_name.to_string()))?;

        // 3200-UPDATE-PROCESS-STATUS
        rec.status = new_status;
        rec.return_code = return_code;
        if let Some(desc) = error_desc {
            rec.error_desc = desc.to_string();
        }

        Ok(ReturnCode::SUCCESS)
    }

    /// Terminate a job — mark as Done/Error and record completion timestamp
    /// (mirrors FUNC-TERM → 4000-PROCESS-TERMINATE).
    pub fn terminate_job(
        &mut self,
        job_name: &str,
        process_date: &NaiveDate,
        return_code: i16,
    ) -> Result<ReturnCode, BatchControlError> {
        let key = Self::make_key(job_name, process_date);
        let rec = self
            .records
            .get_mut(&key)
            .ok_or_else(|| BatchControlError::JobNotFound(job_name.to_string()))?;

        // 4100-UPDATE-COMPLETION
        let now = Utc::now();
        rec.end_time = Some(now.format("%H:%M:%S").to_string());
        rec.complete_ts = Some(now.to_rfc3339());
        rec.return_code = return_code;

        rec.status = if return_code <= ReturnCode::WARNING.0 {
            JobStatus::Done
        } else {
            JobStatus::Error
        };

        Ok(ReturnCode::SUCCESS)
    }

    /// Retrieve a record by key.
    pub fn get_record(
        &self,
        job_name: &str,
        process_date: &NaiveDate,
    ) -> Option<&BatchControlRecord> {
        let key = Self::make_key(job_name, process_date);
        self.records.get(&key)
    }

    /// List all records (snapshot).
    pub fn records(&self) -> impl Iterator<Item = &BatchControlRecord> {
        self.records.values()
    }

    fn make_key(job_name: &str, process_date: &NaiveDate) -> String {
        format!("{job_name}:{process_date}")
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::NaiveDate;

    fn date(y: i32, m: u32, d: u32) -> NaiveDate {
        NaiveDate::from_ymd_opt(y, m, d).unwrap()
    }

    #[test]
    fn job_status_roundtrip() {
        for s in [
            JobStatus::Ready,
            JobStatus::Active,
            JobStatus::Waiting,
            JobStatus::Done,
            JobStatus::Error,
        ] {
            assert_eq!(JobStatus::from_code(s.code()), Some(s));
        }
        assert!(JobStatus::from_code('Z').is_none());
    }

    #[test]
    fn init_sets_active() {
        let mut ctl = BatchController::new();
        let rec = BatchControlRecord::new("TRNVAL00", date(2024, 1, 15), 1);
        ctl.register_job(rec);

        let rc = ctl.init_job("TRNVAL00", &date(2024, 1, 15)).unwrap();
        assert_eq!(rc, ReturnCode::SUCCESS);

        let rec = ctl.get_record("TRNVAL00", &date(2024, 1, 15)).unwrap();
        assert_eq!(rec.status, JobStatus::Active);
        assert!(rec.start_time.is_some());
    }

    #[test]
    fn init_from_error_increments_restart() {
        let mut ctl = BatchController::new();
        let mut rec = BatchControlRecord::new("LOAD01", date(2024, 1, 15), 1);
        rec.status = JobStatus::Error;
        ctl.register_job(rec);

        ctl.init_job("LOAD01", &date(2024, 1, 15)).unwrap();
        let rec = ctl.get_record("LOAD01", &date(2024, 1, 15)).unwrap();
        assert_eq!(rec.restart_count, 1);
        assert_eq!(rec.status, JobStatus::Active);
    }

    #[test]
    fn init_from_active_fails() {
        let mut ctl = BatchController::new();
        let mut rec = BatchControlRecord::new("JOB1", date(2024, 1, 15), 1);
        rec.status = JobStatus::Active;
        ctl.register_job(rec);

        let err = ctl.init_job("JOB1", &date(2024, 1, 15)).unwrap_err();
        assert!(matches!(err, BatchControlError::InvalidTransition { .. }));
    }

    #[test]
    fn max_restarts_exceeded() {
        let mut ctl = BatchController::new().with_max_restarts(2);
        let mut rec = BatchControlRecord::new("JOB2", date(2024, 1, 15), 1);
        rec.status = JobStatus::Error;
        rec.restart_count = 2;
        ctl.register_job(rec);

        let err = ctl.init_job("JOB2", &date(2024, 1, 15)).unwrap_err();
        assert!(matches!(err, BatchControlError::MaxRestartsExceeded { .. }));
    }

    #[test]
    fn prerequisites_all_done() {
        let mut ctl = BatchController::new();
        let d = date(2024, 1, 15);

        let mut dep = BatchControlRecord::new("STEP_A", d, 1);
        dep.status = JobStatus::Done;
        dep.return_code = 0;
        ctl.register_job(dep);

        let mut main_job = BatchControlRecord::new("STEP_B", d, 2);
        main_job.prerequisites.push(Prerequisite {
            job_name: "STEP_A".into(),
            sequence_no: 1,
            max_return_code: 4,
        });
        ctl.register_job(main_job);

        let rc = ctl.check_prerequisites("STEP_B", &d).unwrap();
        assert_eq!(rc, ReturnCode::SUCCESS);
    }

    #[test]
    fn prerequisites_not_done() {
        let mut ctl = BatchController::new();
        let d = date(2024, 1, 15);

        let dep = BatchControlRecord::new("DEP1", d, 1); // still Ready
        ctl.register_job(dep);

        let mut job = BatchControlRecord::new("JOB_X", d, 2);
        job.prerequisites.push(Prerequisite {
            job_name: "DEP1".into(),
            sequence_no: 1,
            max_return_code: 4,
        });
        ctl.register_job(job);

        let rc = ctl.check_prerequisites("JOB_X", &d).unwrap();
        assert_eq!(rc, ReturnCode::WARNING);
    }

    #[test]
    fn prerequisites_bad_return_code() {
        let mut ctl = BatchController::new();
        let d = date(2024, 1, 15);

        let mut dep = BatchControlRecord::new("DEP2", d, 1);
        dep.status = JobStatus::Done;
        dep.return_code = 8;
        ctl.register_job(dep);

        let mut job = BatchControlRecord::new("JOB_Y", d, 2);
        job.prerequisites.push(Prerequisite {
            job_name: "DEP2".into(),
            sequence_no: 1,
            max_return_code: 4,
        });
        ctl.register_job(job);

        let rc = ctl.check_prerequisites("JOB_Y", &d).unwrap();
        assert_eq!(rc, ReturnCode::ERROR);
    }

    #[test]
    fn terminate_success() {
        let mut ctl = BatchController::new();
        let d = date(2024, 1, 15);
        ctl.register_job(BatchControlRecord::new("JOB_T", d, 1));
        ctl.init_job("JOB_T", &d).unwrap();

        let rc = ctl.terminate_job("JOB_T", &d, 0).unwrap();
        assert_eq!(rc, ReturnCode::SUCCESS);

        let rec = ctl.get_record("JOB_T", &d).unwrap();
        assert_eq!(rec.status, JobStatus::Done);
        assert!(rec.complete_ts.is_some());
    }

    #[test]
    fn terminate_with_error() {
        let mut ctl = BatchController::new();
        let d = date(2024, 1, 15);
        ctl.register_job(BatchControlRecord::new("JOB_E", d, 1));
        ctl.init_job("JOB_E", &d).unwrap();

        ctl.terminate_job("JOB_E", &d, 8).unwrap();
        let rec = ctl.get_record("JOB_E", &d).unwrap();
        assert_eq!(rec.status, JobStatus::Error);
    }

    #[test]
    fn update_status_sets_fields() {
        let mut ctl = BatchController::new();
        let d = date(2024, 1, 15);
        ctl.register_job(BatchControlRecord::new("UPD_J", d, 1));
        ctl.init_job("UPD_J", &d).unwrap();

        ctl.update_status("UPD_J", &d, JobStatus::Waiting, 4, Some("waiting on deps"))
            .unwrap();
        let rec = ctl.get_record("UPD_J", &d).unwrap();
        assert_eq!(rec.status, JobStatus::Waiting);
        assert_eq!(rec.return_code, 4);
        assert_eq!(rec.error_desc, "waiting on deps");
    }

    #[test]
    fn return_code_ordering() {
        assert!(ReturnCode::SUCCESS < ReturnCode::WARNING);
        assert!(ReturnCode::WARNING < ReturnCode::ERROR);
        assert!(ReturnCode::ERROR < ReturnCode::SEVERE);
        assert!(ReturnCode::SEVERE < ReturnCode::CRITICAL);
    }

    #[test]
    fn process_type_roundtrip() {
        for pt in [
            ProcessType::Initial,
            ProcessType::Update,
            ProcessType::Report,
            ProcessType::Cleanup,
        ] {
            assert_eq!(ProcessType::from_code(pt.code()), Some(pt));
        }
        assert!(ProcessType::from_code("ZZZ").is_none());
    }

    #[test]
    fn job_not_found() {
        let ctl = BatchController::new();
        let err = ctl
            .check_prerequisites("NOPE", &date(2024, 1, 1))
            .unwrap_err();
        assert!(matches!(err, BatchControlError::JobNotFound(_)));
    }
}
