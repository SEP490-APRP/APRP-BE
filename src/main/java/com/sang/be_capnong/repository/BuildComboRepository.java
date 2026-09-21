package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.BuildCombo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildComboRepository extends JpaRepository<BuildCombo, Integer>, JpaSpecificationExecutor<BuildCombo> {

    Page<BuildCombo> findByShop_Id(Integer shopId, Pageable pageable);
}
