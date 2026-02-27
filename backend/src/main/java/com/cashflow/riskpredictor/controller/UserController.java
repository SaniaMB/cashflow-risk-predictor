package com.cashflow.riskpredictor.controller;

import com.cashflow.riskpredictor.service.FeatureEngineeringService;
import com.cashflow.riskpredictor.service.RiskPredictionService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final FeatureEngineeringService featureEngineeringService;
    private final RiskPredictionService riskPredictionService;

    public UserController(FeatureEngineeringService featureEngineeringService,
                          RiskPredictionService riskPredictionService) {
        this.featureEngineeringService = featureEngineeringService;
        this.riskPredictionService = riskPredictionService;
    }

    @PostMapping("/{id}/compute-summary/{month}")
    public Object computeSummary(
            @PathVariable Long id,
            @PathVariable String month
    ) {
        LocalDate parsedMonth = LocalDate.parse(month);
        return featureEngineeringService.computeMonthlySummary(id, parsedMonth);
    }

    @PostMapping("/{id}/recompute-risk/{month}")
    public Object recomputeRisk(
            @PathVariable Long id,
            @PathVariable String month
    ) {
        LocalDate parsedMonth = LocalDate.parse(month);
        return riskPredictionService.recomputeRisk(id, parsedMonth);
    }
}