package com.cashflow.riskpredictor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "monthly_financial_summary",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "month"})
        }
)
public class MonthlyFinancialSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate month;

    @Column(name = "total_income", precision = 12, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expense", precision = 12, scale = 2)
    private BigDecimal totalExpense;

    @Column(name = "savings_ratio", precision = 6, scale = 4)
    private BigDecimal savingsRatio;

    @Column(name = "emi_ratio", precision = 6, scale = 4)
    private BigDecimal emiRatio;

    @Column(name = "expense_volatility", precision = 8, scale = 4)
    private BigDecimal expenseVolatility;

    @Column(name = "spending_trend_slope", precision = 8, scale = 4)
    private BigDecimal spendingTrendSlope;

    @Column(name = "income_irregularity_score", precision = 8, scale = 4)
    private BigDecimal incomeIrregularityScore;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}