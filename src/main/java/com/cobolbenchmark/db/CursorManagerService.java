package com.cobolbenchmark.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Cursor Manager Service - migrated from CURSMGR.cbl.
 * Replaces EXEC SQL DECLARE CURSOR / OPEN / FETCH / CLOSE with JdbcTemplate.
 * Operations: CURS-DECLARE, CURS-OPEN, CURS-FETCH, CURS-CLOSE.
 * Array fetch up to 20 rows from CURSMGR.cbl.
 */
@Service
public class CursorManagerService {

    private static final Logger logger = LoggerFactory.getLogger(CursorManagerService.class);
    private static final int DEFAULT_FETCH_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;

    public CursorManagerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Execute a query and return results - replaces DECLARE/OPEN/FETCH/CLOSE cycle.
     * Spring JDBC handles cursor lifecycle automatically.
     */
    public <T> List<T> executeQuery(String sql, RowMapper<T> rowMapper, Object... params) {
        logger.debug("Executing cursor query: {}", sql);
        jdbcTemplate.setFetchSize(DEFAULT_FETCH_SIZE);
        return jdbcTemplate.query(sql, rowMapper, params);
    }

    /**
     * Execute a query with custom fetch size.
     */
    public <T> List<T> executeQuery(String sql, int fetchSize, RowMapper<T> rowMapper, Object... params) {
        logger.debug("Executing cursor query with fetch size {}: {}", fetchSize, sql);
        jdbcTemplate.setFetchSize(fetchSize);
        return jdbcTemplate.query(sql, rowMapper, params);
    }

    /**
     * Execute a parameterized query for position history.
     * Replaces INQHIST cursor operations.
     */
    public <T> List<T> queryPositionHistory(String portfolioId, RowMapper<T> rowMapper) {
        String sql = "SELECT * FROM POSHIST WHERE PORTFOLIO_ID = ? ORDER BY TRANS_DATE DESC, TRANS_TIME DESC";
        return executeQuery(sql, rowMapper, portfolioId);
    }

    /**
     * Execute a parameterized query for positions.
     * Replaces INQPORT cursor operations.
     */
    public <T> List<T> queryPositions(String portfolioId, RowMapper<T> rowMapper) {
        String sql = "SELECT * FROM INVESTMENT_POSITIONS WHERE PORTFOLIO_ID = ? ORDER BY POSITION_DATE DESC";
        return executeQuery(sql, rowMapper, portfolioId);
    }
}
