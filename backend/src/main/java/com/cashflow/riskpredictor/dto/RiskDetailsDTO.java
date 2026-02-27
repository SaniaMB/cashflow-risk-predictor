package com.cashflow.riskpredictor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class RiskDetailsDTO {

    private List<String> topFactors;
    private BigDecimal modelConfidence;
}