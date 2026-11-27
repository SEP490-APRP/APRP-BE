package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.DeliveryHub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryHubRepository extends JpaRepository<DeliveryHub, Integer> {

    List<DeliveryHub> findByActiveTrueOrderByNameAsc();

    List<DeliveryHub> findAllByOrderByActiveDescNameAsc();
}
