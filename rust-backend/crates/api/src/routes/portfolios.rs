//! Portfolio REST endpoints.
//!
//! Translated from COBOL programs:
//! - `INQONLN.cbl` — P300-PORTFOLIO-INQUIRY, P400-HISTORY-INQUIRY
//! - `INQSET.bms`  — POSMAP (position fields), HISMAP (history fields)
//!
//! The CICS online inquiry supported portfolio position lookup and
//! transaction history browsing via BMS screen maps. Here we expose
//! the same data through RESTful CRUD endpoints plus sub-resource
//! routes for positions and transactions.

use axum::extract::{Path, Query, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use chrono::NaiveDate;
use domain::{
    ClientType, PortfolioRecord, PortfolioStatus, PositionRecord, PositionStatus,
    TransactionRecord, TransactionStatus,
};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use crate::app::AppState;
use crate::error::ApiError;
use crate::middleware::auth::{require_role, AuthUser, Role};

// ---------------------------------------------------------------------------
// DTOs
// ---------------------------------------------------------------------------

/// Request body for creating / updating a portfolio.
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CreatePortfolioRequest {
    pub account_no: String,
    pub client_name: String,
    pub client_type: ClientType,
    #[serde(default)]
    pub cash_balance: Decimal,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpdatePortfolioRequest {
    pub client_name: Option<String>,
    pub client_type: Option<ClientType>,
    pub status: Option<PortfolioStatus>,
    pub cash_balance: Option<Decimal>,
}

/// Summary returned from list / get endpoints.
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PortfolioResponse {
    pub id: String,
    pub account_no: String,
    pub client_name: String,
    pub client_type: ClientType,
    pub status: PortfolioStatus,
    pub total_value: Decimal,
    pub cash_balance: Decimal,
    pub create_date: Option<NaiveDate>,
}

impl From<&PortfolioRecord> for PortfolioResponse {
    fn from(r: &PortfolioRecord) -> Self {
        Self {
            id: r.id.clone(),
            account_no: r.account_no.clone(),
            client_name: r.client_name.clone(),
            client_type: r.client_type,
            status: r.status,
            total_value: r.total_value,
            cash_balance: r.cash_balance,
            create_date: r.create_date,
        }
    }
}

/// Query parameters for list endpoints.
#[derive(Debug, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct ListParams {
    pub limit: Option<usize>,
    pub offset: Option<usize>,
    pub status: Option<String>,
}

/// Paginated list wrapper.
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PaginatedResponse<T> {
    pub data: Vec<T>,
    pub total: usize,
    pub limit: usize,
    pub offset: usize,
}

// ---------------------------------------------------------------------------
// Position DTOs (POSMAP fields from INQSET.bms)
// ---------------------------------------------------------------------------

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PositionResponse {
    pub portfolio_id: String,
    pub investment_id: String,
    pub quantity: Decimal,
    pub cost_basis: Decimal,
    pub market_value: Decimal,
    pub currency: String,
    pub status: PositionStatus,
    pub unrealized_gain_loss: Decimal,
}

impl From<&PositionRecord> for PositionResponse {
    fn from(r: &PositionRecord) -> Self {
        Self {
            portfolio_id: r.portfolio_id.clone(),
            investment_id: r.investment_id.clone(),
            quantity: r.quantity,
            cost_basis: r.cost_basis,
            market_value: r.market_value,
            currency: r.currency.clone(),
            status: r.status,
            unrealized_gain_loss: r.unrealized_gain_loss(),
        }
    }
}

// ---------------------------------------------------------------------------
// Transaction DTOs (HISMAP fields from INQSET.bms)
// ---------------------------------------------------------------------------

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TransactionResponse {
    pub date: Option<NaiveDate>,
    pub portfolio_id: String,
    pub investment_id: String,
    pub transaction_type: domain::TransactionType,
    pub quantity: Decimal,
    pub price: Decimal,
    pub amount: Decimal,
    pub status: TransactionStatus,
}

impl From<&TransactionRecord> for TransactionResponse {
    fn from(r: &TransactionRecord) -> Self {
        Self {
            date: r.date,
            portfolio_id: r.portfolio_id.clone(),
            investment_id: r.investment_id.clone(),
            transaction_type: r.transaction_type,
            quantity: r.quantity,
            price: r.price,
            amount: r.amount,
            status: r.status,
        }
    }
}

// ---------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------

/// Build the `/api/portfolios` router.
pub fn router() -> Router<AppState> {
    Router::new()
        .route("/", get(list_portfolios).post(create_portfolio))
        .route(
            "/{id}",
            get(get_portfolio)
                .put(update_portfolio)
                .delete(delete_portfolio),
        )
        .route("/{id}/positions", get(list_positions))
        .route("/{id}/transactions", get(list_transactions))
}

// ---------------------------------------------------------------------------
// Handlers — Portfolio CRUD
// ---------------------------------------------------------------------------

/// GET /api/portfolios — list all portfolios (P300-PORTFOLIO-INQUIRY).
async fn list_portfolios(
    user: AuthUser,
    State(state): State<AppState>,
    Query(params): Query<ListParams>,
) -> Result<Json<PaginatedResponse<PortfolioResponse>>, ApiError> {
    require_role(&user.claims, Role::Readonly)?;

    let limit = params.limit.unwrap_or(20).min(100);
    let offset = params.offset.unwrap_or(0);

    let store = state.store.read();
    let mut items: Vec<&PortfolioRecord> = store.portfolios.values().collect();

    if let Some(ref status_filter) = params.status {
        items.retain(|p| {
            let api_name = serde_json::to_value(p.status)
                .ok()
                .and_then(|v| v.as_str().map(String::from));
            api_name
                .as_deref()
                .is_some_and(|name| name.eq_ignore_ascii_case(status_filter))
        });
    }

    let total = items.len();
    items.sort_by(|a, b| a.id.cmp(&b.id));
    let page: Vec<PortfolioResponse> = items
        .into_iter()
        .skip(offset)
        .take(limit)
        .map(PortfolioResponse::from)
        .collect();

    Ok(Json(PaginatedResponse {
        data: page,
        total,
        limit,
        offset,
    }))
}

/// POST /api/portfolios — create a new portfolio.
async fn create_portfolio(
    user: AuthUser,
    State(state): State<AppState>,
    Json(body): Json<CreatePortfolioRequest>,
) -> Result<(StatusCode, Json<PortfolioResponse>), ApiError> {
    require_role(&user.claims, Role::User)?;

    let now = chrono::Utc::now().date_naive();
    let id = state
        .next_portfolio_id()
        .ok_or_else(|| ApiError::Conflict("portfolio ID space exhausted".into()))?;

    let record = PortfolioRecord {
        id: id.clone(),
        account_no: body.account_no,
        client_name: body.client_name,
        client_type: body.client_type,
        create_date: Some(now),
        last_maintenance_date: Some(now),
        status: PortfolioStatus::Active,
        total_value: body.cash_balance,
        cash_balance: body.cash_balance,
        last_user: user.claims.sub.clone(),
        last_transaction_date: None,
    };
    record.validate()?;

    let resp = PortfolioResponse::from(&record);
    let mut store = state.store.write();
    if store.portfolios.contains_key(&id) {
        return Err(ApiError::Conflict(format!("portfolio {id} already exists")));
    }
    store.portfolios.insert(id, record);

    Ok((StatusCode::CREATED, Json(resp)))
}

/// GET /api/portfolios/:id
async fn get_portfolio(
    user: AuthUser,
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Json<PortfolioResponse>, ApiError> {
    require_role(&user.claims, Role::Readonly)?;

    let store = state.store.read();
    let record = store
        .portfolios
        .get(&id)
        .ok_or_else(|| ApiError::NotFound(format!("portfolio {id}")))?;

    Ok(Json(PortfolioResponse::from(record)))
}

/// PUT /api/portfolios/:id
async fn update_portfolio(
    user: AuthUser,
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(body): Json<UpdatePortfolioRequest>,
) -> Result<Json<PortfolioResponse>, ApiError> {
    require_role(&user.claims, Role::User)?;

    let mut store = state.store.write();
    let existing = store
        .portfolios
        .get(&id)
        .ok_or_else(|| ApiError::NotFound(format!("portfolio {id}")))?;

    let mut updated = existing.clone();
    if let Some(name) = body.client_name {
        updated.client_name = name;
    }
    if let Some(ct) = body.client_type {
        updated.client_type = ct;
    }
    if let Some(st) = body.status {
        updated.status = st;
    }
    if let Some(cb) = body.cash_balance {
        updated.cash_balance = cb;
    }
    updated.last_maintenance_date = Some(chrono::Utc::now().date_naive());
    updated.last_user = user.claims.sub.clone();
    updated.validate()?;

    let resp = PortfolioResponse::from(&updated);
    store.portfolios.insert(id, updated);

    Ok(Json(resp))
}

/// DELETE /api/portfolios/:id
async fn delete_portfolio(
    user: AuthUser,
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<StatusCode, ApiError> {
    require_role(&user.claims, Role::Admin)?;

    let mut store = state.store.write();
    store
        .portfolios
        .remove(&id)
        .ok_or_else(|| ApiError::NotFound(format!("portfolio {id}")))?;

    Ok(StatusCode::NO_CONTENT)
}

// ---------------------------------------------------------------------------
// Handlers — Sub-resources
// ---------------------------------------------------------------------------

/// GET /api/portfolios/:id/positions (P300-PORTFOLIO-INQUIRY via POSMAP).
async fn list_positions(
    user: AuthUser,
    State(state): State<AppState>,
    Path(portfolio_id): Path<String>,
    Query(params): Query<ListParams>,
) -> Result<Json<PaginatedResponse<PositionResponse>>, ApiError> {
    require_role(&user.claims, Role::Readonly)?;

    let store = state.store.read();
    if !store.portfolios.contains_key(&portfolio_id) {
        return Err(ApiError::NotFound(format!("portfolio {portfolio_id}")));
    }

    let limit = params.limit.unwrap_or(20).min(100);
    let offset = params.offset.unwrap_or(0);

    let mut items: Vec<&PositionRecord> = store
        .positions
        .iter()
        .filter(|p| p.portfolio_id == portfolio_id)
        .collect();
    let total = items.len();
    items.sort_by(|a, b| a.investment_id.cmp(&b.investment_id));
    let page: Vec<PositionResponse> = items
        .into_iter()
        .skip(offset)
        .take(limit)
        .map(PositionResponse::from)
        .collect();

    Ok(Json(PaginatedResponse {
        data: page,
        total,
        limit,
        offset,
    }))
}

/// GET /api/portfolios/:id/transactions (P400-HISTORY-INQUIRY via HISMAP).
async fn list_transactions(
    user: AuthUser,
    State(state): State<AppState>,
    Path(portfolio_id): Path<String>,
    Query(params): Query<ListParams>,
) -> Result<Json<PaginatedResponse<TransactionResponse>>, ApiError> {
    require_role(&user.claims, Role::Readonly)?;

    let store = state.store.read();
    if !store.portfolios.contains_key(&portfolio_id) {
        return Err(ApiError::NotFound(format!("portfolio {portfolio_id}")));
    }

    let limit = params.limit.unwrap_or(20).min(100);
    let offset = params.offset.unwrap_or(0);

    let mut items: Vec<&TransactionRecord> = store
        .transactions
        .iter()
        .filter(|t| t.portfolio_id == portfolio_id)
        .collect();
    let total = items.len();
    items.sort_by_key(|t| std::cmp::Reverse(t.date));
    let page: Vec<TransactionResponse> = items
        .into_iter()
        .skip(offset)
        .take(limit)
        .map(TransactionResponse::from)
        .collect();

    Ok(Json(PaginatedResponse {
        data: page,
        total,
        limit,
        offset,
    }))
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn portfolio_response_from_record() {
        let rec = PortfolioRecord {
            id: "P001".into(),
            account_no: "ACC001".into(),
            client_name: "Test Client".into(),
            client_type: ClientType::Individual,
            create_date: Some(NaiveDate::from_ymd_opt(2025, 1, 1).unwrap()),
            last_maintenance_date: None,
            status: PortfolioStatus::Active,
            total_value: Decimal::new(100_000, 2),
            cash_balance: Decimal::new(50_000, 2),
            last_user: "user1".into(),
            last_transaction_date: None,
        };
        let resp = PortfolioResponse::from(&rec);
        assert_eq!(resp.id, "P001");
        assert_eq!(resp.account_no, "ACC001");
    }

    #[test]
    fn position_response_includes_gain_loss() {
        let rec = PositionRecord {
            portfolio_id: "P001".into(),
            investment_id: "INV01".into(),
            quantity: Decimal::new(100, 0),
            cost_basis: Decimal::new(1000, 0),
            market_value: Decimal::new(1200, 0),
            currency: "USD".into(),
            status: PositionStatus::Active,
            ..Default::default()
        };
        let resp = PositionResponse::from(&rec);
        assert_eq!(resp.unrealized_gain_loss, Decimal::new(200, 0));
    }
}
