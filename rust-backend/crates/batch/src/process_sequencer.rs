//! Process sequence manager.
//!
//! Ported from COBOL program `PRCSEQ00.cbl` and copybook `PRCSEQ.cpy`.
//!
//! Manages ordered execution of batch steps: builds the run sequence from
//! process definitions, checks inter-step dependencies, and tracks
//! completion status.

use std::fmt;

use chrono::NaiveDate;
use serde::{Deserialize, Serialize};

use crate::batch_control::{JobStatus, ReturnCode};

// ---------------------------------------------------------------------------
// PRCSEQ.cpy — Process sequence definitions
// ---------------------------------------------------------------------------

/// Sequence type (mirrors PSR-TYPE level-88 values).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum SequenceType {
    Init,
    Process,
    Report,
    Terminate,
}

impl SequenceType {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Init => "INI",
            Self::Process => "PRC",
            Self::Report => "RPT",
            Self::Terminate => "TRM",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "INI" => Some(Self::Init),
            "PRC" => Some(Self::Process),
            "RPT" => Some(Self::Report),
            "TRM" => Some(Self::Terminate),
            _ => None,
        }
    }
}

/// Scheduling frequency (mirrors PSR-FREQ level-88 values).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Frequency {
    Daily,
    Weekly,
    Monthly,
}

impl Frequency {
    pub fn code(&self) -> char {
        match self {
            Self::Daily => 'D',
            Self::Weekly => 'W',
            Self::Monthly => 'M',
        }
    }
}

/// Dependency type (mirrors PSR-DEP-TYPE: Hard / Soft).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum DepType {
    Hard,
    Soft,
}

impl DepType {
    pub fn code(&self) -> char {
        match self {
            Self::Hard => 'H',
            Self::Soft => 'S',
        }
    }
}

/// A single dependency entry (mirrors PSR-DEP-ENTRY).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Dependency {
    pub process_id: String,
    pub dep_type: DepType,
    pub max_return_code: i16,
}

/// A process-sequence definition (mirrors PROCESS-SEQUENCE-RECORD).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProcessDefinition {
    pub process_id: String,
    pub version: u8,
    pub description: String,
    pub seq_type: SequenceType,
    pub frequency: Frequency,
    pub start_time: u16,
    pub max_time: u16,
    pub dependencies: Vec<Dependency>,
    pub program: String,
    pub parm: String,
    pub max_rc: i16,
    pub restartable: bool,
    pub active_days: String,
    pub month_end: bool,
    pub holiday_run: bool,
    pub recovery_pgm: String,
    pub recovery_parm: String,
    pub error_limit: u16,
}

impl ProcessDefinition {
    pub fn new(process_id: impl Into<String>, seq_type: SequenceType) -> Self {
        Self {
            process_id: process_id.into(),
            version: 1,
            description: String::new(),
            seq_type,
            frequency: Frequency::Daily,
            start_time: 0,
            max_time: 0,
            dependencies: Vec::new(),
            program: String::new(),
            parm: String::new(),
            max_rc: 4,
            restartable: true,
            active_days: "YYYYYNN".into(), // weekdays
            month_end: false,
            holiday_run: false,
            recovery_pgm: String::new(),
            recovery_parm: String::new(),
            error_limit: 100,
        }
    }
}

// ---------------------------------------------------------------------------
// Runtime step tracking — mirrors WS-PROCESS-TABLE
// ---------------------------------------------------------------------------

/// Runtime state of a step within a sequence.
#[derive(Debug, Clone)]
pub struct StepState {
    pub process_id: String,
    pub sequence_no: u16,
    pub status: JobStatus,
    pub return_code: i16,
}

// ---------------------------------------------------------------------------
// ProcessSequencer — port of PRCSEQ00
// ---------------------------------------------------------------------------

/// Errors produced by [`ProcessSequencer`].
#[derive(Debug, Clone, thiserror::Error)]
pub enum SequencerError {
    #[error("no processes match filter (type={0:?}, date={1})")]
    NoProcessesFound(SequenceType, NaiveDate),

    #[error("process definition not found: {0}")]
    ProcessNotFound(String),

    #[error("dependency not satisfied: {process} depends on {dependency}")]
    DependencyNotSatisfied { process: String, dependency: String },

    #[error("sequencer error: {0}")]
    Other(String),
}

/// Standard sequences matching the COBOL STANDARD-SEQUENCES constants.
pub struct StandardSequences;

impl StandardSequences {
    pub const START_OF_DAY: &'static [&'static str] = &["INITDAY", "CKPCLR", "DATEVAL"];
    pub const MAIN_PROCESS: &'static [&'static str] = &["TRNVAL00", "POSUPD00", "HISTLD00"];
    pub const END_OF_DAY: &'static [&'static str] = &["RPTGEN00", "BCKLOD00", "ENDDAY"];
}

/// Process sequencer (port of PRCSEQ00).
///
/// Builds an execution sequence from process definitions, manages
/// dependency checking, and tracks step completion.
#[derive(Debug, Clone)]
pub struct ProcessSequencer {
    definitions: Vec<ProcessDefinition>,
    steps: Vec<StepState>,
    process_date: NaiveDate,
}

impl ProcessSequencer {
    pub fn new(process_date: NaiveDate) -> Self {
        Self {
            definitions: Vec::new(),
            steps: Vec::new(),
            process_date,
        }
    }

    /// Register a process definition.
    pub fn add_definition(&mut self, def: ProcessDefinition) {
        self.definitions.push(def);
    }

    /// Build the execution sequence for a given type (mirrors
    /// FUNC-INIT → 1000-INITIALIZE-SEQUENCE / 1200-BUILD-SEQUENCE).
    pub fn build_sequence(&mut self, seq_type: SequenceType) -> Result<usize, SequencerError> {
        self.steps.clear();

        let matching: Vec<_> = self
            .definitions
            .iter()
            .filter(|d| d.seq_type == seq_type)
            .collect();

        if matching.is_empty() {
            return Err(SequencerError::NoProcessesFound(
                seq_type,
                self.process_date,
            ));
        }

        // 1210-ADD-TO-SEQUENCE — assign sequence numbers in definition order.
        for (i, def) in matching.iter().enumerate() {
            self.steps.push(StepState {
                process_id: def.process_id.clone(),
                sequence_no: (i + 1) as u16,
                status: JobStatus::Ready,
                return_code: 0,
            });
        }

        Ok(self.steps.len())
    }

    /// Build a sequence from an explicit list of process IDs.
    pub fn build_sequence_from_ids(
        &mut self,
        process_ids: &[&str],
    ) -> Result<usize, SequencerError> {
        self.steps.clear();
        for (i, &pid) in process_ids.iter().enumerate() {
            if !self.definitions.iter().any(|d| d.process_id == pid) {
                return Err(SequencerError::ProcessNotFound(pid.to_string()));
            }
            self.steps.push(StepState {
                process_id: pid.to_string(),
                sequence_no: (i + 1) as u16,
                status: JobStatus::Ready,
                return_code: 0,
            });
        }
        Ok(self.steps.len())
    }

    /// Get the next ready process, checking dependencies
    /// (mirrors FUNC-NEXT → 2000-GET-NEXT-PROCESS).
    pub fn next_process(&mut self) -> Result<Option<String>, SequencerError> {
        // 2100-FIND-NEXT-READY
        let next_idx = self.steps.iter().position(|s| s.status == JobStatus::Ready);

        let idx = match next_idx {
            Some(i) => i,
            None => return Ok(None),
        };

        let process_id = self.steps[idx].process_id.clone();

        // 2200-CHECK-DEPENDENCIES
        self.check_dependencies(&process_id)?;

        // 2300-UPDATE-PROCESS-STATUS → mark as Active
        self.steps[idx].status = JobStatus::Active;

        Ok(Some(process_id))
    }

    /// Report completion of a step (updates internal table).
    pub fn complete_step(
        &mut self,
        process_id: &str,
        return_code: i16,
    ) -> Result<(), SequencerError> {
        let step = self
            .steps
            .iter_mut()
            .find(|s| s.process_id == process_id)
            .ok_or_else(|| SequencerError::ProcessNotFound(process_id.into()))?;

        step.return_code = return_code;
        step.status = if return_code <= ReturnCode::WARNING.0 {
            JobStatus::Done
        } else {
            JobStatus::Error
        };

        Ok(())
    }

    /// Mark a step as failed.
    pub fn fail_step(&mut self, process_id: &str, return_code: i16) -> Result<(), SequencerError> {
        let step = self
            .steps
            .iter_mut()
            .find(|s| s.process_id == process_id)
            .ok_or_else(|| SequencerError::ProcessNotFound(process_id.into()))?;

        step.return_code = return_code;
        step.status = JobStatus::Error;

        Ok(())
    }

    /// Check overall completion status
    /// (mirrors FUNC-STAT → 3000-CHECK-STATUS / 3300-CHECK-COMPLETION).
    pub fn check_completion(&self) -> CompletionStatus {
        let active = self
            .steps
            .iter()
            .filter(|s| s.status == JobStatus::Active)
            .count();
        let errors = self
            .steps
            .iter()
            .filter(|s| s.status == JobStatus::Error)
            .count();
        let ready = self
            .steps
            .iter()
            .filter(|s| s.status == JobStatus::Ready)
            .count();

        if errors > 0 {
            CompletionStatus::Failed {
                error_count: errors,
            }
        } else if active > 0 || ready > 0 {
            CompletionStatus::InProgress {
                active_count: active,
                remaining: ready,
            }
        } else {
            CompletionStatus::Complete
        }
    }

    /// Final status check (mirrors FUNC-TERM → 4000-TERMINATE-SEQUENCE /
    /// 4100-CHECK-FINAL-STATUS).
    pub fn final_status(&self) -> ReturnCode {
        let completion = self.check_completion();
        match completion {
            CompletionStatus::Failed { .. } => ReturnCode::ERROR,
            CompletionStatus::InProgress { .. } => ReturnCode::WARNING,
            CompletionStatus::Complete => ReturnCode::SUCCESS,
        }
    }

    /// Snapshot of all steps.
    pub fn steps(&self) -> &[StepState] {
        &self.steps
    }

    /// Get the process definition for a given ID.
    pub fn get_definition(&self, process_id: &str) -> Option<&ProcessDefinition> {
        self.definitions.iter().find(|d| d.process_id == process_id)
    }

    // -- private helpers (mirror PRCSEQ00 paragraphs) -------------------------

    /// 2200-CHECK-DEPENDENCIES / 2210-CHECK-DEP-STATUS
    fn check_dependencies(&self, process_id: &str) -> Result<(), SequencerError> {
        let def = self
            .definitions
            .iter()
            .find(|d| d.process_id == process_id)
            .ok_or_else(|| SequencerError::ProcessNotFound(process_id.into()))?;

        for dep in &def.dependencies {
            let dep_step = self.steps.iter().find(|s| s.process_id == dep.process_id);

            match dep_step {
                Some(step) => {
                    if step.status != JobStatus::Done {
                        if dep.dep_type == DepType::Hard {
                            return Err(SequencerError::DependencyNotSatisfied {
                                process: process_id.into(),
                                dependency: dep.process_id.clone(),
                            });
                        }
                        // Soft dependency: continue anyway.
                    } else if step.return_code > dep.max_return_code {
                        return Err(SequencerError::DependencyNotSatisfied {
                            process: process_id.into(),
                            dependency: dep.process_id.clone(),
                        });
                    }
                }
                None => {
                    if dep.dep_type == DepType::Hard {
                        return Err(SequencerError::DependencyNotSatisfied {
                            process: process_id.into(),
                            dependency: dep.process_id.clone(),
                        });
                    }
                }
            }
        }

        Ok(())
    }
}

/// Overall completion status of a sequence.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum CompletionStatus {
    Complete,
    InProgress {
        active_count: usize,
        remaining: usize,
    },
    Failed {
        error_count: usize,
    },
}

impl fmt::Display for CompletionStatus {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Complete => write!(f, "COMPLETE"),
            Self::InProgress {
                active_count,
                remaining,
            } => write!(f, "IN_PROGRESS (active={active_count}, ready={remaining})"),
            Self::Failed { error_count } => write!(f, "FAILED (errors={error_count})"),
        }
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::NaiveDate;

    fn date() -> NaiveDate {
        NaiveDate::from_ymd_opt(2024, 1, 15).unwrap()
    }

    fn make_def(id: &str, seq_type: SequenceType) -> ProcessDefinition {
        ProcessDefinition::new(id, seq_type)
    }

    #[test]
    fn build_sequence_filters_by_type() {
        let mut seq = ProcessSequencer::new(date());
        seq.add_definition(make_def("STEP_A", SequenceType::Init));
        seq.add_definition(make_def("STEP_B", SequenceType::Process));
        seq.add_definition(make_def("STEP_C", SequenceType::Init));

        let count = seq.build_sequence(SequenceType::Init).unwrap();
        assert_eq!(count, 2);
        assert_eq!(seq.steps()[0].process_id, "STEP_A");
        assert_eq!(seq.steps()[1].process_id, "STEP_C");
    }

    #[test]
    fn no_matching_processes_returns_error() {
        let mut seq = ProcessSequencer::new(date());
        seq.add_definition(make_def("X", SequenceType::Report));

        let err = seq.build_sequence(SequenceType::Init).unwrap_err();
        assert!(matches!(err, SequencerError::NoProcessesFound(..)));
    }

    #[test]
    fn next_process_returns_in_order() {
        let mut seq = ProcessSequencer::new(date());
        seq.add_definition(make_def("A", SequenceType::Process));
        seq.add_definition(make_def("B", SequenceType::Process));
        seq.add_definition(make_def("C", SequenceType::Process));
        seq.build_sequence(SequenceType::Process).unwrap();

        assert_eq!(seq.next_process().unwrap(), Some("A".into()));
        seq.complete_step("A", 0).unwrap();
        assert_eq!(seq.next_process().unwrap(), Some("B".into()));
        seq.complete_step("B", 0).unwrap();
        assert_eq!(seq.next_process().unwrap(), Some("C".into()));
        seq.complete_step("C", 0).unwrap();
        assert_eq!(seq.next_process().unwrap(), None);
    }

    #[test]
    fn hard_dependency_blocks_execution() {
        let mut seq = ProcessSequencer::new(date());

        let mut step_b = make_def("B", SequenceType::Process);
        step_b.dependencies.push(Dependency {
            process_id: "A".into(),
            dep_type: DepType::Hard,
            max_return_code: 4,
        });

        seq.add_definition(make_def("A", SequenceType::Process));
        seq.add_definition(step_b);
        seq.build_sequence(SequenceType::Process).unwrap();

        // A runs first — fine.
        assert_eq!(seq.next_process().unwrap(), Some("A".into()));
        // B has hard dep on A, which is only Active — should fail.
        let err = seq.next_process().unwrap_err();
        assert!(matches!(err, SequencerError::DependencyNotSatisfied { .. }));

        // Complete A, then B can proceed.
        seq.complete_step("A", 0).unwrap();
        assert_eq!(seq.next_process().unwrap(), Some("B".into()));
    }

    #[test]
    fn soft_dependency_does_not_block() {
        let mut seq = ProcessSequencer::new(date());

        let mut step_b = make_def("B", SequenceType::Process);
        step_b.dependencies.push(Dependency {
            process_id: "A".into(),
            dep_type: DepType::Soft,
            max_return_code: 4,
        });

        seq.add_definition(make_def("A", SequenceType::Process));
        seq.add_definition(step_b);
        seq.build_sequence(SequenceType::Process).unwrap();

        // A starts running.
        seq.next_process().unwrap();
        // B has soft dep on A (Active) — allowed to proceed.
        assert_eq!(seq.next_process().unwrap(), Some("B".into()));
    }

    #[test]
    fn dependency_rc_exceeded() {
        let mut seq = ProcessSequencer::new(date());

        let mut step_b = make_def("B", SequenceType::Process);
        step_b.dependencies.push(Dependency {
            process_id: "A".into(),
            dep_type: DepType::Hard,
            max_return_code: 0,
        });

        seq.add_definition(make_def("A", SequenceType::Process));
        seq.add_definition(step_b);
        seq.build_sequence(SequenceType::Process).unwrap();

        seq.next_process().unwrap(); // A
        seq.complete_step("A", 4).unwrap(); // RC=4 > max_rc=0

        let err = seq.next_process().unwrap_err();
        assert!(matches!(err, SequencerError::DependencyNotSatisfied { .. }));
    }

    #[test]
    fn completion_status_complete() {
        let mut seq = ProcessSequencer::new(date());
        seq.add_definition(make_def("X", SequenceType::Init));
        seq.build_sequence(SequenceType::Init).unwrap();
        seq.next_process().unwrap();
        seq.complete_step("X", 0).unwrap();

        assert_eq!(seq.check_completion(), CompletionStatus::Complete);
        assert_eq!(seq.final_status(), ReturnCode::SUCCESS);
    }

    #[test]
    fn completion_status_failed() {
        let mut seq = ProcessSequencer::new(date());
        seq.add_definition(make_def("F", SequenceType::Init));
        seq.build_sequence(SequenceType::Init).unwrap();
        seq.next_process().unwrap();
        seq.fail_step("F", 8).unwrap();

        assert!(matches!(
            seq.check_completion(),
            CompletionStatus::Failed { error_count: 1 }
        ));
        assert_eq!(seq.final_status(), ReturnCode::ERROR);
    }

    #[test]
    fn completion_status_in_progress() {
        let mut seq = ProcessSequencer::new(date());
        seq.add_definition(make_def("P", SequenceType::Init));
        seq.add_definition(make_def("Q", SequenceType::Init));
        seq.build_sequence(SequenceType::Init).unwrap();
        seq.next_process().unwrap(); // P → Active

        let status = seq.check_completion();
        assert!(matches!(
            status,
            CompletionStatus::InProgress {
                active_count: 1,
                remaining: 1,
            }
        ));
        assert_eq!(seq.final_status(), ReturnCode::WARNING);
    }

    #[test]
    fn sequence_type_roundtrip() {
        for st in [
            SequenceType::Init,
            SequenceType::Process,
            SequenceType::Report,
            SequenceType::Terminate,
        ] {
            assert_eq!(SequenceType::from_code(st.code()), Some(st));
        }
        assert!(SequenceType::from_code("ZZZ").is_none());
    }

    #[test]
    fn build_from_explicit_ids() {
        let mut seq = ProcessSequencer::new(date());
        seq.add_definition(make_def("TRNVAL00", SequenceType::Process));
        seq.add_definition(make_def("POSUPD00", SequenceType::Process));
        seq.add_definition(make_def("HISTLD00", SequenceType::Process));

        let count = seq
            .build_sequence_from_ids(StandardSequences::MAIN_PROCESS)
            .unwrap();
        assert_eq!(count, 3);
        assert_eq!(seq.steps()[0].process_id, "TRNVAL00");
        assert_eq!(seq.steps()[2].process_id, "HISTLD00");
    }

    #[test]
    fn build_from_ids_unknown_returns_error() {
        let mut seq = ProcessSequencer::new(date());
        let err = seq.build_sequence_from_ids(&["NOPE"]).unwrap_err();
        assert!(matches!(err, SequencerError::ProcessNotFound(_)));
    }

    #[test]
    fn standard_sequences_have_correct_members() {
        assert_eq!(StandardSequences::START_OF_DAY.len(), 3);
        assert_eq!(StandardSequences::MAIN_PROCESS.len(), 3);
        assert_eq!(StandardSequences::END_OF_DAY.len(), 3);
    }
}
