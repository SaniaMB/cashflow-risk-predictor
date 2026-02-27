DROP DATABASE IF EXISTS cashflow_risk_db;
CREATE DATABASE cashflow_risk_db;
USE cashflow_risk_db;

-- =========================
-- USERS TABLE
-- =========================
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    monthly_income DECIMAL(12,2) NOT NULL,
    employment_type ENUM('SALARIED','FREELANCE','BUSINESS') DEFAULT 'SALARIED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- TRANSACTIONS TABLE
-- =========================
CREATE TABLE transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    transaction_type ENUM('INCOME','EXPENSE') NOT NULL,
    category VARCHAR(50) NOT NULL,
    transaction_date DATE NOT NULL,
    is_recurring BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user (user_id),
    INDEX idx_date (transaction_date),

    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =========================
-- MONTHLY FINANCIAL SUMMARY (FEATURE ENGINEERING)
-- =========================
CREATE TABLE monthly_financial_summary (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    month DATE NOT NULL,

    total_income DECIMAL(12,2),
    total_expense DECIMAL(12,2),
    savings_ratio DECIMAL(6,4),
    emi_ratio DECIMAL(6,4),
    expense_volatility DECIMAL(8,4),
    spending_trend_slope DECIMAL(8,4),
    income_irregularity_score DECIMAL(8,4),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY unique_user_month (user_id, month),

    CONSTRAINT fk_summary_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =========================
-- RISK SCORES (MODEL OUTPUT)
-- =========================
CREATE TABLE risk_scores (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    month DATE NOT NULL,

    risk_probability DECIMAL(5,4),
    financial_stress_score INT,
    risk_category ENUM('LOW','MEDIUM','HIGH'),
    model_confidence DECIMAL(5,4),

    top_factor_1 VARCHAR(255),
    top_factor_2 VARCHAR(255),
    top_factor_3 VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY unique_risk_user_month (user_id, month),

    CONSTRAINT fk_risk_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);