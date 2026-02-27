package com.cashflow.riskpredictor.entity;

import com.cashflow.riskpredictor.enums.RiskCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "risk_scores",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "month"})
        }
)
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate month;

    @Column(name = "risk_probability", precision = 5, scale = 4)
    private BigDecimal riskProbability;

    @Column(name = "financial_stress_score")
    private Integer financialStressScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category")
    private RiskCategory riskCategory;

    @Column(name = "model_confidence", precision = 5, scale = 4)
    private BigDecimal modelConfidence;

    @Column(name = "top_factor_1")
    private String topFactor1;

    @Column(name = "top_factor_2")
    private String topFactor2;

    @Column(name = "top_factor_3")
    private String topFactor3;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}