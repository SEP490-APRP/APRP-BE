package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.UserHealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHealthConditionRepository extends JpaRepository<UserHealthCondition, Integer> {
    List<UserHealthCondition> findByBuyer_Id(Integer buyerId);
    boolean existsByBuyer_IdAndCondition_Id(Integer buyerId, Integer conditionId);
    void deleteByBuyer_IdAndCondition_Id(Integer buyerId, Integer conditionId);
    void deleteByBuyer_Id(Integer buyerId);
}
