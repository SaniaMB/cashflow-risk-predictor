package com.cashflow.riskpredictor.dto;

import com.cashflow.riskpredictor.enums.RiskCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class UserOverviewDTO {

    private String fullName;
    private BigDecimal monthlyIncome;
    private Integer financialStressScore;
    private RiskCategory riskCategory;
    private Double riskProbability;
    private BigDecimal modelConfidence;
}