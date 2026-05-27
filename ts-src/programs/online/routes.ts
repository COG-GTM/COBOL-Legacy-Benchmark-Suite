/**
 * Express Route Definitions.
 * Migrated from: src/cics/PORTDFN.csd (CICS resource definitions)
 *
 * Maps CICS DEFINE TRANSACTION / PROGRAM / MAPSET to Express routes,
 * middleware, and handler bindings.
 */

import express, { Application } from 'express';
import { Knex } from 'knex';
import { createInquiryRouter } from './inquiry-controller';
import { SecurityManager, authMiddleware } from './security-manager';

/**
 * Register all routes on the Express application.
 *
 * CICS mapping:
 *   DEFINE TRANSACTION(PINQ) PROGRAM(INQONLN) → GET /api/*
 *   DEFINE PROGRAM(INQONLN)   → inquiry-controller.ts
 *   DEFINE PROGRAM(INQPORT)   → inquiry-portfolio.ts
 *   DEFINE PROGRAM(INQHIST)   → inquiry-history.ts
 *   DEFINE PROGRAM(SECMGR)    → security-manager.ts (middleware)
 *   DEFINE FILE(POSFILE)      → INVESTMENT_POSITIONS table
 *   DEFINE MAPSET(INQSET)     → api-schemas.ts
 */
export function registerRoutes(app: Application, db: Knex): void {
  const secMgr = new SecurityManager();

  // Global middleware
  app.use(express.json());

  // Health check (no auth)
  app.get('/health', (_req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  // Protected API routes
  app.use('/api', authMiddleware(secMgr));
  app.use('/api', createInquiryRouter(db));
}
