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

        BigDecimal savingsRatio = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRatio = totalIncome.subtract(totalExpense)
                    .divide(totalIncome, 4, RoundingMode.HALF_UP);
        }

        // EMI ratio
        BigDecimal totalEmi = expenseTx.stream()
                .filter(t -> "EMI".equalsIgnoreCase(t.getCategory()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal emiRatio = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            emiRatio = totalEmi.divide(totalIncome, 4, RoundingMode.HALF_UP);
        }

        // Expense volatility (std dev of daily expense)
        Map<LocalDate, BigDecimal> dailyExpenseMap = expenseTx.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getTransactionDate,
                        Collectors.mapping(
                                Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        double volatility = calculateStandardDeviation(
                dailyExpenseMap.values().stream()
                        .map(BigDecimal::doubleValue)
                        .toList()
        );

        BigDecimal expenseVolatility =
                BigDecimal.valueOf(volatility).setScale(4, RoundingMode.HALF_UP);

        // Spending trend (first half vs second half)
        int midDay = startDate.lengthOfMonth() / 2;

        BigDecimal firstHalfExpense = expenseTx.stream()
                .filter(t -> t.getTransactionDate().getDayOfMonth() <= midDay)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal secondHalfExpense = expenseTx.stream()
                .filter(t -> t.getTransactionDate().getDayOfMonth() > midDay)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal spendingTrendSlope = secondHalfExpense.subtract(firstHalfExpense)
                .setScale(4, RoundingMode.HALF_UP);

        // Income irregularity (std dev of income)
        double incomeStd = calculateStandardDeviation(
                incomeTx.stream()
                        .map(t -> t.getAmount().doubleValue())
                        .toList()
        );

        BigDecimal incomeIrregularity =
                BigDecimal.valueOf(incomeStd).setScale(4, RoundingMode.HALF_UP);

        // Save or update
        MonthlyFinancialSummary summary =
                summaryRepository.findByUserIdAndMonth(userId, month)
                        .orElse(new MonthlyFinancialSummary());

        summary.setUser(user);
        summary.setMonth(month);
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpense(totalExpense);
        summary.setSavingsRatio(savingsRatio);
        summary.setEmiRatio(emiRatio);
        summary.setExpenseVolatility(expenseVolatility);
        summary.setSpendingTrendSlope(spendingTrendSlope);
        summary.setIncomeIrregularityScore(incomeIrregularity);

        return summaryRepository.save(summary);
    }

    private double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) return 0.0;

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