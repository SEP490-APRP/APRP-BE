package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.PlanMealItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanMealItemRepository extends JpaRepository<PlanMealItem, Integer> {
    Optional<PlanMealItem> findByIdAndPlanMeal_PlanDay_BuildPlan_Buyer_Id(int itemId, Integer buyerId);
    List<PlanMealItem> findByPlanMeal_IdOrderByPositionAsc(int mealId);
}
