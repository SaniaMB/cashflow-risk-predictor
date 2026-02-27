package com.cashflow.riskpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cashflow.riskpredictor.entity.RiskScore;

import java.time.LocalDate;
import java.util.Optional;

public interface RiskScoreRepository
        extends JpaRepository<RiskScore, Long> {

    Optional<RiskScore> findByUserIdAndMonth(
            Long userId,
            LocalDate month
    );
}