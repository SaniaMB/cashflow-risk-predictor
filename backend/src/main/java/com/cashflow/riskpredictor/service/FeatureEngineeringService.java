package com.cashflow.riskpredictor.service;

import com.cashflow.riskpredictor.entity.*;
import com.cashflow.riskpredictor.enums.TransactionType;
import com.cashflow.riskpredictor.repository.*;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeatureEngineeringService {

    private final TransactionRepository transactionRepository;
    private final MonthlyFinancialSummaryRepository summaryRepository;
    private final UserRepository userRepository;

    public FeatureEngineeringService(TransactionRepository transactionRepository,
                                     MonthlyFinancialSummaryRepository summaryRepository,
                                     UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.summaryRepository = summaryRepository;
        this.userRepository = userRepository;
    }

    public MonthlyFinancialSummary computeMonthlySummary(Long userId, LocalDate month) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = month.withDayOfMonth(month.lengthOfMonth());

        List<Transaction> transactions =
                transactionRepository.findByUserIdAndTransactionDateBetween(
                        userId, startDate, endDate
                );

        // Separate income and expense
        List<Transaction> incomeTx = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .toList();

        List<Transaction> expenseTx = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .toList();

        BigDecimal totalIncome = incomeTx.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = expenseTx.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Savings Ratio
        BigDecimal savingsRatio = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRatio = totalIncome.subtract(totalExpense)
                    .divide(totalIncome, 4, RoundingMode.HALF_UP);
        }

        // EMI Ratio
        BigDecimal totalEmi = expenseTx.stream()
                .filter(t -> "EMI".equalsIgnoreCase(t.getCategory()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal emiRatio = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            emiRatio = totalEmi.divide(totalIncome, 4, RoundingMode.HALF_UP);
        }

        // Save or update current month first (needed for multi-month calculations)
        MonthlyFinancialSummary summary =
                summaryRepository.findByUserIdAndMonth(userId, month)
                        .orElse(new MonthlyFinancialSummary());

        summary.setUser(user);
        summary.setMonth(month);
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpense(totalExpense);
        summary.setSavingsRatio(savingsRatio);
        summary.setEmiRatio(emiRatio);

        summaryRepository.save(summary);

        // ---------------- MULTI-MONTH FEATURES ----------------

        LocalDate sixMonthsAgo = month.minusMonths(5);

        List<MonthlyFinancialSummary> pastSummaries =
                summaryRepository.findByUserIdAndMonthBetween(
                                userId,
                                sixMonthsAgo,
                                month
                        ).stream()
                        .sorted(Comparator.comparing(MonthlyFinancialSummary::getMonth))
                        .toList();

        List<Double> monthlyExpenses = pastSummaries.stream()
                .map(s -> s.getTotalExpense() != null ? s.getTotalExpense().doubleValue() : 0.0)
                .toList();

        List<Double> monthlyIncome = pastSummaries.stream()
                .map(s -> s.getTotalIncome() != null ? s.getTotalIncome().doubleValue() : 0.0)
                .toList();

        // Expense Volatility (normalized std dev)
        double expenseStd = calculateStandardDeviation(monthlyExpenses);
        double meanExpense = monthlyExpenses.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double normalizedVolatility =
                meanExpense == 0 ? 0.0 : expenseStd / meanExpense;

        BigDecimal expenseVolatility =
                BigDecimal.valueOf(normalizedVolatility)
                        .setScale(4, RoundingMode.HALF_UP);

        // Spending Trend (net change across months)
        double spendingTrend = 0.0;
        if (monthlyExpenses.size() >= 2) {
            spendingTrend =
                    monthlyExpenses.get(monthlyExpenses.size() - 1)
                            - monthlyExpenses.get(0);
        }

        BigDecimal spendingTrendSlope =
                BigDecimal.valueOf(spendingTrend)
                        .setScale(4, RoundingMode.HALF_UP);

        // Income Irregularity (normalized std dev)
        double incomeStd = calculateStandardDeviation(monthlyIncome);
        double meanIncome = monthlyIncome.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double normalizedIncomeIrregularity =
                meanIncome == 0 ? 0.0 : incomeStd / meanIncome;

        BigDecimal incomeIrregularity =
                BigDecimal.valueOf(normalizedIncomeIrregularity)
                        .setScale(4, RoundingMode.HALF_UP);

        // Update summary
        summary.setExpenseVolatility(expenseVolatility);
        summary.setSpendingTrendSlope(spendingTrendSlope);
        summary.setIncomeIrregularityScore(incomeIrregularity);

        return summaryRepository.save(summary);
    }

    private double calculateStandardDeviation(List<Double> values) {
        if (values.size() < 2) return 0.0;

        double mean = values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }
}