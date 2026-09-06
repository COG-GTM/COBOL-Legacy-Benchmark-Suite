package com.cog.portfolio.dto;

/**
 * DB2REQ.cpy DB2-REQUEST-AREA (DB2-STATUS request). Connect/disconnect requests
 * are handled by HikariCP; only the status query survives as a DTO.
 */
public record DatabaseStatusResponse(
        String poolName,
        int activeConnections,
        int idleConnections,
        int totalConnections,
        int maxPoolSize,
        int responseCode,
        String errorMessage) {
}
