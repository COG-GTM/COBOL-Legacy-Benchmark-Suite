/**
 * Migration: Create portfolio_master table
 * Source: src/database/db2/db2-definitions.sql (lines 10-24)
 */
exports.up = async function (knex) {
  const exists = await knex.schema.hasTable('portfolio_master');
  if (exists) return;

  await knex.schema.createTable('portfolio_master', (table) => {
    table.string('portfolio_id', 8).notNullable().primary();
    table.string('account_type', 2).notNullable();
    table.string('branch_id', 2).notNullable();
    table.string('client_id', 10).notNullable();
    table.string('portfolio_name', 50).notNullable();
    table.string('currency_code', 3).notNullable();
    table.string('risk_level', 1).notNullable();
    table.string('status', 1).notNullable();
    table.date('open_date').notNullable();
    table.date('close_date').nullable();
    table.timestamp('last_maint_date', { useTz: false }).notNullable();
    table.string('last_maint_user', 8).notNullable();
  });
};

exports.down = async function (knex) {
  await knex.schema.dropTableIfExists('portfolio_master');
};
