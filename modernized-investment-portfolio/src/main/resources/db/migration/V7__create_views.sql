--====================================================================
-- VIEWS (PostgreSQL)
-- Migrated from: src/database/db2/db2-definitions.sql
--====================================================================

-- Active Portfolios view
-- DB2: CURRENT DATE -> PostgreSQL: CURRENT_DATE
CREATE VIEW active_portfolios AS
    SELECT *
    FROM portfolio_master
    WHERE status = 'A'
    AND (close_date IS NULL OR close_date > CURRENT_DATE);

-- Current Positions view (previous business day positions)
-- DB2: CURRENT DATE - 1 DAY -> PostgreSQL: CURRENT_DATE - INTERVAL '1 day'
CREATE VIEW current_positions AS
    SELECT p.*, pm.portfolio_name, pm.client_id
    FROM investment_positions p
    JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
    WHERE p.position_date = CURRENT_DATE - INTERVAL '1 day';
