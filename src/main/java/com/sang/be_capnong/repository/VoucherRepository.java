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
    
    // Lấy danh sách voucher do Shop/Admin tạo
    List<Voucher> findByShop_Id(int shopId);

    // Lấy danh sách voucher do Shop tạo có phân trang
    org.springframework.data.domain.Page<Voucher> findByShop_Id(int shopId, org.springframework.data.domain.Pageable pageable);

    // Lấy danh sách voucher quản lý của Admin có phân trang
    @org.springframework.data.jpa.repository.Query("SELECT v FROM Voucher v WHERE v.shop.role.name = 'ADMIN' OR v.voucherScope IN :scopes")
    org.springframework.data.domain.Page<Voucher> findAllAdminVouchers(@org.springframework.data.repository.query.Param("scopes") List<com.sang.be_capnong.enums.VoucherScope> scopes, org.springframework.data.domain.Pageable pageable);
    
    // Lấy tất cả voucher đang active và public (không có autoCreatedReason = "HIDDEN")
    List<Voucher> findByActiveTrueAndAutoCreatedReasonNot(String hiddenReason);
    
    List<Voucher> findByActiveTrueAndAutoCreatedReasonIsNull();
}
