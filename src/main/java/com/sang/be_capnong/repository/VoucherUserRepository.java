package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.VoucherUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherUserRepository extends JpaRepository<VoucherUser, Integer> {
    List<VoucherUser> findByUser_Id(int userId);
    Optional<VoucherUser> findByUser_IdAndVoucher_VoucherId(int userId, int voucherId);
}
