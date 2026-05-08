/**
 * Knex configuration for PostgreSQL migrations.
 * Connection is configured via DATABASE_URL or individual PG* env vars.
 */
module.exports = {
  client: 'pg',
  connection: process.env.DATABASE_URL || {
    host: process.env.PGHOST || 'localhost',
    port: parseInt(process.env.PGPORT || '5432', 10),
    database: process.env.PGDATABASE || 'portfolio_db',
    user: process.env.PGUSER || 'postgres',
    password: process.env.PGPASSWORD || 'postgres',
  },
  migrations: {
    directory: './migrations',
    tableName: 'knex_migrations',
  },
};
