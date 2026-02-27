package com.cashflow.riskpredictor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class RiskTrendDTO {

    private LocalDate month;
    private Double riskProbability;
    private Integer financialStressScore;
}