//! Batch processing CLI.
//!
//! Provides sub-commands for the batch-processing pipelines and utility
//! programs ported from COBOL.

use chrono::NaiveDate;
use clap::{Parser, Subcommand};
use tracing_subscriber::EnvFilter;

use batch::batch_control::{BatchControlRecord, BatchController, Prerequisite};
use batch::history_loader::{HistoryLoader, LoadOutcome};
use batch::process_sequencer::{ProcessDefinition, ProcessSequencer, SequenceType};
use batch::utilities::maintenance::{MaintenanceFunction, MaintenanceRequest, MaintenanceRunner};
use batch::utilities::monitoring::{PrometheusExporter, SystemMonitor};
use batch::utilities::validation::{DataValidator, ValidationRequest, ValidationType};

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

    /// Run database maintenance (UTLMNT00).
    Maintain {
        /// Maintenance function: ARCHIVE, CLEANUP, REORG, ANALYZE.
        #[arg(long)]
        function: String,

        /// Target table name.
        #[arg(long)]
        table: String,

        /// Cutoff date for archive/cleanup (YYYY-MM-DD).
        #[arg(long)]
        cutoff_date: Option<String>,

        /// Retention days for cleanup.
        #[arg(long)]
        retention_days: Option<i64>,

        /// Database URL.
        #[arg(long, env = "DATABASE_URL")]
        database_url: String,
    },

    /// Run system monitoring (UTLMON00).
    Monitor {
        /// Output format: text or prometheus.
        #[arg(long, default_value = "text")]
        format: String,

        /// Database URL.
        #[arg(long, env = "DATABASE_URL")]
        database_url: String,
    },

    /// Run data validation checks (UTLVAL00).
    Validate {
        /// Validation types to run (comma-separated): INTEGRITY, XREF, FORMAT, BALANCE.
        #[arg(long, default_value = "INTEGRITY,XREF,FORMAT,BALANCE")]
        checks: String,

        /// Database URL.
        #[arg(long, env = "DATABASE_URL")]
        database_url: String,
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

async fn run_maintain(
    function: &str,
    table: &str,
    cutoff_date: Option<&str>,
    retention_days: Option<i64>,
    database_url: &str,
) {
    let func = MaintenanceFunction::from_code(function)
        .unwrap_or_else(|| panic!("unknown maintenance function: {function}"));

    let cutoff = cutoff_date.map(|d| {
        NaiveDate::parse_from_str(d, "%Y-%m-%d").expect("invalid cutoff date (expected YYYY-MM-DD)")
    });

    let pool = sqlx::postgres::PgPoolOptions::new()
        .max_connections(5)
        .connect(database_url)
        .await
        .expect("failed to connect to database");

    let request = MaintenanceRequest {
        function: func,
        table_name: table.to_string(),
        cutoff_date: cutoff,
        retention_days,
    };

    let runner = MaintenanceRunner::new();
    match runner.run(&pool, &[request]).await {
        Ok(report) => {
            println!("{report}");
            std::process::exit(report.return_code());
        }
        Err(e) => {
            eprintln!("ERROR: {e}");
            std::process::exit(12);
        }
    }
}

async fn run_monitor(format: &str, database_url: &str) {
    let pool = sqlx::postgres::PgPoolOptions::new()
        .max_connections(5)
        .connect(database_url)
        .await
        .expect("failed to connect to database");

    let monitor = SystemMonitor::new();
    match monitor.check(&pool).await {
        Ok(report) => match format {
            "prometheus" => print!("{}", PrometheusExporter::render(&report)),
            _ => println!("{report}"),
        },
        Err(e) => {
            eprintln!("ERROR: {e}");
            std::process::exit(12);
        }
    }
}

async fn run_validate(checks: &str, database_url: &str) {
    let pool = sqlx::postgres::PgPoolOptions::new()
        .max_connections(5)
        .connect(database_url)
        .await
        .expect("failed to connect to database");

    let requests: Vec<ValidationRequest> = checks
        .split(',')
        .map(|s| {
            let vt = ValidationType::from_code(s.trim())
                .unwrap_or_else(|| panic!("unknown validation type: {s}"));
            ValidationRequest {
                validation_type: vt,
                table_name: None,
            }
        })
        .collect();

    let validator = DataValidator::new();
    match validator.run(&pool, &requests).await {
        Ok(report) => {
            println!("{report}");
            std::process::exit(report.return_code());
        }
        Err(e) => {
            eprintln!("ERROR: {e}");
            std::process::exit(12);
        }
    }
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::from_default_env())
        .init();

    let cli = Cli::parse();

    match &cli.command {
        Command::RunBatch { date, job } => run_batch(date, job),
        Command::LoadHistory { count, commit_freq } => load_history(*count, *commit_freq),
        Command::RunSequence { seq_type, date } => run_sequence(seq_type, date),
        Command::Maintain {
            function,
            table,
            cutoff_date,
            retention_days,
            database_url,
        } => {
            run_maintain(
                function,
                table,
                cutoff_date.as_deref(),
                *retention_days,
                database_url,
            )
            .await;
        }
        Command::Monitor {
            format,
            database_url,
        } => {
            run_monitor(format, database_url).await;
        }
        Command::Validate {
            checks,
            database_url,
        } => {
            run_validate(checks, database_url).await;
        }
    }
}
