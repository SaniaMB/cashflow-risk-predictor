package com.cashflow.riskpredictor.controller;

import com.cashflow.riskpredictor.dto.*;
import com.cashflow.riskpredictor.entity.*;
import com.cashflow.riskpredictor.repository.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class DashboardController {

    private final UserRepository userRepository;
    private final RiskScoreRepository riskRepository;
    private final MonthlyFinancialSummaryRepository summaryRepository;

    public DashboardController(UserRepository userRepository,
                               RiskScoreRepository riskRepository,
                               MonthlyFinancialSummaryRepository summaryRepository) {
        this.userRepository = userRepository;
        this.riskRepository = riskRepository;
        this.summaryRepository = summaryRepository;
    }

    // 1️⃣ List all users with latest risk
    @GetMapping
    public List<UserListDTO> getUsers() {

        return userRepository.findAll().stream()
                .map(user -> {

                    RiskScore latest = riskRepository
                            .findTopByUserIdOrderByMonthDesc(user.getId())
                            .orElse(null);

                    if (latest == null) {
                        return new UserListDTO(
                                user.getId(),
                                user.getFullName(),
                                null,
                                null,
                                null
                        );
                    }

                    return new UserListDTO(
                            user.getId(),
                            user.getFullName(),
                            latest.getRiskCategory(),
                            latest.getRiskProbability().doubleValue(),
                            latest.getFinancialStressScore()
                    );
                })
                .collect(Collectors.toList());
    }

    // 2️⃣ Overview
    @GetMapping("/{id}/overview")
    public UserOverviewDTO getOverview(@PathVariable Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RiskScore latest = riskRepository
                .findTopByUserIdOrderByMonthDesc(id)
                .orElseThrow(() -> new RuntimeException("No risk data"));

        return new UserOverviewDTO(
                user.getFullName(),
                user.getMonthlyIncome(),
                latest.getFinancialStressScore(),
                latest.getRiskCategory(),
                latest.getRiskProbability().doubleValue(),
                latest.getModelConfidence()
        );
    }

    // 3️⃣ Risk Trend
    @GetMapping("/{id}/risk-trend")
    public List<RiskTrendDTO> getRiskTrend(@PathVariable Long id) {

        return riskRepository.findByUserIdOrderByMonthAsc(id)
                .stream()
                .map(r -> new RiskTrendDTO(
                        r.getMonth(),
                        r.getRiskProbability().doubleValue(),
                        r.getFinancialStressScore()
                ))
                .collect(Collectors.toList());
    }

    // 4️⃣ Risk Details
    @GetMapping("/{id}/risk-details")
    public RiskDetailsDTO getRiskDetails(@PathVariable Long id) {

        RiskScore latest = riskRepository
                .findTopByUserIdOrderByMonthDesc(id)
                .orElseThrow(() -> new RuntimeException("No risk data"));

        List<String> factors = List.of(
                latest.getTopFactor1(),
                latest.getTopFactor2(),
                latest.getTopFactor3()
        );

        return new RiskDetailsDTO(
                factors,
                latest.getModelConfidence()
        );
    }
}