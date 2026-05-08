/**
 * Migration: Create investment_positions table
 * Source: src/database/db2/db2-definitions.sql (lines 29-41)
 */
exports.up = async function (knex) {
  const exists = await knex.schema.hasTable('investment_positions');
  if (exists) return;

  await knex.schema.createTable('investment_positions', (table) => {
    table.string('portfolio_id', 8).notNullable();
    table.string('investment_id', 10).notNullable();
    table.date('position_date').notNullable();
    table.decimal('quantity', 18, 4).notNullable();
    table.decimal('cost_basis', 18, 2).notNullable();
    table.decimal('market_value', 18, 2).notNullable();
    table.string('currency_code', 3).notNullable();
    table.timestamp('last_maint_date', { useTz: false }).notNullable();
    table.string('last_maint_user', 8).notNullable();

    table.primary(['portfolio_id', 'investment_id', 'position_date']);
    table
      .foreign('portfolio_id')
      .references('portfolio_id')
      .inTable('portfolio_master');
  });
};

exports.down = async function (knex) {
  await knex.schema.dropTableIfExists('investment_positions');
};
