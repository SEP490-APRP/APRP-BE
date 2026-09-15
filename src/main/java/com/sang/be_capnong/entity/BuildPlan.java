package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.PlanSource;
import com.sang.be_capnong.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "build_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    int planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    User buyer;

    @Column(length = 255)
    String planName;

    Integer numberOfDays;
    Integer numberOfPeople;
    String mealType;
    BigDecimal targetBudget;

    @Enumerated(EnumType.STRING)
    PlanStatus status;

    @Enumerated(EnumType.STRING)
    PlanSource source;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "buildPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PlanDay> planDays;

    @OneToMany(mappedBy = "buildPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<PlanGenerationRequest> generationRequests;
}
