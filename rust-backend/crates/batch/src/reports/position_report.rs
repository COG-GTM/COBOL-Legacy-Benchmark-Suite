//! Position report generator.
//!
//! Ported from COBOL program `RPTPOS00.cbl`.
//!
//! Generates daily position reports including portfolio holdings, market
//! values, and unrealised gains/losses.  Output is available as JSON or CSV.

use std::io::Write;

use chrono::NaiveDate;
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use domain::position::{PositionRecord, PositionStatus};

// ---------------------------------------------------------------------------
// Report row
// ---------------------------------------------------------------------------

/// A single line in the position report (mirrors WS-POSITION-DETAIL).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PositionReportRow {
    pub portfolio_id: String,
    pub investment_id: String,
    pub status: String,
    pub quantity: Decimal,
    pub cost_basis: Decimal,
    pub market_value: Decimal,
    pub unrealized_gain_loss: Decimal,
    pub gain_loss_pct: Decimal,
    pub currency: String,
}

impl PositionReportRow {
    pub fn from_position(pos: &PositionRecord) -> Self {
        let gain_loss = pos.unrealized_gain_loss();
        let pct = if pos.cost_basis != Decimal::ZERO {
            (gain_loss / pos.cost_basis) * Decimal::from(100)
        } else {
            Decimal::ZERO
        };
        Self {
            portfolio_id: pos.portfolio_id.clone(),
            investment_id: pos.investment_id.clone(),
            status: pos.status.code().to_string(),
            quantity: pos.quantity,
            cost_basis: pos.cost_basis,
            market_value: pos.market_value,
            unrealized_gain_loss: gain_loss,
            gain_loss_pct: pct,
            currency: pos.currency.clone(),
        }
    }
}

// ---------------------------------------------------------------------------
// Portfolio-level summary
// ---------------------------------------------------------------------------

/// Aggregate totals for a single portfolio.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PortfolioSummary {
    pub portfolio_id: String,
    pub position_count: u32,
    pub total_cost_basis: Decimal,
    pub total_market_value: Decimal,
    pub total_unrealized_gain_loss: Decimal,
    pub total_gain_loss_pct: Decimal,
}

// ---------------------------------------------------------------------------
// Full report
// ---------------------------------------------------------------------------

/// Complete position report (mirrors RPTPOS00 output).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PositionReport {
    pub report_title: String,
    pub report_date: NaiveDate,
    pub positions: Vec<PositionReportRow>,
    pub portfolio_summaries: Vec<PortfolioSummary>,
    pub grand_total_cost_basis: Decimal,
    pub grand_total_market_value: Decimal,
    pub grand_total_unrealized_gain_loss: Decimal,
    pub total_positions: u32,
}

// ---------------------------------------------------------------------------
// Generator
// ---------------------------------------------------------------------------

/// Build a `PositionReport` from a slice of position records.
///
/// Only active positions are included (matching COBOL behaviour that reads
/// through the position master sequentially and formats each record).
pub fn generate(positions: &[PositionRecord], report_date: NaiveDate) -> PositionReport {
    let mut rows: Vec<PositionReportRow> = Vec::new();

    for pos in positions {
        if pos.status == PositionStatus::Active {
            rows.push(PositionReportRow::from_position(pos));
        }
    }

    let mut summaries: Vec<PortfolioSummary> = Vec::new();
    let mut grand_cost = Decimal::ZERO;
    let mut grand_market = Decimal::ZERO;

    // Group rows by portfolio_id (positions are assumed sorted).
    let mut i = 0;
    while i < rows.len() {
        let pid = rows[i].portfolio_id.clone();
        let mut count: u32 = 0;
        let mut cost = Decimal::ZERO;
        let mut market = Decimal::ZERO;

        while i < rows.len() && rows[i].portfolio_id == pid {
            count += 1;
            cost += rows[i].cost_basis;
            market += rows[i].market_value;
            i += 1;
        }

        let gl = market - cost;
        let pct = if cost != Decimal::ZERO {
            (gl / cost) * Decimal::from(100)
        } else {
            Decimal::ZERO
        };

        summaries.push(PortfolioSummary {
            portfolio_id: pid,
            position_count: count,
            total_cost_basis: cost,
            total_market_value: market,
            total_unrealized_gain_loss: gl,
            total_gain_loss_pct: pct,
        });

        grand_cost += cost;
        grand_market += market;
    }

    let grand_gl = grand_market - grand_cost;

    PositionReport {
        report_title: "DAILY POSITION REPORT".into(),
        report_date,
        positions: rows.clone(),
        portfolio_summaries: summaries,
        grand_total_cost_basis: grand_cost,
        grand_total_market_value: grand_market,
        grand_total_unrealized_gain_loss: grand_gl,
        total_positions: rows.len() as u32,
    }
}

/// Render the report as a JSON string.
pub fn to_json(report: &PositionReport) -> Result<String, serde_json::Error> {
    serde_json::to_string_pretty(report)
}

/// Render the position rows as CSV.
pub fn to_csv<W: Write>(report: &PositionReport, writer: W) -> Result<(), csv::Error> {
    let mut wtr = csv::Writer::from_writer(writer);
    wtr.write_record([
        "portfolio_id",
        "investment_id",
        "status",
        "quantity",
        "cost_basis",
        "market_value",
        "unrealized_gain_loss",
        "gain_loss_pct",
        "currency",
    ])?;
    for row in &report.positions {
        wtr.write_record([
            &row.portfolio_id,
            &row.investment_id,
            &row.status,
            &row.quantity.to_string(),
            &row.cost_basis.to_string(),
            &row.market_value.to_string(),
            &row.unrealized_gain_loss.to_string(),
            &row.gain_loss_pct.to_string(),
            &row.currency,
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

    fn sample_positions() -> Vec<PositionRecord> {
        vec![
            PositionRecord {
                portfolio_id: "PORT001".into(),
                date: Some(NaiveDate::from_ymd_opt(2024, 1, 15).unwrap()),
                investment_id: "AAPL".into(),
                quantity: dec!(100),
                cost_basis: dec!(15000),
                market_value: dec!(17500),
                currency: "USD".into(),
                status: PositionStatus::Active,
                ..Default::default()
            },
            PositionRecord {
                portfolio_id: "PORT001".into(),
                date: Some(NaiveDate::from_ymd_opt(2024, 1, 15).unwrap()),
                investment_id: "GOOGL".into(),
                quantity: dec!(50),
                cost_basis: dec!(70000),
                market_value: dec!(68000),
                currency: "USD".into(),
                status: PositionStatus::Active,
                ..Default::default()
            },
            PositionRecord {
                portfolio_id: "PORT001".into(),
                date: Some(NaiveDate::from_ymd_opt(2024, 1, 15).unwrap()),
                investment_id: "TSLA".into(),
                quantity: dec!(25),
                cost_basis: dec!(5000),
                market_value: dec!(6200),
                currency: "USD".into(),
                status: PositionStatus::Closed,
                ..Default::default()
            },
            PositionRecord {
                portfolio_id: "PORT002".into(),
                date: Some(NaiveDate::from_ymd_opt(2024, 1, 15).unwrap()),
                investment_id: "MSFT".into(),
                quantity: dec!(200),
                cost_basis: dec!(60000),
                market_value: dec!(62000),
                currency: "USD".into(),
                status: PositionStatus::Active,
                ..Default::default()
            },
        ]
    }

    #[test]
    fn generate_filters_closed_positions() {
        let report = generate(
            &sample_positions(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        assert_eq!(report.total_positions, 3);
        assert!(report.positions.iter().all(|r| r.investment_id != "TSLA"));
    }

    #[test]
    fn portfolio_summaries_computed() {
        let report = generate(
            &sample_positions(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        assert_eq!(report.portfolio_summaries.len(), 2);

        let port1 = &report.portfolio_summaries[0];
        assert_eq!(port1.portfolio_id, "PORT001");
        assert_eq!(port1.position_count, 2);
        assert_eq!(port1.total_cost_basis, dec!(85000));
        assert_eq!(port1.total_market_value, dec!(85500));
        assert_eq!(port1.total_unrealized_gain_loss, dec!(500));
    }

    #[test]
    fn grand_totals() {
        let report = generate(
            &sample_positions(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        assert_eq!(report.grand_total_cost_basis, dec!(145000));
        assert_eq!(report.grand_total_market_value, dec!(147500));
        assert_eq!(report.grand_total_unrealized_gain_loss, dec!(2500));
    }

    #[test]
    fn gain_loss_pct_calculated() {
        let report = generate(
            &sample_positions(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        let aapl = &report.positions[0];
        // (17500-15000)/15000 * 100 ≈ 16.666…
        assert!(aapl.gain_loss_pct > dec!(16) && aapl.gain_loss_pct < dec!(17));
    }

    #[test]
    fn zero_cost_basis_no_panic() {
        let positions = vec![PositionRecord {
            portfolio_id: "PORT003".into(),
            date: Some(NaiveDate::from_ymd_opt(2024, 1, 1).unwrap()),
            investment_id: "FREE".into(),
            quantity: dec!(10),
            cost_basis: dec!(0),
            market_value: dec!(500),
            currency: "USD".into(),
            status: PositionStatus::Active,
            ..Default::default()
        }];
        let report = generate(&positions, NaiveDate::from_ymd_opt(2024, 4, 9).unwrap());
        assert_eq!(report.positions[0].gain_loss_pct, Decimal::ZERO);
    }

    #[test]
    fn json_roundtrip() {
        let report = generate(
            &sample_positions(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        let json = to_json(&report).unwrap();
        let back: PositionReport = serde_json::from_str(&json).unwrap();
        assert_eq!(back.total_positions, report.total_positions);
        assert_eq!(back.report_title, "DAILY POSITION REPORT");
    }

    #[test]
    fn csv_format_valid() {
        let report = generate(
            &sample_positions(),
            NaiveDate::from_ymd_opt(2024, 4, 9).unwrap(),
        );
        let mut buf = Vec::new();
        to_csv(&report, &mut buf).unwrap();
        let csv_str = String::from_utf8(buf).unwrap();

        let lines: Vec<&str> = csv_str.lines().collect();
        // header + 3 data rows
        assert_eq!(lines.len(), 4);
        assert!(lines[0].starts_with("portfolio_id,"));
        assert!(lines[1].contains("PORT001"));
    }

    #[test]
    fn empty_input() {
        let report = generate(&[], NaiveDate::from_ymd_opt(2024, 4, 9).unwrap());
        assert_eq!(report.total_positions, 0);
        assert!(report.portfolio_summaries.is_empty());
        assert_eq!(report.grand_total_cost_basis, Decimal::ZERO);
    }
}
