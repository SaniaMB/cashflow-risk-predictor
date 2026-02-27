package com.cashflow.riskpredictor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RiskRequestDTO {

    @JsonProperty("user_id")
    private Long userId;

    private String month;

    private FinancialFeatures features;

    @Data
    public static class FinancialFeatures {

        @JsonProperty("savings_ratio")
        private Double savingsRatio;

        @JsonProperty("emi_ratio")
        private Double emiRatio;

        @JsonProperty("expense_volatility")
        private Double expenseVolatility;

        @JsonProperty("spending_trend_slope")
        private Double spendingTrendSlope;

        @JsonProperty("income_irregularity_score")
        private Double incomeIrregularityScore;

        @JsonProperty("expense_to_income_ratio")
        private Double expenseToIncomeRatio;
    }
}