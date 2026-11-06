package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWallet_WalletIdOrderByCreatedAtDescIdDesc(int walletId, Pageable pageable);

    List<WalletTransaction> findByOrder_OrderIdOrderByIdAsc(int orderId);

    /** Tổng tiền theo loại bút toán của 1 ví từ thời điểm {from} */
    @org.springframework.data.jpa.repository.Query("select t.type, coalesce(sum(t.amount), 0) from WalletTransaction t "
            + "where t.wallet.walletId = :walletId and t.createdAt >= :from group by t.type")
    List<Object[]> sumByTypeSince(@org.springframework.data.repository.query.Param("walletId") int walletId,
                                  @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from);

    /** (walletId, orderId, phần tạm giữ còn lại) cho các đơn còn tiền tạm giữ */
    @org.springframework.data.jpa.repository.Query("select t.wallet.walletId, t.order.orderId, sum(t.amount) from WalletTransaction t "
            + "where t.type in :types and t.order is not null group by t.wallet.walletId, t.order.orderId having sum(t.amount) <> 0")
    List<Object[]> openHoldsByWalletAndOrder(@org.springframework.data.repository.query.Param("types")
                                             java.util.Collection<com.sang.be_capnong.enums.WalletTransactionType> types);

    boolean existsByWallet_WalletIdAndOrder_OrderIdAndType(int walletId, int orderId, com.sang.be_capnong.enums.WalletTransactionType type);
}
