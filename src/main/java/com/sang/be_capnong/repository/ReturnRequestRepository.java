package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ReturnRequest;
import com.sang.be_capnong.enums.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Integer>, JpaSpecificationExecutor<ReturnRequest> {

    boolean existsByOrderDetail_OrderDetailIdAndStatusNotIn(int orderDetailId, Collection<ReturnStatus> closedStatuses);

    List<ReturnRequest> findByOrderDetail_OrderDetailId(int orderDetailId);

    List<ReturnRequest> findByOrderDetail_Order_OrderId(int orderId);

    Page<ReturnRequest> findByBuyer_Id(int buyerId, Pageable pageable);

    Page<ReturnRequest> findByBuyer_IdAndStatus(int buyerId, ReturnStatus status, Pageable pageable);

    Page<ReturnRequest> findByShop_Id(int shopId, Pageable pageable);

    Page<ReturnRequest> findByShop_IdAndStatus(int shopId, ReturnStatus status, Pageable pageable);

    Page<ReturnRequest> findByShipper_Id(int shipperId, Pageable pageable);

    Page<ReturnRequest> findByShipper_IdAndStatus(int shipperId, ReturnStatus status, Pageable pageable);

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

    Page<ReturnRequest> findByStatusIn(Collection<ReturnStatus> statuses, Pageable pageable);

    Page<ReturnRequest> findByStatusAndShipperIsNull(ReturnStatus status, Pageable pageable);

    long countByStatus(ReturnStatus status);

    long countByShop_IdAndStatus(int shopId, ReturnStatus status);
}
