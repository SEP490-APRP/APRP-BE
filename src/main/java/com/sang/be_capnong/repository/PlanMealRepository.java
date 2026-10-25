package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.PlanMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanMealRepository extends JpaRepository<PlanMeal, Integer> {
    Optional<PlanMeal> findByIdAndPlanDay_BuildPlan_Buyer_Id(int mealId, Integer buyerId);
    Optional<PlanMeal> findByIdAndPlanDay_BuildPlan_PlanId(int mealId, int planId);
}
