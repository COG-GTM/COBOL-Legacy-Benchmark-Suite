/**
 * Migration: Create indexes and views
 * Source: src/database/db2/db2-definitions.sql (lines 67-92)
 */
exports.up = async function (knex) {
  // Indexes
  await knex.raw(`
    CREATE INDEX IF NOT EXISTS idx_port_master_client
      ON portfolio_master (client_id, status)
  `);

  await knex.raw(`
    CREATE INDEX IF NOT EXISTS idx_positions_date
      ON investment_positions (position_date, portfolio_id)
  `);

  await knex.raw(`
    CREATE INDEX IF NOT EXISTS idx_trans_hist_port
      ON transaction_history (portfolio_id, transaction_date)
  `);

  await knex.raw(`
    CREATE INDEX IF NOT EXISTS idx_trans_hist_date
      ON transaction_history (transaction_date, portfolio_id)
  `);

  // Views
  await knex.raw(`
    CREATE OR REPLACE VIEW active_portfolios AS
      SELECT *
      FROM portfolio_master
      WHERE status = 'A'
        AND (close_date IS NULL OR close_date > CURRENT_DATE)
  `);

  await knex.raw(`
    CREATE OR REPLACE VIEW current_positions AS
      SELECT p.*, pm.portfolio_name, pm.client_id
      FROM investment_positions p
      JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
      WHERE p.position_date = CURRENT_DATE - INTERVAL '1 day'
  `);
};

exports.down = async function (knex) {
  await knex.raw('DROP VIEW IF EXISTS current_positions');
  await knex.raw('DROP VIEW IF EXISTS active_portfolios');
  await knex.raw('DROP INDEX IF EXISTS idx_trans_hist_date');
  await knex.raw('DROP INDEX IF EXISTS idx_trans_hist_port');
  await knex.raw('DROP INDEX IF EXISTS idx_positions_date');
  await knex.raw('DROP INDEX IF EXISTS idx_port_master_client');
};
