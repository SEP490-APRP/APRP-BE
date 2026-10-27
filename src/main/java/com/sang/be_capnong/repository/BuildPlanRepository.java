package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.BuildPlan;
import com.sang.be_capnong.enums.PlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuildPlanRepository extends JpaRepository<BuildPlan, Integer> {
    Optional<BuildPlan> findByPlanIdAndBuyer_Id(int planId, Integer buyerId);
    Page<BuildPlan> findByBuyer_Id(Integer buyerId, Pageable pageable);
    Page<BuildPlan> findByBuyer_IdAndStatus(Integer buyerId, PlanStatus status, Pageable pageable);
    List<BuildPlan> findTop5ByBuyer_IdOrderByPlanIdDesc(Integer buyerId);
}
