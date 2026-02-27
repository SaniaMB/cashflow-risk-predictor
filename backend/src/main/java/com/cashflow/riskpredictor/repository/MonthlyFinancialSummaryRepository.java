package com.cashflow.riskpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cashflow.riskpredictor.entity.MonthlyFinancialSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlyFinancialSummaryRepository
        extends JpaRepository<MonthlyFinancialSummary, Long> {

    Optional<MonthlyFinancialSummary> findByUserIdAndMonth(
            Long userId,
            LocalDate month
    );

    List<MonthlyFinancialSummary> findByUserIdAndMonthBetween(
            Long userId,
            LocalDate start,
            LocalDate end
    );

    List<MonthlyFinancialSummary> findByUserIdOrderByMonthAsc(Long userId);
}