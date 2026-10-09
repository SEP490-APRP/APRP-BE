package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ShipperLocation;
import com.sang.be_capnong.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipperLocationRepository extends JpaRepository<ShipperLocation, Integer> {
    Optional<ShipperLocation> findByShipper(User shipper);
    Optional<ShipperLocation> findTopByOrder_OrderIdOrderByUpdatedAtDesc(int orderId);
    Optional<ShipperLocation> findByShipperAndOrder_OrderId(User shipper, int orderId);
    void deleteByShipper(User shipper);
}
