package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.PlanDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanDayRepository extends JpaRepository<PlanDay, Integer> {
    List<PlanDay> findByBuildPlan_PlanIdOrderByDayIndexAsc(int planId);
    Optional<PlanDay> findByIdAndBuildPlan_PlanId(int id, int planId);
}
