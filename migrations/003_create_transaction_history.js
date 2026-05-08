/**
 * Migration: Create transaction_history table
 * Source: src/database/db2/db2-definitions.sql (lines 46-62)
 */
exports.up = async function (knex) {
  const exists = await knex.schema.hasTable('transaction_history');
  if (exists) return;

  await knex.schema.createTable('transaction_history', (table) => {
    table.string('transaction_id', 20).notNullable().primary();
    table.string('portfolio_id', 8).notNullable();
    table.date('transaction_date').notNullable();
    table.time('transaction_time').notNullable();
    table.string('investment_id', 10).notNullable();
    table.string('transaction_type', 2).notNullable();
    table.decimal('quantity', 18, 4).notNullable();
    table.decimal('price', 18, 4).notNullable();
    table.decimal('amount', 18, 2).notNullable();
    table.string('currency_code', 3).notNullable();
    table.string('status', 1).notNullable();
    table.timestamp('process_date', { useTz: false }).notNullable();
    table.string('process_user', 8).notNullable();

    table
      .foreign('portfolio_id')
      .references('portfolio_id')
      .inTable('portfolio_master');
  });
};

exports.down = async function (knex) {
  await knex.schema.dropTableIfExists('transaction_history');
};
