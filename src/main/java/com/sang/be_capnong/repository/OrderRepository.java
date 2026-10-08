package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Page<Order> findByShop_Id(int shopId, Pageable pageable);
    Page<Order> findByShop_IdAndStatus(int shopId, OrderStatus status, Pageable pageable);
    Page<Order> findByShop_IdAndStatusIn(int shopId, List<OrderStatus> statuses, Pageable pageable);
    Page<Order> findByShop_IdAndStatusNotIn(int shopId, List<OrderStatus> statuses, Pageable pageable);
    Page<Order> findByBuyer_Id(int buyerId, Pageable pageable);
    Page<Order> findByBuyer_IdAndStatusIn(int buyerId, List<OrderStatus> statuses, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByShipper_Id(int shipperId, Pageable pageable);
    Page<Order> findByShipper_IdAndStatusIn(int shipperId, List<OrderStatus> statuses, Pageable pageable);
    List<Order> findByCheckoutBatch_Id(int batchId);
}
