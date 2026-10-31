package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.OrderDetail;
import com.sang.be_capnong.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    List<OrderDetail> findByOrder_OrderId(int orderId);

    @Query("SELECT od FROM OrderDetail od " +
            "WHERE od.mysteryBox.mysteryId = :mysteryId " +
            "AND od.order.buyer.id = :buyerId " +
            "AND od.order.status IN :statuses")
    List<OrderDetail> findDeliveredDetailsByMysteryBoxAndBuyer(
            @Param("mysteryId") int mysteryId,
            @Param("buyerId") int buyerId,
            @Param("statuses") Collection<OrderStatus> statuses
    );
}
