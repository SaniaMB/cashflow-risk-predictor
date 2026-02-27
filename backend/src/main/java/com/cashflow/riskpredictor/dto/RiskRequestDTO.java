package com.cashflow.riskpredictor.dto;
import lombok.Data;

@Data
public class RiskRequestDTO {
    private Double savings_ratio;
    private Double emi_ratio;
    private Double expense_volatility;
    private Double spending_trend_slope;
    private Double income_irregularity_score;
}
