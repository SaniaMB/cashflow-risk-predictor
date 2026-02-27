package com.cashflow.riskpredictor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class RiskResponseDTO {

    @JsonProperty("risk_probability")
    private Double riskProbability;

    @JsonProperty("risk_category")
    private String riskCategory;

    private Double confidence;

    @JsonProperty("top_factors")
    private List<String> topFactors;
}