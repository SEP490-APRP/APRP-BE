# Luồng Tiếp thị Liên kết & Quyết toán Hoa hồng (Referral / Affiliate Flow)

Tài liệu kỹ thuật đặc tả hệ thống Tiếp thị Liên kết (Referral / Affiliate) cho nền tảng CapNong, bao gồm tạo link, tracking click, ghi nhận attribution 7 ngày, tính hoa hồng, xử lý huỷ đơn, hoàn hàng và quyết toán tự động vào ví của người giới thiệu.

---

## 1. Nguyên tắc Cốt lõi & Kiến trúc

1. **Backend là Single Source of Truth:**
   Mọi logic định tuyến, phân giải mã referral, ghi nhận attribution, tính toán tỷ lệ, khấu trừ voucher và cộng tiền ví được thực hiện 100% tại `APRP-BE`. Client Web và Mobile chỉ hiển thị và kích hoạt chia sẻ.
2. **Ví nhận hoa hồng thống nhất:**
   Toàn bộ tiền hoa hồng được cộng trực tiếp vào ví **`TYPE_BUYER`** của người giới thiệu với bút toán `WalletTransactionType.REFERRAL_COMMISSION`. Người giới thiệu có thể sử dụng số dư này để mua hàng tiếp hoặc thực hiện rút tiền về tài khoản ngân hàng thông qua luồng rút tiền hiện có (`/wallets/withdraw`).
3. **Cơ chế Ghi nhận Multi-Conversion Không giới hạn trong 7 ngày (Uncapped Multi-Conversion):**
   - Áp dụng cửa sổ ghi nhận kéo dài **7 ngày (168 giờ)** kể từ lần click hợp lệ cuối cùng (Last-Touch).
   - **Không giới hạn số lần nhận hoa hồng** trong 7 ngày hiệu lực (`max_conversions = 0` / unlimited). Mọi đơn hàng thành công của người mua qua link đều được ghi nhận hoa hồng.
   - **Đối tượng áp dụng:** Tính theo từng sản phẩm hợp lệ, bao gồm:
     - Nông sản đơn lẻ (`PRODUCT`)
     - Combo nông sản / rau củ (`BUILD_COMBO`) - tính là 1 sản phẩm
     - Hộp / Túi mù nông sản (`MYSTERY_BOX`) - tính là 1 sản phẩm
   - **Loại trừ tuyệt đối:** Kế hoạch thực đơn / Gói gợi ý AI (`BUILD_PLAN` / `PLAN`) không tính hoa hồng tiếp thị.
4. **Phân tách Sự kiện Quyết toán (Decoupled Settlement):**
   Quyết toán hoa hồng tiếp thị được tách khỏi transaction chia tiền cho Shop/Shipper bằng Spring Events (`@TransactionalEventListener(phase = AFTER_COMMIT)` với `Propagation.REQUIRES_NEW`), kèm `ReferralCommissionRetryScheduler` quét bù mỗi 15 phút. Lỗi ở module hoa hồng tuyệt đối không làm ảnh hưởng tiền của Shop.

---

## 2. Vòng đời Ghi nhận & Quyết toán

```
[Buyer A] ──(Tạo link chia sẻ)──► https://capnong.vn/ref/{code}
                                            │
                                            ▼
[Buyer B] ──(Click link trên Zalo/Web)──► Lưu tạm mã referral vào Cookie/Storage
                                            │
                                            ▼
[Buyer B] ──(Đăng nhập / Đăng ký)──────► POST /referrals/claim ──► Ghi nhận ReferralAttribution (ACTIVE, 7 ngày)
                                            │
                                            ▼
[Buyer B] ──(Đặt hàng thành công)──────► OrderServiceImpl.createOrder
                                            ├── Phân bổ voucher shop (pro-rata)
                                            ├── Tính Safety Cap (không vượt phí sàn)
                                            ├── Tạo ReferralCommission (PENDING)
                                            └── Tăng converted_order_count (+1)
                                            │
         ┌──────────────────────────────────┴──────────────────────────────────┐
         ▼                                                                     ▼
[Đơn bị Huỷ / PayOS quá hạn]                                          [Đơn Giao thành công & Đạt 24h đối soát]
  ├── ReferralCommission: CANCELLED                                     ├── WalletService.settleOrder: Chia tiền Shop, Shipper, Sàn
  ├── Giảm converted_order_count (-1)                                  ├── Phát sự kiện: OrderSettledEvent(orderId)
  └── Phục hồi Attribution sang ACTIVE (nếu còn trong 7 ngày)           └── ReferralSettlementListener (AFTER_COMMIT, REQUIRES_NEW):
                                                                              ├── Kiểm tra khiếu nại hoàn trả hàng (ReturnRequest)
                                                                              ├── Cập nhật atomic PENDING -> COMPLETED
                                                                              └── Cộng tiền vào ví TYPE_BUYER của Người giới thiệu
```

---

## 3. Công thức Tính Hoa hồng & Chặn trần An toàn (Safety Cap)

### 3.1. Phân bổ Voucher Shop cho từng dòng hàng
Vì `order_details.unitPrice` là giá sau giảm giá trực tiếp của sản phẩm, còn Voucher Shop áp dụng ở cấp đơn hàng, giá trị cơ sở được tính pro-rata:
$$\text{itemGross} = \text{orderDetail.unitPrice} \times \text{orderDetail.quantity}$$
$$\text{itemVoucherShare} = \text{shopVoucherTotal} \times \frac{\text{itemGross}}{\sum \text{itemGross}}$$
$$\text{commissionBaseAmount} = \max(0, \text{itemGross} - \text{itemVoucherShare})$$

### 3.2. Tính toán Hoa hồng & Chặn trần Phí Sàn Thực Thu
Tỷ lệ hoa hồng mặc định khởi điểm là **2.0%** (`capnong.referral.default-commission-rate = 2.0`).

Hoa hồng chi trả tuyệt đối **không được vượt quá phần phí sàn thực thu từ món hàng đó** (`farmerFeeRate = 0.10`):
$$\text{platformNetRetained} = \text{commissionBaseAmount} \times \text{farmerFeeRate}$$
$$\text{rawCommission} = \text{commissionBaseAmount} \times \frac{\text{commissionRate}}{100}$$
$$\text{finalCommissionAmount} = \text{Round}\left(\min(\text{rawCommission}, \text{platformNetRetained})\right)$$

*(Làm tròn đến hàng đơn vị VNĐ theo quy tắc `RoundingMode.HALF_UP`).*

### 3.3. Xử lý Hoàn trả hàng (Return & Refund)
- Khi có yêu cầu hoàn hàng nhưng bị từ chối/huỷ (`REJECTED`, `CANCELLED`, `RETURN_FAILED`): Giữ nguyên 100% hoa hồng.
- Khi yêu cầu hoàn hàng thành công (`COMPLETED`):
  $$\text{refundRatio} = \frac{\text{refundAmount}}{\text{itemGross}}$$
  $$\text{remainingFactor} = \max(0, 1 - \text{refundRatio})$$
  $$\text{adjustedCommission} = \text{commissionAmount} \times \text{remainingFactor}$$
- Nếu hoàn hàng toàn bộ hoặc số tiền còn lại $\le 0$, bản ghi `ReferralCommission` chuyển sang `CANCELLED`.

---

## 4. Cơ chế Chống gian lận & Idempotency 3 Tầng

1. **Chống Tự giới thiệu (Self-referral Protection):**
   - Không cho phép người mua tự mua hàng qua link của chính mình (`referrer_id != buyer_id`).
   - Không cho phép chủ shop tự tạo link tiếp thị cho sản phẩm hoặc gian hàng của chính mình.
2. **Rate Limit Click:**
   - Giới hạn tối đa 10 lượt click / IP / phút (`capnong.referral.click-rate-limit-per-ip`) để chống bot spam làm sai lệch thống kê.
3. **Idempotency 3 Tầng chống cộng tiền trùng:**
   - **Tầng 1 (DB Constraint):** Bảng `referral_commissions` có `UNIQUE KEY uk_ref_comm_order_detail (order_detail_id)` đảm bảo 1 món hàng chỉ tạo duy nhất 1 bản ghi hoa hồng.
   - **Tầng 2 (Atomic Update):** `UPDATE referral_commissions SET status='COMPLETED' ... WHERE id=:id AND status='PENDING'`. Nếu trả về 0 row updated thì bỏ qua không cộng tiền.
   - **Tầng 3 (Ledger Unique Constraint):** Bảng `wallet_transactions` có cột `referral_commission_id BIGINT NULL UNIQUE`. CSDL sẽ chặn cứng nếu một khoản hoa hồng bị ghi nhận bút toán lần thứ hai.

---

## 5. Danh mục API Endpoints

Tất cả endpoint đều chạy dưới context path `/api`:

| Phương thức | Endpoint | Phân quyền | Mô tả chức năng |
|---|---|---|---|
| `POST` | `/referrals/links` | `BUYER` | Tạo mới hoặc lấy link tiếp thị cho sản phẩm / shop / combo / túi mù |
| `GET` | `/referrals/links/my` | `BUYER` | Danh sách link đã tạo của Buyer (0-based page) |
| `GET` | `/referrals/resolve/{code}` | Public | Phân giải mã link: Trả metadata sản phẩm, ẩn danh người giới thiệu |
| `POST` | `/referrals/clicks` | Public | Ghi nhận click thật từ client JS kèm IP và User-Agent |
| `POST` | `/referrals/claim` | `BUYER` | Gắn mã referral lưu tạm vào tài khoản sau khi đăng nhập |
| `GET` | `/referrals/dashboard` | `BUYER` | Thống kê: Tổng hoa hồng, hoa hồng chờ duyệt, số click, số đơn |
| `GET` | `/referrals/commissions` | `BUYER` | Lịch sử hoa hồng chi tiết theo từng đơn hàng/dòng hàng |

---

## 6. Cấu hình Hệ thống (`application.yml`)

```yaml
capnong:
  referral:
    # Tỷ lệ hoa hồng mặc định (%) cho nông sản đơn lẻ
    default-commission-rate: ${CAPNONG_REFERRAL_COMMISSION_RATE:2.0}
    # Số đơn hàng tối đa được tính hoa hồng cho mỗi attribution trong cửa sổ ghi nhận
    max-conversions-per-attribution: ${CAPNONG_REFERRAL_MAX_CONVERSIONS:3}
    # Cửa sổ ghi nhận referral attribution (ngày)
    window-days: ${CAPNONG_REFERRAL_WINDOW_DAYS:7}
    # Giới hạn số lượt nhấp được ghi nhận từ 1 IP trong 1 phút
    click-rate-limit-per-ip: ${CAPNONG_REFERRAL_CLICK_RATE_LIMIT:10}
    # Chu kỳ quét bù quyết toán hoa hồng cho các đơn đã settle (ms) - Mặc định 15 phút
    retry-delay-ms: 900000
```

---

## 7. Tài liệu Kiểm thử (Test Cases)

Toàn bộ kịch bản kiểm thử chi tiết bao gồm **Input, Các bước thực hiện, Output trên giao diện Web (`APRP-FE`), Output trên giao diện Mobile (`APRP-Mobile`) và Trạng thái CSDL (`APRP-BE`)** được lưu trữ tại:
👉 **[Kịch bản kiểm thử Tiếp thị liên kết (referral-test-cases.md)](file:///d:/SEP/APRP-BE/doc/referral-test-cases.md)**


