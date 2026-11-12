package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    boolean existsByShipper_IdAndStatus(int shipperId, OrderStatus status);
    boolean existsByShipper_IdAndStatusIn(int shipperId, List<OrderStatus> statuses);

    /** Đơn READY chưa có shipper, dùng cho điều phối theo đợt */
    List<Order> findByStatusAndShipperIsNull(OrderStatus status);

    /** Shipper đang có đơn chưa giao xong (để loại khỏi danh sách mời) */
    @Query("select distinct o.shipper.id from Order o where o.shipper is not null and o.status in :statuses")
    List<Integer> findShipperIdsWithStatusIn(@Param("statuses") List<OrderStatus> statuses);

    /** Khoá dòng đơn khi quyết toán ví để chống chia tiền 2 lần */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :id")
    Optional<Order> findByIdForUpdate(@Param("id") int id);

    /** Đơn đã giao xong quá N ngày mà Buyer chưa bấm "Đã nhận hàng" -> tự động hoàn tất */
    List<Order> findTop200ByStatusAndDeliveredAtBefore(OrderStatus status, LocalDateTime before);

    /** Đơn COMPLETED nhưng chưa quyết toán (ví dụ lỗi giữa chừng) -> chạy bù */
    List<Order> findTop200ByStatusAndSettledAtIsNull(OrderStatus status);

    /** Đơn COMPLETED hoặc REFUNDED nhưng chưa quyết toán -> chạy bù */
    List<Order> findTop200ByStatusInAndSettledAtIsNull(List<OrderStatus> statuses);

    /** Đơn chờ thanh toán quá hạn -> tự huỷ để trả kho */
    List<Order> findTop200ByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime before);

    /** Đơn COD Shipper đã thu tiền mặt nhưng chưa nộp về sàn */
    List<Order> findByCodCashConfirmedAtIsNotNullAndCodSettledFalseOrderByCodCashConfirmedAtAsc();

    /** Đơn cần sàn hoàn tiền cho Buyer (đã thanh toán online rồi bị huỷ / thanh toán trùng) */
    List<Order> findByRefundAmountGreaterThanOrderByCreatedAtDesc(BigDecimal amount);

    /** Đơn đã chia tiền vào ví kể từ {from} (dòng tiền của màn Ví Sàn) */
    java.util.List<Order> findBySettledAtGreaterThanEqual(java.time.LocalDateTime from);
}
