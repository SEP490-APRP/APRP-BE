package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    Optional<Wallet> findFirstByUser_IdAndType(int userId, String type);

    Optional<Wallet> findFirstByType(String type);

    java.util.List<Wallet> findByTypeIn(java.util.Collection<String> types);

    /** Khoá dòng ví khi cộng/trừ tiền để 2 lần quyết toán song song không ghi đè số dư của nhau */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.walletId = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") int id);
}
