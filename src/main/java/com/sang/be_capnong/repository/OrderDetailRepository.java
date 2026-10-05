package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    java.util.List<OrderDetail> findByOrder_OrderId(int orderId);
}
