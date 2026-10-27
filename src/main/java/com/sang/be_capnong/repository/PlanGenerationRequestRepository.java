package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.PlanGenerationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanGenerationRequestRepository extends JpaRepository<PlanGenerationRequest, Integer> {
    List<PlanGenerationRequest> findByBuyer_IdOrderByRequestedAtDesc(Integer buyerId);
    List<PlanGenerationRequest> findByBuildPlan_PlanId(int planId);
}
