//! Position repository — CRUD operations for investment positions.
//!
//! Translated from COBOL programs:
//! - `PORTTRAN.cbl` — Position reads and updates during transaction processing
//! - `POSUPD00.cbl` — Batch position updates
//!
//! VSAM keyed-file semantics are replaced by PostgreSQL + SQLx.
//! The COBOL READ/REWRITE pattern for positions maps to SQL
//! SELECT FOR UPDATE / UPDATE within a transaction.

use chrono::{DateTime, NaiveDate, Utc};
use rust_decimal::Decimal;
use sqlx::{FromRow, PgPool, Postgres, Transaction};
use tracing::instrument;
use uuid::Uuid;

use crate::pool::{map_sqlx_error, DbError};

// ---------------------------------------------------------------------------
// Row type returned by queries
// ---------------------------------------------------------------------------

/// Database row for the `positions` table.
#[derive(Debug, Clone, FromRow)]
pub struct PositionRow {
    pub id: Uuid,
    pub portfolio_id: Uuid,
    pub investment_id: String,
    pub position_date: NaiveDate,
    pub quantity: Decimal,
    pub cost_basis: Decimal,
    pub market_value: Decimal,
    pub currency_code: String,
    pub status: String,
    pub last_maint_date: DateTime<Utc>,
    pub last_maint_user: String,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

// ---------------------------------------------------------------------------
// Input types
// ---------------------------------------------------------------------------

/// Input for creating a new position.
#[derive(Debug, Clone)]
pub struct NewPosition {
    pub portfolio_id: Uuid,
    pub investment_id: String,
    pub position_date: NaiveDate,
    pub quantity: Decimal,
    pub cost_basis: Decimal,
    pub market_value: Decimal,
    pub currency_code: String,
    pub status: String,
    pub last_maint_user: String,
}

/// Input for updating position quantity and cost basis (used by transaction processing).
#[derive(Debug, Clone)]
pub struct PositionAdjustment {
    pub quantity_delta: Decimal,
    pub cost_basis_delta: Decimal,
    pub last_maint_user: String,
}

// ---------------------------------------------------------------------------
// Trait — PositionRepository
// ---------------------------------------------------------------------------

/// Repository trait for position CRUD.
///
/// Each method mirrors a COBOL operation:
/// - `create`                       → INSERT new position
/// - `find_by_id`                   → keyed READ by UUID
/// - `find_by_portfolio`            → READ positions for a portfolio
/// - `find_by_portfolio_investment` → keyed READ by portfolio + investment
/// - `adjust_in_tx`                 → REWRITE within a transaction (for PORTTRAN)
/// - `close`                        → soft-close position (status → 'C')
#[allow(async_fn_in_trait)]
pub trait PositionRepository: Send + Sync {
    async fn create(&self, input: &NewPosition) -> Result<PositionRow, DbError>;
    async fn find_by_id(&self, id: Uuid) -> Result<PositionRow, DbError>;
    async fn find_by_portfolio(&self, portfolio_id: Uuid) -> Result<Vec<PositionRow>, DbError>;
    async fn find_by_portfolio_investment(
        &self,
        portfolio_id: Uuid,
        investment_id: &str,
    ) -> Result<PositionRow, DbError>;
    async fn adjust_in_tx(
        tx: &mut Transaction<'_, Postgres>,
        position_id: Uuid,
        adj: &PositionAdjustment,
    ) -> Result<PositionRow, DbError>;
    async fn close(&self, id: Uuid, user: &str) -> Result<PositionRow, DbError>;
}

// ---------------------------------------------------------------------------
// Concrete implementation — PgPositionRepository
// ---------------------------------------------------------------------------

/// PostgreSQL-backed position repository.
pub struct PgPositionRepository {
    pool: PgPool,
}

impl PgPositionRepository {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

impl PositionRepository for PgPositionRepository {
    #[instrument(skip(self, input), fields(portfolio_id = %input.portfolio_id, investment_id = %input.investment_id))]
    async fn create(&self, input: &NewPosition) -> Result<PositionRow, DbError> {
        if input.investment_id.is_empty() {
            return Err(DbError::Other("investment_id must not be empty".into()));
        }

        let row = sqlx::query_as::<_, PositionRow>(
            r#"
            INSERT INTO positions (
                portfolio_id, investment_id, position_date,
                quantity, cost_basis, market_value,
                currency_code, status, last_maint_user
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            RETURNING *
            "#,
        )
        .bind(input.portfolio_id)
        .bind(&input.investment_id)
        .bind(input.position_date)
        .bind(input.quantity)
        .bind(input.cost_basis)
        .bind(input.market_value)
        .bind(&input.currency_code)
        .bind(&input.status)
        .bind(&input.last_maint_user)
        .fetch_one(&self.pool)
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok(row)
    }

    #[instrument(skip(self))]
    async fn find_by_id(&self, id: Uuid) -> Result<PositionRow, DbError> {
        let row = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(id)
            .fetch_one(&self.pool)
            .await
            .map_err(|e| map_sqlx_error(&e))?;

        Ok(row)
    }

    #[instrument(skip(self))]
    async fn find_by_portfolio(&self, portfolio_id: Uuid) -> Result<Vec<PositionRow>, DbError> {
        let rows = sqlx::query_as::<_, PositionRow>(
            r#"
            SELECT * FROM positions
            WHERE portfolio_id = $1
              AND status <> 'C'
            ORDER BY investment_id
            "#,
        )
        .bind(portfolio_id)
        .fetch_all(&self.pool)
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok(rows)
    }

    #[instrument(skip(self))]
    async fn find_by_portfolio_investment(
        &self,
        portfolio_id: Uuid,
        investment_id: &str,
    ) -> Result<PositionRow, DbError> {
        let row = sqlx::query_as::<_, PositionRow>(
            r#"
            SELECT * FROM positions
            WHERE portfolio_id = $1
              AND investment_id = $2
              AND status <> 'C'
            ORDER BY position_date DESC
            LIMIT 1
            "#,
        )
        .bind(portfolio_id)
        .bind(investment_id)
        .fetch_one(&self.pool)
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok(row)
    }

    /// Adjust position quantity and cost basis within an existing database
    /// transaction. Mirrors PORTTRAN.cbl's ADD/SUBTRACT + REWRITE pattern.
    #[instrument(skip(tx, adj))]
    async fn adjust_in_tx(
        tx: &mut Transaction<'_, Postgres>,
        position_id: Uuid,
        adj: &PositionAdjustment,
    ) -> Result<PositionRow, DbError> {
        let row = sqlx::query_as::<_, PositionRow>(
            r#"
            UPDATE positions
            SET
                quantity        = quantity + $1,
                cost_basis      = cost_basis + $2,
                last_maint_user = $3,
                last_maint_date = now(),
                updated_at      = now()
            WHERE id = $4
              AND status <> 'C'
            RETURNING *
            "#,
        )
        .bind(adj.quantity_delta)
        .bind(adj.cost_basis_delta)
        .bind(&adj.last_maint_user)
        .bind(position_id)
        .fetch_optional(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?
        .ok_or(DbError::NotFound)?;

        Ok(row)
    }

    #[instrument(skip(self))]
    async fn close(&self, id: Uuid, user: &str) -> Result<PositionRow, DbError> {
        let row = sqlx::query_as::<_, PositionRow>(
            r#"
            UPDATE positions
            SET
                status          = 'C',
                last_maint_user = $1,
                last_maint_date = now(),
                updated_at      = now()
            WHERE id = $2
              AND status <> 'C'
            RETURNING *
            "#,
        )
        .bind(user)
        .bind(id)
        .fetch_optional(&self.pool)
        .await
        .map_err(|e| map_sqlx_error(&e))?
        .ok_or(DbError::NotFound)?;

        Ok(row)
    }
}
