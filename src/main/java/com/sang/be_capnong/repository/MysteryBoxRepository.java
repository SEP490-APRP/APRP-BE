package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.MysteryBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MysteryBoxRepository extends JpaRepository<MysteryBox, Integer>, JpaSpecificationExecutor<MysteryBox> {

    Page<MysteryBox> findByShop_Id(Integer shopId, Pageable pageable);
}
