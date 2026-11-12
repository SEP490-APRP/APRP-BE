package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.WalletTopup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTopupRepository extends JpaRepository<WalletTopup, Integer> {

    Optional<WalletTopup> findByPayosOrderCode(String payosOrderCode);

    boolean existsByPayosOrderCode(String payosOrderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WalletTopup t where t.payosOrderCode = :code")
    Optional<WalletTopup> findByPayosOrderCodeForUpdate(@Param("code") String code);

    List<WalletTopup> findTop20ByWallet_WalletIdOrderByCreatedAtDesc(int walletId);
}
