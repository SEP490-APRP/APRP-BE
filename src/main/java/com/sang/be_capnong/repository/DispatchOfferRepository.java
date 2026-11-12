package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.DispatchOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispatchOfferRepository extends JpaRepository<DispatchOffer, Integer> {

    @Query("select d.shipper.id from DispatchOffer d where d.order.orderId = :orderId")
    List<Integer> findShipperIdsByOrderId(@Param("orderId") int orderId);
}
