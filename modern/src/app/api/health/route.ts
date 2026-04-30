// API Route: GET /api/health
// Database health check endpoint - migrated from DB2STAT.cbl / DB2CONN.cbl
//
// DB2CONN 3000-CHECK-STATUS ran:
//   SELECT CURRENT SERVER INTO :WS-DB-NAME FROM SYSIBM.SYSDUMMY1
// to verify the DB2 connection was active.
//
// DB2STAT 4000-DISPLAY-STATS retrieved DB statistics from SESSION.DBSTATS.
//
// This endpoint combines both: connectivity check + basic stats.

import { NextResponse } from "next/server";
import { checkDatabaseHealth } from "@/lib/db";
import type { HealthCheckResponse } from "@/types";

const APP_VERSION = "1.0.0";

export async function GET(): Promise<NextResponse> {
  const dbHealth = await checkDatabaseHealth();

  const status: HealthCheckResponse["status"] = dbHealth.connected
    ? dbHealth.responseTimeMs > 1000
      ? "degraded"
      : "healthy"
    : "unhealthy";

  const response: HealthCheckResponse = {
    status,
    database: dbHealth,
    timestamp: new Date().toISOString(),
    version: APP_VERSION,
  };

  const httpStatus = status === "unhealthy" ? 503 : 200;

  return NextResponse.json(response, { status: httpStatus });
}
