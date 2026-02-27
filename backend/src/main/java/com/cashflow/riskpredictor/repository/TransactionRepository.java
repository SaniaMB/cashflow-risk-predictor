package com.cashflow.riskpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cashflow.riskpredictor.entity.Transaction;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdAndTransactionDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}