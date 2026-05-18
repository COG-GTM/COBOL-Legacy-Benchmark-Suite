//! System statistics report generator.
//!
//! Ported from COBOL program `RPTSTA00.cbl`.
//!
//! Generates system performance and statistics reports including transaction
//! volumes, error rates, and processing times.  Output is available as JSON
//! or CSV.

use std::io::Write;

use chrono::NaiveDate;
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

// ---------------------------------------------------------------------------
// Input records — mirrors DB2STAT / BCHCTL stat fields
// ---------------------------------------------------------------------------

/// A DB2 statistics snapshot (mirrors COBOL WS-DB2-METRICS).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Db2StatsRecord {
    pub date: NaiveDate,
    pub total_calls: u64,
    pub elapsed_seconds: Decimal,
    pub cpu_seconds: Decimal,
    pub wait_seconds: Decimal,
}

/// A batch-job statistics snapshot (mirrors COBOL WS-BATCH-METRICS).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BatchStatsRecord {
    pub date: NaiveDate,
    pub total_jobs: u64,
    pub successful_jobs: u64,
    pub failed_jobs: u64,
    pub elapsed_seconds: Decimal,
}

// ---------------------------------------------------------------------------
// Report rows
// ---------------------------------------------------------------------------

/// DB2 performance summary line (mirrors WS-DB2-DETAIL).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Db2PerformanceSummary {
    pub total_calls: u64,
    pub avg_response_ms: Decimal,
    pub total_elapsed_seconds: Decimal,
    pub total_cpu_seconds: Decimal,
    pub total_wait_seconds: Decimal,
    pub cpu_pct: Decimal,
}

/// Batch processing summary line (mirrors WS-BATCH-DETAIL).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BatchProcessingSummary {
    pub total_jobs: u64,
    pub successful_jobs: u64,
    pub failed_jobs: u64,
    pub success_rate_pct: Decimal,
    pub total_elapsed_seconds: Decimal,
    pub avg_job_seconds: Decimal,
}

/// A daily volume row for the trend section.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DailyVolumeRow {
    pub date: NaiveDate,
    pub db2_calls: u64,
    pub batch_jobs: u64,
    pub batch_failures: u64,
    pub error_rate_pct: Decimal,
}

// ---------------------------------------------------------------------------
// Full report
// ---------------------------------------------------------------------------

/// Complete statistics report (mirrors RPTSTA00 output).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StatisticsReport {
    pub report_title: String,
    pub report_date: NaiveDate,
    pub db2_summary: Db2PerformanceSummary,
    pub batch_summary: BatchProcessingSummary,
    pub daily_volumes: Vec<DailyVolumeRow>,
}

// ---------------------------------------------------------------------------
// Generator
// ---------------------------------------------------------------------------

/// Build a `StatisticsReport` from DB2 and batch statistics records.
pub fn generate(
    db2_stats: &[Db2StatsRecord],
    batch_stats: &[BatchStatsRecord],
    report_date: NaiveDate,
) -> StatisticsReport {
    let db2_summary = compute_db2_summary(db2_stats);
    let batch_summary = compute_batch_summary(batch_stats);
    let daily_volumes = compute_daily_volumes(db2_stats, batch_stats);

    StatisticsReport {
        report_title: "SYSTEM STATISTICS AND PERFORMANCE REPORT".into(),
        report_date,
        db2_summary,
        batch_summary,
        daily_volumes,
    }
}

fn compute_db2_summary(records: &[Db2StatsRecord]) -> Db2PerformanceSummary {
    let mut total_calls: u64 = 0;
    let mut total_elapsed = Decimal::ZERO;
    let mut total_cpu = Decimal::ZERO;
    let mut total_wait = Decimal::ZERO;

    for r in records {
        total_calls += r.total_calls;
        total_elapsed += r.elapsed_seconds;
        total_cpu += r.cpu_seconds;
        total_wait += r.wait_seconds;
    }

    let avg_response_ms = if total_calls > 0 {
        (total_elapsed / Decimal::from(total_calls)) * Decimal::from(1000)
    } else {
        Decimal::ZERO
    };

    let cpu_pct = if total_elapsed > Decimal::ZERO {
        (total_cpu / total_elapsed) * Decimal::from(100)
    } else {
        Decimal::ZERO
    };

    Db2PerformanceSummary {
        total_calls,
        avg_response_ms,
        total_elapsed_seconds: total_elapsed,
        total_cpu_seconds: total_cpu,
        total_wait_seconds: total_wait,
        cpu_pct,
    }
}

fn compute_batch_summary(records: &[BatchStatsRecord]) -> BatchProcessingSummary {
    let mut total_jobs: u64 = 0;
    let mut successful: u64 = 0;
    let mut failed: u64 = 0;
    let mut total_elapsed = Decimal::ZERO;

    for r in records {
        total_jobs += r.total_jobs;
        successful += r.successful_jobs;
        failed += r.failed_jobs;
        total_elapsed += r.elapsed_seconds;
    }

    let success_rate = if total_jobs > 0 {
        (Decimal::from(successful) / Decimal::from(total_jobs)) * Decimal::from(100)
    } else {
        Decimal::ZERO
    };

    let avg_job = if total_jobs > 0 {
        total_elapsed / Decimal::from(total_jobs)
    } else {
        Decimal::ZERO
    };

    BatchProcessingSummary {
        total_jobs,
        successful_jobs: successful,
        failed_jobs: failed,
        success_rate_pct: success_rate,
        total_elapsed_seconds: total_elapsed,
        avg_job_seconds: avg_job,
    }
}

fn compute_daily_volumes(
    db2_stats: &[Db2StatsRecord],
    batch_stats: &[BatchStatsRecord],
) -> Vec<DailyVolumeRow> {
    use std::collections::BTreeMap;

    let mut map: BTreeMap<NaiveDate, DailyVolumeRow> = BTreeMap::new();

    for r in db2_stats {
        let entry = map.entry(r.date).or_insert_with(|| DailyVolumeRow {
            date: r.date,
            db2_calls: 0,
            batch_jobs: 0,
            batch_failures: 0,
            error_rate_pct: Decimal::ZERO,
        });
        entry.db2_calls += r.total_calls;
    }

    for r in batch_stats {
        let entry = map.entry(r.date).or_insert_with(|| DailyVolumeRow {
            date: r.date,
            db2_calls: 0,
            batch_jobs: 0,
            batch_failures: 0,
            error_rate_pct: Decimal::ZERO,
        });
        entry.batch_jobs += r.total_jobs;
        entry.batch_failures += r.failed_jobs;
    }

    for row in map.values_mut() {
        if row.batch_jobs > 0 {
            row.error_rate_pct = (Decimal::from(row.batch_failures)
                / Decimal::from(row.batch_jobs))
                * Decimal::from(100);
        }
    }

    map.into_values().collect()
}

/// Render the report as a JSON string.
pub fn to_json(report: &StatisticsReport) -> Result<String, serde_json::Error> {
    serde_json::to_string_pretty(report)
}

/// Render the daily-volume rows as CSV.
pub fn to_csv<W: Write>(report: &StatisticsReport, writer: W) -> Result<(), csv::Error> {
    let mut wtr = csv::Writer::from_writer(writer);
    wtr.write_record([
        "date",
        "db2_calls",
        "batch_jobs",
        "batch_failures",
        "error_rate_pct",
    ])?;
    for row in &report.daily_volumes {
        wtr.write_record([
            &row.date.to_string(),
            &row.db2_calls.to_string(),
            &row.batch_jobs.to_string(),
            &row.batch_failures.to_string(),
            &row.error_rate_pct.to_string(),
        ])?;
    }
    wtr.flush()?;
    Ok(())
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use rust_decimal_macros::dec;

    fn sample_db2() -> Vec<Db2StatsRecord> {
        vec![
            Db2StatsRecord {
                date: NaiveDate::from_ymd_opt(2024, 3, 15).unwrap(),
                total_calls: 10_000,
                elapsed_seconds: dec!(50),
                cpu_seconds: dec!(30),
                wait_seconds: dec!(20),
            },
            Db2StatsRecord {
                date: NaiveDate::from_ymd_opt(2024, 3, 16).unwrap(),
                total_calls: 12_000,
                elapsed_seconds: dec!(60),
                cpu_seconds: dec!(35),
                wait_seconds: dec!(25),
            },
        ]
    }

    fn sample_batch() -> Vec<BatchStatsRecord> {
        vec![
            BatchStatsRecord {
                date: NaiveDate::from_ymd_opt(2024, 3, 15).unwrap(),
                total_jobs: 50,
                successful_jobs: 48,
                failed_jobs: 2,
                elapsed_seconds: dec!(3600),
            },
            BatchStatsRecord {
                date: NaiveDate::from_ymd_opt(2024, 3, 16).unwrap(),
                total_jobs: 55,
                successful_jobs: 55,
                failed_jobs: 0,
                elapsed_seconds: dec!(3300),
            },
        ]
    }

    #[test]
    fn db2_summary() {
        let report = generate(
            &sample_db2(),
            &sample_batch(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        let s = &report.db2_summary;
        assert_eq!(s.total_calls, 22_000);
        assert_eq!(s.total_elapsed_seconds, dec!(110));
        assert_eq!(s.total_cpu_seconds, dec!(65));
        assert_eq!(s.total_wait_seconds, dec!(45));
        // avg = 110/22000 * 1000 = 5.0 ms
        assert_eq!(s.avg_response_ms, dec!(5.000));
    }

    #[test]
    fn batch_summary() {
        let report = generate(
            &sample_db2(),
            &sample_batch(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        let s = &report.batch_summary;
        assert_eq!(s.total_jobs, 105);
        assert_eq!(s.successful_jobs, 103);
        assert_eq!(s.failed_jobs, 2);
        // success rate = 103/105 * 100
        assert!(s.success_rate_pct > dec!(98) && s.success_rate_pct < dec!(99));
        assert_eq!(s.total_elapsed_seconds, dec!(6900));
    }

    #[test]
    fn daily_volumes() {
        let report = generate(
            &sample_db2(),
            &sample_batch(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        assert_eq!(report.daily_volumes.len(), 2);

        let d1 = &report.daily_volumes[0];
        assert_eq!(d1.date, NaiveDate::from_ymd_opt(2024, 3, 15).unwrap());
        assert_eq!(d1.db2_calls, 10_000);
        assert_eq!(d1.batch_jobs, 50);
        assert_eq!(d1.batch_failures, 2);
        // error_rate = 2/50 * 100 = 4
        assert_eq!(d1.error_rate_pct, dec!(4));

        let d2 = &report.daily_volumes[1];
        assert_eq!(d2.error_rate_pct, dec!(0));
    }

    #[test]
    fn report_title() {
        let report = generate(
            &sample_db2(),
            &sample_batch(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        assert_eq!(
            report.report_title,
            "SYSTEM STATISTICS AND PERFORMANCE REPORT"
        );
    }

    #[test]
    fn json_roundtrip() {
        let report = generate(
            &sample_db2(),
            &sample_batch(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        let json = to_json(&report).unwrap();
        let back: StatisticsReport = serde_json::from_str(&json).unwrap();
        assert_eq!(back.db2_summary.total_calls, 22_000);
        assert_eq!(back.batch_summary.total_jobs, 105);
    }

    #[test]
    fn csv_format_valid() {
        let report = generate(
            &sample_db2(),
            &sample_batch(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        let mut buf = Vec::new();
        to_csv(&report, &mut buf).unwrap();
        let csv_str = String::from_utf8(buf).unwrap();

        let lines: Vec<&str> = csv_str.lines().collect();
        assert_eq!(lines.len(), 3); // header + 2 daily rows
        assert!(lines[0].starts_with("date,"));
        assert!(lines[1].contains("2024-03-15"));
    }

    #[test]
    fn empty_input() {
        let report = generate(&[], &[], NaiveDate::from_ymd_opt(2024, 4, 9).unwrap());
        assert_eq!(report.db2_summary.total_calls, 0);
        assert_eq!(report.batch_summary.total_jobs, 0);
        assert!(report.daily_volumes.is_empty());
    }

    #[test]
    fn zero_calls_no_panic() {
        let db2 = vec![Db2StatsRecord {
            date: NaiveDate::from_ymd_opt(2024, 1, 1).unwrap(),
            total_calls: 0,
            elapsed_seconds: dec!(0),
            cpu_seconds: dec!(0),
            wait_seconds: dec!(0),
        }];
        let report = generate(&db2, &[], NaiveDate::from_ymd_opt(2024, 4, 9).unwrap());
        assert_eq!(report.db2_summary.avg_response_ms, Decimal::ZERO);
        assert_eq!(report.db2_summary.cpu_pct, Decimal::ZERO);
    }
}
