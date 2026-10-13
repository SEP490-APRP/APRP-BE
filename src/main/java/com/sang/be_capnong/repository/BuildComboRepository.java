package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.BuildCombo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildComboRepository extends JpaRepository<BuildCombo, Integer>, JpaSpecificationExecutor<BuildCombo> {

    @EntityGraph(attributePaths = {"shop"})
    Page<BuildCombo> findByShop_Id(Integer shopId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"shop"})
    Page<BuildCombo> findAll(Specification<BuildCombo> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"shop"})
    Optional<BuildCombo> findById(Integer id);
}
