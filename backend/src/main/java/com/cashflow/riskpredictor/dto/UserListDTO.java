package com.cashflow.riskpredictor.dto;

import com.cashflow.riskpredictor.enums.RiskCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserListDTO {

    private Long id;
    private String fullName;
    private RiskCategory riskCategory;
    private Double riskProbability;
    private Integer financialStressScore;
}