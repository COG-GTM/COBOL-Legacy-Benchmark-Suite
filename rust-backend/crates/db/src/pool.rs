//! Database pool management.
//!
//! Translated from COBOL programs:
//! - `DB2CONN.cbl` — Connection management → pool configuration
//! - `DB2CMT.cbl`  — Commit/rollback handling → transaction helpers
//! - `DB2ERR.cbl`  — SQLCODE error mapping → Rust error types
//! - `DB2STAT.cbl` — Statistics collection → connection health checks
//!
//! DB2 connection pooling is replaced by `sqlx::PgPool`; individual
//! connection management (CONNECT/DISCONNECT) maps to pool
//! acquire/release semantics.

use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::Arc;
use std::time::{Duration, Instant};

use sqlx::postgres::PgPoolOptions;
use sqlx::{PgPool, Postgres, Transaction};
use tracing::{error, info, warn};

// ---------------------------------------------------------------------------
// DB2ERR.cbl — SQLCODE → Rust error mapping
// ---------------------------------------------------------------------------

/// Database errors translated from DB2 SQLCODE categories.
///
/// Mirrors the error classification in DB2ERR.cbl's 1100-SET-SEVERITY and
/// 2000-DIAGNOSE-ERROR paragraphs.
#[derive(Debug, Clone, thiserror::Error)]
pub enum DbError {
    #[error("deadlock detected — retry transaction")]
    Deadlock,

    #[error("timeout — retry transaction")]
    Timeout,

    #[error("connection error — check availability")]
    ConnectionError,

    #[error("duplicate key violation")]
    DuplicateKey,

    #[error("record not found")]
    NotFound,

    #[error("DB2 warning condition: {0}")]
    Warning(String),

    #[error("unhandled database error: {0}")]
    Other(String),

    #[error("pool error: {0}")]
    Pool(String),
}

impl DbError {
    /// Whether the caller should retry the operation.
    ///
    /// Mirrors DB2ERR.cbl LS-RETRY-FLAG: deadlocks and timeouts are
    /// retryable; all other errors are not.
    pub fn should_retry(&self) -> bool {
        matches!(self, Self::Deadlock | Self::Timeout)
    }

    /// Severity level (maps to DB2ERR EL-ERROR-SEVERITY).
    /// 1 = Info, 2 = Warning, 3 = Error, 4 = Severe.
    pub fn severity(&self) -> i16 {
        match self {
            Self::Deadlock | Self::Timeout => 2,
            Self::ConnectionError => 4,
            Self::DuplicateKey | Self::NotFound => 1,
            Self::Warning(_) => 1,
            Self::Other(_) | Self::Pool(_) => 3,
        }
    }
}

/// Map a `sqlx::Error` into a [`DbError`].
///
/// Mirrors DB2ERR.cbl's SQLCODE classification.
pub fn map_sqlx_error(err: &sqlx::Error) -> DbError {
    match err {
        sqlx::Error::Database(db_err) => {
            let code = db_err.code().unwrap_or_default();
            match code.as_ref() {
                // PostgreSQL deadlock_detected
                "40P01" => DbError::Deadlock,
                // PostgreSQL lock_not_available / statement_timeout
                "55P03" | "57014" => DbError::Timeout,
                // unique_violation (maps to DB2 -803 DUP-KEY)
                "23505" => DbError::DuplicateKey,
                // connection_exception class
                c if c.starts_with("08") => DbError::ConnectionError,
                _ => DbError::Other(db_err.message().to_string()),
            }
        }
        sqlx::Error::RowNotFound => DbError::NotFound,
        sqlx::Error::PoolTimedOut => DbError::ConnectionError,
        _ => DbError::Other(err.to_string()),
    }
}

// ---------------------------------------------------------------------------
// DB2CONN.cbl — Connection pool configuration
// ---------------------------------------------------------------------------

/// Configuration for the database pool (derived from DB2CONN parameters).
#[derive(Debug, Clone)]
pub struct PoolConfig {
    /// Database connection URL.
    pub database_url: String,
    /// Maximum number of connections (maps to DB2CONN max-connections concept).
    pub max_connections: u32,
    /// Minimum idle connections to keep in the pool.
    pub min_connections: u32,
    /// Connection timeout (maps to DB2CONN retry wait logic).
    pub connect_timeout: Duration,
    /// Maximum connection lifetime before recycling.
    pub max_lifetime: Duration,
    /// Idle timeout before a connection is closed.
    pub idle_timeout: Duration,
    /// Number of connection retries (maps to WS-MAX-RETRIES = 3).
    pub max_retries: u32,
}

impl Default for PoolConfig {
    fn default() -> Self {
        Self {
            database_url: String::new(),
            max_connections: 10,
            min_connections: 1,
            connect_timeout: Duration::from_secs(5),
            max_lifetime: Duration::from_secs(30 * 60),
            idle_timeout: Duration::from_secs(10 * 60),
            max_retries: 3,
        }
    }
}

// ---------------------------------------------------------------------------
// DB2STAT.cbl — Connection statistics
// ---------------------------------------------------------------------------

/// Runtime statistics for the pool (port of DB2STAT.cbl counters).
#[derive(Debug)]
pub struct PoolStats {
    pub rows_read: AtomicI64,
    pub rows_inserted: AtomicI64,
    pub rows_updated: AtomicI64,
    pub rows_deleted: AtomicI64,
    pub commits: AtomicI64,
    pub rollbacks: AtomicI64,
    pub started_at: Instant,
}

impl Default for PoolStats {
    fn default() -> Self {
        Self::new()
    }
}

impl PoolStats {
    pub fn new() -> Self {
        Self {
            rows_read: AtomicI64::new(0),
            rows_inserted: AtomicI64::new(0),
            rows_updated: AtomicI64::new(0),
            rows_deleted: AtomicI64::new(0),
            commits: AtomicI64::new(0),
            rollbacks: AtomicI64::new(0),
            started_at: Instant::now(),
        }
    }

    pub fn record_read(&self, count: i64) {
        self.rows_read.fetch_add(count, Ordering::Relaxed);
    }

    pub fn record_insert(&self, count: i64) {
        self.rows_inserted.fetch_add(count, Ordering::Relaxed);
    }

    pub fn record_update(&self, count: i64) {
        self.rows_updated.fetch_add(count, Ordering::Relaxed);
    }

    pub fn record_delete(&self, count: i64) {
        self.rows_deleted.fetch_add(count, Ordering::Relaxed);
    }

    pub fn record_commit(&self) {
        self.commits.fetch_add(1, Ordering::Relaxed);
    }

    pub fn record_rollback(&self) {
        self.rollbacks.fetch_add(1, Ordering::Relaxed);
    }

    /// Elapsed wall-clock time since pool creation.
    pub fn elapsed(&self) -> Duration {
        self.started_at.elapsed()
    }

    /// Return a snapshot of all counters (mirrors DB2STAT 4000-DISPLAY-STATS).
    pub fn snapshot(&self) -> StatsSnapshot {
        StatsSnapshot {
            rows_read: self.rows_read.load(Ordering::Relaxed),
            rows_inserted: self.rows_inserted.load(Ordering::Relaxed),
            rows_updated: self.rows_updated.load(Ordering::Relaxed),
            rows_deleted: self.rows_deleted.load(Ordering::Relaxed),
            commits: self.commits.load(Ordering::Relaxed),
            rollbacks: self.rollbacks.load(Ordering::Relaxed),
            elapsed_secs: self.elapsed().as_secs_f64(),
        }
    }
}

/// Point-in-time statistics snapshot (serializable).
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct StatsSnapshot {
    pub rows_read: i64,
    pub rows_inserted: i64,
    pub rows_updated: i64,
    pub rows_deleted: i64,
    pub commits: i64,
    pub rollbacks: i64,
    pub elapsed_secs: f64,
}

impl std::fmt::Display for StatsSnapshot {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "read={} ins={} upd={} del={} commits={} rollbacks={} elapsed={:.2}s",
            self.rows_read,
            self.rows_inserted,
            self.rows_updated,
            self.rows_deleted,
            self.commits,
            self.rollbacks,
            self.elapsed_secs,
        )
    }
}

// ---------------------------------------------------------------------------
// DatabasePool — unified wrapper
// ---------------------------------------------------------------------------

/// Database connection pool (port of DB2CONN + DB2CMT + DB2ERR + DB2STAT).
///
/// Wraps `sqlx::PgPool` with:
/// - Retry-aware connection establishment (DB2CONN)
/// - Transaction management helpers (DB2CMT)
/// - SQLCODE → Rust error mapping (DB2ERR)
/// - Runtime statistics (DB2STAT)
#[derive(Debug, Clone)]
pub struct DatabasePool {
    inner: PgPool,
    config: PoolConfig,
    stats: Arc<PoolStats>,
}

impl DatabasePool {
    /// Create a new pool from the given configuration.
    ///
    /// Mirrors DB2CONN's 1000-CONNECT paragraph with retry logic.
    pub async fn connect(config: PoolConfig) -> Result<Self, DbError> {
        let mut last_err = None;

        for attempt in 1..=config.max_retries {
            match PgPoolOptions::new()
                .max_connections(config.max_connections)
                .min_connections(config.min_connections)
                .acquire_timeout(config.connect_timeout)
                .max_lifetime(config.max_lifetime)
                .idle_timeout(config.idle_timeout)
                .connect(&config.database_url)
                .await
            {
                Ok(pool) => {
                    info!(attempt, "database pool connected");
                    return Ok(Self {
                        inner: pool,
                        config,
                        stats: Arc::new(PoolStats::new()),
                    });
                }
                Err(e) => {
                    let db_err = map_sqlx_error(&e);
                    warn!(attempt, error = %db_err, "connection attempt failed");
                    last_err = Some(db_err);

                    if attempt < config.max_retries {
                        tokio::time::sleep(Duration::from_millis(100 * u64::from(attempt))).await;
                    }
                }
            }
        }

        Err(last_err.unwrap_or(DbError::Pool("max retries exceeded".into())))
    }

    /// Wrap an already-created `PgPool` (useful for testing).
    pub fn from_pool(pool: PgPool, config: PoolConfig) -> Self {
        Self {
            inner: pool,
            config,
            stats: Arc::new(PoolStats::new()),
        }
    }

    /// Access the underlying `sqlx::PgPool`.
    pub fn inner(&self) -> &PgPool {
        &self.inner
    }

    /// Access pool configuration.
    pub fn config(&self) -> &PoolConfig {
        &self.config
    }

    /// Access runtime statistics.
    pub fn stats(&self) -> &PoolStats {
        &self.stats
    }

    // -- DB2CMT transaction helpers ------------------------------------------

    /// Begin a new transaction (maps to DB2CMT INIT + implicit begin).
    pub async fn begin(&self) -> Result<Transaction<'_, Postgres>, DbError> {
        self.inner.begin().await.map_err(|e| {
            let db_err = map_sqlx_error(&e);
            error!(error = %db_err, "failed to begin transaction");
            db_err
        })
    }

    /// Commit a transaction (maps to DB2CMT 2100-ISSUE-COMMIT).
    pub async fn commit(&self, tx: Transaction<'_, Postgres>) -> Result<(), DbError> {
        tx.commit().await.map_err(|e| {
            let db_err = map_sqlx_error(&e);
            error!(error = %db_err, "commit failed");
            self.stats.record_rollback();
            db_err
        })?;
        self.stats.record_commit();
        Ok(())
    }

    /// Rollback a transaction (maps to DB2CMT 3000-ROLLBACK).
    pub async fn rollback(&self, tx: Transaction<'_, Postgres>) -> Result<(), DbError> {
        tx.rollback().await.map_err(|e| {
            let db_err = map_sqlx_error(&e);
            error!(error = %db_err, "rollback failed");
            db_err
        })?;
        self.stats.record_rollback();
        Ok(())
    }

    // -- DB2STAT health check ------------------------------------------------

    /// Check pool health by executing a trivial query.
    ///
    /// Mirrors DB2CONN's 3000-CHECK-STATUS (SELECT CURRENT SERVER).
    pub async fn health_check(&self) -> Result<(), DbError> {
        sqlx::query_scalar::<_, i32>("SELECT 1")
            .fetch_one(&self.inner)
            .await
            .map_err(|e| {
                let db_err = map_sqlx_error(&e);
                warn!(error = %db_err, "health check failed");
                db_err
            })?;
        Ok(())
    }

    /// Log current statistics (mirrors DB2STAT 4000-DISPLAY-STATS).
    pub fn display_stats(&self) {
        let snap = self.stats.snapshot();
        info!(%snap, "database pool statistics");
    }

    /// Close the pool (mirrors DB2CONN 2000-DISCONNECT).
    pub async fn close(&self) {
        self.display_stats();
        self.inner.close().await;
        info!("database pool closed");
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    // -- DbError classification ----------------------------------------------

    #[test]
    fn deadlock_is_retryable() {
        assert!(DbError::Deadlock.should_retry());
    }

    #[test]
    fn timeout_is_retryable() {
        assert!(DbError::Timeout.should_retry());
    }

    #[test]
    fn connection_error_not_retryable() {
        assert!(!DbError::ConnectionError.should_retry());
    }

    #[test]
    fn duplicate_key_not_retryable() {
        assert!(!DbError::DuplicateKey.should_retry());
    }

    #[test]
    fn not_found_not_retryable() {
        assert!(!DbError::NotFound.should_retry());
    }

    #[test]
    fn severity_values() {
        assert_eq!(DbError::Deadlock.severity(), 2);
        assert_eq!(DbError::Timeout.severity(), 2);
        assert_eq!(DbError::ConnectionError.severity(), 4);
        assert_eq!(DbError::DuplicateKey.severity(), 1);
        assert_eq!(DbError::NotFound.severity(), 1);
        assert_eq!(DbError::Other("x".into()).severity(), 3);
    }

    // -- PoolConfig defaults -------------------------------------------------

    #[test]
    fn pool_config_defaults() {
        let cfg = PoolConfig::default();
        assert_eq!(cfg.max_connections, 10);
        assert_eq!(cfg.min_connections, 1);
        assert_eq!(cfg.max_retries, 3);
        assert_eq!(cfg.connect_timeout, Duration::from_secs(5));
    }

    // -- PoolStats -----------------------------------------------------------

    #[test]
    fn pool_stats_counters() {
        let stats = PoolStats::new();
        stats.record_read(5);
        stats.record_insert(2);
        stats.record_update(3);
        stats.record_delete(1);
        stats.record_commit();
        stats.record_commit();
        stats.record_rollback();

        let snap = stats.snapshot();
        assert_eq!(snap.rows_read, 5);
        assert_eq!(snap.rows_inserted, 2);
        assert_eq!(snap.rows_updated, 3);
        assert_eq!(snap.rows_deleted, 1);
        assert_eq!(snap.commits, 2);
        assert_eq!(snap.rollbacks, 1);
        assert!(snap.elapsed_secs >= 0.0);
    }

    #[test]
    fn stats_snapshot_display() {
        let stats = PoolStats::new();
        stats.record_read(10);
        stats.record_commit();
        let snap = stats.snapshot();
        let s = snap.to_string();
        assert!(s.contains("read=10"));
        assert!(s.contains("commits=1"));
    }

    #[test]
    fn stats_snapshot_serde_roundtrip() {
        let snap = StatsSnapshot {
            rows_read: 1,
            rows_inserted: 2,
            rows_updated: 3,
            rows_deleted: 4,
            commits: 5,
            rollbacks: 6,
            elapsed_secs: 1.23,
        };
        let json = serde_json::to_string(&snap).unwrap();
        let back: StatsSnapshot = serde_json::from_str(&json).unwrap();
        assert_eq!(back.rows_read, 1);
        assert_eq!(back.commits, 5);
    }

    // -- map_sqlx_error ------------------------------------------------------

    #[test]
    fn map_row_not_found() {
        let err = sqlx::Error::RowNotFound;
        let db_err = map_sqlx_error(&err);
        assert!(matches!(db_err, DbError::NotFound));
    }

    #[test]
    fn map_pool_timed_out() {
        let err = sqlx::Error::PoolTimedOut;
        let db_err = map_sqlx_error(&err);
        assert!(matches!(db_err, DbError::ConnectionError));
    }
}
