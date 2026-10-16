package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.entity.Voucher;
import com.sang.be_capnong.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Integer> {
    int countByUserAndVoucher(com.sang.be_capnong.entity.User user, com.sang.be_capnong.entity.Voucher voucher);
    java.util.List<VoucherUsage> findByOrder(com.sang.be_capnong.entity.Order order);
    java.util.List<VoucherUsage> findByOrder_OrderId(int orderId);
    void deleteByVoucher_VoucherId(int voucherId);
}
