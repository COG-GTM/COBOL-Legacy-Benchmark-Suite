//! Transaction processing service.
//!
//! Translated from COBOL program `PORTTRAN.cbl` — Portfolio Transaction Processing.
//!
//! The COBOL program reads a sequential transaction file, validates each record,
//! dispatches to type-specific handlers (Buy/Sell/Transfer/Fee), updates VSAM
//! position records, and writes audit trail entries via CALL 'AUDPROC'.
//!
//! In this Rust port the sequential file becomes an iterator of
//! [`TransactionRequest`] values, VSAM READ/REWRITE becomes PostgreSQL
//! SELECT FOR UPDATE / UPDATE inside a database transaction, and the
//! AUDPROC CALL becomes an INSERT into the `audit_trail` table.
//!
//! ## PORTTRAN.cbl mapping
//!
//! | COBOL paragraph         | Rust equivalent                        |
//! |-------------------------|----------------------------------------|
//! | 2100-VALIDATE-TRANSACTION | `TransactionService::validate`       |
//! | 2200-UPDATE-POSITIONS     | `TransactionService::process`        |
//! | 2210-PROCESS-BUY          | `process_buy`                        |
//! | 2220-PROCESS-SELL         | `process_sell`                        |
//! | 2230-PROCESS-TRANSFER     | `process_transfer`                   |
//! | 2240-PROCESS-FEE          | `process_fee`                        |
//! | 2300-UPDATE-AUDIT-TRAIL   | `write_audit_entry`                  |
//! | 9000-ERROR-ROUTINE        | `TransactionError` variants          |

use chrono::Utc;
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};
use sqlx::{FromRow, PgPool, Postgres, Transaction};
use tracing::{info, instrument};
use uuid::Uuid;

use crate::pool::{map_sqlx_error, DbError};
use crate::position_repo::PositionRow;

// ---------------------------------------------------------------------------
// Error type — maps PORTTRAN 9000-ERROR-ROUTINE
// ---------------------------------------------------------------------------

/// Errors that can occur during transaction processing.
///
/// Each variant corresponds to an ERR-TEXT value set in PORTTRAN.cbl's
/// validation and processing paragraphs.
#[derive(Debug, Clone, thiserror::Error)]
pub enum TransactionError {
    #[error("portfolio ID is required")]
    MissingPortfolioId,

    #[error("invalid portfolio ID: {0}")]
    InvalidPortfolio(String),

    #[error("invalid transaction type: {0}")]
    InvalidTransactionType(String),

    #[error("quantity must be greater than zero")]
    InvalidQuantity,

    #[error("price must be greater than zero")]
    InvalidPrice,

    #[error("amount must be greater than zero")]
    InvalidAmount,

    #[error("insufficient units for sale (available: {available}, requested: {requested})")]
    InsufficientUnits {
        available: Decimal,
        requested: Decimal,
    },

    #[error("position not found for portfolio {portfolio_id}, investment {investment_id}")]
    PositionNotFound {
        portfolio_id: String,
        investment_id: String,
    },

    #[error("source portfolio not found: {0}")]
    SourcePortfolioNotFound(String),

    #[error("destination portfolio not found: {0}")]
    DestinationPortfolioNotFound(String),

    #[error("amount overflow during transaction processing")]
    Overflow,

    #[error("database error: {0}")]
    Db(#[from] DbError),
}

// ---------------------------------------------------------------------------
// Transaction request — input type
// ---------------------------------------------------------------------------

/// Type of transaction to process.
///
/// Maps to PORTTRAN.cbl TRN-TYPE: BU/SL/TR/FE.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum TxnType {
    Buy,
    Sell,
    Transfer,
    Fee,
}

impl TxnType {
    pub fn code(&self) -> &'static str {
        match self {
            Self::Buy => "BU",
            Self::Sell => "SL",
            Self::Transfer => "TR",
            Self::Fee => "FE",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "BU" => Some(Self::Buy),
            "SL" => Some(Self::Sell),
            "TR" => Some(Self::Transfer),
            "FE" => Some(Self::Fee),
            _ => None,
        }
    }

    /// Audit action for this transaction type.
    ///
    /// Maps to PORTTRAN.cbl 2300-UPDATE-AUDIT-TRAIL:
    /// - BU → CREATE
    /// - SL → DELETE
    /// - TR → UPDATE
    /// - FE → UPDATE
    fn audit_action(&self) -> &'static str {
        match self {
            Self::Buy => "CREATE",
            Self::Sell => "DELETE",
            Self::Transfer => "UPDATE",
            Self::Fee => "UPDATE",
        }
    }
}

/// A request to process a single transaction.
///
/// Corresponds to one record from PORTTRAN.cbl's TRANSACTION-FILE.
#[derive(Debug, Clone)]
pub struct TransactionRequest {
    pub portfolio_id: Uuid,
    /// Human-readable portfolio identifier (PORT-ID) for audit messages.
    pub portfolio_code: String,
    pub account_number: String,
    pub investment_id: String,
    pub txn_type: TxnType,
    pub quantity: Decimal,
    pub price: Decimal,
    pub amount: Decimal,
    pub currency_code: String,
    pub process_user: String,
    /// For transfers: the destination portfolio UUID.
    pub destination_portfolio_id: Option<Uuid>,
    /// For transfers: the destination portfolio code for audit.
    pub destination_portfolio_code: Option<String>,
}

// ---------------------------------------------------------------------------
// Transaction result
// ---------------------------------------------------------------------------

/// Outcome of a successfully processed transaction.
#[derive(Debug, Clone)]
pub struct TransactionResult {
    pub transaction_id: Uuid,
    pub txn_type: TxnType,
    pub portfolio_id: Uuid,
    pub position_id: Uuid,
    pub quantity_applied: Decimal,
    pub cost_basis_applied: Decimal,
    pub audit_trail_id: Uuid,
}

// ---------------------------------------------------------------------------
// Audit trail row
// ---------------------------------------------------------------------------

/// Row returned after inserting an audit trail entry.
#[derive(Debug, Clone, FromRow)]
pub struct AuditTrailRow {
    pub id: Uuid,
}

// ---------------------------------------------------------------------------
// Transaction row (for recording the transaction itself)
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, FromRow)]
struct InsertedTransaction {
    id: Uuid,
}

// ---------------------------------------------------------------------------
// TransactionService
// ---------------------------------------------------------------------------

/// Service that processes portfolio transactions.
///
/// Implements the full pipeline from PORTTRAN.cbl:
/// 1. Validate transaction (2100-VALIDATE-TRANSACTION)
/// 2. Update positions (2200-UPDATE-POSITIONS)
/// 3. Write audit trail (2300-UPDATE-AUDIT-TRAIL)
/// 4. Handle rollback on failure (database transaction)
pub struct TransactionService {
    pool: PgPool,
}

impl TransactionService {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    /// Validate a transaction request.
    ///
    /// Mirrors PORTTRAN.cbl paragraphs 2100 through 2130:
    /// - 2110-CHECK-PORTFOLIO: portfolio ID must be non-empty
    /// - 2120-CHECK-TRANSACTION-TYPE: must be BU/SL/TR/FE
    /// - 2130-CHECK-AMOUNTS: quantity > 0, price > 0, amount > 0
    ///   (price and amount checks are relaxed for transfers)
    pub fn validate(req: &TransactionRequest) -> Result<(), TransactionError> {
        // 2110-CHECK-PORTFOLIO
        if req.portfolio_code.is_empty() {
            return Err(TransactionError::MissingPortfolioId);
        }

        // 2120-CHECK-TRANSACTION-TYPE — already enforced by TxnType enum,
        // but we validate investment_id is present.
        if req.investment_id.is_empty() {
            return Err(TransactionError::InvalidTransactionType(
                "investment_id is required".into(),
            ));
        }

        // 2130-CHECK-AMOUNTS
        if req.quantity <= Decimal::ZERO {
            return Err(TransactionError::InvalidQuantity);
        }

        if req.price <= Decimal::ZERO && req.txn_type != TxnType::Transfer {
            return Err(TransactionError::InvalidPrice);
        }

        if req.amount <= Decimal::ZERO && req.txn_type != TxnType::Transfer {
            return Err(TransactionError::InvalidAmount);
        }

        Ok(())
    }

    /// Process a single transaction inside a database transaction.
    ///
    /// Mirrors PORTTRAN.cbl 2200-UPDATE-POSITIONS: dispatches to the
    /// appropriate handler, then writes an audit trail entry. The entire
    /// operation is wrapped in a PostgreSQL transaction so that a failure
    /// at any point triggers a full rollback — replacing COBOL's manual
    /// error-routing through 9000-ERROR-ROUTINE.
    #[instrument(skip(self, req), fields(txn_type = req.txn_type.code(), portfolio = %req.portfolio_id))]
    pub async fn process(
        &self,
        req: &TransactionRequest,
    ) -> Result<TransactionResult, TransactionError> {
        Self::validate(req)?;

        let mut tx = self
            .pool
            .begin()
            .await
            .map_err(|e| DbError::from_sqlx(&e))?;

        let (position_id, qty_applied, cost_applied) = match req.txn_type {
            TxnType::Buy => self.process_buy(&mut tx, req).await?,
            TxnType::Sell => self.process_sell(&mut tx, req).await?,
            TxnType::Transfer => self.process_transfer(&mut tx, req).await?,
            TxnType::Fee => self.process_fee(&mut tx, req).await?,
        };

        // Record the transaction itself in the transactions table.
        let txn_row = self.record_transaction(&mut tx, req).await?;

        // 2300-UPDATE-AUDIT-TRAIL
        let audit_id = self.write_audit_entry(&mut tx, req, true).await?;

        tx.commit().await.map_err(|e| DbError::from_sqlx(&e))?;

        info!(
            txn_id = %txn_row.id,
            position_id = %position_id,
            "transaction processed successfully"
        );

        Ok(TransactionResult {
            transaction_id: txn_row.id,
            txn_type: req.txn_type,
            portfolio_id: req.portfolio_id,
            position_id,
            quantity_applied: qty_applied,
            cost_basis_applied: cost_applied,
            audit_trail_id: audit_id,
        })
    }

    // -----------------------------------------------------------------------
    // 2210-PROCESS-BUY
    // -----------------------------------------------------------------------

    /// Buy: add quantity and cost basis to the position.
    ///
    /// PORTTRAN.cbl:
    /// ```cobol
    /// ADD TRN-QUANTITY TO PORT-TOTAL-UNITS
    /// ADD TRN-AMOUNT   TO PORT-TOTAL-COST
    /// ```
    ///
    /// If no active position exists for the portfolio + investment,
    /// we create one (upsert semantics).
    async fn process_buy(
        &self,
        tx: &mut Transaction<'_, Postgres>,
        req: &TransactionRequest,
    ) -> Result<(Uuid, Decimal, Decimal), TransactionError> {
        let position = self.find_or_create_position(tx, req).await?;

        let updated = sqlx::query_as::<_, PositionRow>(
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
        .bind(req.quantity)
        .bind(req.amount)
        .bind(&req.process_user)
        .bind(position.id)
        .fetch_optional(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?
        .ok_or(DbError::NotFound)?;

        // Check for overflow: quantity or cost_basis should not be negative
        // after a buy.
        if updated.quantity < Decimal::ZERO || updated.cost_basis < Decimal::ZERO {
            return Err(TransactionError::Overflow);
        }

        Ok((updated.id, req.quantity, req.amount))
    }

    // -----------------------------------------------------------------------
    // 2220-PROCESS-SELL
    // -----------------------------------------------------------------------

    /// Sell: subtract quantity and cost basis from the position.
    ///
    /// PORTTRAN.cbl:
    /// ```cobol
    /// IF PORT-TOTAL-UNITS < TRN-QUANTITY
    ///     MOVE 'Insufficient units for sale' TO ERR-TEXT
    ///     ...
    /// SUBTRACT TRN-QUANTITY FROM PORT-TOTAL-UNITS
    /// SUBTRACT TRN-AMOUNT   FROM PORT-TOTAL-COST
    /// ```
    async fn process_sell(
        &self,
        tx: &mut Transaction<'_, Postgres>,
        req: &TransactionRequest,
    ) -> Result<(Uuid, Decimal, Decimal), TransactionError> {
        let position = self
            .find_active_position(tx, req.portfolio_id, &req.investment_id)
            .await?;

        // Check sufficient units (PORTTRAN line 210)
        if position.quantity < req.quantity {
            return Err(TransactionError::InsufficientUnits {
                available: position.quantity,
                requested: req.quantity,
            });
        }

        let updated = sqlx::query_as::<_, PositionRow>(
            r#"
            UPDATE positions
            SET
                quantity        = quantity - $1,
                cost_basis      = cost_basis - $2,
                last_maint_user = $3,
                last_maint_date = now(),
                updated_at      = now()
            WHERE id = $4
              AND status <> 'C'
            RETURNING *
            "#,
        )
        .bind(req.quantity)
        .bind(req.amount)
        .bind(&req.process_user)
        .bind(position.id)
        .fetch_optional(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?
        .ok_or(DbError::NotFound)?;

        // If position is fully sold, close it.
        if updated.quantity == Decimal::ZERO {
            sqlx::query(
                r#"
                UPDATE positions
                SET status = 'C', updated_at = now()
                WHERE id = $1
                "#,
            )
            .bind(updated.id)
            .execute(tx.as_mut())
            .await
            .map_err(|e| map_sqlx_error(&e))?;
        }

        Ok((updated.id, -req.quantity, -req.amount))
    }

    // -----------------------------------------------------------------------
    // 2230-PROCESS-TRANSFER
    // -----------------------------------------------------------------------

    /// Transfer: move units from one portfolio to another.
    ///
    /// PORTTRAN.cbl had this as a stub:
    /// ```cobol
    /// MOVE 'Transfer processing not implemented' TO ERR-TEXT
    /// ```
    ///
    /// We implement it fully: subtract from source, add to destination,
    /// preserving cost basis proportionally.
    async fn process_transfer(
        &self,
        tx: &mut Transaction<'_, Postgres>,
        req: &TransactionRequest,
    ) -> Result<(Uuid, Decimal, Decimal), TransactionError> {
        let dest_portfolio_id = req.destination_portfolio_id.ok_or_else(|| {
            TransactionError::DestinationPortfolioNotFound("not specified".into())
        })?;

        // Verify destination portfolio exists.
        let dest_exists = sqlx::query_scalar::<_, bool>(
            "SELECT EXISTS(SELECT 1 FROM portfolios WHERE id = $1 AND status = 'A')",
        )
        .bind(dest_portfolio_id)
        .fetch_one(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        if !dest_exists {
            return Err(TransactionError::DestinationPortfolioNotFound(
                dest_portfolio_id.to_string(),
            ));
        }

        // Read source position.
        let source = self
            .find_active_position(tx, req.portfolio_id, &req.investment_id)
            .await?;

        if source.quantity < req.quantity {
            return Err(TransactionError::InsufficientUnits {
                available: source.quantity,
                requested: req.quantity,
            });
        }

        // Calculate proportional cost basis for the transferred units.
        let cost_per_unit = if source.quantity != Decimal::ZERO {
            source.cost_basis / source.quantity
        } else {
            Decimal::ZERO
        };
        let transfer_cost = cost_per_unit * req.quantity;

        // Subtract from source.
        let updated_source = sqlx::query_as::<_, PositionRow>(
            r#"
            UPDATE positions
            SET
                quantity        = quantity - $1,
                cost_basis      = cost_basis - $2,
                last_maint_user = $3,
                last_maint_date = now(),
                updated_at      = now()
            WHERE id = $4
              AND status <> 'C'
            RETURNING *
            "#,
        )
        .bind(req.quantity)
        .bind(transfer_cost)
        .bind(&req.process_user)
        .bind(source.id)
        .fetch_optional(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?
        .ok_or(DbError::NotFound)?;

        // Close source position if fully transferred.
        if updated_source.quantity == Decimal::ZERO {
            sqlx::query(
                r#"
                UPDATE positions
                SET status = 'C', updated_at = now()
                WHERE id = $1
                "#,
            )
            .bind(updated_source.id)
            .execute(tx.as_mut())
            .await
            .map_err(|e| map_sqlx_error(&e))?;
        }

        // Upsert destination position.
        let dest_position = self
            .find_or_create_position_for(
                tx,
                dest_portfolio_id,
                &req.investment_id,
                &req.currency_code,
                &req.process_user,
            )
            .await?;

        sqlx::query(
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
            "#,
        )
        .bind(req.quantity)
        .bind(transfer_cost)
        .bind(&req.process_user)
        .bind(dest_position.id)
        .execute(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok((source.id, -req.quantity, -transfer_cost))
    }

    // -----------------------------------------------------------------------
    // 2240-PROCESS-FEE
    // -----------------------------------------------------------------------

    /// Fee: subtract amount from cost basis without changing quantity.
    ///
    /// PORTTRAN.cbl:
    /// ```cobol
    /// SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST
    /// ```
    async fn process_fee(
        &self,
        tx: &mut Transaction<'_, Postgres>,
        req: &TransactionRequest,
    ) -> Result<(Uuid, Decimal, Decimal), TransactionError> {
        let position = self
            .find_active_position(tx, req.portfolio_id, &req.investment_id)
            .await?;

        let updated = sqlx::query_as::<_, PositionRow>(
            r#"
            UPDATE positions
            SET
                cost_basis      = cost_basis - $1,
                last_maint_user = $2,
                last_maint_date = now(),
                updated_at      = now()
            WHERE id = $3
              AND status <> 'C'
            RETURNING *
            "#,
        )
        .bind(req.amount)
        .bind(&req.process_user)
        .bind(position.id)
        .fetch_optional(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?
        .ok_or(DbError::NotFound)?;

        Ok((updated.id, Decimal::ZERO, -req.amount))
    }

    // -----------------------------------------------------------------------
    // 2300-UPDATE-AUDIT-TRAIL
    // -----------------------------------------------------------------------

    /// Write an audit trail entry.
    ///
    /// Maps to PORTTRAN.cbl 2300-UPDATE-AUDIT-TRAIL / 2310-WRITE-AUDIT-RECORD.
    /// The COBOL program populates:
    /// - AUD-TIMESTAMP    → FUNCTION CURRENT-DATE
    /// - AUD-PROGRAM      → 'PORTTRAN'
    /// - AUD-USER-ID      → FUNCTION USER-ID
    /// - AUD-TYPE         → 'TRAN'
    /// - AUD-ACTION       → based on TRN-TYPE (BU→CREATE, SL→DELETE, TR/FE→UPDATE)
    /// - AUD-STATUS       → 'SUCC' or 'FAIL'
    /// - AUD-PORTFOLIO-ID → TRN-PORTFOLIO-ID
    /// - AUD-ACCOUNT-NO   → PORT-ACCOUNT-NO
    /// - AUD-MESSAGE      → 'Transaction: XX Amount: NNN Units: NNN'
    async fn write_audit_entry(
        &self,
        tx: &mut Transaction<'_, Postgres>,
        req: &TransactionRequest,
        success: bool,
    ) -> Result<Uuid, TransactionError> {
        let action = req.txn_type.audit_action();
        let status = if success { "SUCC" } else { "FAIL" };
        let message = format!(
            "Transaction: {} Amount: {} Units: {}",
            req.txn_type.code(),
            req.amount,
            req.quantity,
        );

        let row = sqlx::query_as::<_, AuditTrailRow>(
            r#"
            INSERT INTO audit_trail (
                user_id, action, entity_type, entity_id,
                audit_type, audit_status, program_id, message
            )
            VALUES ($1, $2, 'TRANSACTION', $3, 'TRAN', $4, 'PORTTRAN', $5)
            RETURNING id
            "#,
        )
        .bind(&req.process_user)
        .bind(action)
        .bind(req.portfolio_id.to_string())
        .bind(status)
        .bind(&message)
        .fetch_one(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok(row.id)
    }

    // -----------------------------------------------------------------------
    // Record the transaction in the transactions table
    // -----------------------------------------------------------------------

    async fn record_transaction(
        &self,
        tx: &mut Transaction<'_, Postgres>,
        req: &TransactionRequest,
    ) -> Result<InsertedTransaction, TransactionError> {
        let now = Utc::now();
        let txn_date = now.date_naive();
        let txn_time = now.time();
        let txn_id = format!(
            "{}{}",
            now.format("%Y%m%d%H%M%S"),
            &Uuid::new_v4().to_string()[..6],
        );

        let row = sqlx::query_as::<_, InsertedTransaction>(
            r#"
            INSERT INTO transactions (
                transaction_id, portfolio_id, transaction_date, transaction_time,
                investment_id, transaction_type, quantity, price, amount,
                currency_code, status, process_user
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'D', $11)
            RETURNING id
            "#,
        )
        .bind(&txn_id)
        .bind(req.portfolio_id)
        .bind(txn_date)
        .bind(txn_time)
        .bind(&req.investment_id)
        .bind(req.txn_type.code())
        .bind(req.quantity)
        .bind(req.price)
        .bind(req.amount)
        .bind(&req.currency_code)
        .bind(&req.process_user)
        .fetch_one(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok(row)
    }

    // -----------------------------------------------------------------------
    // Position helpers
    // -----------------------------------------------------------------------

    /// Find an active position or create one if none exists (for buys).
    async fn find_or_create_position(
        &self,
        tx: &mut Transaction<'_, Postgres>,
        req: &TransactionRequest,
    ) -> Result<PositionRow, TransactionError> {
        self.find_or_create_position_for(
            tx,
            req.portfolio_id,
            &req.investment_id,
            &req.currency_code,
            &req.process_user,
        )
        .await
    }

    async fn find_or_create_position_for(
        &self,
        tx: &mut Transaction<'_, Postgres>,
        portfolio_id: Uuid,
        investment_id: &str,
        currency_code: &str,
        user: &str,
    ) -> Result<PositionRow, TransactionError> {
        // First try to find an active position (any date).
        let existing = sqlx::query_as::<_, PositionRow>(
            r#"
            SELECT * FROM positions
            WHERE portfolio_id = $1
              AND investment_id = $2
              AND status <> 'C'
            ORDER BY position_date DESC
            LIMIT 1
            FOR UPDATE
            "#,
        )
        .bind(portfolio_id)
        .bind(investment_id)
        .fetch_optional(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        if let Some(row) = existing {
            return Ok(row);
        }

        // No active position. Use INSERT ... ON CONFLICT to handle the case
        // where a closed position already occupies today's
        // (portfolio_id, investment_id, position_date) slot — reactivate it
        // instead of failing with a unique-constraint violation.
        let row = sqlx::query_as::<_, PositionRow>(
            r#"
            INSERT INTO positions (
                portfolio_id, investment_id, position_date,
                quantity, cost_basis, market_value,
                currency_code, status, last_maint_user
            )
            VALUES ($1, $2, CURRENT_DATE, 0, 0, 0, $3, 'A', $4)
            ON CONFLICT (portfolio_id, investment_id, position_date)
            DO UPDATE SET
                status          = 'A',
                quantity        = 0,
                cost_basis      = 0,
                market_value    = 0,
                last_maint_user = $4,
                last_maint_date = now(),
                updated_at      = now()
            RETURNING *
            "#,
        )
        .bind(portfolio_id)
        .bind(investment_id)
        .bind(currency_code)
        .bind(user)
        .fetch_one(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        Ok(row)
    }

    /// Find an active position — returns error if none exists (for sells and fees).
    async fn find_active_position(
        &self,
        tx: &mut Transaction<'_, Postgres>,
        portfolio_id: Uuid,
        investment_id: &str,
    ) -> Result<PositionRow, TransactionError> {
        let row = sqlx::query_as::<_, PositionRow>(
            r#"
            SELECT * FROM positions
            WHERE portfolio_id = $1
              AND investment_id = $2
              AND status <> 'C'
            ORDER BY position_date DESC
            LIMIT 1
            FOR UPDATE
            "#,
        )
        .bind(portfolio_id)
        .bind(investment_id)
        .fetch_optional(tx.as_mut())
        .await
        .map_err(|e| map_sqlx_error(&e))?;

        row.ok_or_else(|| TransactionError::PositionNotFound {
            portfolio_id: portfolio_id.to_string(),
            investment_id: investment_id.to_string(),
        })
    }
}

// ---------------------------------------------------------------------------
// DbError helper (avoids orphan-rule issues)
// ---------------------------------------------------------------------------

impl DbError {
    fn from_sqlx(e: &sqlx::Error) -> Self {
        map_sqlx_error(e)
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use rust_decimal_macros::dec;
    use sqlx::PgPool;
    use testcontainers::runners::AsyncRunner;
    use testcontainers::ImageExt;
    use testcontainers_modules::postgres::Postgres;

    // -- Test infrastructure ------------------------------------------------

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

    /// Insert a test portfolio and return its UUID.
    async fn insert_portfolio(pool: &PgPool, code: &str) -> Uuid {
        sqlx::query_scalar::<_, Uuid>(
            r#"
            INSERT INTO portfolios (
                portfolio_id, account_number, account_type, branch_id,
                client_id, portfolio_name, currency_code, risk_level,
                status, last_maint_user
            )
            VALUES ($1, 'ACC001', 'SA', '01', 'CL001', 'Test Portfolio',
                    'USD', 'M', 'A', 'TESTUSER')
            RETURNING id
            "#,
        )
        .bind(code)
        .fetch_one(pool)
        .await
        .unwrap()
    }

    /// Insert a test position for a portfolio and return its UUID.
    async fn insert_position(
        pool: &PgPool,
        portfolio_id: Uuid,
        investment_id: &str,
        quantity: Decimal,
        cost_basis: Decimal,
    ) -> Uuid {
        sqlx::query_scalar::<_, Uuid>(
            r#"
            INSERT INTO positions (
                portfolio_id, investment_id, position_date,
                quantity, cost_basis, market_value,
                currency_code, status, last_maint_user
            )
            VALUES ($1, $2, CURRENT_DATE, $3, $4, $4, 'USD', 'A', 'TESTUSER')
            RETURNING id
            "#,
        )
        .bind(portfolio_id)
        .bind(investment_id)
        .bind(quantity)
        .bind(cost_basis)
        .fetch_one(pool)
        .await
        .unwrap()
    }

    fn buy_request(portfolio_id: Uuid, code: &str) -> TransactionRequest {
        TransactionRequest {
            portfolio_id,
            portfolio_code: code.into(),
            account_number: "ACC001".into(),
            investment_id: "AAPL".into(),
            txn_type: TxnType::Buy,
            quantity: dec!(100),
            price: dec!(150.00),
            amount: dec!(15000.00),
            currency_code: "USD".into(),
            process_user: "TRNUSER".into(),
            destination_portfolio_id: None,
            destination_portfolio_code: None,
        }
    }

    fn sell_request(portfolio_id: Uuid, code: &str) -> TransactionRequest {
        TransactionRequest {
            portfolio_id,
            portfolio_code: code.into(),
            account_number: "ACC001".into(),
            investment_id: "AAPL".into(),
            txn_type: TxnType::Sell,
            quantity: dec!(50),
            price: dec!(160.00),
            amount: dec!(8000.00),
            currency_code: "USD".into(),
            process_user: "TRNUSER".into(),
            destination_portfolio_id: None,
            destination_portfolio_code: None,
        }
    }

    fn fee_request(portfolio_id: Uuid, code: &str) -> TransactionRequest {
        TransactionRequest {
            portfolio_id,
            portfolio_code: code.into(),
            account_number: "ACC001".into(),
            investment_id: "AAPL".into(),
            txn_type: TxnType::Fee,
            quantity: dec!(1),
            price: dec!(25.00),
            amount: dec!(25.00),
            currency_code: "USD".into(),
            process_user: "TRNUSER".into(),
            destination_portfolio_id: None,
            destination_portfolio_code: None,
        }
    }

    fn transfer_request(
        src_id: Uuid,
        src_code: &str,
        dest_id: Uuid,
        dest_code: &str,
    ) -> TransactionRequest {
        TransactionRequest {
            portfolio_id: src_id,
            portfolio_code: src_code.into(),
            account_number: "ACC001".into(),
            investment_id: "AAPL".into(),
            txn_type: TxnType::Transfer,
            quantity: dec!(30),
            price: Decimal::ZERO,
            amount: Decimal::ZERO,
            currency_code: "USD".into(),
            process_user: "TRNUSER".into(),
            destination_portfolio_id: Some(dest_id),
            destination_portfolio_code: Some(dest_code.into()),
        }
    }

    // -- Validation tests ---------------------------------------------------

    #[test]
    fn validate_rejects_missing_portfolio_id() {
        let req = TransactionRequest {
            portfolio_id: Uuid::new_v4(),
            portfolio_code: String::new(),
            account_number: "ACC001".into(),
            investment_id: "AAPL".into(),
            txn_type: TxnType::Buy,
            quantity: dec!(10),
            price: dec!(100),
            amount: dec!(1000),
            currency_code: "USD".into(),
            process_user: "USR".into(),
            destination_portfolio_id: None,
            destination_portfolio_code: None,
        };
        assert!(matches!(
            TransactionService::validate(&req),
            Err(TransactionError::MissingPortfolioId)
        ));
    }

    #[test]
    fn validate_rejects_zero_quantity() {
        let req = TransactionRequest {
            portfolio_id: Uuid::new_v4(),
            portfolio_code: "PT001".into(),
            account_number: "ACC001".into(),
            investment_id: "AAPL".into(),
            txn_type: TxnType::Buy,
            quantity: Decimal::ZERO,
            price: dec!(100),
            amount: dec!(1000),
            currency_code: "USD".into(),
            process_user: "USR".into(),
            destination_portfolio_id: None,
            destination_portfolio_code: None,
        };
        assert!(matches!(
            TransactionService::validate(&req),
            Err(TransactionError::InvalidQuantity)
        ));
    }

    #[test]
    fn validate_rejects_zero_price_non_transfer() {
        let req = TransactionRequest {
            portfolio_id: Uuid::new_v4(),
            portfolio_code: "PT001".into(),
            account_number: "ACC001".into(),
            investment_id: "AAPL".into(),
            txn_type: TxnType::Sell,
            quantity: dec!(10),
            price: Decimal::ZERO,
            amount: dec!(1000),
            currency_code: "USD".into(),
            process_user: "USR".into(),
            destination_portfolio_id: None,
            destination_portfolio_code: None,
        };
        assert!(matches!(
            TransactionService::validate(&req),
            Err(TransactionError::InvalidPrice)
        ));
    }

    #[test]
    fn validate_rejects_zero_amount_non_transfer() {
        let req = TransactionRequest {
            portfolio_id: Uuid::new_v4(),
            portfolio_code: "PT001".into(),
            account_number: "ACC001".into(),
            investment_id: "AAPL".into(),
            txn_type: TxnType::Buy,
            quantity: dec!(10),
            price: dec!(100),
            amount: Decimal::ZERO,
            currency_code: "USD".into(),
            process_user: "USR".into(),
            destination_portfolio_id: None,
            destination_portfolio_code: None,
        };
        assert!(matches!(
            TransactionService::validate(&req),
            Err(TransactionError::InvalidAmount)
        ));
    }

    #[test]
    fn validate_allows_zero_price_for_transfer() {
        let req = TransactionRequest {
            portfolio_id: Uuid::new_v4(),
            portfolio_code: "PT001".into(),
            account_number: "ACC001".into(),
            investment_id: "AAPL".into(),
            txn_type: TxnType::Transfer,
            quantity: dec!(10),
            price: Decimal::ZERO,
            amount: Decimal::ZERO,
            currency_code: "USD".into(),
            process_user: "USR".into(),
            destination_portfolio_id: Some(Uuid::new_v4()),
            destination_portfolio_code: Some("PT002".into()),
        };
        assert!(TransactionService::validate(&req).is_ok());
    }

    #[test]
    fn validate_rejects_empty_investment_id() {
        let req = TransactionRequest {
            portfolio_id: Uuid::new_v4(),
            portfolio_code: "PT001".into(),
            account_number: "ACC001".into(),
            investment_id: String::new(),
            txn_type: TxnType::Buy,
            quantity: dec!(10),
            price: dec!(100),
            amount: dec!(1000),
            currency_code: "USD".into(),
            process_user: "USR".into(),
            destination_portfolio_id: None,
            destination_portfolio_code: None,
        };
        assert!(matches!(
            TransactionService::validate(&req),
            Err(TransactionError::InvalidTransactionType(_))
        ));
    }

    // -- TxnType tests ------------------------------------------------------

    #[test]
    fn txn_type_code_roundtrip() {
        for tt in [TxnType::Buy, TxnType::Sell, TxnType::Transfer, TxnType::Fee] {
            assert_eq!(TxnType::from_code(tt.code()), Some(tt));
        }
        assert_eq!(TxnType::from_code("XX"), None);
    }

    #[test]
    fn txn_type_audit_actions() {
        assert_eq!(TxnType::Buy.audit_action(), "CREATE");
        assert_eq!(TxnType::Sell.audit_action(), "DELETE");
        assert_eq!(TxnType::Transfer.audit_action(), "UPDATE");
        assert_eq!(TxnType::Fee.audit_action(), "UPDATE");
    }

    // -- Integration tests (require Docker) ---------------------------------

    #[tokio::test]
    async fn buy_creates_position_and_audit() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "BUY001").await;
        let req = buy_request(port_id, "BUY001");

        let result = svc.process(&req).await.unwrap();
        assert_eq!(result.txn_type, TxnType::Buy);
        assert_eq!(result.quantity_applied, dec!(100));
        assert_eq!(result.cost_basis_applied, dec!(15000.00));

        // Verify position was created.
        let pos = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(result.position_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(pos.quantity, dec!(100));
        assert_eq!(pos.cost_basis, dec!(15000.00));
        assert_eq!(pos.status, "A");

        // Verify audit trail entry.
        let audit_count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM audit_trail WHERE id = $1")
            .bind(result.audit_trail_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(audit_count, 1);
    }

    #[tokio::test]
    async fn buy_adds_to_existing_position() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "BUY002").await;
        let _pos_id = insert_position(&pool, port_id, "AAPL", dec!(50), dec!(7500)).await;

        let req = buy_request(port_id, "BUY002");
        let result = svc.process(&req).await.unwrap();

        let pos = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(result.position_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(pos.quantity, dec!(150));
        assert_eq!(pos.cost_basis, dec!(22500.00));
    }

    #[tokio::test]
    async fn sell_subtracts_from_position() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "SL001").await;
        let _pos_id = insert_position(&pool, port_id, "AAPL", dec!(100), dec!(15000)).await;

        let req = sell_request(port_id, "SL001");
        let result = svc.process(&req).await.unwrap();

        assert_eq!(result.quantity_applied, dec!(-50));
        assert_eq!(result.cost_basis_applied, dec!(-8000.00));

        let pos = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(result.position_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(pos.quantity, dec!(50));
        assert_eq!(pos.cost_basis, dec!(7000));
    }

    #[tokio::test]
    async fn sell_closes_position_when_fully_sold() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "SL002").await;
        let _pos_id = insert_position(&pool, port_id, "AAPL", dec!(50), dec!(8000)).await;

        let mut req = sell_request(port_id, "SL002");
        req.quantity = dec!(50);

        let result = svc.process(&req).await.unwrap();

        let pos = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(result.position_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(pos.quantity, dec!(0));
        assert_eq!(pos.status, "C");
    }

    #[tokio::test]
    async fn sell_rejects_insufficient_units() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "SL003").await;
        let _pos_id = insert_position(&pool, port_id, "AAPL", dec!(10), dec!(1500)).await;

        let req = sell_request(port_id, "SL003");
        let err = svc.process(&req).await.unwrap_err();
        assert!(
            matches!(err, TransactionError::InsufficientUnits { available, requested }
                if available == dec!(10) && requested == dec!(50))
        );
    }

    #[tokio::test]
    async fn sell_rejects_missing_position() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "SL004").await;

        let req = sell_request(port_id, "SL004");
        let err = svc.process(&req).await.unwrap_err();
        assert!(matches!(err, TransactionError::PositionNotFound { .. }));
    }

    #[tokio::test]
    async fn fee_subtracts_from_cost_basis_only() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "FE001").await;
        let _pos_id = insert_position(&pool, port_id, "AAPL", dec!(100), dec!(15000)).await;

        let req = fee_request(port_id, "FE001");
        let result = svc.process(&req).await.unwrap();

        assert_eq!(result.quantity_applied, Decimal::ZERO);
        assert_eq!(result.cost_basis_applied, dec!(-25.00));

        let pos = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(result.position_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(pos.quantity, dec!(100));
        assert_eq!(pos.cost_basis, dec!(14975.00));
    }

    #[tokio::test]
    async fn transfer_moves_units_between_portfolios() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let src_id = insert_portfolio(&pool, "TR001S").await;
        let dst_id = insert_portfolio(&pool, "TR001D").await;
        let src_pos_id = insert_position(&pool, src_id, "AAPL", dec!(100), dec!(15000)).await;

        let req = transfer_request(src_id, "TR001S", dst_id, "TR001D");
        let result = svc.process(&req).await.unwrap();

        assert_eq!(result.quantity_applied, dec!(-30));

        // Source position should have 70 units.
        let src_pos = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(src_pos_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(src_pos.quantity, dec!(70));
        // Cost basis: 15000 - (15000/100 * 30) = 15000 - 4500 = 10500
        assert_eq!(src_pos.cost_basis, dec!(10500));

        // Destination position should have 30 units with proportional cost basis.
        let dst_pos = sqlx::query_as::<_, PositionRow>(
            r#"
            SELECT * FROM positions
            WHERE portfolio_id = $1
              AND investment_id = 'AAPL'
              AND status <> 'C'
            "#,
        )
        .bind(dst_id)
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!(dst_pos.quantity, dec!(30));
        assert_eq!(dst_pos.cost_basis, dec!(4500));
    }

    #[tokio::test]
    async fn transfer_rejects_missing_destination() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let src_id = insert_portfolio(&pool, "TR002S").await;
        let _pos_id = insert_position(&pool, src_id, "AAPL", dec!(100), dec!(15000)).await;

        let mut req = transfer_request(src_id, "TR002S", Uuid::new_v4(), "NONE");
        req.destination_portfolio_id = Some(Uuid::new_v4());

        let err = svc.process(&req).await.unwrap_err();
        assert!(matches!(
            err,
            TransactionError::DestinationPortfolioNotFound(_)
        ));
    }

    #[tokio::test]
    async fn transfer_rejects_no_destination_specified() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let src_id = insert_portfolio(&pool, "TR003S").await;
        let _pos_id = insert_position(&pool, src_id, "AAPL", dec!(100), dec!(15000)).await;

        let mut req = transfer_request(src_id, "TR003S", Uuid::new_v4(), "NONE");
        req.destination_portfolio_id = None;

        let err = svc.process(&req).await.unwrap_err();
        assert!(matches!(
            err,
            TransactionError::DestinationPortfolioNotFound(_)
        ));
    }

    #[tokio::test]
    async fn transfer_insufficient_units() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let src_id = insert_portfolio(&pool, "TR004S").await;
        let dst_id = insert_portfolio(&pool, "TR004D").await;
        let _pos_id = insert_position(&pool, src_id, "AAPL", dec!(10), dec!(1500)).await;

        let req = transfer_request(src_id, "TR004S", dst_id, "TR004D");
        let err = svc.process(&req).await.unwrap_err();
        assert!(matches!(err, TransactionError::InsufficientUnits { .. }));
    }

    #[tokio::test]
    async fn multiple_buys_accumulate() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "MB001").await;

        let req = buy_request(port_id, "MB001");
        svc.process(&req).await.unwrap();
        let result2 = svc.process(&req).await.unwrap();

        let pos = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(result2.position_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(pos.quantity, dec!(200));
        assert_eq!(pos.cost_basis, dec!(30000.00));
    }

    #[tokio::test]
    async fn audit_trail_records_correct_action() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "AU001").await;
        let _pos_id = insert_position(&pool, port_id, "AAPL", dec!(100), dec!(15000)).await;

        // Buy → CREATE
        let buy = buy_request(port_id, "AU001");
        let buy_result = svc.process(&buy).await.unwrap();
        let buy_action: String = sqlx::query_scalar("SELECT action FROM audit_trail WHERE id = $1")
            .bind(buy_result.audit_trail_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(buy_action, "CREATE");

        // Sell → DELETE
        let sell = sell_request(port_id, "AU001");
        let sell_result = svc.process(&sell).await.unwrap();
        let sell_action: String =
            sqlx::query_scalar("SELECT action FROM audit_trail WHERE id = $1")
                .bind(sell_result.audit_trail_id)
                .fetch_one(&pool)
                .await
                .unwrap();
        assert_eq!(sell_action, "DELETE");

        // Fee → UPDATE
        let fee = fee_request(port_id, "AU001");
        let fee_result = svc.process(&fee).await.unwrap();
        let fee_action: String = sqlx::query_scalar("SELECT action FROM audit_trail WHERE id = $1")
            .bind(fee_result.audit_trail_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(fee_action, "UPDATE");
    }

    #[tokio::test]
    async fn buy_after_full_sell_same_day_reactivates_position() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let port_id = insert_portfolio(&pool, "RS001").await;

        // Buy 100 units.
        let buy = buy_request(port_id, "RS001");
        let _buy_result = svc.process(&buy).await.unwrap();

        // Sell all 100 units — position closes (status='C').
        let mut sell = sell_request(port_id, "RS001");
        sell.quantity = dec!(100);
        sell.amount = dec!(15000.00);
        let sell_result = svc.process(&sell).await.unwrap();

        let closed = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(sell_result.position_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(closed.status, "C");

        // Buy again on the same day — should NOT fail with unique constraint
        // violation. The closed position should be reactivated.
        let buy2 = buy_request(port_id, "RS001");
        let result = svc.process(&buy2).await.unwrap();

        let pos = sqlx::query_as::<_, PositionRow>("SELECT * FROM positions WHERE id = $1")
            .bind(result.position_id)
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(pos.status, "A");
        assert_eq!(pos.quantity, dec!(100));
        assert_eq!(pos.cost_basis, dec!(15000.00));
    }

    #[tokio::test]
    async fn transfer_after_close_same_day_reactivates_dest_position() {
        let (pool, _container) = setup_pool().await;
        let svc = TransactionService::new(pool.clone());

        let src_id = insert_portfolio(&pool, "RD01S").await;
        let dst_id = insert_portfolio(&pool, "RD01D").await;

        // Create position in destination, then close it via full sell.
        let mut buy_dst = buy_request(dst_id, "RD01D");
        buy_dst.quantity = dec!(10);
        buy_dst.amount = dec!(1500);
        svc.process(&buy_dst).await.unwrap();

        let mut sell_dst = sell_request(dst_id, "RD01D");
        sell_dst.quantity = dec!(10);
        sell_dst.amount = dec!(1500);
        svc.process(&sell_dst).await.unwrap();

        // Create source position.
        let _src_pos = insert_position(&pool, src_id, "AAPL", dec!(100), dec!(15000)).await;

        // Transfer to destination — should reactivate the closed position.
        let req = transfer_request(src_id, "RD01S", dst_id, "RD01D");
        let result = svc.process(&req).await.unwrap();
        assert!(result.transaction_id != Uuid::nil());

        // Verify destination position is active with transferred units.
        let dst_pos = sqlx::query_as::<_, PositionRow>(
            r#"
            SELECT * FROM positions
            WHERE portfolio_id = $1
              AND investment_id = 'AAPL'
              AND status = 'A'
            "#,
        )
        .bind(dst_id)
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!(dst_pos.quantity, dec!(30));
    }
}
