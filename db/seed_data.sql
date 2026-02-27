-- ========================================
-- USERS
-- ========================================
INSERT INTO users (full_name, email, monthly_income) VALUES
('Aman Stable', 'aman@example.com', 60000),
('Riya Moderate', 'riya@example.com', 60000),
('Karan HighRisk', 'karan@example.com', 60000);


-- ========================================
-- MONTHLY FINANCIAL SUMMARY
-- User 1 (Stable - Low Risk Pattern)
-- ========================================
INSERT INTO monthly_financial_summary
(user_id, month, total_income, total_expense, savings_ratio, emi_ratio, expense_volatility, spending_trend_slope, income_irregularity_score)
VALUES
(1,'2025-09-01',60000,40000,0.3333,0.1667,0.05,0.01,0.01),
(1,'2025-10-01',60000,41000,0.3167,0.1667,0.04,0.01,0.01),
(1,'2025-11-01',60000,39500,0.3417,0.1667,0.03,-0.01,0.01),
(1,'2025-12-01',60000,40500,0.3250,0.1667,0.04,0.01,0.01),
(1,'2026-01-01',60000,39800,0.3367,0.1667,0.03,-0.01,0.01),
(1,'2026-02-01',60000,40200,0.3300,0.1667,0.04,0.01,0.01);


-- ========================================
-- User 2 (Moderate Risk Pattern)
-- ========================================
INSERT INTO monthly_financial_summary
(user_id, month, total_income, total_expense, savings_ratio, emi_ratio, expense_volatility, spending_trend_slope, income_irregularity_score)
VALUES
(2,'2025-09-01',60000,45000,0.2500,0.3333,0.10,0.02,0.02),
(2,'2025-10-01',60000,48000,0.2000,0.3333,0.12,0.03,0.02),
(2,'2025-11-01',60000,50000,0.1667,0.3333,0.14,0.04,0.02),
(2,'2025-12-01',60000,52000,0.1333,0.3333,0.16,0.05,0.02),
(2,'2026-01-01',60000,54000,0.1000,0.3333,0.18,0.06,0.02),
(2,'2026-02-01',60000,55000,0.0833,0.3333,0.20,0.07,0.02);


-- ========================================
-- User 3 (High Risk Pattern)
-- ========================================
INSERT INTO monthly_financial_summary
(user_id, month, total_income, total_expense, savings_ratio, emi_ratio, expense_volatility, spending_trend_slope, income_irregularity_score)
VALUES
(3,'2025-09-01',60000,50000,0.1667,0.5000,0.20,0.05,0.03),
(3,'2025-10-01',60000,55000,0.0833,0.5000,0.25,0.06,0.03),
(3,'2025-11-01',60000,60000,0.0000,0.5000,0.30,0.07,0.03),
(3,'2025-12-01',60000,62000,-0.0333,0.5000,0.35,0.08,0.03),
(3,'2026-01-01',60000,65000,-0.0833,0.5000,0.40,0.09,0.03),
(3,'2026-02-01',60000,68000,-0.1333,0.5000,0.45,0.10,0.03);


-- ========================================
-- RISK SCORES (MODEL OUTPUT SAMPLE)
-- ========================================
INSERT INTO risk_scores
(user_id, month, risk_probability, financial_stress_score, risk_category, model_confidence, top_factor_1, top_factor_2, top_factor_3)
VALUES
(1,'2026-02-01',0.18,25,'LOW',0.90,'Stable savings ratio','Low EMI burden','Low volatility'),
(2,'2026-02-01',0.55,55,'MEDIUM',0.84,'Declining savings','Rising expense trend','Moderate EMI burden'),
(3,'2026-02-01',0.87,85,'HIGH',0.82,'High EMI ratio','Negative savings','Rapid expense growth');