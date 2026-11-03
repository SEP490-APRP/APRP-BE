package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ReturnRequest;
import com.sang.be_capnong.enums.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Integer> {

    boolean existsByOrderDetail_OrderDetailIdAndStatusNotIn(int orderDetailId, Collection<ReturnStatus> closedStatuses);

    List<ReturnRequest> findByOrderDetail_Order_OrderId(int orderId);

    Page<ReturnRequest> findByBuyer_Id(int buyerId, Pageable pageable);

    Page<ReturnRequest> findByBuyer_IdAndStatus(int buyerId, ReturnStatus status, Pageable pageable);

    Page<ReturnRequest> findByShop_Id(int shopId, Pageable pageable);

    Page<ReturnRequest> findByShop_IdAndStatus(int shopId, ReturnStatus status, Pageable pageable);

    Page<ReturnRequest> findByShipper_Id(int shipperId, Pageable pageable);

    Page<ReturnRequest> findByShipper_IdAndStatus(int shipperId, ReturnStatus status, Pageable pageable);

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

    Page<ReturnRequest> findByStatusIn(Collection<ReturnStatus> statuses, Pageable pageable);

    @Query("SELECT r FROM ReturnRequest r WHERE r.status = 'RETURN_SHIPPING' AND r.shipper IS NULL")
    Page<ReturnRequest> findAvailableReturnsForShipper(Pageable pageable);

    @Query("SELECT r FROM ReturnRequest r " +
            "WHERE (:status IS NULL OR r.status = :status) " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "   OR LOWER(r.reason) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(r.buyer.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(r.shop.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR CAST(r.orderDetail.order.orderId AS string) LIKE CONCAT('%', :keyword, '%'))")
    Page<ReturnRequest> findDisputesForAdmin(
            @Param("status") ReturnStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByStatus(ReturnStatus status);

    long countByShop_IdAndStatus(int shopId, ReturnStatus status);
}
