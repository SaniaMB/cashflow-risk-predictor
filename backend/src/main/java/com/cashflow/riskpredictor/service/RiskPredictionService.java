package com.cashflow.riskpredictor.service;

import com.cashflow.riskpredictor.dto.RiskRequestDTO;
import com.cashflow.riskpredictor.dto.RiskResponseDTO;
import com.cashflow.riskpredictor.entity.MonthlyFinancialSummary;
import com.cashflow.riskpredictor.entity.RiskScore;
import com.cashflow.riskpredictor.entity.User;
import com.cashflow.riskpredictor.enums.RiskCategory;
import com.cashflow.riskpredictor.repository.RiskScoreRepository;
import com.cashflow.riskpredictor.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RiskPredictionService {

    private final FeatureEngineeringService featureService;
    private final RiskScoreRepository riskRepository;
    private final UserRepository userRepository;
    private final WebClient webClient;

    public RiskPredictionService(FeatureEngineeringService featureService,
                                 RiskScoreRepository riskRepository,
                                 UserRepository userRepository,
                                 WebClient webClient) {
        this.featureService = featureService;
        this.riskRepository = riskRepository;
        this.userRepository = userRepository;
        this.webClient = webClient;
    }

    public RiskScore recomputeRisk(Long userId, LocalDate month) {

        // 1️⃣ Compute features
        MonthlyFinancialSummary summary =
                featureService.computeMonthlySummary(userId, month);

        // 2️⃣ Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3️⃣ Build ML request (NEW STRUCTURE)
        RiskRequestDTO request = new RiskRequestDTO();
        request.setUserId(userId);
        request.setMonth(month.toString());

        RiskRequestDTO.FinancialFeatures features =
                new RiskRequestDTO.FinancialFeatures();

        features.setSavingsRatio(summary.getSavingsRatio().doubleValue());
        features.setEmiRatio(summary.getEmiRatio().doubleValue());
        features.setExpenseVolatility(summary.getExpenseVolatility().doubleValue());
        features.setSpendingTrendSlope(summary.getSpendingTrendSlope().doubleValue());
        features.setIncomeIrregularityScore(summary.getIncomeIrregularityScore().doubleValue());

        // compute expense_to_income_ratio dynamically
        double expenseToIncomeRatio = 0.0;

        if (summary.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            expenseToIncomeRatio =
                    summary.getTotalExpense()
                            .divide(summary.getTotalIncome(), 4, java.math.RoundingMode.HALF_UP)
                            .doubleValue();
        }

        features.setExpenseToIncomeRatio(expenseToIncomeRatio);

        request.setFeatures(features);

        // 4️⃣ Call ML
        RiskResponseDTO response = webClient.post()
                .uri("http://localhost:8000/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RiskResponseDTO.class)
                .block();

        if (response == null) {
            throw new RuntimeException("ML service did not respond");
        }

        // 5️⃣ Get or create RiskScore
        RiskScore risk = riskRepository
                .findByUserIdAndMonth(userId, month)
                .orElse(new RiskScore());

        risk.setUser(user);
        risk.setMonth(month);

        // 6️⃣ Map ML response
        risk.setRiskProbability(
                BigDecimal.valueOf(response.getRiskProbability())
        );

        risk.setFinancialStressScore(
                (int) (response.getRiskProbability() * 100)
        );

        risk.setRiskCategory(
                RiskCategory.valueOf(response.getRiskCategory())
        );

        risk.setModelConfidence(
                BigDecimal.valueOf(response.getConfidence())
        );

        List<String> factors = response.getTopFactors();
        if (factors != null) {
            if (factors.size() > 0) risk.setTopFactor1(factors.get(0));
            if (factors.size() > 1) risk.setTopFactor2(factors.get(1));
            if (factors.size() > 2) risk.setTopFactor3(factors.get(2));
        }

        return riskRepository.save(risk);
    }
}