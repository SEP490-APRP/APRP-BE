package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ShipperLocation;
import com.sang.be_capnong.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipperLocationRepository extends JpaRepository<ShipperLocation, Integer> {
    Optional<ShipperLocation> findByShipper(User shipper);

    /** Dữ liệu cũ có thể có nhiều dòng cho 1 shipper: luôn lấy dòng mới nhất */
    Optional<ShipperLocation> findFirstByShipperOrderByUpdatedAtDesc(User shipper);
    Optional<ShipperLocation> findTopByOrder_OrderIdOrderByUpdatedAtDesc(int orderId);
    Optional<ShipperLocation> findByShipperAndOrder_OrderId(User shipper, int orderId);
    java.util.List<ShipperLocation> findByOrder_OrderId(int orderId);
    void deleteByShipper(User shipper);

    /** Shipper đang bật nhận đơn và còn heartbeat sau mốc thời gian */
    @org.springframework.data.jpa.repository.Query("select l from ShipperLocation l join fetch l.shipper "
            + "where l.online = true and l.lastSeenAt >= :since")
    java.util.List<ShipperLocation> findOnlineSince(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    /** Shipper đang online nhưng mất heartbeat */
    java.util.List<ShipperLocation> findByOnlineTrueAndLastSeenAtBefore(java.time.LocalDateTime before);
}
