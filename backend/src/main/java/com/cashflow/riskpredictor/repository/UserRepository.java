package com.cashflow.riskpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cashflow.riskpredictor.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}