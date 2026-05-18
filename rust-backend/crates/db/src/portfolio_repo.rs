//! Portfolio repository — CRUD operations.
//!
//! Translated from COBOL programs:
//! - `PORTMSTR.cbl` — Master control (CRUD dispatch, validation)
//! - `PORTADD.cbl`  — Create portfolio (duplicate-key check)
//! - `PORTREAD.cbl` — Read / list portfolios (sequential scan)
//! - `PORTUPDT.cbl` — Update portfolio (field-level patching)
//! - `PORTDEL.cbl`  — Delete portfolio (soft-delete + audit)
//!
//! VSAM keyed-file semantics are replaced by PostgreSQL + SQLx.
//! Optimistic locking uses the `updated_at` column in place of
//! COBOL's REWRITE-with-file-status pattern.

use chrono::{DateTime, NaiveDate, Utc};
use rust_decimal::Decimal;
use sqlx::{FromRow, PgPool};
use tracing::instrument;
use uuid::Uuid;

use crate::pool::{map_sqlx_error, DbError};

// ---------------------------------------------------------------------------
// Row type returned by queries
// ---------------------------------------------------------------------------

/// Database row for the `portfolios` table.
#[derive(Debug, Clone, FromRow)]
pub struct PortfolioRow {
    pub id: Uuid,
    pub portfolio_id: String,
    pub account_number: String,
    pub account_type: String,
    pub branch_id: String,
    pub client_id: String,
    pub client_name: Option<String>,
    pub client_type: Option<String>,
    pub portfolio_name: String,
    pub currency_code: String,
    pub risk_level: String,
    pub status: String,
    pub total_value: Decimal,
    pub cash_balance: Decimal,
    pub open_date: NaiveDate,
    pub close_date: Option<NaiveDate>,
    pub last_maint_date: DateTime<Utc>,
    pub last_maint_user: String,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

// ---------------------------------------------------------------------------
// Supporting input types (PORTADD / PORTUPDT equivalents)
// ---------------------------------------------------------------------------

/// Input for creating a new portfolio (maps to PORTADD validation rules).
#[derive(Debug, Clone)]
pub struct NewPortfolio {
    pub portfolio_id: String,
    pub account_number: String,
    pub account_type: String,
    pub branch_id: String,
    pub client_id: String,
    pub client_name: Option<String>,
    pub client_type: Option<String>,
    pub portfolio_name: String,
    pub currency_code: String,
    pub risk_level: String,
    pub status: String,
    pub total_value: Decimal,
    pub cash_balance: Decimal,
    pub open_date: NaiveDate,
    pub last_maint_user: String,
}

/// Input for updating an existing portfolio (maps to PORTUPDT actions).
#[derive(Debug, Clone, Default)]
pub struct UpdatePortfolio {
    pub client_name: Option<String>,
    pub client_type: Option<String>,
    pub portfolio_name: Option<String>,
    pub status: Option<String>,
    pub total_value: Option<Decimal>,
    pub cash_balance: Option<Decimal>,
    pub risk_level: Option<String>,
    pub close_date: Option<Option<NaiveDate>>,
    pub last_maint_user: String,
    /// Optimistic lock: must match the row's current `updated_at`.
    pub expected_version: DateTime<Utc>,
}

/// Pagination parameters (replaces PORTREAD sequential scan).
#[derive(Debug, Clone)]
pub struct Pagination {
    pub limit: i64,
    pub offset: i64,
}

impl Default for Pagination {
    fn default() -> Self {
        Self {
            limit: 50,
            offset: 0,
        }
    }
}

/// Portfolio joined with its positions (maps to PORTMSTR read + position lookup).
#[derive(Debug, Clone)]
pub struct PortfolioWithPositions {
    pub portfolio: PortfolioRow,
    pub positions: Vec<PositionSummary>,
}

/// Lightweight position summary returned alongside a portfolio.
#[derive(Debug, Clone, FromRow)]
pub struct PositionSummary {
    pub id: Uuid,
    pub investment_id: String,
    pub quantity: Decimal,
    pub cost_basis: Decimal,
    pub market_value: Decimal,
    pub currency_code: String,
    pub status: String,
}

// ---------------------------------------------------------------------------
// Trait — PortfolioRepository
// ---------------------------------------------------------------------------

/// Repository trait for portfolio CRUD.
///
/// Each method mirrors a COBOL program:
/// - `create`           → PORTADD   (WRITE + duplicate check)
/// - `find_by_id`       → PORTREAD  (keyed READ)
/// - `find_all`         → PORTREAD  (sequential scan)
/// - `update`           → PORTUPDT  (REWRITE with optimistic lock)
/// - `delete`           → PORTDEL   (soft DELETE + audit trail)
/// - `find_with_positions` → PORTMSTR (READ + position join)
#[allow(async_fn_in_trait)]
pub trait PortfolioRepository: Send + Sync {
    async fn create(&self, input: &NewPortfolio) -> Result<PortfolioRow, DbError>;
    async fn find_by_id(&self, id: Uuid) -> Result<PortfolioRow, DbError>;
    async fn find_all(&self, page: &Pagination) -> Result<Vec<PortfolioRow>, DbError>;
    async fn update(&self, id: Uuid, input: &UpdatePortfolio) -> Result<PortfolioRow, DbError>;
    async fn delete(&self, id: Uuid, user: &str) -> Result<PortfolioRow, DbError>;
    async fn find_with_positions(&self, id: Uuid) -> Result<PortfolioWithPositions, DbError>;
}

// ---------------------------------------------------------------------------
// Concrete implementation — PgPortfolioRepository
// ---------------------------------------------------------------------------

/// PostgreSQL-backed portfolio repository.
pub struct PgPortfolioRepository {
    pool: PgPool,
}

impl PgPortfolioRepository {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

impl PortfolioRepository for PgPortfolioRepository {
    // -- PORTADD: create ----------------------------------------------------

    #[instrument(skip(self, input), fields(portfolio_id = %input.portfolio_id))]
    async fn create(&self, input: &NewPortfolio) -> Result<PortfolioRow, DbError> {
        // PORTADD 2100-VALIDATE-AND-ADD: rejects blank ID / name / invalid status.
        if input.portfolio_id.is_empty() || input.portfolio_name.is_empty() {
            return Err(DbError::Other(
                "portfolio_id and portfolio_name must not be empty".into(),
            ));
        }

        let row = sqlx::query_as::<_, PortfolioRow>(
            r#"
            INSERT INTO portfolios (
                portfolio_id, account_number, account_type, branch_id,
                client_id, client_name, client_type,
                portfolio_name, currency_code, risk_level,
                status, total_value, cash_balance,
                open_date, last_maint_user
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
            RETURNING *
            "#,
        )
        .bind(&input.portfolio_id)
        .bind(&input.account_number)
        .bind(&input.account_type)
        .bind(&input.branch_id)
        .bind(&input.client_id)
        .bind(&input.client_name)
        .bind(&input.client_type)
        .bind(&input.portfolio_name)
        .bind(&input.currency_code)
        .bind(&input.risk_level)
        .bind(&input.status)
        .bind(input.total_value)
        .bind(input.cash_balance)
        .bind(input.open_date)
        .bind(&input.last_maint_user)
        .fetch_one(&self.pool)
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok(row)
    }

    // -- PORTREAD: find by primary key --------------------------------------

    #[instrument(skip(self))]
    async fn find_by_id(&self, id: Uuid) -> Result<PortfolioRow, DbError> {
        let row = sqlx::query_as::<_, PortfolioRow>("SELECT * FROM portfolios WHERE id = $1")
            .bind(id)
            .fetch_one(&self.pool)
            .await
            .map_err(|e| map_sqlx_error(&e))?;

        Ok(row)
    }

    // -- PORTREAD: sequential scan with pagination --------------------------

    #[instrument(skip(self))]
    async fn find_all(&self, page: &Pagination) -> Result<Vec<PortfolioRow>, DbError> {
        let rows = sqlx::query_as::<_, PortfolioRow>(
            "SELECT * FROM portfolios ORDER BY portfolio_id LIMIT $1 OFFSET $2",
        )
        .bind(page.limit)
        .bind(page.offset)
        .fetch_all(&self.pool)
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok(rows)
    }

    // -- PORTUPDT: update with optimistic locking ---------------------------

    #[instrument(skip(self, input))]
    async fn update(&self, id: Uuid, input: &UpdatePortfolio) -> Result<PortfolioRow, DbError> {
        // Optimistic locking: the WHERE clause checks `updated_at` matches
        // the caller's expected version, similar to COBOL's REWRITE failing
        // with file-status 23 when another task modified the record.
        let row = sqlx::query_as::<_, PortfolioRow>(
            r#"
            UPDATE portfolios
            SET
                client_name     = COALESCE($1, client_name),
                client_type     = COALESCE($2, client_type),
                portfolio_name  = COALESCE($3, portfolio_name),
                status          = COALESCE($4, status),
                total_value     = COALESCE($5, total_value),
                cash_balance    = COALESCE($6, cash_balance),
                risk_level      = COALESCE($7, risk_level),
                close_date      = CASE WHEN $8 THEN $9 ELSE close_date END,
                last_maint_user = $10,
                last_maint_date = now(),
                updated_at      = now()
            WHERE id = $11
              AND updated_at = $12
            RETURNING *
            "#,
        )
        .bind(&input.client_name)
        .bind(&input.client_type)
        .bind(&input.portfolio_name)
        .bind(&input.status)
        .bind(input.total_value)
        .bind(input.cash_balance)
        .bind(&input.risk_level)
        .bind(input.close_date.is_some())
        .bind(input.close_date.flatten())
        .bind(&input.last_maint_user)
        .bind(id)
        .bind(input.expected_version)
        .fetch_optional(&self.pool)
        .await
        .map_err(|e| map_sqlx_error(&e))?
        .ok_or(DbError::NotFound)?;

        Ok(row)
    }

    // -- PORTDEL: soft delete -----------------------------------------------
    // PORTDEL sets status to 'C' (Closed) and writes an audit record.
    // We implement soft-delete by setting status + close_date.

    #[instrument(skip(self))]
    async fn delete(&self, id: Uuid, user: &str) -> Result<PortfolioRow, DbError> {
        let row = sqlx::query_as::<_, PortfolioRow>(
            r#"
            UPDATE portfolios
            SET
                status          = 'C',
                close_date      = CURRENT_DATE,
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

    // -- PORTMSTR: read portfolio with positions ----------------------------

    #[instrument(skip(self))]
    async fn find_with_positions(&self, id: Uuid) -> Result<PortfolioWithPositions, DbError> {
        let portfolio = self.find_by_id(id).await?;

        let positions = sqlx::query_as::<_, PositionSummary>(
            r#"
            SELECT
                id, investment_id, quantity, cost_basis,
                market_value, currency_code, status
            FROM positions
            WHERE portfolio_id = $1
              AND status <> 'C'
            ORDER BY investment_id
            "#,
        )
        .bind(id)
        .fetch_all(&self.pool)
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok(PortfolioWithPositions {
            portfolio,
            positions,
        })
    }
}

// ---------------------------------------------------------------------------
// Integration tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use sqlx::PgPool;
    use testcontainers::runners::AsyncRunner;
    use testcontainers::ImageExt;
    use testcontainers_modules::postgres::Postgres;

    async fn setup_pool() -> (PgPool, testcontainers::ContainerAsync<Postgres>) {
        let container = Postgres::default()
            .with_tag("16-alpine")
            .start()
            .await
            .unwrap();
        let port = container.get_host_port_ipv4(5432).await.unwrap();
        let url = format!("postgres://postgres:postgres@127.0.0.1:{port}/postgres");

        let pool = PgPool::connect(&url).await.unwrap();
        sqlx::migrate!("../../migrations").run(&pool).await.unwrap();

        (pool, container)
    }

    fn sample_new_portfolio(suffix: &str) -> NewPortfolio {
        NewPortfolio {
            portfolio_id: format!("PT{suffix}"),
            account_number: format!("AC{suffix}"),
            account_type: "SA".into(),
            branch_id: "01".into(),
            client_id: format!("CL{suffix}"),
            client_name: Some("Test Client".into()),
            client_type: Some("I".into()),
            portfolio_name: format!("Portfolio {suffix}"),
            currency_code: "USD".into(),
            risk_level: "M".into(),
            status: "A".into(),
            total_value: Decimal::new(10_000_000, 2),
            cash_balance: Decimal::new(1_000_000, 2),
            open_date: NaiveDate::from_ymd_opt(2024, 1, 15).unwrap(),
            last_maint_user: "TESTUSER".into(),
        }
    }

    // -- CRUD lifecycle -----------------------------------------------------

    #[tokio::test]
    async fn crud_lifecycle() {
        let (pool, _container) = setup_pool().await;
        let repo = PgPortfolioRepository::new(pool);

        // CREATE (PORTADD)
        let created = repo.create(&sample_new_portfolio("001")).await.unwrap();
        assert_eq!(created.portfolio_id, "PT001");
        assert_eq!(created.status, "A");
        assert_eq!(created.total_value, Decimal::new(10_000_000, 2));

        // READ (PORTREAD — keyed)
        let found = repo.find_by_id(created.id).await.unwrap();
        assert_eq!(found.portfolio_id, "PT001");
        assert_eq!(found.client_name, Some("Test Client".into()));

        // UPDATE (PORTUPDT)
        let update = UpdatePortfolio {
            client_name: Some("Updated Client".into()),
            total_value: Some(Decimal::new(20_000_000, 2)),
            last_maint_user: "UPDTUSER".into(),
            expected_version: found.updated_at,
            ..Default::default()
        };
        let updated = repo.update(found.id, &update).await.unwrap();
        assert_eq!(updated.client_name, Some("Updated Client".into()));
        assert_eq!(updated.total_value, Decimal::new(20_000_000, 2));
        assert_eq!(updated.last_maint_user, "UPDTUSER");

        // DELETE (PORTDEL — soft)
        let deleted = repo.delete(updated.id, "DELUSER").await.unwrap();
        assert_eq!(deleted.status, "C");
        assert!(deleted.close_date.is_some());
    }

    // -- Duplicate key (PORTADD WS-DUP-STATUS '22') -------------------------

    #[tokio::test]
    async fn create_duplicate_returns_error() {
        let (pool, _container) = setup_pool().await;
        let repo = PgPortfolioRepository::new(pool);

        repo.create(&sample_new_portfolio("DUP")).await.unwrap();
        let err = repo.create(&sample_new_portfolio("DUP")).await.unwrap_err();
        assert!(matches!(err, DbError::DuplicateKey));
    }

    // -- Pagination (PORTREAD sequential scan) ------------------------------

    #[tokio::test]
    async fn find_all_pagination() {
        let (pool, _container) = setup_pool().await;
        let repo = PgPortfolioRepository::new(pool);

        for i in 0..5 {
            repo.create(&sample_new_portfolio(&format!("PG{i:02}")))
                .await
                .unwrap();
        }

        let page1 = repo
            .find_all(&Pagination {
                limit: 3,
                offset: 0,
            })
            .await
            .unwrap();
        assert_eq!(page1.len(), 3);

        let page2 = repo
            .find_all(&Pagination {
                limit: 3,
                offset: 3,
            })
            .await
            .unwrap();
        assert_eq!(page2.len(), 2);

        let empty = repo
            .find_all(&Pagination {
                limit: 3,
                offset: 10,
            })
            .await
            .unwrap();
        assert!(empty.is_empty());
    }

    // -- Soft delete idempotency (PORTDEL — already closed) -----------------

    #[tokio::test]
    async fn delete_already_closed_returns_not_found() {
        let (pool, _container) = setup_pool().await;
        let repo = PgPortfolioRepository::new(pool);

        let created = repo.create(&sample_new_portfolio("DEL")).await.unwrap();
        repo.delete(created.id, "USER1").await.unwrap();

        let err = repo.delete(created.id, "USER2").await.unwrap_err();
        assert!(matches!(err, DbError::NotFound));
    }

    // -- Optimistic locking (PORTUPDT REWRITE failure) ----------------------

    #[tokio::test]
    async fn update_stale_version_returns_not_found() {
        let (pool, _container) = setup_pool().await;
        let repo = PgPortfolioRepository::new(pool);

        let created = repo.create(&sample_new_portfolio("OPT")).await.unwrap();

        // First update succeeds
        let update1 = UpdatePortfolio {
            client_name: Some("V1".into()),
            last_maint_user: "U1".into(),
            expected_version: created.updated_at,
            ..Default::default()
        };
        let v1 = repo.update(created.id, &update1).await.unwrap();

        // Second update with stale version fails
        let stale = UpdatePortfolio {
            client_name: Some("V2-stale".into()),
            last_maint_user: "U2".into(),
            expected_version: created.updated_at, // stale!
            ..Default::default()
        };
        let err = repo.update(created.id, &stale).await.unwrap_err();
        assert!(matches!(err, DbError::NotFound));

        // Correct version succeeds
        let update2 = UpdatePortfolio {
            client_name: Some("V2".into()),
            last_maint_user: "U2".into(),
            expected_version: v1.updated_at,
            ..Default::default()
        };
        let v2 = repo.update(created.id, &update2).await.unwrap();
        assert_eq!(v2.client_name, Some("V2".into()));
    }

    // -- Find not found -----------------------------------------------------

    #[tokio::test]
    async fn find_by_id_not_found() {
        let (pool, _container) = setup_pool().await;
        let repo = PgPortfolioRepository::new(pool);

        let err = repo.find_by_id(Uuid::new_v4()).await.unwrap_err();
        assert!(matches!(err, DbError::NotFound));
    }

    // -- Find with positions ------------------------------------------------

    #[tokio::test]
    async fn find_with_positions_empty() {
        let (pool, _container) = setup_pool().await;
        let repo = PgPortfolioRepository::new(pool);

        let created = repo.create(&sample_new_portfolio("WP1")).await.unwrap();
        let result = repo.find_with_positions(created.id).await.unwrap();
        assert_eq!(result.portfolio.id, created.id);
        assert!(result.positions.is_empty());
    }
}
