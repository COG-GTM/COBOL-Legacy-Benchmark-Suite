//! Audit report generator.
//!
//! Ported from COBOL program `RPTAUD00.cbl`.
//!
//! Generates audit trail reports with filtering by date range, user, and
//! entity.  Output is available as JSON or CSV.

use std::io::Write;

use chrono::NaiveDate;
use serde::{Deserialize, Serialize};

use domain::audit::{AuditAction, AuditRecord, AuditStatus, AuditType};

// ---------------------------------------------------------------------------
// Filter
// ---------------------------------------------------------------------------

/// Criteria for selecting audit records.
#[derive(Debug, Clone, Default)]
pub struct AuditFilter {
    pub start_date: Option<NaiveDate>,
    pub end_date: Option<NaiveDate>,
    pub user_id: Option<String>,
    pub entity_id: Option<String>,
    pub audit_type: Option<AuditType>,
    pub action: Option<AuditAction>,
    pub status: Option<AuditStatus>,
}

impl AuditFilter {
    /// Returns `true` if the record passes every non-`None` criterion.
    ///
    /// Records with unparseable timestamps are excluded when a date filter
    /// is active.
    pub fn matches(&self, rec: &AuditRecord) -> bool {
        if self.start_date.is_some() || self.end_date.is_some() {
            let ts_date = parse_date_prefix(&rec.timestamp);
            if let Some(ref start) = self.start_date {
                match ts_date {
                    Ok(d) if d < *start => return false,
                    Err(_) => return false,
                    _ => {}
                }
            }
            if let Some(ref end) = self.end_date {
                match ts_date {
                    Ok(d) if d > *end => return false,
                    Err(_) => return false,
                    _ => {}
                }
            }
        }
        if let Some(ref uid) = self.user_id {
            if rec.user_id != *uid {
                return false;
            }
        }
        if let Some(ref eid) = self.entity_id {
            if rec.portfolio_id != *eid && rec.account_no != *eid {
                return false;
            }
        }
        if let Some(at) = self.audit_type {
            if rec.audit_type != at {
                return false;
            }
        }
        if let Some(act) = self.action {
            if rec.action != act {
                return false;
            }
        }
        if let Some(st) = self.status {
            if rec.status != st {
                return false;
            }
        }
        true
    }
}

/// Try to extract a `NaiveDate` from the leading `YYYY-MM-DD` of a timestamp.
fn parse_date_prefix(ts: &str) -> Result<NaiveDate, ()> {
    if ts.len() < 10 {
        return Err(());
    }
    NaiveDate::parse_from_str(&ts[..10], "%Y-%m-%d").map_err(|_| ())
}

// ---------------------------------------------------------------------------
// Report row
// ---------------------------------------------------------------------------

/// A single line in the audit report (mirrors WS-AUDIT-DETAIL).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuditReportRow {
    pub timestamp: String,
    pub user_id: String,
    pub program: String,
    pub audit_type: String,
    pub action: String,
    pub status: String,
    pub portfolio_id: String,
    pub account_no: String,
    pub message: String,
}

impl AuditReportRow {
    pub fn from_record(rec: &AuditRecord) -> Self {
        Self {
            timestamp: rec.timestamp.clone(),
            user_id: rec.user_id.clone(),
            program: rec.program.clone(),
            audit_type: rec.audit_type.code().to_string(),
            action: rec.action.code().to_string(),
            status: rec.status.code().to_string(),
            portfolio_id: rec.portfolio_id.clone(),
            account_no: rec.account_no.clone(),
            message: rec.message.clone(),
        }
    }
}

// ---------------------------------------------------------------------------
// Summary
// ---------------------------------------------------------------------------

/// Aggregate counts by audit type (mirrors 2310-WRITE-AUDIT-SUMMARY).
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuditSummary {
    pub total_records: u32,
    pub transaction_count: u32,
    pub user_action_count: u32,
    pub system_event_count: u32,
    pub success_count: u32,
    pub failure_count: u32,
    pub warning_count: u32,
}

// ---------------------------------------------------------------------------
// Full report
// ---------------------------------------------------------------------------

/// Complete audit report (mirrors RPTAUD00 output).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuditReport {
    pub report_title: String,
    pub report_date: NaiveDate,
    pub filter_description: String,
    pub rows: Vec<AuditReportRow>,
    pub summary: AuditSummary,
}

// ---------------------------------------------------------------------------
// Generator
// ---------------------------------------------------------------------------

/// Build an `AuditReport` from a slice of audit records with optional
/// filtering.
pub fn generate(
    records: &[AuditRecord],
    report_date: NaiveDate,
    filter: &AuditFilter,
) -> AuditReport {
    let mut rows: Vec<AuditReportRow> = Vec::new();
    let mut summary = AuditSummary::default();

    for rec in records {
        if !filter.matches(rec) {
            continue;
        }

        rows.push(AuditReportRow::from_record(rec));

        summary.total_records += 1;
        match rec.audit_type {
            AuditType::Transaction => summary.transaction_count += 1,
            AuditType::UserAction => summary.user_action_count += 1,
            AuditType::SystemEvent => summary.system_event_count += 1,
        }
        match rec.status {
            AuditStatus::Success => summary.success_count += 1,
            AuditStatus::Failure => summary.failure_count += 1,
            AuditStatus::Warning => summary.warning_count += 1,
        }
    }

    let desc = build_filter_description(filter);

    AuditReport {
        report_title: "SYSTEM AUDIT REPORT".into(),
        report_date,
        filter_description: desc,
        rows,
        summary,
    }
}

fn build_filter_description(f: &AuditFilter) -> String {
    let mut parts: Vec<String> = Vec::new();
    if let Some(d) = f.start_date {
        parts.push(format!("from {d}"));
    }
    if let Some(d) = f.end_date {
        parts.push(format!("to {d}"));
    }
    if let Some(ref u) = f.user_id {
        parts.push(format!("user={u}"));
    }
    if let Some(ref e) = f.entity_id {
        parts.push(format!("entity={e}"));
    }
    if let Some(at) = f.audit_type {
        parts.push(format!("type={}", at.code()));
    }
    if let Some(act) = f.action {
        parts.push(format!("action={}", act.code()));
    }
    if let Some(st) = f.status {
        parts.push(format!("status={}", st.code()));
    }
    if parts.is_empty() {
        "ALL RECORDS".into()
    } else {
        parts.join(", ")
    }
}

/// Render the report as a JSON string.
pub fn to_json(report: &AuditReport) -> Result<String, serde_json::Error> {
    serde_json::to_string_pretty(report)
}

/// Render the audit rows as CSV.
pub fn to_csv<W: Write>(report: &AuditReport, writer: W) -> Result<(), csv::Error> {
    let mut wtr = csv::Writer::from_writer(writer);
    wtr.write_record([
        "timestamp",
        "user_id",
        "program",
        "audit_type",
        "action",
        "status",
        "portfolio_id",
        "account_no",
        "message",
    ])?;
    for row in &report.rows {
        wtr.write_record([
            &row.timestamp,
            &row.user_id,
            &row.program,
            &row.audit_type,
            &row.action,
            &row.status,
            &row.portfolio_id,
            &row.account_no,
            &row.message,
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

    fn sample_records() -> Vec<AuditRecord> {
        vec![
            AuditRecord {
                timestamp: "2024-03-15T10:30:00.000000".into(),
                system_id: "SYS1".into(),
                user_id: "USER01".into(),
                program: "TRNVAL00".into(),
                terminal: "T001".into(),
                audit_type: AuditType::Transaction,
                action: AuditAction::Create,
                status: AuditStatus::Success,
                portfolio_id: "PORT001".into(),
                account_no: "ACC001".into(),
                before_image: String::new(),
                after_image: String::new(),
                message: "Transaction validated".into(),
            },
            AuditRecord {
                timestamp: "2024-03-15T11:00:00.000000".into(),
                system_id: "SYS1".into(),
                user_id: "USER02".into(),
                program: "POSUPD00".into(),
                terminal: "T002".into(),
                audit_type: AuditType::UserAction,
                action: AuditAction::Update,
                status: AuditStatus::Success,
                portfolio_id: "PORT002".into(),
                account_no: "ACC002".into(),
                before_image: String::new(),
                after_image: String::new(),
                message: "Position updated".into(),
            },
            AuditRecord {
                timestamp: "2024-03-16T09:00:00.000000".into(),
                system_id: "SYS1".into(),
                user_id: "USER01".into(),
                program: "BCHCTL00".into(),
                terminal: String::new(),
                audit_type: AuditType::SystemEvent,
                action: AuditAction::Startup,
                status: AuditStatus::Success,
                portfolio_id: String::new(),
                account_no: String::new(),
                before_image: String::new(),
                after_image: String::new(),
                message: "Batch startup".into(),
            },
            AuditRecord {
                timestamp: "2024-03-17T14:00:00.000000".into(),
                system_id: "SYS1".into(),
                user_id: "USER03".into(),
                program: "TRNVAL00".into(),
                terminal: "T003".into(),
                audit_type: AuditType::Transaction,
                action: AuditAction::Create,
                status: AuditStatus::Failure,
                portfolio_id: "PORT001".into(),
                account_no: "ACC001".into(),
                before_image: String::new(),
                after_image: String::new(),
                message: "Validation failed".into(),
            },
        ]
    }

    #[test]
    fn generate_no_filter() {
        let report = generate(
            &sample_records(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &AuditFilter::default(),
        );
        assert_eq!(report.summary.total_records, 4);
        assert_eq!(report.summary.transaction_count, 2);
        assert_eq!(report.summary.user_action_count, 1);
        assert_eq!(report.summary.system_event_count, 1);
        assert_eq!(report.summary.success_count, 3);
        assert_eq!(report.summary.failure_count, 1);
        assert_eq!(report.filter_description, "ALL RECORDS");
    }

    #[test]
    fn filter_by_date_range() {
        let filter = AuditFilter {
            start_date: Some(NaiveDate::from_ymd_opt(2024, 3, 15).unwrap()),
            end_date: Some(NaiveDate::from_ymd_opt(2024, 3, 15).unwrap()),
            ..Default::default()
        };
        let report = generate(
            &sample_records(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &filter,
        );
        assert_eq!(report.summary.total_records, 2);
    }

    #[test]
    fn filter_by_user() {
        let filter = AuditFilter {
            user_id: Some("USER01".into()),
            ..Default::default()
        };
        let report = generate(
            &sample_records(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &filter,
        );
        assert_eq!(report.summary.total_records, 2);
    }

    #[test]
    fn filter_by_entity() {
        let filter = AuditFilter {
            entity_id: Some("PORT001".into()),
            ..Default::default()
        };
        let report = generate(
            &sample_records(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &filter,
        );
        assert_eq!(report.summary.total_records, 2);
    }

    #[test]
    fn filter_by_audit_type() {
        let filter = AuditFilter {
            audit_type: Some(AuditType::Transaction),
            ..Default::default()
        };
        let report = generate(
            &sample_records(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &filter,
        );
        assert_eq!(report.summary.total_records, 2);
    }

    #[test]
    fn filter_by_status() {
        let filter = AuditFilter {
            status: Some(AuditStatus::Failure),
            ..Default::default()
        };
        let report = generate(
            &sample_records(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &filter,
        );
        assert_eq!(report.summary.total_records, 1);
        assert_eq!(report.rows[0].message, "Validation failed");
    }

    #[test]
    fn combined_filters() {
        let filter = AuditFilter {
            user_id: Some("USER01".into()),
            audit_type: Some(AuditType::Transaction),
            ..Default::default()
        };
        let report = generate(
            &sample_records(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &filter,
        );
        assert_eq!(report.summary.total_records, 1);
        assert_eq!(report.rows[0].program, "TRNVAL00");
    }

    #[test]
    fn json_roundtrip() {
        let report = generate(
            &sample_records(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &AuditFilter::default(),
        );
        let json = to_json(&report).unwrap();
        let back: AuditReport = serde_json::from_str(&json).unwrap();
        assert_eq!(back.summary.total_records, 4);
        assert_eq!(back.report_title, "SYSTEM AUDIT REPORT");
    }

    #[test]
    fn csv_format_valid() {
        let report = generate(
            &sample_records(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &AuditFilter::default(),
        );
        let mut buf = Vec::new();
        to_csv(&report, &mut buf).unwrap();
        let csv_str = String::from_utf8(buf).unwrap();

        let lines: Vec<&str> = csv_str.lines().collect();
        assert_eq!(lines.len(), 5); // header + 4 rows
        assert!(lines[0].starts_with("timestamp,"));
    }

    #[test]
    fn empty_input() {
        let report = generate(
            &[],
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
            &AuditFilter::default(),
        );
        assert_eq!(report.summary.total_records, 0);
        assert!(report.rows.is_empty());
    }

    #[test]
    fn filter_description_formats() {
        let f = AuditFilter {
            start_date: Some(NaiveDate::from_ymd_opt(2024, 1, 1).unwrap()),
            user_id: Some("ADMIN".into()),
            ..Default::default()
        };
        let desc = build_filter_description(&f);
        assert!(desc.contains("from 2024-01-01"));
        assert!(desc.contains("user=ADMIN"));
    }

    #[test]
    fn unparseable_timestamp_excluded_by_date_filter() {
        let bad_record = AuditRecord {
            timestamp: "INVALID".into(),
            system_id: "SYS1".into(),
            user_id: "USER01".into(),
            program: "TEST".into(),
            terminal: String::new(),
            audit_type: AuditType::Transaction,
            action: AuditAction::Create,
            status: AuditStatus::Success,
            portfolio_id: String::new(),
            account_no: String::new(),
            before_image: String::new(),
            after_image: String::new(),
            message: "bad ts".into(),
        };
        let filter = AuditFilter {
            start_date: Some(NaiveDate::from_ymd_opt(2024, 1, 1).unwrap()),
            ..Default::default()
        };
        assert!(!filter.matches(&bad_record));

        // Without date filter the record passes
        assert!(AuditFilter::default().matches(&bad_record));
    }
}
