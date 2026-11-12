# Bộ Kịch bản Kiểm thử Hệ thống Tiếp thị Liên kết (Referral / Affiliate Test Cases)
### Dự án: CapNong (Web Frontend, Mobile App, Backend)

Tài liệu này cung cấp danh mục kịch bản kiểm thử (Test Cases) chi tiết cho tính năng **Tiếp thị Liên kết & Quyết toán Hoa hồng** trên sàn nông sản CapNong. Kịch bản bao gồm đầy đủ **Input** (dữ liệu đầu vào), **Các bước thực hiện**, cùng với **Output hiển thị trên màn hình Web Frontend (`APRP-FE`)** và **Mobile App (`APRP-Mobile`)**, cũng như kiểm tra tính toàn vẹn dữ liệu trong Cơ sở dữ liệu (`APRP-BE`).

---

## Bảng Tổng hợp Kịch bản Kiểm thử

| STT | Mã TC | Tên Kịch bản Kiểm thử | Mức độ | Phân loại |
|---|---|---|---|---|
| 1 | **TC-REF-01** | Tạo và chia sẻ liên kết tiếp thị sản phẩm / gian hàng | **P0** | UI & Tạo link |
| 2 | **TC-REF-02** | Khách vãng lai truy cập link tiếp thị & Smart Deep Link | **P0** | Tracking & Điều hướng |
| 3 | **TC-REF-03** | Tự động Claim Attribution sau khi Đăng nhập / Đăng ký | **P0** | Attribution Lifecycle |
| 4 | **TC-REF-04** | Đặt hàng thành công & Ghi nhận hoa hồng PENDING (Safety Cap & Voucher) | **P0** | Tính toán hoa hồng |
| 5 | **TC-REF-05** | Đơn hàng bị huỷ hoặc hết hạn PayOS (Huỷ PENDING & Trả lại lượt) | **P0** | Xử lý huỷ đơn |
| 6 | **TC-REF-06** | Quyết toán đơn hàng & Cộng tiền tự động vào Ví Người mua (`TYPE_BUYER`) | **P0** | Settlement & Ledger |
| 7 | **TC-REF-07** | Đơn hàng khiếu nại hoàn tiền một phần (Partial Return & Refund) | **P1** | Khấu trừ hoàn trả |
| 8 | **TC-REF-08** | Chặn tự giới thiệu (Self-referral Protection) | **P1** | Chống gian lận |
| 9 | **TC-REF-09** | Giới hạn tối đa 3 đơn chuyển đổi trên 1 Attribution (Capped Multi-Conversion) | **P1** | Giới hạn chính sách |
| 10 | **TC-REF-10** | Hết hạn cửa sổ 7 ngày (Expired Attribution Window) | **P1** | Cửa sổ thời gian |
| 11 | **TC-REF-11** | Chống Spam Click (Rate Limiting Click) | **P2** | Bảo mật & Phòng thủ |
| 12 | **TC-REF-12** | Quản lý & Xem thống kê trên Ví của tôi (Dashboard & Lịch sử) | **P0** | UI Ví & Báo cáo |

---

## Chi tiết Từng Kịch bản Kiểm thử (Test Cases)

---

### TC-REF-01: Tạo và chia sẻ liên kết tiếp thị sản phẩm / gian hàng

- **Mục tiêu:** Kiểm tra Buyer có thể sinh link tiếp thị riêng biệt (gắn mã Base62 8 ký tự), hiển thị tỷ lệ hoa hồng 2% và sao chép/chia sẻ link thành công.
- **Tiền điều kiện:** 
  - Đã đăng nhập tài khoản Khách mua (Buyer A: `buyer_a@gmail.com`).
  - Sản phẩm `ID=701` ("Bắp Cải Tươi Đà Lạt", giá 25.000đ/kg) đang mở bán.
- **Input:**
  - Bấm nút "Chia sẻ" tại trang chi tiết sản phẩm `ID=701`.

#### Các bước thực hiện:
1. Đăng nhập tài khoản Buyer A trên Web / Mobile.
2. Điều hướng tới trang chi tiết sản phẩm `ID=701`.
3. Bấm vào nút hoặc banner chia sẻ.
4. Chọn "Sao chép liên kết" hoặc "Chia sẻ ngay".

#### Output mong đợi trên Web Frontend (`APRP-FE`):
- **Tại chi tiết sản phẩm:** Popover chia sẻ hiển thị:
  - Badge nổi bật màu xanh/vàng: `Hoa hồng tiếp thị 2%`.
  - Dòng text: *"Nhận hoa hồng vào ví khi bạn bè mua sản phẩm này qua link của bạn"*.
  - Ô input chứa link dạng: `https://capnong.com/ref/{referralCode}` (mã 8 ký tự, vd: `ref/aB3x9kL1`).
  - Bấm nút **"Sao chép link"**: Icon chuyển sang dấu check xanh kèm chữ *"Đã chép!"* trong 2 giây. Nội dung clipboard chứa đúng đường dẫn link.
  - Các nút chia sẻ nhanh: Facebook, Zalo, Twitter.

#### Output mong đợi trên Mobile App (`APRP-Mobile`):
- **Tại chi tiết sản phẩm:**
  - Hiển thị Banner viền hổ phách: `[Icon Sparkles] Chia sẻ kiếm hoa hồng 2% - Nhận tiền trực tiếp vào ví khi bạn bè mua qua link của bạn` kèm nút `Chia sẻ`.
  - Icon `Share2` trên thanh tiêu đề góc trên bên phải.
  - Khi bấm chia sẻ: Mở Native Share Sheet của hệ điều hành (Android Intent / iOS UIActivityViewController) với nội dung:
    `"🌱 Giới thiệu bạn món Bắp Cải Tươi Đà Lạt tươi ngon từ Nông trại Đà Lạt trên CapNong: https://capnong.com/ref/{referralCode}"`.

#### Output tại Database (`APRP-BE`):
- Bảng `referral_links`: Thêm mới (hoặc trả về link đã có) 1 dòng:
  - `referrer_id = [ID của Buyer A]`
  - `target_type = 'PRODUCT'`, `target_id = 701`
  - `code = 'aB3x9kL1'`, `commission_rate = 2.0`
  - `is_active = 1`

---

### TC-REF-02: Khách vãng lai truy cập link tiếp thị & Smart Deep Link

- **Mục tiêu:** Người dùng chưa đăng nhập (Buyer B) truy cập vào link tiếp thị của Buyer A. Hệ thống phải phân giải được link, ghi nhận click, lưu tạm mã giới thiệu và cung cấp nút mở app hoặc xem trên web.
- **Tiền điều kiện:** Chưa đăng nhập tài khoản trên trình duyệt / thiết bị di động. Link hợp lệ: `https://capnong.com/ref/aB3x9kL1`.
- **Input:** Truy cập đường link `https://capnong.com/ref/aB3x9kL1` từ trình duyệt hoặc ứng dụng chat (Zalo, Messenger).

#### Các bước thực hiện:
1. Mở tab ẩn danh hoặc thiết bị chưa đăng nhập.
2. Dán link `https://capnong.com/ref/aB3x9kL1` và nhấn Enter (hoặc click link từ Zalo).

#### Output mong đợi trên Web Frontend (`APRP-FE`):
- Màn hình chuyển tiếp `ReferralRedirectPage` xuất hiện:
  - Header xanh: *"Liên kết giới thiệu nông sản - Người chia sẻ: [Tên Buyer A]"*.
  - Khung Preview sản phẩm: Ảnh sản phẩm, tên "Bắp Cải Tươi Đà Lạt", nhãn "Nông sản an lành", mã liên kết `aB3x9kL1`.
  - Nút chính màu xanh lá: `Xem ngay trên Web (5s)` kèm hiệu ứng đếm ngược tự động từ 5 về 0. Khi hết thời gian, tự động chuyển hướng tới `/san-pham/701`.
  - Nút phụ viền xanh: `Mở trong ứng dụng CapNong` kích hoạt scheme `capnong://ref/aB3x9kL1`.
  - **Storage:** `localStorage.getItem('capnong_pending_referral')` có giá trị `{"code":"aB3x9kL1","timestamp":...}`.

#### Output mong đợi trên Mobile App (`APRP-Mobile`):
- Khi click mở bằng app hoặc deep link `capnong://ref/aB3x9kL1`:
  - Mở trực tiếp màn hình `ref/[code].tsx` (không bị chặn bởi Role Guard).
  - Khung card bo góc:
    - Ảnh sản phẩm, tên nông sản, mã giới thiệu.
    - Nhãn: *"Nông sản được giới thiệu từ người bạn: [Tên Buyer A]"*.
    - Nút 1: `Đăng nhập để nhận ưu đãi` -> Chuyển về màn hình đăng nhập `/(auth)/login`.
    - Nút 2: `Xem sản phẩm ngay` -> Điều hướng tới màn hình chi tiết sản phẩm `/buyer/product/701`.
  - **Storage:** `AsyncStorage.getItem('capnong_mobile_pending_referral')` lưu `{"code":"aB3x9kL1","timestamp":...}`.

#### Output tại Database (`APRP-BE`):
- Bảng `referral_clicks`: Thêm 1 bản ghi mới:
  - `referral_link_id = [ID của link]`
  - `ip_address = [IP của client]`
  - `user_agent = [User Agent client]`
- Bảng `referral_links`: `clicks_count` tăng thêm `+1`.

---

### TC-REF-03: Tự động Claim Attribution sau khi Đăng nhập / Đăng ký

- **Mục tiêu:** Khách mua B sau khi xem link giới thiệu thực hiện đăng nhập hoặc tạo tài khoản mới. Hệ thống tự động claim mã và kích hoạt attribution trong 7 ngày.
- **Tiền điều kiện:** Trình duyệt hoặc app đang có mã pending `aB3x9kL1` trong Storage (từ TC-REF-02).
- **Input:** Buyer B nhập email `buyer_b@gmail.com` và mật khẩu -> Bấm "Đăng nhập".

#### Các bước thực hiện:
1. Tại Web hoặc Mobile, bấm đăng nhập với tài khoản Buyer B.
2. Hoàn tất xác thực đăng nhập.

#### Output mong đợi trên Web Frontend (`APRP-FE`):
- `StorefrontLayout` phát hiện trạng thái `isAuthenticated = true` và `capnong_pending_referral` tồn tại.
- Kích hoạt ngầm `POST /referrals/claim` với body `{"referralCode": "aB3x9kL1"}`.
- `localStorage.removeItem('capnong_pending_referral')` được gọi dọn dẹp sạch sẽ.
- Người dùng chuyển vào trang mua sắm bình thường không bị gián đoạn hay hiện popup phiền toái.

#### Output mong đợi trên Mobile App (`APRP-Mobile`):
- Tại hàm `processAuthToken` (sau khi lưu session): Kích hoạt `claimPendingReferral()`.
- Gửi API `POST /referrals/claim`.
- Xóa key `capnong_mobile_pending_referral` khỏi `AsyncStorage`.
- Người dùng được chuyển thẳng vào trang chi tiết sản phẩm `/buyer/product/701`.

#### Output tại Database (`APRP-BE`):
- Bảng `referral_attributions`: Thêm 1 dòng mới (hoặc gia hạn Last-Touch nếu đã có):
  - `referrer_id = [ID của Buyer A]`
  - `buyer_id = [ID của Buyer B]`
  - `referral_link_id = [ID của link]`
  - `target_type = 'PRODUCT'`, `target_id = 701`
  - `status = 'ACTIVE'`
  - `converted_order_count = 0`, `max_conversions = 3`
  - `expires_at = NOW() + 7 DAYS`

---

### TC-REF-04: Đặt hàng thành công & Ghi nhận hoa hồng PENDING (Safety Cap & Voucher)

- **Mục tiêu:** Buyer B đặt mua sản phẩm `ID=701` có áp dụng Voucher Shop. Kiểm tra công thức phân bổ voucher pro-rata, Safety Cap sàn 10%, và tạo bản ghi hoa hồng trạng thái `PENDING`.
- **Tiền điều kiện:** 
  - Buyer B đang có Attribution `ACTIVE` cho sản phẩm `ID=701` do Buyer A giới thiệu.
  - Tỷ lệ hoa hồng cấu hình: 2.0% (`capnong.referral.commission-rate = 0.02`).
  - Phí sàn: 10.0% (`farmerFeeRate = 0.10`).
- **Input Đơn hàng:**
  - Sản phẩm 1 (ID 701): 4kg × 25.000đ = 100.000đ.
  - Sản phẩm 2 (ID 802 - Rau xà lách): 2kg × 50.000đ = 100.000đ.
  - Tổng tiền hàng: 200.000đ.
  - Voucher Shop giảm giá: 20.000đ.
  - Phân bổ voucher cho món 1 (ID 701): $20.000 \times \frac{100.000}{200.000} = 10.000đ$.
  - Giá tính hoa hồng (`base_amount`): $100.000 - 10.000 = 90.000đ$.
  - Hoa hồng thô: $90.000 \times 2\% = 1.800đ$.
  - Trần phí sàn thực thu: $90.000 \times 10\% = 9.000đ$.
  - Hoa hồng thực nhận: $\min(1.800, 9.000) = 1.800đ$.

#### Các bước thực hiện:
1. Buyer B thêm sản phẩm `ID=701` và `ID=802` vào giỏ hàng.
2. Tiến hành Checkout, chọn voucher shop 20.000đ.
3. Đặt hàng và thanh toán thành công.

#### Output mong đợi trên Web Frontend (`APRP-FE`):
- **Phía Người mua B:** Màn hình kết quả thanh toán `PaymentResultPage` hiển thị thành công.
- **Phía Người giới thiệu A:**
  - Vào menu `/vi-cua-toi` -> Chọn Tab **"Tiếp thị liên kết"**.
  - Thẻ **"HOA HỒNG ĐANG CHỜ DUYỆT"**: Tăng thêm `+1.800đ`.
  - Bảng **"Lịch sử hoa hồng chi tiết"**:
    - Mã đơn: `#1001` (hoặc mã đơn vừa tạo).
    - Sản phẩm: `Bắp Cải Tươi Đà Lạt`.
    - Giá tính hoa hồng: `90.000đ`.
    - Tỷ lệ: `2.0%`.
    - Hoa hồng: `+1.800đ`.
    - Trạng thái: Badge màu vàng **"Chờ quyết toán"**.
    - Sản phẩm 2 (`ID=802`): Không có dòng hoa hồng (vì không thuộc link được share).

#### Output mong đợi trên Mobile App (`APRP-Mobile`):
- **Phía Người giới thiệu A:**
  - Mở menu Tài khoản -> Chọn **"Ví của tôi & Tiếp thị liên kết"**.
  - Phần banner ví: Dòng *"Chờ duyệt: 1.800đ"*.
  - Tab "Tiếp thị liên kết" -> Lịch sử hoa hồng:
    - Card: `Bắp Cải Tươi Đà Lạt`, `Đơn #1001`.
    - Hoa hồng: `+1.800đ` (xanh lá).
    - Dưới có badge vàng: `Chờ quyết toán`.

#### Output tại Database (`APRP-BE`):
- Bảng `referral_commissions`: Tạo đúng 1 dòng:
  - `attribution_id = [ID attribution]`
  - `order_id = [ID đơn hàng]`, `order_detail_id = [ID dòng sản phẩm 701]`
  - `referrer_id = [ID Buyer A]`, `buyer_id = [ID Buyer B]`
  - `commission_rate = 2.0`
  - `base_amount = 90000.00`
  - `commission_amount = 1800.00`
  - `status = 'PENDING'`
  - `settled_at = NULL`
- Bảng `referral_attributions`:
  - `converted_order_count = 1` (tăng từ 0 lên 1).
  - `status = 'ACTIVE'` (chưa chạm ngưỡng 3).

---

### TC-REF-05: Đơn hàng bị huỷ hoặc hết hạn PayOS (Huỷ PENDING & Trả lại lượt)

- **Mục tiêu:** Đơn hàng có hoa hồng PENDING bị huỷ trước khi giao hoặc hết hạn thanh toán PayOS. Hệ thống phải chuyển hoa hồng sang `CANCELLED` và trả lại lượt chuyển đổi cho Attribution.
- **Tiền điều kiện:** Đơn hàng từ TC-REF-04 đang ở trạng thái `AWAITING_PAYMENT` hoặc `PENDING`.
- **Input:** Khách huỷ đơn hoặc Webhook PayOS báo hết hạn thanh toán.

#### Các bước thực hiện:
1. Đơn hàng chuyển sang `CANCELLED`.
2. Kiểm tra lại màn hình ví của Buyer A.

#### Output mong đợi trên Web Frontend (`APRP-FE`):
- Tại `/vi-cua-toi` của Buyer A:
  - Thẻ "HOA HỒNG ĐANG CHỜ DUYỆT": Giảm trừ `-1.800đ` (về 0đ).
  - Bảng "Lịch sử hoa hồng chi tiết": Bản ghi đơn `#1001` đổi badge sang màu đỏ: **"Đã hủy"**.

#### Output mong đợi trên Mobile App (`APRP-Mobile`):
- Tại màn hình `/buyer/wallet`:
  - Chờ duyệt trở về `0đ`.
  - Dòng hoa hồng đơn `#1001` hiển thị badge đỏ: **"Đã hủy"**.

#### Output tại Database (`APRP-BE`):
- Bảng `referral_commissions`:
  - `status = 'CANCELLED'`.
- Bảng `referral_attributions`:
  - `converted_order_count = 0` (giảm từ 1 về 0).
  - `status = 'ACTIVE'` (phục hồi nếu còn trong 7 ngày).

---

### TC-REF-06: Quyết toán đơn hàng & Cộng tiền tự động vào Ví Người mua (`TYPE_BUYER`)

- **Mục tiêu:** Khi đơn hàng giao thành công và hoàn tất đối soát 24h, scheduler hoặc admin gọi `settleOrder()`. Tiền hoa hồng 1.800đ phải được cộng tự động vào ví `TYPE_BUYER` của Buyer A, tạo bút toán `REFERRAL_COMMISSION` duy nhất.
- **Tiền điều kiện:** Đơn hàng ở trạng thái `DELIVERED`, hoa hồng của đơn đang `PENDING`.
- **Input:** Hệ thống thực hiện quyết toán đơn hàng `settleOrder(orderId)`.

#### Các bước thực hiện:
1. Kích hoạt quyết toán đơn hàng.
2. Kiểm tra log sự kiện: `OrderSettledEvent` được phát ra sau commit transaction chính.
3. Listener `ReferralSettlementListener` thực thi `creditReferralCommission`.

#### Output mong đợi trên Web Frontend (`APRP-FE`):
- **Tại `/vi-cua-toi` của Buyer A:**
  - **Thẻ SỐ DƯ KHẢ DỤNG:** Tăng đúng `+1.800đ`.
  - **Thẻ HOA HỒNG ĐANG CHỜ DUYỆT:** Giảm `-1.800đ` (chuyển sang số dư).
  - **Thẻ TỔNG THU NHẬP ĐÃ NHẬN:** Tăng `+1.800đ`.
  - **Tab "Tiếp thị liên kết":** Trạng thái hoa hồng đơn đổi từ "Chờ quyết toán" sang badge xanh: **"Đã vào ví"**.
  - **Tab "Tổng quan ví & Giao dịch":** Bảng biến động có dòng:
    - Loại giao dịch: `Cộng tiền hoa hồng tiếp thị liên kết`.
    - Số tiền: `+1.800đ` (màu xanh lá).
    - Mã đơn liên quan: `#1001`.

#### Output mong đợi trên Mobile App (`APRP-Mobile`):
- **Tại màn hình `/buyer/wallet` của Buyer A:**
  - Banner số dư khả dụng tăng `+1.800đ`.
  - Tab "Tiếp thị liên kết":
    - Thống kê "TỔNG HOA HỒNG": Tăng `+1.800đ`.
    - Dòng hoa hồng đơn `#1001` có badge xanh: `Đã vào ví`.
  - Tab "Biến động số dư":
    - Xuất hiện giao dịch mới có icon mũi tên xanh `ArrowDownLeft`.
    - Tiêu đề: `Hoa hồng tiếp thị liên kết`.
    - Giá trị: `+1.800đ`.

#### Output tại Database (`APRP-BE`):
- Bảng `referral_commissions`:
  - `status = 'COMPLETED'`
  - `settled_at = [Thời gian hiện tại]`
- Bảng `wallets` (của Buyer A, loại `TYPE_BUYER`):
  - `balance = balance + 1800.00`
  - `total_revenue_all_time = total_revenue_all_time + 1800.00`
- Bảng `wallet_transactions`: Thêm 1 dòng duy nhất:
  - `wallet_id = [ID ví của Buyer A]`
  - `type = 'REFERRAL_COMMISSION'`
  - `amount = 1800.00`
  - `referral_commission_id = [ID của bản ghi referral_commissions]` (Ràng buộc UNIQUE chống trùng).

---

### TC-REF-07: Đơn hàng khiếu nại hoàn tiền một phần (Partial Return & Refund)

- **Mục tiêu:** Người mua khiếu nại hoàn trả 2kg bắp cải trên tổng 4kg đã mua. Hệ thống tính lại tỷ lệ hoàn và giảm trừ tiền hoa hồng tương ứng.
- **Tiền điều kiện:** 
  - Đơn hàng 4kg bắp cải (tổng 100.000đ, base 90.000đ, hoa hồng gốc 1.800đ).
  - Người mua gửi `ReturnRequest` cho 2kg, số tiền hoàn `refundAmount = 50.000đ`.
- **Input:** Shop và Admin duyệt khiếu nại hoàn tiền thành công (`ReturnRequest.status = 'COMPLETED'`).
  - Tỷ lệ hoàn: $\text{refundRatio} = \frac{50.000}{100.000} = 50\%$.
  - Tỷ lệ còn lại: $1 - 0.5 = 50\%$.
  - Hoa hồng sau điều chỉnh: $1.800đ \times 50\% = 900đ$.

#### Các bước thực hiện:
1. Duyệt hoàn tiền thành công cho yêu cầu khiếu nại.
2. Chạy quyết toán đơn hàng `settleOrder()`.

#### Output mong đợi trên Web Frontend (`APRP-FE`):
- Hoa hồng cộng vào ví của Buyer A chỉ là `+900đ` (thay vì 1.800đ).
- Bảng chi tiết hoa hồng ghi nhận `900đ` trạng thái **"Đã vào ví"**.

#### Output mong đợi trên Mobile App (`APRP-Mobile`):
- Màn hình ví hiển thị biến động cộng: `+900đ`.

#### Output tại Database (`APRP-BE`):
- Bảng `referral_commissions`:
  - `commission_amount = 900.00` (đã cập nhật giảm trừ).
  - `status = 'COMPLETED'`.
- Bảng `wallet_transactions`:
  - `amount = 900.00`.

---

### TC-REF-08: Chặn tự giới thiệu (Self-referral Protection)

- **Mục tiêu:** Đảm bảo người dùng không thể tự click/claim link của chính mình hoặc chủ shop không thể làm affiliate cho chính gian hàng của mình.
- **Tiền điều kiện:** Tài khoản Buyer A đã tạo link giới thiệu `aB3x9kL1`.
- **Input:** Buyer A đang đăng nhập trên trình duyệt và tự mở link `https://capnong.com/ref/aB3x9kL1` rồi bấm mua hàng.

#### Các bước thực hiện:
1. Buyer A đăng nhập và truy cập link tiếp thị của chính mình.
2. Hệ thống gọi `POST /referrals/claim`.

#### Output mong đợi trên Web Frontend (`APRP-FE`):
- Web không tạo pending và không kích hoạt claim cho chính mình.
- Không có bất kỳ dòng attribution nào được tạo.
- Khi Buyer A mua hàng, giá trị đơn không tạo dòng hoa hồng nào cho chính Buyer A.

#### Output mong đợi trên Mobile App (`APRP-Mobile`):
- Mobile phát hiện `referrer_id == user.id`, bỏ qua không gọi claim.

#### Output tại Database (`APRP-BE`):
- API trả mã lỗi: `400 Bad Request` với JSON Error:
  ```json
  {
    "code": 18002,
    "message": "Không thể tự giới thiệu cho chính mình"
  }
  ```
- Bảng `referral_attributions`: **Không có bản ghi nào được tạo**.

---

### TC-REF-09: Giới hạn tối đa 3 đơn chuyển đổi trên 1 Attribution (Capped Multi-Conversion)

- **Mục tiêu:** Trong cửa sổ 7 ngày, Buyer B mua liên tiếp 4 đơn hàng. Kiểm tra 3 đơn đầu được tính hoa hồng, đơn thứ 4 attribution chuyển sang `EXHAUSTED` và không tính thêm hoa hồng.
- **Tiền điều kiện:** Buyer B có Attribution `ACTIVE` cho sản phẩm `ID=701` với `max_conversions = 3`.
- **Input:** Buyer B đặt thành công lần lượt: Đơn 1, Đơn 2, Đơn 3, Đơn 4 trong vòng 3 ngày.

#### Các bước thực hiện:
1. Đặt Đơn 1 -> Kiểm tra attribution `converted_order_count = 1`.
2. Đặt Đơn 2 -> Kiểm tra attribution `converted_order_count = 2`.
3. Đặt Đơn 3 -> Kiểm tra attribution `converted_order_count = 3`, trạng thái đổi sang `EXHAUSTED`.
4. Đặt Đơn 4 -> Hệ thống tìm attribution hợp lệ (`status = 'ACTIVE'`).

#### Output mong đợi trên Web Frontend & Mobile:
- Đơn 1, 2, 3: Đều xuất hiện trong danh sách hoa hồng `PENDING` của Buyer A.
- Đơn 4: Không xuất hiện trong danh sách hoa hồng của Buyer A (bộ đếm đơn dừng ở 3).

#### Output tại Database (`APRP-BE`):
- Sau Đơn 3: Bảng `referral_attributions` có:
  - `converted_order_count = 3`
  - `status = 'EXHAUSTED'`
- Tại Đơn 4: Câu truy vấn `findFirstByBuyer_UserIdAndTargetTypeAndTargetIdAndStatus` trả về `Optional.empty()` -> Không tạo bản ghi trong `referral_commissions`.

---

### TC-REF-10: Hết hạn cửa sổ 7 ngày (Expired Attribution Window)

- **Mục tiêu:** Kiểm tra đơn hàng đặt sau khi attribution đã hết hạn 7 ngày sẽ không được tính hoa hồng.
- **Tiền điều kiện:** Attribution được tạo vào ngày `01/11/2026`, hạn chót là `08/11/2026`.
- **Input:** Buyer B đặt hàng vào ngày `09/11/2026` (ngày thứ 8) mà không click lại link mới.

#### Các bước thực hiện:
1. Cập nhật ngày hết hạn của attribution về quá khứ trong DB test: `UPDATE referral_attributions SET expires_at = NOW() - INTERVAL 1 HOUR WHERE id = ...`.
2. Buyer B tiến hành tạo đơn hàng mới.

#### Output mong đợi trên Web Frontend & Mobile:
- Đơn hàng được tạo thành công bình thường.
- Không phát sinh dòng hoa hồng nào cho Buyer A.

#### Output tại Database (`APRP-BE`):
- Bảng `referral_commissions`: Không tạo bản ghi cho đơn hàng này.
- Bảng `referral_attributions`: Bản ghi cũ giữ nguyên hoặc chuyển sang `EXPIRED`.

---

### TC-REF-11: Chống Spam Click (Rate Limiting Click)

- **Mục tiêu:** Kiểm tra cơ chế giới hạn 10 click / IP / phút nhằm chống bot spam làm tràn DB và sai lệch thống kê.
- **Tiền điều kiện:** Địa chỉ IP `113.160.20.10`.
- **Input:** Gửi liên tiếp 15 request `POST /referrals/clicks` trong vòng 10 giây từ cùng một IP.

#### Các bước thực hiện:
1. Chạy script hoặc công cụ Postman gửi 15 request liên tục tới `/referrals/clicks`.

#### Output mong đợi trên UI:
- 10 click đầu tiên ghi nhận bình thường (HTTP 200).
- Từ click thứ 11: Backend từ chối với HTTP 429. UI client vẫn hoạt động mượt mà, nuốt lỗi ngầm không làm đơ giao diện của người dùng.

#### Output tại Database (`APRP-BE`):
- Bảng `referral_clicks`: Chỉ lưu đúng 10 bản ghi đầu tiên, không bị ghi đè rác.

---

### TC-REF-12: Quản lý & Xem thống kê trên Ví của tôi (Dashboard & Lịch sử)

- **Mục tiêu:** Kiểm tra giao diện người dùng hiển thị trực quan, đầy đủ số liệu thống kê, danh sách link đã tạo và lịch sử biến động.
- **Tiền điều kiện:** Buyer A đã có 2 link tiếp thị, 15 lượt click, 2 đơn thành công, tổng hoa hồng nhận 35.000đ, đang chờ duyệt 5.000đ.
- **Input:** Buyer A truy cập màn hình Ví.

#### Output mong đợi trên Web Frontend (`APRP-FE` - `/my-wallet`):
1. **Thanh Header:** Tiêu đề "Ví của tôi & Tiếp thị", nút "Làm mới", nút "Rút tiền".
2. **3 Thẻ số dư chính:**
   - Số dư khả dụng (màu xanh gradient đậm): format tiền rõ ràng (VND).
   - Tổng thu nhập đã nhận.
   - Hoa hồng đang chờ duyệt: thẻ nền vàng hổ phách nổi bật.
3. **Tab "Tiếp thị liên kết (Affiliate)":**
   - 4 ô thống kê: `LƯỢT CLICK (15)`, `ĐƠN THÀNH CÔNG (2)`, `TỔNG HOA HỒNG (35.000đ)`, `TỶ LỆ HOA HỒNG (2.0%)`.
   - Khung "Liên kết tiếp thị của tôi": Danh sách 2 card sản phẩm, mỗi card có hình ảnh, tên, mã code, số lượt click và nút "Sao chép link".
   - Khung "Lịch sử hoa hồng chi tiết": Bảng dữ liệu có phân trang, hiển thị từng dòng đơn hàng, tên nông sản, giá tính hoa hồng, số tiền hoa hồng `+...đ` và badge trạng thái rõ ràng.

#### Output mong đợi trên Mobile App (`APRP-Mobile` - `/buyer/wallet`):
1. **Tiêu đề & Nút Back:** Thanh Header chuẩn mobile, nút quay lại trang Tài khoản.
2. **Banner Ví xanh lá:** Số dư khả dụng, dòng ghi chú "Đã rút" và "Chờ duyệt: 5.000đ".
3. **Thanh chuyển Tab (Segmented Control):**
   - Tab "Tiếp thị liên kết":
     - Grid 4 ô chỉ số bo góc mềm mại.
     - Danh sách liên kết tiếp thị: Card bo viền có ảnh, tên, mã code, nút "Sao chép" và nút "Chia sẻ" native.
     - Lịch sử hoa hồng: Danh sách cuộn mượt mà có badge phân biệt màu sắc (`Đã vào ví` - xanh, `Chờ quyết toán` - vàng hổ phách, `Đã hủy` - đỏ).
   - Tab "Biến động số dư": Danh sách giao dịch nạp/rút/hoa hồng kèm icon đổi màu theo chiều thu chi.

