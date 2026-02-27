package com.cashflow.riskpredictor.service;

import com.cashflow.riskpredictor.dto.*;
import com.cashflow.riskpredictor.entity.*;
import com.cashflow.riskpredictor.enums.RiskCategory;
import com.cashflow.riskpredictor.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class RiskPredictionService {

    private final FeatureEngineeringService featureService;
    private final RiskScoreRepository riskRepository;
    private final WebClient webClient;

    public RiskPredictionService(FeatureEngineeringService featureService,
                                 RiskScoreRepository riskRepository,
                                 WebClient webClient) {
        this.featureService = featureService;
        this.riskRepository = riskRepository;
        this.webClient = webClient;
    }

    public RiskScore recomputeRisk(Long userId, LocalDate month) {

        MonthlyFinancialSummary summary =
                featureService.computeMonthlySummary(userId, month);

        RiskRequestDTO request = new RiskRequestDTO();
        request.setSavings_ratio(summary.getSavingsRatio().doubleValue());
        request.setEmi_ratio(summary.getEmiRatio().doubleValue());
        request.setExpense_volatility(summary.getExpenseVolatility().doubleValue());
        request.setSpending_trend_slope(summary.getSpendingTrendSlope().doubleValue());
        request.setIncome_irregularity_score(summary.getIncomeIrregularityScore().doubleValue());

        RiskResponseDTO response = webClient.post()
                .uri("http://localhost:8000/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RiskResponseDTO.class)
                .block();

        if (response == null) {
            throw new RuntimeException("ML service did not respond");
        }

        RiskScore risk = riskRepository
                .findByUserIdAndMonth(userId, month)
                .orElse(new RiskScore());

        risk.setMonth(month);
        risk.setRiskProbability(BigDecimal.valueOf(response.getRisk_probability()));
        risk.setFinancialStressScore(
                (int)(response.getRisk_probability() * 100)
        );
        risk.setRiskCategory(
                RiskCategory.valueOf(response.getRisk_category())
        );
        risk.setModelConfidence(
                BigDecimal.valueOf(response.getConfidence())
        );

        if (response.getTop_factors() != null) {
            if (response.getTop_factors().size() > 0)
                risk.setTopFactor1(response.getTop_factors().get(0));
            if (response.getTop_factors().size() > 1)
                risk.setTopFactor2(response.getTop_factors().get(1));
            if (response.getTop_factors().size() > 2)
                risk.setTopFactor3(response.getTop_factors().get(2));
        }

        return riskRepository.save(risk);
    }
}