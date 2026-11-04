package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Voucher;
import com.sang.be_capnong.enums.VoucherScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    
    // Tìm voucher theo mã code
    Optional<Voucher> findByVoucherCode(String voucherCode);

    // Khóa bi quan không dùng lệnh @Query
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Voucher> findByVoucherId(int voucherId);
    
    // Lấy danh sách voucher do Shop/Admin tạo
    List<Voucher> findByShop_Id(int shopId);

    // Lấy danh sách voucher do Shop tạo có phân trang
    Page<Voucher> findByShop_Id(int shopId, Pageable pageable);

    // Lấy danh sách voucher quản lý của Admin có phân trang không dùng lệnh @Query
    Page<Voucher> findByShop_Role_NameOrVoucherScopeIn(String roleName, Collection<VoucherScope> scopes, Pageable pageable);
    
    // Lấy tất cả voucher đang active và public (không có autoCreatedReason = "HIDDEN")
    List<Voucher> findByActiveTrueAndAutoCreatedReasonNot(String hiddenReason);
    
    List<Voucher> findByActiveTrueAndAutoCreatedReasonIsNull();
}
