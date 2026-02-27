package com.cashflow.riskpredictor.dto;

import lombok.Data;
import java.util.List;

@Data
public class RiskResponseDTO {

    private Double risk_probability;
    private String risk_category;
    private Double confidence;
    private List<String> top_factors;
}