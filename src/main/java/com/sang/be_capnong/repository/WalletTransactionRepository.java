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

    boolean existsByWallet_WalletIdAndOrder_OrderIdAndType(int walletId, int orderId, com.sang.be_capnong.enums.WalletTransactionType type);
}
