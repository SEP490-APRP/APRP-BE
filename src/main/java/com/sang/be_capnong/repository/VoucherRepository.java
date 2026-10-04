package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    
    // Tìm voucher theo mã code
    Optional<Voucher> findByVoucherCode(String voucherCode);
    
    // Lấy danh sách voucher do Admin tạo (shop_id = id của admin)
    List<Voucher> findByShop_Id(int shopId);
    
    // Lấy tất cả voucher đang active và public (không có autoCreatedReason = "HIDDEN")
    List<Voucher> findByActiveTrueAndAutoCreatedReasonNot(String hiddenReason);
    
    List<Voucher> findByActiveTrueAndAutoCreatedReasonIsNull();
}
