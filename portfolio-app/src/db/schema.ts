import {
  pgTable,
  char,
  varchar,
  numeric,
  date,
  timestamp,
  text,
  jsonb,
  bigserial,
  check,
} from "drizzle-orm/pg-core";
import { sql } from "drizzle-orm";

/**
 * portfolios table — replaces DB2 PORTFOLIO_MASTER (db2-definitions.sql lines 10-24)
 * Fields map to PORTFLIO.cpy record layout.
 */
export const portfolios = pgTable(
  "portfolios",
  {
    portfolio_id: char("portfolio_id", { length: 8 }).primaryKey(),
    account_no: char("account_no", { length: 10 }).unique().notNull(),
    client_name: varchar("client_name", { length: 30 }).notNull(),
    client_type: char("client_type", { length: 1 }).notNull(),
    portfolio_name: varchar("portfolio_name", { length: 50 }),
    currency_code: char("currency_code", { length: 3 })
      .default("USD")
      .notNull(),
    risk_level: char("risk_level", { length: 1 }),
    branch_id: char("branch_id", { length: 2 }),
    total_value: numeric("total_value", { precision: 15, scale: 2 }).default(
      "0"
    ),
    cash_balance: numeric("cash_balance", { precision: 15, scale: 2 }).default(
      "0"
    ),
    status: char("status", { length: 1 }).default("A").notNull(),
    open_date: date("open_date").defaultNow(),
    close_date: date("close_date"),
    created_at: timestamp("created_at", { withTimezone: true }).defaultNow(),
    updated_at: timestamp("updated_at", { withTimezone: true }).defaultNow(),
    updated_by: varchar("updated_by", { length: 8 }).notNull(),
  },
  (table) => ({
    clientTypeCheck: check(
      "client_type_check",
      sql`${table.client_type} IN ('I', 'C', 'T')`
    ),
    statusCheck: check(
      "status_check",
      sql`${table.status} IN ('A', 'C', 'S')`
    ),
  })
);

/**
 * audit_log table — from AUDITLOG.cpy
 * Stores all portfolio CRUD audit trail entries.
 */
export const auditLog = pgTable(
  "audit_log",
  {
    id: bigserial("id", { mode: "number" }).primaryKey(),
    event_ts: timestamp("event_ts", { withTimezone: true }).defaultNow(),
    user_id: varchar("user_id", { length: 8 }).notNull(),
    program_id: varchar("program_id", { length: 8 }).notNull(),
    event_type: varchar("event_type", { length: 4 }),
    action: varchar("action", { length: 8 }).notNull(),
    status: varchar("status", { length: 4 }),
    portfolio_id: char("portfolio_id", { length: 8 }),
    account_no: char("account_no", { length: 10 }),
    before_image: jsonb("before_image"),
    after_image: jsonb("after_image"),
    message: text("message"),
  },
  (table) => ({
    eventTypeCheck: check(
      "event_type_check",
      sql`${table.event_type} IN ('TRAN', 'USER', 'SYST')`
    ),
    auditStatusCheck: check(
      "audit_status_check",
      sql`${table.status} IN ('SUCC', 'FAIL', 'WARN')`
    ),
  })
);

export type Portfolio = typeof portfolios.$inferSelect;
export type NewPortfolio = typeof portfolios.$inferInsert;
export type AuditLogEntry = typeof auditLog.$inferSelect;
export type NewAuditLogEntry = typeof auditLog.$inferInsert;
