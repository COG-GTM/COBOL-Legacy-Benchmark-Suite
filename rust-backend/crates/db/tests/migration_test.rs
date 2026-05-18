//! Integration tests for 001_initial_schema.sql
//!
//! Spins up a PostgreSQL container via testcontainers, applies the migration,
//! then validates table structure, constraint enforcement, and sample inserts.

use sqlx::{postgres::PgPoolOptions, Executor, PgPool, Row};
use testcontainers::runners::AsyncRunner;
use testcontainers_modules::postgres::Postgres;

const MIGRATION_SQL: &str = include_str!("../../../migrations/001_initial_schema.sql");

/// All tables that should exist after running the migration.
const EXPECTED_TABLES: &[&str] = &[
    "portfolios",
    "positions",
    "transactions",
    "transaction_history",
    "batch_control",
    "error_log",
    "audit_trail",
    "return_codes",
];

async fn setup_pool() -> (PgPool, testcontainers::ContainerAsync<Postgres>) {
    let container = Postgres::default().start().await.unwrap();
    let host_port = container.get_host_port_ipv4(5432).await.unwrap();
    let connection_string = format!("postgres://postgres:postgres@127.0.0.1:{host_port}/postgres");

    let pool = PgPoolOptions::new()
        .max_connections(5)
        .connect(&connection_string)
        .await
        .unwrap();

    // Apply migration
    pool.execute(MIGRATION_SQL).await.unwrap();

    (pool, container)
}

// ── Table existence ─────────────────────────────────────────────────────

#[tokio::test]
async fn tables_exist_with_correct_columns() {
    let (pool, _container) = setup_pool().await;

    for table in EXPECTED_TABLES {
        let row = sqlx::query(
            "SELECT COUNT(*) as cnt FROM information_schema.tables \
             WHERE table_schema = 'public' AND table_name = $1",
        )
        .bind(table)
        .fetch_one(&pool)
        .await
        .unwrap();
        let cnt: i64 = row.get("cnt");
        assert_eq!(cnt, 1, "table '{table}' should exist");
    }

    // Spot-check a few key columns
    let cols = get_columns(&pool, "portfolios").await;
    assert!(cols.contains(&"portfolio_id".to_string()));
    assert!(cols.contains(&"client_id".to_string()));
    assert!(cols.contains(&"status".to_string()));
    assert!(cols.contains(&"total_value".to_string()));
    assert!(cols.contains(&"cash_balance".to_string()));

    let cols = get_columns(&pool, "positions").await;
    assert!(cols.contains(&"investment_id".to_string()));
    assert!(cols.contains(&"quantity".to_string()));
    assert!(cols.contains(&"market_value".to_string()));

    let cols = get_columns(&pool, "transactions").await;
    assert!(cols.contains(&"transaction_id".to_string()));
    assert!(cols.contains(&"transaction_type".to_string()));
    assert!(cols.contains(&"price".to_string()));

    let cols = get_columns(&pool, "transaction_history").await;
    assert!(cols.contains(&"security_id".to_string()));
    assert!(cols.contains(&"fees".to_string()));
    assert!(cols.contains(&"gain_loss".to_string()));

    let cols = get_columns(&pool, "batch_control").await;
    assert!(cols.contains(&"job_id".to_string()));
    assert!(cols.contains(&"records_processed".to_string()));
    assert!(cols.contains(&"checkpoint_data".to_string()));

    let cols = get_columns(&pool, "error_log").await;
    assert!(cols.contains(&"error_code".to_string()));
    assert!(cols.contains(&"error_severity".to_string()));

    let cols = get_columns(&pool, "audit_trail").await;
    assert!(cols.contains(&"old_value".to_string()));
    assert!(cols.contains(&"new_value".to_string()));
    assert!(cols.contains(&"entity_type".to_string()));

    let cols = get_columns(&pool, "return_codes").await;
    assert!(cols.contains(&"return_code".to_string()));
    assert!(cols.contains(&"highest_code".to_string()));
    assert!(cols.contains(&"status_code".to_string()));
}

// ── Sample insert + FK ──────────────────────────────────────────────────

#[tokio::test]
async fn insert_sample_data_and_fk_enforcement() {
    let (pool, _container) = setup_pool().await;

    // Insert a portfolio
    let port_id: uuid::Uuid = sqlx::query_scalar(
        "INSERT INTO portfolios \
             (portfolio_id, account_number, account_type, branch_id, \
              client_id, portfolio_name, currency_code, risk_level, \
              status, last_maint_user) \
         VALUES ('PORT0001', 'ACCT000001', 'IN', '01', \
                 'CLIENT0001', 'Test Portfolio', 'USD', 'M', \
                 'A', 'SYSTEM') \
         RETURNING id",
    )
    .fetch_one(&pool)
    .await
    .unwrap();

    // Insert a position referencing that portfolio
    sqlx::query(
        "INSERT INTO positions \
             (portfolio_id, investment_id, position_date, quantity, \
              cost_basis, market_value, currency_code, status, last_maint_user) \
         VALUES ($1, 'INV0000001', CURRENT_DATE, 100.0000, \
                 5000.00, 5200.00, 'USD', 'A', 'SYSTEM')",
    )
    .bind(port_id)
    .execute(&pool)
    .await
    .unwrap();

    // Insert a transaction referencing that portfolio
    sqlx::query(
        "INSERT INTO transactions \
             (transaction_id, portfolio_id, transaction_date, transaction_time, \
              investment_id, transaction_type, quantity, price, amount, \
              currency_code, status, process_user) \
         VALUES ('20240101120000000001', $1, '2024-01-01', '12:00:00', \
                 'INV0000001', 'BU', 100.0000, 52.0000, 5200.00, \
                 'USD', 'D', 'SYSTEM')",
    )
    .bind(port_id)
    .execute(&pool)
    .await
    .unwrap();

    // FK violation — insert a position with a random UUID
    let bad_id = uuid::Uuid::new_v4();
    let result = sqlx::query(
        "INSERT INTO positions \
             (portfolio_id, investment_id, position_date, quantity, \
              cost_basis, market_value, currency_code, status, last_maint_user) \
         VALUES ($1, 'INV9999999', CURRENT_DATE, 1.0, 1.0, 1.0, 'USD', 'A', 'SYSTEM')",
    )
    .bind(bad_id)
    .execute(&pool)
    .await;
    assert!(result.is_err(), "FK violation should be rejected");
}

// ── CHECK constraints ───────────────────────────────────────────────────

#[tokio::test]
async fn check_constraints_reject_invalid_values() {
    let (pool, _container) = setup_pool().await;

    // Invalid portfolio status (must be A/C/S)
    let result = sqlx::query(
        "INSERT INTO portfolios \
             (portfolio_id, account_number, account_type, branch_id, \
              client_id, portfolio_name, currency_code, risk_level, \
              status, last_maint_user) \
         VALUES ('PORT9999', 'ACCT999999', 'IN', '01', \
                 'CLIENT9999', 'Bad Portfolio', 'USD', 'H', \
                 'X', 'SYSTEM')",
    )
    .execute(&pool)
    .await;
    assert!(
        result.is_err(),
        "status 'X' should violate chk_portfolios_status"
    );

    // Invalid transaction type (must be BU/SL/TR/FE)
    // First, insert a valid portfolio
    let port_id: uuid::Uuid = sqlx::query_scalar(
        "INSERT INTO portfolios \
             (portfolio_id, account_number, account_type, branch_id, \
              client_id, portfolio_name, currency_code, risk_level, \
              status, last_maint_user) \
         VALUES ('PORT0002', 'ACCT000002', 'IN', '01', \
                 'CLIENT0002', 'Helper Portfolio', 'USD', 'L', \
                 'A', 'SYSTEM') \
         RETURNING id",
    )
    .fetch_one(&pool)
    .await
    .unwrap();

    let result = sqlx::query(
        "INSERT INTO transactions \
             (transaction_id, portfolio_id, transaction_date, transaction_time, \
              investment_id, transaction_type, quantity, price, amount, \
              currency_code, status, process_user) \
         VALUES ('20240101120000000099', $1, '2024-01-01', '12:00:00', \
                 'INV0000001', 'ZZ', 1.0, 1.0, 1.0, \
                 'USD', 'P', 'SYSTEM')",
    )
    .bind(port_id)
    .execute(&pool)
    .await;
    assert!(
        result.is_err(),
        "transaction_type 'ZZ' should violate chk_transactions_type"
    );

    // Invalid error_log severity (must be 1..4)
    let result = sqlx::query(
        "INSERT INTO error_log \
             (program_id, error_type, error_severity, error_code, \
              error_message, user_id) \
         VALUES ('TESTPROG', 'S', 9, 'ERR00001', 'test', 'SYSTEM')",
    )
    .execute(&pool)
    .await;
    assert!(
        result.is_err(),
        "error_severity 9 should violate chk_error_log_severity"
    );

    // Invalid audit action (must be CREATE/UPDATE/DELETE/INQUIRE/LOGIN/LOGOUT/STARTUP/SHUTDOWN)
    let result = sqlx::query(
        "INSERT INTO audit_trail \
             (user_id, action, entity_type, entity_id) \
         VALUES ('SYSTEM', 'INVALID', 'portfolio', 'P1')",
    )
    .execute(&pool)
    .await;
    assert!(
        result.is_err(),
        "action 'INVALID' should violate chk_audit_action"
    );

    // Invalid return_codes status (must be S/W/E/F)
    let result = sqlx::query(
        "INSERT INTO return_codes \
             (program_id, return_code, highest_code, status_code) \
         VALUES ('TESTPROG', 0, 0, 'Z')",
    )
    .execute(&pool)
    .await;
    assert!(
        result.is_err(),
        "status_code 'Z' should violate chk_return_codes_status"
    );
}

// ── Remaining tables insert smoke test ──────────────────────────────────

#[tokio::test]
async fn insert_into_all_remaining_tables() {
    let (pool, _container) = setup_pool().await;

    // transaction_history
    sqlx::query(
        "INSERT INTO transaction_history \
             (account_number, portfolio_id, trans_date, trans_time, \
              trans_type, security_id, quantity, price, amount, \
              fees, total_amount, cost_basis, gain_loss, \
              process_date, process_time, program_id, user_id) \
         VALUES ('ACCT0001', 'PORT000001', '2024-01-15', '10:30:00', \
                 'BU', 'SEC000000001', 100.000, 52.500, 5250.00, \
                 10.00, 5260.00, 5250.00, 0.00, \
                 '2024-01-15', '10:31:00', 'HISTLD00', 'BATCH')",
    )
    .execute(&pool)
    .await
    .unwrap();

    // batch_control
    sqlx::query(
        "INSERT INTO batch_control \
             (job_id, status, step_name, program_name, \
              process_date, sequence_number, records_processed) \
         VALUES ('TRNVAL00', 'D', 'STEP010', 'TRNVAL00', \
                 '2024-01-15', 1, 15000)",
    )
    .execute(&pool)
    .await
    .unwrap();

    // error_log
    sqlx::query(
        "INSERT INTO error_log \
             (program_id, error_type, error_severity, error_code, \
              error_message, user_id, additional_info) \
         VALUES ('POSUPD00', 'A', 2, 'WARN0001', \
                 'Position recalculation required', 'BATCH', \
                 'Portfolio PORT0001 needs revaluation')",
    )
    .execute(&pool)
    .await
    .unwrap();

    // audit_trail with JSONB
    sqlx::query(
        "INSERT INTO audit_trail \
             (user_id, action, entity_type, entity_id, \
              old_value, new_value, audit_type, audit_status) \
         VALUES ('ANALYST1', 'UPDATE', 'portfolio', 'PORT0001', \
                 '{\"status\": \"S\"}'::jsonb, '{\"status\": \"A\"}'::jsonb, \
                 'TRAN', 'SUCC')",
    )
    .execute(&pool)
    .await
    .unwrap();

    // return_codes
    sqlx::query(
        "INSERT INTO return_codes \
             (program_id, return_code, highest_code, status_code, message_text) \
         VALUES ('TRNVAL00', 0, 4, 'W', 'Completed with warnings')",
    )
    .execute(&pool)
    .await
    .unwrap();
}

// ── Helpers ─────────────────────────────────────────────────────────────

async fn get_columns(pool: &PgPool, table: &str) -> Vec<String> {
    sqlx::query_scalar(
        "SELECT column_name::text FROM information_schema.columns \
         WHERE table_schema = 'public' AND table_name = $1 \
         ORDER BY ordinal_position",
    )
    .bind(table)
    .fetch_all(pool)
    .await
    .unwrap()
}
