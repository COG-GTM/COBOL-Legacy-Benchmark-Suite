//! System monitoring utility.
//!
//! Ported from COBOL program `UTLMON00.cbl` — System Monitoring Utility.
//!
//! Original responsibilities:
//! - Resource utilisation tracking (CPU, Memory, DASD, DB2)
//! - Performance metrics collection
//! - Threshold monitoring with configurable alert levels
//! - Alert generation (INFO / WARNING / CRITICAL)
//!
//! In the Rust port the four resource types become variants of
//! [`ResourceType`].  Threshold checks, metric snapshots, and alert
//! generation are handled by [`SystemMonitor`].  Prometheus text-format
//! export is available via [`PrometheusExporter`].

use std::collections::HashMap;
use std::fmt;
use std::time::Instant;

use chrono::Utc;
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use tracing::{info, warn};

// ---------------------------------------------------------------------------
// WS-RESOURCE-TYPES
// ---------------------------------------------------------------------------

/// Resource types monitored by the utility (mirrors WS-RESOURCE-TYPES).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ResourceType {
    Cpu,
    Memory,
    Dasd,
    Db2,
}

impl ResourceType {
    pub fn from_code(s: &str) -> Option<Self> {
        match s.trim().to_uppercase().as_str() {
            "CPU" => Some(Self::Cpu),
            "MEMORY" => Some(Self::Memory),
            "DASD" => Some(Self::Dasd),
            "DB2" => Some(Self::Db2),
            _ => None,
        }
    }

    pub fn code(&self) -> &'static str {
        match self {
            Self::Cpu => "CPU",
            Self::Memory => "MEMORY",
            Self::Dasd => "DASD",
            Self::Db2 => "DB2",
        }
    }
}

impl fmt::Display for ResourceType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.code())
    }
}

// ---------------------------------------------------------------------------
// WS-THRESHOLD-TYPES / WS-ALERT-LEVELS
// ---------------------------------------------------------------------------

/// Threshold types (mirrors WS-THRESHOLD-TYPES).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ThresholdType {
    Utilization,
    Response,
    Queue,
    Error,
}

impl ThresholdType {
    pub fn from_code(s: &str) -> Option<Self> {
        match s.trim().to_uppercase().as_str() {
            "UTIL" | "UTILIZATION" => Some(Self::Utilization),
            "RESPONSE" => Some(Self::Response),
            "QUEUE" => Some(Self::Queue),
            "ERROR" => Some(Self::Error),
            _ => None,
        }
    }
}

/// Alert severity level (mirrors WS-ALERT-LEVELS).
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum AlertLevel {
    Info,
    Warning,
    Critical,
}

impl AlertLevel {
    pub fn from_code(s: &str) -> Option<Self> {
        match s.trim().to_uppercase().as_str() {
            "INFO" => Some(Self::Info),
            "WARNING" => Some(Self::Warning),
            "CRITICAL" => Some(Self::Critical),
            _ => None,
        }
    }

    pub fn code(&self) -> &'static str {
        match self {
            Self::Info => "INFO",
            Self::Warning => "WARNING",
            Self::Critical => "CRITICAL",
        }
    }
}

impl fmt::Display for AlertLevel {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.code())
    }
}

// ---------------------------------------------------------------------------
// CONFIG-RECORD / threshold configuration
// ---------------------------------------------------------------------------

/// Threshold rule (mirrors one CONFIG-RECORD from MONCFG file).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ThresholdConfig {
    pub resource_type: ResourceType,
    pub threshold_type: ThresholdType,
    pub threshold_value: f64,
    pub alert_level: AlertLevel,
    pub alert_action: String,
}

// ---------------------------------------------------------------------------
// WS-CURRENT-METRICS — metric snapshot
// ---------------------------------------------------------------------------

/// Point-in-time metrics snapshot (mirrors WS-CURRENT-METRICS).
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct MetricsSnapshot {
    pub timestamp: String,
    pub cpu_util: f64,
    pub memory_util: f64,
    pub dasd_util: f64,
    pub db2_util: f64,
    pub db2_response_ms: f64,
    pub db2_queue_depth: u64,
    pub db2_error_count: u64,
    pub pool_size: u32,
    pub pool_idle: u32,
    pub pool_active: u32,
}

// ---------------------------------------------------------------------------
// ALERT-RECORD
// ---------------------------------------------------------------------------

/// Generated alert (mirrors ALERT-RECORD).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Alert {
    pub timestamp: String,
    pub level: AlertLevel,
    pub resource: ResourceType,
    pub message: String,
}

impl fmt::Display for Alert {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "[{}] {} ({}) {}",
            self.timestamp, self.level, self.resource, self.message
        )
    }
}

// ---------------------------------------------------------------------------
// Monitoring result
// ---------------------------------------------------------------------------

/// Full result of a monitoring check cycle.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct MonitoringReport {
    pub metrics: MetricsSnapshot,
    pub alerts: Vec<Alert>,
    pub batch_jobs_active: u64,
    pub batch_jobs_completed: u64,
    pub batch_jobs_failed: u64,
}

impl fmt::Display for MonitoringReport {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "=== System Monitoring Report ===")?;
        writeln!(f, "Timestamp: {}", self.metrics.timestamp)?;
        writeln!(f, "CPU: {:.1}%", self.metrics.cpu_util)?;
        writeln!(f, "Memory: {:.1}%", self.metrics.memory_util)?;
        writeln!(f, "DASD: {:.1}%", self.metrics.dasd_util)?;
        writeln!(f, "DB2 util: {:.1}%", self.metrics.db2_util)?;
        writeln!(f, "DB2 response: {:.1}ms", self.metrics.db2_response_ms)?;
        writeln!(f, "DB2 queue: {}", self.metrics.db2_queue_depth)?;
        writeln!(f, "DB2 errors: {}", self.metrics.db2_error_count)?;
        writeln!(
            f,
            "Pool: size={} idle={} active={}",
            self.metrics.pool_size, self.metrics.pool_idle, self.metrics.pool_active
        )?;
        writeln!(
            f,
            "Batch jobs: active={} completed={} failed={}",
            self.batch_jobs_active, self.batch_jobs_completed, self.batch_jobs_failed
        )?;
        if self.alerts.is_empty() {
            writeln!(f, "Alerts: none")?;
        } else {
            writeln!(f, "Alerts ({}):", self.alerts.len())?;
            for a in &self.alerts {
                writeln!(f, "  {a}")?;
            }
        }
        Ok(())
    }
}

// ---------------------------------------------------------------------------
// Error type
// ---------------------------------------------------------------------------

#[derive(Debug, thiserror::Error)]
pub enum MonitoringError {
    #[error("database error: {0}")]
    Db(#[from] sqlx::Error),

    #[error("invalid resource type: {0}")]
    InvalidResource(String),
}

// ---------------------------------------------------------------------------
// SystemMonitor
// ---------------------------------------------------------------------------

/// Collects system metrics and evaluates threshold rules.
///
/// Maps to the UTLMON00 main loop: initialise → read config → collect
/// metrics → check thresholds → log status → generate alerts.
pub struct SystemMonitor {
    thresholds: Vec<ThresholdConfig>,
}

impl Default for SystemMonitor {
    fn default() -> Self {
        Self::new()
    }
}

impl SystemMonitor {
    pub fn new() -> Self {
        Self {
            thresholds: Vec::new(),
        }
    }

    /// Load threshold configs (mirrors 1300-READ-CONFIG).
    pub fn load_thresholds(&mut self, configs: Vec<ThresholdConfig>) {
        self.thresholds = configs;
    }

    /// Run a full monitoring cycle (mirrors 2000-PROCESS).
    pub async fn check(&self, pool: &PgPool) -> Result<MonitoringReport, MonitoringError> {
        let now = Utc::now().to_rfc3339();

        let metrics = self.collect_metrics(pool, &now).await?;
        let alerts = self.evaluate_thresholds(&metrics);
        let (active, completed, failed) = self.batch_job_status(pool).await?;

        for alert in &alerts {
            warn!(%alert, "threshold alert fired");
        }

        Ok(MonitoringReport {
            metrics,
            alerts,
            batch_jobs_active: active,
            batch_jobs_completed: completed,
            batch_jobs_failed: failed,
        })
    }

    /// Collect metrics from the database pool (mirrors 2100-COLLECT-METRICS).
    async fn collect_metrics(
        &self,
        pool: &PgPool,
        timestamp: &str,
    ) -> Result<MetricsSnapshot, MonitoringError> {
        let pool_opts = pool.options();
        let pool_size = pool.size();
        let pool_idle = pool.num_idle() as u32;
        let pool_active = pool_size.saturating_sub(pool_idle);

        let start = Instant::now();
        let _: (i32,) = sqlx::query_as("SELECT 1").fetch_one(pool).await?;
        let db2_response_ms = start.elapsed().as_secs_f64() * 1000.0;

        let db2_util = (pool_active as f64 / pool_opts.get_max_connections() as f64) * 100.0;

        let db_errors: (i64,) = sqlx::query_as(
            "SELECT count(*)::bigint FROM pg_stat_activity WHERE state = 'idle in transaction (aborted)'"
        )
        .fetch_one(pool)
        .await
        .unwrap_or((0,));

        Ok(MetricsSnapshot {
            timestamp: timestamp.to_string(),
            cpu_util: 0.0,
            memory_util: 0.0,
            dasd_util: 0.0,
            db2_util,
            db2_response_ms,
            db2_queue_depth: pool_active as u64,
            db2_error_count: db_errors.0 as u64,
            pool_size,
            pool_idle,
            pool_active,
        })
    }

    /// Evaluate threshold rules (mirrors 2200-CHECK-THRESHOLDS).
    fn evaluate_thresholds(&self, metrics: &MetricsSnapshot) -> Vec<Alert> {
        let mut alerts = Vec::new();

        for cfg in &self.thresholds {
            let current_value = self.metric_value(metrics, cfg.resource_type, cfg.threshold_type);

            if current_value >= cfg.threshold_value {
                alerts.push(Alert {
                    timestamp: metrics.timestamp.clone(),
                    level: cfg.alert_level,
                    resource: cfg.resource_type,
                    message: format!(
                        "{} {:?} {:.2} >= threshold {:.2} — {}",
                        cfg.resource_type,
                        cfg.threshold_type,
                        current_value,
                        cfg.threshold_value,
                        cfg.alert_action
                    ),
                });
            }
        }

        alerts
    }

    /// Look up the current value for a (resource, threshold_type) pair.
    fn metric_value(
        &self,
        m: &MetricsSnapshot,
        resource: ResourceType,
        threshold: ThresholdType,
    ) -> f64 {
        match (resource, threshold) {
            (ResourceType::Cpu, ThresholdType::Utilization) => m.cpu_util,
            (ResourceType::Memory, ThresholdType::Utilization) => m.memory_util,
            (ResourceType::Dasd, ThresholdType::Utilization) => m.dasd_util,
            (ResourceType::Db2, ThresholdType::Utilization) => m.db2_util,
            (ResourceType::Db2, ThresholdType::Response) => m.db2_response_ms,
            (ResourceType::Db2, ThresholdType::Queue) => m.db2_queue_depth as f64,
            (ResourceType::Db2, ThresholdType::Error) => m.db2_error_count as f64,
            _ => 0.0,
        }
    }

    /// Query batch-job status counters from the database.
    async fn batch_job_status(&self, pool: &PgPool) -> Result<(u64, u64, u64), MonitoringError> {
        let result: Result<Vec<(String, i64)>, _> = sqlx::query_as(
            "SELECT status::text, count(*)::bigint \
             FROM batch_jobs \
             GROUP BY status",
        )
        .fetch_all(pool)
        .await;

        match result {
            Ok(rows) => {
                let mut map: HashMap<String, u64> = HashMap::new();
                for (status, count) in rows {
                    map.insert(status.to_uppercase(), count as u64);
                }
                Ok((
                    map.get("ACTIVE").copied().unwrap_or(0),
                    map.get("DONE").copied().unwrap_or(0)
                        + map.get("COMPLETED").copied().unwrap_or(0),
                    map.get("ERROR").copied().unwrap_or(0)
                        + map.get("FAILED").copied().unwrap_or(0),
                ))
            }
            Err(_) => {
                info!("batch_jobs table not available — skipping job status");
                Ok((0, 0, 0))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Prometheus text-format exporter
// ---------------------------------------------------------------------------

/// Renders metrics in Prometheus text exposition format.
pub struct PrometheusExporter;

impl PrometheusExporter {
    /// Render a [`MonitoringReport`] as Prometheus text format.
    pub fn render(report: &MonitoringReport) -> String {
        let m = &report.metrics;
        let mut out = String::with_capacity(2048);

        Self::write_gauge(&mut out, "portfolio_cpu_utilization_percent", m.cpu_util);
        Self::write_gauge(
            &mut out,
            "portfolio_memory_utilization_percent",
            m.memory_util,
        );
        Self::write_gauge(&mut out, "portfolio_dasd_utilization_percent", m.dasd_util);
        Self::write_gauge(&mut out, "portfolio_db2_utilization_percent", m.db2_util);
        Self::write_gauge(
            &mut out,
            "portfolio_db2_response_milliseconds",
            m.db2_response_ms,
        );
        Self::write_gauge(
            &mut out,
            "portfolio_db2_queue_depth",
            m.db2_queue_depth as f64,
        );
        Self::write_gauge(
            &mut out,
            "portfolio_db2_error_count",
            m.db2_error_count as f64,
        );
        Self::write_gauge(&mut out, "portfolio_pool_size", m.pool_size as f64);
        Self::write_gauge(&mut out, "portfolio_pool_idle", m.pool_idle as f64);
        Self::write_gauge(&mut out, "portfolio_pool_active", m.pool_active as f64);

        Self::write_gauge(
            &mut out,
            "portfolio_batch_jobs_active",
            report.batch_jobs_active as f64,
        );
        Self::write_gauge(
            &mut out,
            "portfolio_batch_jobs_completed",
            report.batch_jobs_completed as f64,
        );
        Self::write_gauge(
            &mut out,
            "portfolio_batch_jobs_failed",
            report.batch_jobs_failed as f64,
        );
        Self::write_gauge(
            &mut out,
            "portfolio_alerts_total",
            report.alerts.len() as f64,
        );

        out
    }

    fn write_gauge(out: &mut String, name: &str, value: f64) {
        use std::fmt::Write;
        let _ = writeln!(out, "# TYPE {name} gauge");
        let _ = writeln!(out, "{name} {value}");
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resource_type_round_trip() {
        for rt in [
            ResourceType::Cpu,
            ResourceType::Memory,
            ResourceType::Dasd,
            ResourceType::Db2,
        ] {
            assert_eq!(ResourceType::from_code(rt.code()), Some(rt));
        }
    }

    #[test]
    fn alert_level_ordering() {
        assert!(AlertLevel::Info < AlertLevel::Warning);
        assert!(AlertLevel::Warning < AlertLevel::Critical);
    }

    #[test]
    fn alert_level_from_code() {
        assert_eq!(AlertLevel::from_code("INFO"), Some(AlertLevel::Info));
        assert_eq!(AlertLevel::from_code("warning"), Some(AlertLevel::Warning));
        assert_eq!(
            AlertLevel::from_code("CRITICAL"),
            Some(AlertLevel::Critical)
        );
        assert!(AlertLevel::from_code("BOGUS").is_none());
    }

    #[test]
    fn threshold_type_from_code_aliases() {
        assert_eq!(
            ThresholdType::from_code("UTIL"),
            Some(ThresholdType::Utilization)
        );
        assert_eq!(
            ThresholdType::from_code("UTILIZATION"),
            Some(ThresholdType::Utilization)
        );
    }

    #[test]
    fn evaluate_thresholds_fires_alert() {
        let monitor = SystemMonitor {
            thresholds: vec![ThresholdConfig {
                resource_type: ResourceType::Db2,
                threshold_type: ThresholdType::Utilization,
                threshold_value: 80.0,
                alert_level: AlertLevel::Warning,
                alert_action: "notify DBA".to_string(),
            }],
        };

        let metrics = MetricsSnapshot {
            db2_util: 90.0,
            ..Default::default()
        };

        let alerts = monitor.evaluate_thresholds(&metrics);
        assert_eq!(alerts.len(), 1);
        assert_eq!(alerts[0].level, AlertLevel::Warning);
        assert_eq!(alerts[0].resource, ResourceType::Db2);
    }

    #[test]
    fn evaluate_thresholds_no_alert_below_threshold() {
        let monitor = SystemMonitor {
            thresholds: vec![ThresholdConfig {
                resource_type: ResourceType::Cpu,
                threshold_type: ThresholdType::Utilization,
                threshold_value: 90.0,
                alert_level: AlertLevel::Critical,
                alert_action: "page oncall".to_string(),
            }],
        };

        let metrics = MetricsSnapshot {
            cpu_util: 50.0,
            ..Default::default()
        };

        let alerts = monitor.evaluate_thresholds(&metrics);
        assert!(alerts.is_empty());
    }

    #[test]
    fn prometheus_exporter_output() {
        let report = MonitoringReport {
            metrics: MetricsSnapshot {
                cpu_util: 45.5,
                memory_util: 60.0,
                pool_size: 10,
                pool_idle: 7,
                pool_active: 3,
                ..Default::default()
            },
            batch_jobs_active: 2,
            batch_jobs_completed: 15,
            batch_jobs_failed: 1,
            alerts: vec![],
        };

        let output = PrometheusExporter::render(&report);
        assert!(output.contains("# TYPE portfolio_cpu_utilization_percent gauge"));
        assert!(output.contains("portfolio_cpu_utilization_percent 45.5"));
        assert!(output.contains("portfolio_pool_size 10"));
        assert!(output.contains("portfolio_pool_idle 7"));
        assert!(output.contains("portfolio_pool_active 3"));
        assert!(output.contains("portfolio_batch_jobs_active 2"));
        assert!(output.contains("portfolio_batch_jobs_failed 1"));
        assert!(output.contains("portfolio_alerts_total 0"));
    }

    #[test]
    fn monitoring_report_display() {
        let report = MonitoringReport {
            metrics: MetricsSnapshot {
                timestamp: "2024-01-15T10:00:00Z".to_string(),
                cpu_util: 45.0,
                memory_util: 60.0,
                dasd_util: 30.0,
                db2_util: 20.0,
                db2_response_ms: 5.0,
                db2_queue_depth: 2,
                db2_error_count: 0,
                pool_size: 10,
                pool_idle: 8,
                pool_active: 2,
            },
            alerts: vec![],
            batch_jobs_active: 1,
            batch_jobs_completed: 10,
            batch_jobs_failed: 0,
        };
        let output = format!("{report}");
        assert!(output.contains("System Monitoring Report"));
        assert!(output.contains("CPU: 45.0%"));
        assert!(output.contains("Alerts: none"));
    }

    #[test]
    fn alert_display() {
        let alert = Alert {
            timestamp: "2024-01-15T10:00:00Z".to_string(),
            level: AlertLevel::Critical,
            resource: ResourceType::Db2,
            message: "pool exhausted".to_string(),
        };
        let s = format!("{alert}");
        assert!(s.contains("CRITICAL"));
        assert!(s.contains("DB2"));
        assert!(s.contains("pool exhausted"));
    }

    #[test]
    fn metric_value_lookup() {
        let monitor = SystemMonitor::new();
        let m = MetricsSnapshot {
            cpu_util: 10.0,
            memory_util: 20.0,
            dasd_util: 30.0,
            db2_util: 40.0,
            db2_response_ms: 5.0,
            db2_queue_depth: 3,
            db2_error_count: 1,
            ..Default::default()
        };

        assert!(
            (monitor.metric_value(&m, ResourceType::Cpu, ThresholdType::Utilization) - 10.0).abs()
                < f64::EPSILON
        );
        assert!(
            (monitor.metric_value(&m, ResourceType::Db2, ThresholdType::Response) - 5.0).abs()
                < f64::EPSILON
        );
        assert!(
            (monitor.metric_value(&m, ResourceType::Db2, ThresholdType::Queue) - 3.0).abs()
                < f64::EPSILON
        );
        assert!(
            (monitor.metric_value(&m, ResourceType::Cpu, ThresholdType::Queue) - 0.0).abs()
                < f64::EPSILON
        );
    }
}
