# Luồng chia tiền vào ví (Wallet Settlement) & sửa luồng thanh toán

Tài liệu nguồn: đặc tả FA26SE031 mục "Payment, Fees, and Referral System" (13% = 10% Farmer Service Fee + 3% Shipper Fee, chia tiền khi đơn hoàn tất) và `doc/order-and-tracking-flow.md` mục 4 (ai chịu tiền voucher).

Sơ đồ từng bước cho mọi trường hợp (PayOS, COD, huỷ, thất bại, trả muộn): xem [`payment-to-wallet-flow.md`](payment-to-wallet-flow.md).

## 1. Khi nào chia tiền

```
DELIVERED ──(Buyer bấm "Đã nhận hàng": PUT /orders/{id}/complete)──► COMPLETED ──► chia tiền vào ví
          └─(hoặc tự động sau capnong.order.auto-complete-days ngày)─┘
```

- Chia tiền chạy trong cùng transaction với việc chuyển COMPLETED. Lỗi thì rollback cả hai.
- Chạy lại nhiều lần vẫn chỉ chia 1 lần: khoá dòng `orders` + cột `orders.settled_at` + unique `(wallet_id, order_id, type)` trên `wallet_transactions`.
- Scheduler chia bù cho đơn COMPLETED mà `settled_at` còn NULL (dữ liệu cũ).

### Tiền tạm giữ (hiện ngay, rút được khi hoàn tất)

Trước khi COMPLETED, phần của từng bên được ghi vào `wallets.frozen_balance` (mục "Đang tạm giữ"), chưa cộng vào số dư rút được:

| Thời điểm | Ví | Bút toán |
|---|---|---|
| PayOS báo đã thanh toán (đơn → PAID) | Shop, Sàn | `SHOP_INCOME_HOLD`, `PLATFORM_INCOME_HOLD` |
| COD: Shipper giao thành công, đã thu tiền mặt | Shop, Sàn | `SHOP_INCOME_HOLD`, `PLATFORM_INCOME_HOLD` |
| Shipper nhận đơn (READY → SHIPPING), cả COD và PayOS | Shipper | `SHIPPER_INCOME_HOLD` |
| Đơn COMPLETED | Cả 3 | `HOLD_RELEASED` (gỡ tạm giữ) rồi cộng số dư như bảng dưới |
| Đơn huỷ / giao thất bại | Ví nào đang tạm giữ | `HOLD_CANCELLED` (gỡ tạm giữ, không cộng gì) |

Số tạm giữ tính theo cùng công thức ở mục 2. Sàn tạm giữ = Buyer trả − phần Shop − phần Shipper (nên có thể âm nếu sàn bù voucher).
Bút toán tạm giữ có `frozen_after`, không đổi `balance_after`.

## 2. Công thức (làm tròn tới đồng)

| Đại lượng | Công thức |
|---|---|
| Tiền hàng `goods` | Σ đơn giá × số lượng của đơn |
| Voucher shop | Voucher do Shop tạo (Shop tự chịu) |
| Voucher sàn | Voucher Admin tạo, giảm tiền hàng (sàn bù cho Shop) |
| Freeship sàn | Voucher Admin tạo, target SHIPPING / FREE_SHIPPING (sàn bù cho Shipper) |
| Phí dịch vụ nông dân | `farmer-fee-rate` × (goods − voucher shop) |
| **Shop nhận** | goods − voucher shop − phí dịch vụ nông dân |
| Phí dịch vụ shipper | `shipper-fee-rate` × cước giao hàng |
| **Shipper nhận** | cước giao hàng − phí dịch vụ shipper |
| **Sàn nhận** | tiền Buyer trả − Shop nhận − Shipper nhận (= 2 khoản phí − tiền bù voucher sàn) |

Luôn đúng: `buyerPaid = shopReceive + shipperReceive + platformReceive`.

Ví dụ (đã test): tiền hàng 90.000đ, ship 15.000đ, voucher shop 10.000đ, voucher sàn 20.000đ, freeship sàn 10.000đ, Buyer trả 65.000đ
→ Shop 72.000đ, Shipper 14.550đ, Sàn −21.550đ (8.000 + 450 − 10.000 − 20.000).

Tỷ lệ cấu hình trong `application.yml` (hoặc biến môi trường):

```yaml
capnong:
  wallet:
    farmer-fee-rate: 0.10    # CAPNONG_FARMER_FEE_RATE
    shipper-fee-rate: 0.03   # CAPNONG_SHIPPER_FEE_RATE
  order:
    auto-complete-days: 3    # CAPNONG_AUTO_COMPLETE_DAYS, 0 = tắt
  payment:
    expire-minutes: 30       # CAPNONG_PAYMENT_EXPIRE_MINUTES, 0 = tắt
```

## 3. COD: ký quỹ bằng ví (từ 2026-11-06)

Chi tiết: [`shipper-dispatch-flow.md`](shipper-dispatch-flow.md) mục 4.

1. Shipper nhận đơn COD: số dư khả dụng (`total_balance − cod_reserved`) phải ≥ `totalAmount`, nếu thiếu thì lỗi 16013. Tiền được giữ chỗ (`COD_RESERVED`, `wallets.cod_reserved`).
2. Giao thành công: gỡ giữ chỗ (`COD_RESERVE_RELEASED`) và trừ ví `COD_CASH_COLLECTED −totalAmount`. Số dư không bao giờ âm, `cod_settled = true` ngay.
3. Giao thất bại: gỡ giữ chỗ, ví như cũ.
4. Đơn COMPLETED: chia tiền như đơn online.
5. Nạp ví qua PayOS: `POST /wallets/topups` → `TOP_UP`.

**Đơn nhận theo luồng cũ** (`orders.cod_reserved_amount` NULL): vẫn ghi nợ khi giao, Admin xác nhận nộp tiền bằng `POST /wallets/cod/{orderId}/settle` (`COD_CASH_REMITTED`). Ví âm thì không nhận được đơn mới (16014).

## 4. Hoàn tiền

Sàn chưa tự chuyển khoản hoàn tiền. Hệ thống ghi `orders.refund_amount` + `refund_reason` khi:
- Đơn đã thanh toán online bị Buyer/Shop huỷ.
- Đơn đã thanh toán online giao thất bại.
- Khách trả tiền cho link đã huỷ, hoặc trả trùng.

Admin xem danh sách tại `GET /wallets/refunds` và hoàn tiền thủ công.

## 5. API

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| PUT | `/orders/{id}/complete` | BUYER | Đã nhận hàng → COMPLETED → chia tiền |
| GET | `/wallets/me` | SHOP, SHIPPER, ADMIN | Ví của tôi (Admin: ví sàn) |
| GET | `/wallets/me/transactions?page=&size=` | SHOP, SHIPPER, ADMIN | Lịch sử biến động ví |
| GET | `/wallets/orders/{orderId}/settlement` | Shop/Shipper của đơn, ADMIN | Bảng chia tiền của đơn (dự kiến nếu chưa COMPLETED) |
| GET | `/wallets/platform` | ADMIN | Ví sàn |
| GET | `/wallets/platform/transactions` | ADMIN | Lịch sử ví sàn |
| POST | `/wallets/orders/{orderId}/settle` | ADMIN | Chạy lại quyết toán (an toàn khi gọi nhiều lần) |
| GET | `/wallets/cod/pending` | ADMIN | Đơn COD Shipper chưa nộp tiền |
| POST | `/wallets/cod/{orderId}/settle` | ADMIN | Xác nhận Shipper đã nộp tiền COD |
| GET | `/wallets/refunds` | ADMIN | Đơn cần hoàn tiền cho Buyer |
| POST | `/wallets/topups` | SHIPPER, SHOP | Tạo link PayOS nạp ví |
| GET | `/wallets/topups/me` | SHIPPER, SHOP | Lịch sử nạp ví |
| POST | `/wallets/topups/{orderCode}/confirm` | SHIPPER, SHOP, ADMIN | Xác nhận lệnh nạp sau khi quay lại từ PayOS |

## 6. Thay đổi ở luồng thanh toán PayOS

- Tạo link mới (cả batch lẫn đơn lẻ) sẽ huỷ các link cũ còn mở có tính tiền cùng đơn, cả trên PayOS → hết lỗi khách trả 2 lần.
- `POST /payments/{orderCode}/cancel`, `POST /payments/cancel`, `GET /payments/{orderCode}` bắt buộc đăng nhập và đúng Buyer của đơn (Admin được phép). `create-payos` và `resume-options` cũng kiểm tra chủ đơn. Webhook và `/confirm` vẫn công khai.
- Huỷ thanh toán / huỷ đơn chờ thanh toán sẽ huỷ link trên PayOS.
- Webhook tới cho đơn đã huỷ hoặc đã trả: không đổi trạng thái đơn, ghi số tiền cần hoàn.
- Mã `orderCode` = mili-giây × 1000 + 3 số ngẫu nhiên, kiểm tra trùng trước khi dùng. Bỏ việc làm tròn lên 1.000đ.
- Link PayOS có hạn `expire-minutes`; scheduler huỷ đơn AWAITING_PAYMENT quá hạn (hỏi PayOS trước, nếu đã trả thì ghi nhận PAID), trả kho, voucher, giỏ hàng.

## 7. Bảng mới / cột mới (Hibernate `ddl-auto: update` tự tạo)

- Bảng `wallet_transactions` (sổ cái ví).
- `orders.delivered_at`, `orders.completed_at`, `orders.settled_at`.
- Ví được tạo tự động khi cần: mỗi Shop / Shipper 1 ví, ví sàn `type = PLATFORM` gắn với tài khoản ADMIN có id nhỏ nhất.
