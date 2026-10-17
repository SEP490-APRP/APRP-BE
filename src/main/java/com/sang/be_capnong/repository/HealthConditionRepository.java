package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthConditionRepository extends JpaRepository<HealthCondition, Integer> {
    Optional<HealthCondition> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
