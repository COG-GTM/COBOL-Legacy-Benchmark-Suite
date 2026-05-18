//! Batch processing CLI.
//!
//! Provides sub-commands for the three batch-processing pipelines ported
//! from COBOL: batch control, history loading, and process sequencing.

use chrono::NaiveDate;
use clap::{Parser, Subcommand};
use tracing_subscriber::EnvFilter;

use batch::batch_control::{BatchControlRecord, BatchController, Prerequisite};
use batch::history_loader::{HistoryLoader, LoadOutcome};
use batch::process_sequencer::{ProcessDefinition, ProcessSequencer, SequenceType};

// ---------------------------------------------------------------------------
// CLI definition
// ---------------------------------------------------------------------------

/// COBOL-to-Rust batch processing pipeline.
#[derive(Parser)]
#[command(name = "batch", version, about)]
struct Cli {
    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    /// Run the batch control demo (BCHCTL00).
    RunBatch {
        /// Process date (YYYY-MM-DD).
        #[arg(long, default_value = "2024-01-15")]
        date: String,

        /// Job name.
        #[arg(long, default_value = "TRNVAL00")]
        job: String,
    },

    /// Run the history-loading ETL demo (HISTLD00).
    LoadHistory {
        /// Number of synthetic records to generate.
        #[arg(long, default_value_t = 100)]
        count: u32,

        /// Commit frequency (records per commit).
        #[arg(long, default_value_t = 25)]
        commit_freq: u32,
    },

    /// Run the process-sequencer demo (PRCSEQ00).
    RunSequence {
        /// Sequence type: INI, PRC, RPT, TRM.
        #[arg(long, default_value = "PRC")]
        seq_type: String,

        /// Process date (YYYY-MM-DD).
        #[arg(long, default_value = "2024-01-15")]
        date: String,
    },
}

// ---------------------------------------------------------------------------
// Subcommand handlers
// ---------------------------------------------------------------------------

fn run_batch(date_str: &str, job: &str) {
    let process_date = NaiveDate::parse_from_str(date_str, "%Y-%m-%d")
        .expect("invalid date format (expected YYYY-MM-DD)");

    let mut ctl = BatchController::new();

    // Register a simple two-step pipeline with a dependency.
    let prereq = BatchControlRecord::new("DATEVAL", process_date, 1);
    ctl.register_job(prereq);

    let mut main_job = BatchControlRecord::new(job, process_date, 2);
    main_job.prerequisites.push(Prerequisite {
        job_name: "DATEVAL".into(),
        sequence_no: 1,
        max_return_code: 4,
    });
    ctl.register_job(main_job);

    println!("=== Batch Control Demo (BCHCTL00) ===");
    println!("Process date: {process_date}");
    println!();

    // Initialise and run the prerequisite.
    println!("[DATEVAL] Initializing...");
    let rc = ctl.init_job("DATEVAL", &process_date).unwrap();
    println!("[DATEVAL] init rc={rc}");
    let rc = ctl.terminate_job("DATEVAL", &process_date, 0).unwrap();
    println!("[DATEVAL] terminated rc={rc}");

    // Check prereqs for main job.
    println!();
    println!("[{job}] Checking prerequisites...");
    let rc = ctl.check_prerequisites(job, &process_date).unwrap();
    println!("[{job}] prereq check rc={rc}");

    // Initialise, run, and terminate main job.
    let rc = ctl.init_job(job, &process_date).unwrap();
    println!("[{job}] init rc={rc}");
    let rc = ctl.terminate_job(job, &process_date, 0).unwrap();
    println!("[{job}] terminated rc={rc}");

    let rec = ctl.get_record(job, &process_date).unwrap();
    println!();
    println!("Final status: {:?}", rec.status);
}

fn load_history(count: u32, commit_freq: u32) {
    use batch::history_loader::HistoryInputRecord;
    use chrono::NaiveTime;
    use rust_decimal::Decimal;

    println!("=== History Loader Demo (HISTLD00) ===");
    println!("Records: {count}  Commit freq: {commit_freq}");
    println!();

    let records: Vec<HistoryInputRecord> = (0..count)
        .map(|i| HistoryInputRecord {
            account_no: format!("ACC{i:06}"),
            portfolio_id: format!("PF{:04}", i % 100),
            trans_date: NaiveDate::from_ymd_opt(2024, 1, 15).unwrap(),
            trans_time: NaiveTime::from_hms_opt(10, 0, 0).unwrap(),
            trans_type: "BUY".into(),
            security_id: format!("SEC{:04}", i % 50),
            quantity: Decimal::new(100, 0),
            price: Decimal::new(15000, 2),
            amount: Decimal::new(1500000, 2),
            fees: Decimal::new(995, 2),
            total_amount: Decimal::new(1500995, 2),
            cost_basis: Decimal::new(1500995, 2),
            gain_loss: Decimal::ZERO,
        })
        .collect();

    let mut loader = HistoryLoader::new().with_commit_frequency(commit_freq);

    match loader.run(&records, |_| LoadOutcome::Inserted) {
        Ok(stats) => println!("{stats}"),
        Err(e) => eprintln!("ERROR: {e}"),
    }
}

fn run_sequence(seq_type_str: &str, date_str: &str) {
    let process_date = NaiveDate::parse_from_str(date_str, "%Y-%m-%d")
        .expect("invalid date format (expected YYYY-MM-DD)");

    let seq_type = SequenceType::from_code(seq_type_str)
        .unwrap_or_else(|| panic!("unknown sequence type: {seq_type_str}"));

    println!("=== Process Sequencer Demo (PRCSEQ00) ===");
    println!("Sequence type: {seq_type_str}  Date: {process_date}");
    println!();

    let mut sequencer = ProcessSequencer::new(process_date);

    // Register definitions for the MAIN PROCESS pipeline.
    for &name in &["TRNVAL00", "POSUPD00", "HISTLD00"] {
        sequencer.add_definition(ProcessDefinition::new(name, SequenceType::Process));
    }
    // Register definitions for INIT pipeline.
    for &name in &["INITDAY", "CKPCLR", "DATEVAL"] {
        sequencer.add_definition(ProcessDefinition::new(name, SequenceType::Init));
    }
    // Register definitions for REPORT pipeline.
    for &name in &["RPTGEN00", "BCKLOD00"] {
        sequencer.add_definition(ProcessDefinition::new(name, SequenceType::Report));
    }
    // Register definitions for TERMINATE pipeline.
    sequencer.add_definition(ProcessDefinition::new("ENDDAY", SequenceType::Terminate));

    match sequencer.build_sequence(seq_type) {
        Ok(count) => println!("Built sequence with {count} steps."),
        Err(e) => {
            eprintln!("ERROR: {e}");
            return;
        }
    }

    // Execute steps sequentially.
    loop {
        match sequencer.next_process() {
            Ok(Some(pid)) => {
                println!("  Running: {pid}");
                sequencer.complete_step(&pid, 0).unwrap();
                println!("  Completed: {pid} rc=0");
            }
            Ok(None) => break,
            Err(e) => {
                eprintln!("  Error: {e}");
                break;
            }
        }
    }

    let status = sequencer.check_completion();
    let rc = sequencer.final_status();
    println!();
    println!("Completion: {status}  RC={rc}");
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::from_default_env())
        .init();

    let cli = Cli::parse();

    match &cli.command {
        Command::RunBatch { date, job } => run_batch(date, job),
        Command::LoadHistory { count, commit_freq } => load_history(*count, *commit_freq),
        Command::RunSequence { seq_type, date } => run_sequence(seq_type, date),
    }
}
