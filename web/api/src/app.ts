import cors from "cors";
import express, { NextFunction, Request, Response } from "express";
import { createRepository } from "./repository";
import { createPortfolioRouter } from "./routes/portfolios";

/**
 * Builds the Express application. The data layer is chosen via DATA_SOURCE
 * (see repository/index.ts). Exported separately from the server so it can be
 * imported in tests.
 */
export function createApp() {
  const app = express();
  const repo = createRepository();

  app.use(cors());
  app.use(express.json());

  app.get("/api/health", (_req, res) => {
    res.json({ status: "ok", dataSource: process.env.DATA_SOURCE ?? "memory" });
  });

  app.use("/api/portfolios", createPortfolioRouter(repo));

  // 404 for unknown routes
  app.use((_req, res) => {
    res.status(404).json({ error: "NOT_FOUND", message: "Resource not found" });
  });

  // Centralized error handler -> mirrors the COBOL P999-ERROR-ROUTINE path
  app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
    // eslint-disable-next-line no-console
    console.error(err);
    res.status(500).json({
      error: "INTERNAL_ERROR",
      message: err.message || "Error accessing portfolio data",
    });
  });

  return app;
}
