//! Database maintenance utility.
//!
//! Ported from COBOL program `UTLMNT00.cbl` — File Maintenance Utility.
//!
//! Original responsibilities:
//! - Archive processing (move aged records to archive tables)
//! - File cleanup (delete soft-deleted / expired records)
//! - VSAM reorganization (→ `VACUUM ANALYZE` in PostgreSQL)
//! - Space management / catalog analysis (→ table-size statistics)
//!
//! In the Rust port the four COBOL *functions* become methods on
//! [`MaintenanceRunner`], which operates against a `sqlx::PgPool`.

use std::fmt;

use chrono::{NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use tracing::{info, warn};

// ---------------------------------------------------------------------------
// CTL-FUNCTION equivalents
// ---------------------------------------------------------------------------

/// Maintenance function selector (mirrors CTL-FUNCTION in UTLMNT00).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum MaintenanceFunction {
    Archive,
    Cleanup,
    Reorg,
    Analyze,
}

impl MaintenanceFunction {
    pub fn from_code(s: &str) -> Option<Self> {
        match s.trim().to_uppercase().as_str() {
            "ARCHIVE" => Some(Self::Archive),
            "CLEANUP" => Some(Self::Cleanup),
            "REORG" => Some(Self::Reorg),
            "ANALYZE" => Some(Self::Analyze),
            _ => None,
        }
    }

    pub fn code(&self) -> &'static str {
        match self {
            Self::Archive => "ARCHIVE",
            Self::Cleanup => "CLEANUP",
            Self::Reorg => "REORG",
            Self::Analyze => "ANALYZE",
        }
    }
}

impl fmt::Display for MaintenanceFunction {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.code())
    }
}

// ---------------------------------------------------------------------------
// WS-COUNTERS / report structures
// ---------------------------------------------------------------------------

/// Per-function result counters (mirrors WS-COUNTERS in UTLMNT00).
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct MaintenanceStats {
    pub records_read: u64,
    pub records_written: u64,
    pub error_count: u64,
}

impl fmt::Display for MaintenanceStats {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "read={} written={} errors={}",
            self.records_read, self.records_written, self.error_count
        )
    }
}

/// Outcome of a single maintenance function execution.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MaintenanceResult {
    pub function: MaintenanceFunction,
    pub target_table: String,
    pub stats: MaintenanceStats,
    pub return_code: i32,
    pub message: String,
}

/// Aggregate report for an entire maintenance run.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct MaintenanceReport {
    pub results: Vec<MaintenanceResult>,
    pub total_errors: u64,
}

impl MaintenanceReport {
    pub fn return_code(&self) -> i32 {
        if self.total_errors > 100 {
            12
        } else if self.total_errors > 0 {
            4
        } else {
            0
        }
    }
}

impl fmt::Display for MaintenanceReport {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "=== Maintenance Report ===")?;
        for r in &self.results {
            writeln!(
                f,
                "  [{:8}] {} — {} (rc={})",
                r.function, r.target_table, r.stats, r.return_code
            )?;
        }
        writeln!(f, "  Total errors: {}", self.total_errors)?;
        writeln!(f, "  Overall RC: {}", self.return_code())
    }
}

// ---------------------------------------------------------------------------
// Error type
// ---------------------------------------------------------------------------

/// Errors that can occur during maintenance operations.
#[derive(Debug, thiserror::Error)]
pub enum MaintenanceError {
    #[error("invalid function: {0}")]
    InvalidFunction(String),

    #[error("invalid table name: {0}")]
    InvalidTableName(String),

    #[error("database error: {0}")]
    Db(#[from] sqlx::Error),

    #[error("maintenance aborted — error count exceeded limit (>{0})")]
    ErrorLimitExceeded(u64),
}

/// Validate that a table name contains only safe SQL identifier characters.
fn validate_table_name(name: &str) -> Result<&str, MaintenanceError> {
    if !name.is_empty()
        && name
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '.')
    {
        Ok(name)
    } else {
        Err(MaintenanceError::InvalidTableName(name.to_string()))
    }
}

// ---------------------------------------------------------------------------
// MaintenanceRunner
// ---------------------------------------------------------------------------

/// Control record — analogous to one line from the CONTROL-FILE.
#[derive(Debug, Clone)]
pub struct MaintenanceRequest {
    pub function: MaintenanceFunction,
    pub table_name: String,
    pub cutoff_date: Option<NaiveDate>,
    pub retention_days: Option<i64>,
}

/// Executes database maintenance functions.
///
/// Maps to the UTLMNT00 main processing loop: open control file, iterate
/// records, dispatch each function, accumulate counters, close files.
pub struct MaintenanceRunner {
    error_limit: u64,
}

impl Default for MaintenanceRunner {
    fn default() -> Self {
        Self::new()
    }
}

impl MaintenanceRunner {
    pub fn new() -> Self {
        Self { error_limit: 100 }
    }

    pub fn with_error_limit(mut self, limit: u64) -> Self {
        self.error_limit = limit;
        self
    }

    /// Execute a batch of maintenance requests (mirrors 2000-PROCESS).
    pub async fn run(
        &self,
        pool: &PgPool,
        requests: &[MaintenanceRequest],
    ) -> Result<MaintenanceReport, MaintenanceError> {
        let mut report = MaintenanceReport::default();

        for req in requests {
            validate_table_name(&req.table_name)?;

            let result = match req.function {
                MaintenanceFunction::Archive => self.archive(pool, req).await,
                MaintenanceFunction::Cleanup => self.cleanup(pool, req).await,
                MaintenanceFunction::Reorg => self.reorg(pool, req).await,
                MaintenanceFunction::Analyze => self.analyze(pool, req).await,
            };

            match result {
                Ok(r) => {
                    report.total_errors += r.stats.error_count;
                    report.results.push(r);
                }
                Err(e) => {
                    warn!(table = %req.table_name, error = %e, "maintenance function failed");
                    report.total_errors += 1;
                    report.results.push(MaintenanceResult {
                        function: req.function,
                        target_table: req.table_name.clone(),
                        stats: MaintenanceStats {
                            error_count: 1,
                            ..Default::default()
                        },
                        return_code: 12,
                        message: e.to_string(),
                    });
                }
            }

            if report.total_errors > self.error_limit {
                return Err(MaintenanceError::ErrorLimitExceeded(self.error_limit));
            }
        }

        Ok(report)
    }

    /// Archive aged records (mirrors 2200-ARCHIVE-PROCESS).
    ///
    /// Moves rows older than `cutoff_date` from `table_name` into
    /// `{table_name}_archive`.
    async fn archive(
        &self,
        pool: &PgPool,
        req: &MaintenanceRequest,
    ) -> Result<MaintenanceResult, MaintenanceError> {
        let cutoff = req
            .cutoff_date
            .unwrap_or_else(|| Utc::now().date_naive() - chrono::Duration::days(365));

        let table = &req.table_name;
        let archive_table = format!("{table}_archive");

        info!(table, %cutoff, "archiving records");

        let mut tx = pool.begin().await?;

        let insert_sql =
            format!("INSERT INTO {archive_table} SELECT * FROM {table} WHERE created_at < $1");
        let insert_result = sqlx::query(&insert_sql)
            .bind(cutoff)
            .execute(&mut *tx)
            .await?;
        let archived = insert_result.rows_affected();

        let delete_sql = format!("DELETE FROM {table} WHERE created_at < $1");
        sqlx::query(&delete_sql)
            .bind(cutoff)
            .execute(&mut *tx)
            .await?;

        tx.commit().await?;

        info!(table, archived, "archive complete");

        Ok(MaintenanceResult {
            function: MaintenanceFunction::Archive,
            target_table: table.clone(),
            stats: MaintenanceStats {
                records_read: archived,
                records_written: archived,
                error_count: 0,
            },
            return_code: 0,
            message: format!("archived {archived} records older than {cutoff}"),
        })
    }

    /// Delete old / expired records (mirrors 2300-CLEANUP-PROCESS).
    async fn cleanup(
        &self,
        pool: &PgPool,
        req: &MaintenanceRequest,
    ) -> Result<MaintenanceResult, MaintenanceError> {
        let retention = req.retention_days.unwrap_or(90);
        let cutoff = Utc::now().date_naive() - chrono::Duration::days(retention);
        let table = &req.table_name;

        info!(table, %cutoff, retention, "cleaning up old records");

        let sql = format!("DELETE FROM {table} WHERE created_at < $1");
        let result = sqlx::query(&sql).bind(cutoff).execute(pool).await?;
        let deleted = result.rows_affected();

        info!(table, deleted, "cleanup complete");

        Ok(MaintenanceResult {
            function: MaintenanceFunction::Cleanup,
            target_table: table.clone(),
            stats: MaintenanceStats {
                records_read: deleted,
                records_written: 0,
                error_count: 0,
            },
            return_code: 0,
            message: format!("deleted {deleted} records older than {cutoff}"),
        })
    }

    /// Reorganize table (mirrors 2400-REORG-PROCESS → VACUUM ANALYZE).
    async fn reorg(
        &self,
        pool: &PgPool,
        req: &MaintenanceRequest,
    ) -> Result<MaintenanceResult, MaintenanceError> {
        let table = &req.table_name;
        info!(table, "reorganizing (VACUUM ANALYZE)");

        let sql = format!("VACUUM ANALYZE {table}");
        sqlx::query(&sql).execute(pool).await?;

        Ok(MaintenanceResult {
            function: MaintenanceFunction::Reorg,
            target_table: table.clone(),
            stats: MaintenanceStats::default(),
            return_code: 0,
            message: "VACUUM ANALYZE completed".to_string(),
        })
    }

    /// Collect table statistics (mirrors 2500-ANALYZE-PROCESS).
    async fn analyze(
        &self,
        pool: &PgPool,
        req: &MaintenanceRequest,
    ) -> Result<MaintenanceResult, MaintenanceError> {
        let table = &req.table_name;
        info!(table, "collecting table statistics");

        let row: (Option<i64>,) =
            sqlx::query_as(&format!("SELECT pg_total_relation_size('{table}')::bigint"))
                .fetch_one(pool)
                .await?;
        let size_bytes = row.0.unwrap_or(0);

        let count_row: (i64,) = sqlx::query_as(&format!("SELECT count(*)::bigint FROM {table}"))
            .fetch_one(pool)
            .await?;
        let row_count = count_row.0;

        let index_row: (Option<i64>,) =
            sqlx::query_as(&format!("SELECT pg_indexes_size('{table}')::bigint"))
                .fetch_one(pool)
                .await?;
        let index_bytes = index_row.0.unwrap_or(0);

        let msg = format!("rows={row_count} table_size={size_bytes}B index_size={index_bytes}B");
        info!(table, %msg, "analysis complete");

        Ok(MaintenanceResult {
            function: MaintenanceFunction::Analyze,
            target_table: table.clone(),
            stats: MaintenanceStats {
                records_read: row_count as u64,
                ..Default::default()
            },
            return_code: 0,
            message: msg,
        })
    }
}

// ---------------------------------------------------------------------------
// Unit-level helpers (tested without a database)
// ---------------------------------------------------------------------------

/// Parse a COBOL-style function code into [`MaintenanceFunction`].
pub fn parse_function(code: &str) -> Result<MaintenanceFunction, MaintenanceError> {
    MaintenanceFunction::from_code(code)
        .ok_or_else(|| MaintenanceError::InvalidFunction(code.to_string()))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn function_round_trip() {
        for f in [
            MaintenanceFunction::Archive,
            MaintenanceFunction::Cleanup,
            MaintenanceFunction::Reorg,
            MaintenanceFunction::Analyze,
        ] {
            assert_eq!(MaintenanceFunction::from_code(f.code()), Some(f));
        }
    }

    #[test]
    fn function_case_insensitive() {
        assert_eq!(
            MaintenanceFunction::from_code("archive"),
            Some(MaintenanceFunction::Archive)
        );
        assert_eq!(
            MaintenanceFunction::from_code("  CLEANUP  "),
            Some(MaintenanceFunction::Cleanup)
        );
    }

    #[test]
    fn function_invalid() {
        assert!(MaintenanceFunction::from_code("BOGUS").is_none());
        assert!(parse_function("BOGUS").is_err());
    }

    #[test]
    fn report_return_code_zero_when_clean() {
        let report = MaintenanceReport::default();
        assert_eq!(report.return_code(), 0);
    }

    #[test]
    fn report_return_code_4_on_some_errors() {
        let report = MaintenanceReport {
            total_errors: 5,
            ..Default::default()
        };
        assert_eq!(report.return_code(), 4);
    }

    #[test]
    fn report_return_code_12_when_exceeds_100() {
        let report = MaintenanceReport {
            total_errors: 101,
            ..Default::default()
        };
        assert_eq!(report.return_code(), 12);
    }

    #[test]
    fn stats_display() {
        let stats = MaintenanceStats {
            records_read: 10,
            records_written: 5,
            error_count: 1,
        };
        let s = format!("{stats}");
        assert!(s.contains("read=10"));
        assert!(s.contains("written=5"));
        assert!(s.contains("errors=1"));
    }

    #[test]
    fn runner_error_limit() {
        let runner = MaintenanceRunner::new().with_error_limit(50);
        assert_eq!(runner.error_limit, 50);
    }

    #[test]
    fn maintenance_request_builder() {
        let req = MaintenanceRequest {
            function: MaintenanceFunction::Archive,
            table_name: "transactions".into(),
            cutoff_date: Some(NaiveDate::from_ymd_opt(2023, 1, 1).unwrap()),
            retention_days: None,
        };
        assert_eq!(req.function, MaintenanceFunction::Archive);
        assert_eq!(req.table_name, "transactions");
    }

    #[test]
    fn report_display_formatting() {
        let report = MaintenanceReport {
            results: vec![MaintenanceResult {
                function: MaintenanceFunction::Archive,
                target_table: "test_table".to_string(),
                stats: MaintenanceStats {
                    records_read: 100,
                    records_written: 100,
                    error_count: 0,
                },
                return_code: 0,
                message: "ok".to_string(),
            }],
            total_errors: 0,
        };
        let output = format!("{report}");
        assert!(output.contains("Maintenance Report"));
        assert!(output.contains("test_table"));
        assert!(output.contains("ARCHIVE"));
    }

    #[test]
    fn validate_table_name_accepts_valid() {
        assert!(validate_table_name("transactions").is_ok());
        assert!(validate_table_name("public.positions").is_ok());
        assert!(validate_table_name("my_table_123").is_ok());
    }

    #[test]
    fn validate_table_name_rejects_injection() {
        assert!(validate_table_name("users; DROP TABLE users; --").is_err());
        assert!(validate_table_name("").is_err());
        assert!(validate_table_name("table name").is_err());
        assert!(validate_table_name("table'name").is_err());
    }
}
