-- Portfolio Master Table (from PORTFOLIO_MASTER in db2-definitions.sql)
CREATE TABLE portfolio_master (
    portfolio_id   VARCHAR(8) NOT NULL,
    account_type   VARCHAR(2) NOT NULL DEFAULT 'GN',
    branch_id      VARCHAR(2) NOT NULL DEFAULT '01',
    client_id      VARCHAR(10) NOT NULL,
    portfolio_name VARCHAR(50) NOT NULL,
    currency_code  VARCHAR(3) NOT NULL DEFAULT 'USD',
    risk_level     VARCHAR(1) NOT NULL DEFAULT 'M',
    status         CHAR(1) NOT NULL DEFAULT 'A',
    open_date      DATE NOT NULL,
    close_date     DATE,
    last_maint_date TIMESTAMP NOT NULL,
    last_maint_user VARCHAR(8) NOT NULL,
    total_value    DECIMAL(15,2) DEFAULT 0,
    cash_balance   DECIMAL(15,2) DEFAULT 0,
    client_name    VARCHAR(30),
    client_type    CHAR(1),
    PRIMARY KEY (portfolio_id)
);

-- Investment Positions Table (from INVESTMENT_POSITIONS in db2-definitions.sql)
CREATE TABLE investment_positions (
    portfolio_id   VARCHAR(8) NOT NULL,
    investment_id  VARCHAR(10) NOT NULL,
    position_date  DATE NOT NULL,
    quantity       DECIMAL(18,4) NOT NULL DEFAULT 0,
    cost_basis     DECIMAL(18,2) NOT NULL DEFAULT 0,
    market_value   DECIMAL(18,2) NOT NULL DEFAULT 0,
    currency_code  VARCHAR(3) NOT NULL DEFAULT 'USD',
    last_maint_date TIMESTAMP NOT NULL,
    last_maint_user VARCHAR(8) NOT NULL,
    PRIMARY KEY (portfolio_id, investment_id, position_date),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

-- Transaction History Table (from TRANSACTION_HISTORY in db2-definitions.sql)
CREATE TABLE transaction_history (
    transaction_id   VARCHAR(20) NOT NULL,
    portfolio_id     VARCHAR(8) NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_time TIME NOT NULL,
    investment_id    VARCHAR(10) NOT NULL,
    transaction_type VARCHAR(2) NOT NULL,
    quantity         DECIMAL(18,4) NOT NULL,
    price            DECIMAL(18,4) NOT NULL,
    amount           DECIMAL(18,2) NOT NULL,
    currency_code    VARCHAR(3) NOT NULL DEFAULT 'USD',
    status           CHAR(1) NOT NULL DEFAULT 'P',
    process_date     TIMESTAMP NOT NULL,
    process_user     VARCHAR(8) NOT NULL,
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

-- Indexes (from db2-definitions.sql)
CREATE INDEX idx_portfolio_client ON portfolio_master(client_id);
CREATE INDEX idx_position_date ON investment_positions(position_date);
CREATE INDEX idx_trans_portfolio ON transaction_history(portfolio_id);
CREATE INDEX idx_trans_date ON transaction_history(transaction_date);

-- Views (from db2-definitions.sql)
CREATE VIEW active_portfolios AS
    SELECT portfolio_id, client_id, portfolio_name, total_value, cash_balance
    FROM portfolio_master
    WHERE status = 'A';

CREATE VIEW current_positions AS
    SELECT p.portfolio_id, p.investment_id, p.quantity,
           p.cost_basis, p.market_value
    FROM investment_positions p
    INNER JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
    WHERE pm.status = 'A';
