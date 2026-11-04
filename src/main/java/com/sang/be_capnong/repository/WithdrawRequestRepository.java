package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.WithdrawRequest;
import com.sang.be_capnong.enums.WithdrawStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Integer> {

    List<WithdrawRequest> findByWallet_WalletIdOrderByCreatedAtDesc(int walletId);

    List<WithdrawRequest> findByStatusOrderByCreatedAtAsc(WithdrawStatus status);

    List<WithdrawRequest> findByStatusOrderByCreatedAtDesc(WithdrawStatus status);

    List<WithdrawRequest> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from WithdrawRequest r where r.requestId = :id")
    Optional<WithdrawRequest> findByIdForUpdate(@Param("id") int id);
}
