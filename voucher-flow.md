# Tài Liệu Đặc Tả Luồng Xử Lý Voucher Dành Cho Frontend & Mobile (Voucher Flow)

Tài liệu này cung cấp hướng dẫn chi tiết về logic Voucher để team Frontend (FE Web) và Mobile có thể thiết kế giao diện (UI) và gọi API đồng bộ với Backend (BE).

---

## 1. Phân Loại Voucher & Giao Diện Hiển Thị (UI)

Hệ thống cung cấp 2 hình thức Voucher, được phân biệt bằng trường `isPublic` khi tạo:

### A. Voucher Công Khai (Public Voucher)
- **Bản chất:** Là các mã hiển thị công khai trên sàn hoặc trong trang chủ của Shop, ai cũng có thể thấy và lấy.
- **Tạo mã (Create API):** Truyền `"isPublic": true` (Mặc định).
- **Lấy danh sách (GET API):** Gọi API `GET /vouchers`. BE sẽ trả về danh sách các mã đang public, còn hiệu lực và chưa hết lượt lưu.
- **Hành vi UI:**
  - FE/Mobile render thành dạng thẻ (Card) có nút **Lưu (Claim)**.
  - Khi User bấm **Lưu**, gọi API `POST /vouchers/{voucherId}/collect`. Hệ thống sẽ cất mã này vào "Ví Voucher" của User (`VoucherUser`).
  - Trong trang Checkout hoặc Giỏ hàng, FE/Mobile hiển thị các mã đã lưu để User tick chọn.

### B. Voucher Ẩn (Hidden Voucher / Nhập mã thủ công)
- **Bản chất:** Là các mã đặc biệt (mã KOL, sự kiện độc quyền). Người dùng phải biết mã ký tự (VD: `VIP100K`) mới xài được. Mã này bị giấu khỏi mọi danh sách public.
- **Tạo mã (Create API):** Truyền `"isPublic": false`. BE sẽ tự động gán `autoCreatedReason = "HIDDEN"`.
- **Hành vi UI:**
  - KHÔNG hiển thị dạng thẻ danh sách ban đầu.
  - Thiết kế một **Ô input (Textbox)** có nút **"Áp dụng"**.
  - Khi User gõ mã `VIP100K` và bấm Áp dụng:
    - Client gọi API `POST /vouchers/collect-by-code?voucherCode=VIP100K`.
    - Sau khi BE báo thành công, client tải lại ví voucher (`GET /users/me/vouchers`) và tự động tick chọn mã đó vào danh sách `voucherCodes` để preview đơn hàng.

---

## 2. Các Loại Giảm Giá (VoucherType) & Logic Tính Toán Tiền

1. **`FIXED_AMOUNT` (Giảm tiền mặt)**
   - Trừ thẳng số tiền `discountValue` vào tổng tiền hàng (`goodsAmount`).
   - Nếu tiền giảm > tiền hàng => tiền hàng = 0đ (Không để số âm).
2. **`PERCENTAGE` (Giảm phần trăm)**
   - Lấy tiền hàng × `discountValue` (%).
   - Nếu số tiền giảm tính ra vượt quá `maxDiscount`, tiền giảm lấy bằng đúng `maxDiscount`.
3. **`FREE_SHIPPING` (Miễn phí vận chuyển)**
   - Trừ thẳng số tiền giảm vào **Phí vận chuyển (`shippingFee`)**, TUYỆT ĐỐI KHÔNG trừ vào tiền hàng.
   - Nếu tiền giảm > phí ship => Phí ship = 0đ.

---

## 3. Quy Tắc Cộng Dồn Voucher Lúc Checkout (Shopee Stacking Rules)

Hệ thống hỗ trợ áp dụng đồng thời nhiều voucher trong 1 đơn hàng theo nguyên tắc:
- Tối đa **1 mã Freeship** (VoucherTarget = `SHIPPING` hoặc VoucherType = `FREE_SHIPPING`).
- Tối đa **1 mã Voucher Sàn** (VoucherScope = `PLATFORM`, `SYSTEM`, `WELCOME` hoặc creatorRole = `ADMIN`).
- Tối đa **1 mã Voucher của từng Shop** có mặt trong đơn hàng (VoucherScope = `SHOP`).
- Khi người dùng chọn thêm một mã mới cùng loại, mã cũ cùng loại sẽ tự động được thay thế.

---

## 4. Luồng Checkout (Xem Trước & Đặt Hàng)

### Bước 1: Preview Checkout (Xem trước đơn hàng)
Khi User thay đổi giỏ hàng hoặc tick chọn mã Voucher, Client gửi danh sách mã lên BE để tính toán số tiền chính xác nhất:
- **API:** `POST /orders/preview`
- **Payload (Body):**
  ```json
  {
    "shippingAddress": "123 Đường ABC, HCM",
    "voucherCodes": ["FREESHIP50", "SHOP30K", "VIP100K"],
    "items": [
      { "productId": 1, "quantity": 2 },
      { "mysteryBoxId": 5, "quantity": 1 }
    ]
  }
  ```
- **Response trả về:** `CheckoutPreviewResponse`:
  - `totalGoodsAmount`: Tổng tiền hàng gốc.
  - `totalShippingFee`: Tổng phí ship gốc.
  - `totalVoucherDiscount`: Tổng tiền được giảm (được làm tròn chuẩn VNĐ).
  - `finalTotalAmount`: Tổng tiền cuối cùng User phải trả.
  - `shopOrders`: Mảng các đơn lẻ theo từng Shop (chứa sẵn `voucherDiscount` và `totalAmount` của riêng từng Shop).

### Bước 2: Đặt hàng (Create Order)
- **API:** `POST /orders/create`
- **Payload (Body):** Giống API Preview kèm theo `paymentMethod`, `recipientName`, `recipientPhone`, `cartItemIds`.
- **BE tự động xử lý:**
  - Kiểm tra điều kiện tồn kho, hạn sử dụng, điều kiện đơn tối thiểu.
  - Trừ lượt dùng trong ví (`quantityUsed + 1`) và tăng số lần dùng của voucher (`usedCount + 1`).
  - Lưu chi tiết số tiền giảm thực tế vào `VoucherUsage` gắn với đơn hàng.
  - Trả về `voucherDiscountAmount` chính xác trong `OrderResponse` (cả khi tạo đơn lẫn khi tra cứu lại lịch sử đơn hàng sau này).

### Bước 3: Hoàn trả Voucher khi huỷ đơn hoặc giao thất bại
- Khi đơn hàng bị huỷ (`CANCELLED`) hoặc giao hàng thất bại (`FAILED`):
  - BE tự động hoàn lại lượt dùng cho Voucher (`usedCount - 1`) và hoàn lại số lượt trong ví của khách (`quantityUsed - 1`).
  - Đảm bảo khách hàng không bị mất voucher oan khi đơn hàng không thành công.

---

## 5. Bắt Lỗi & Cảnh Báo (Error Handling)

- `VOUCHER_NOT_FOUND` (9002): Mã không tồn tại hoặc nhập sai ký tự.
- `VOUCHER_INVALID` (9003): Mã đã hết hạn sử dụng hoặc đang tạm dừng hoạt động.
- `VOUCHER_OUT_OF_STOCK` (9004): Mã đã hết lượt thu thập trên toàn hệ thống.
- `VOUCHER_LIMIT_REACHED` (9005): Người dùng đã lưu đạt số lượt tối đa cho phép của mã này.
- `VOUCHER_CANNOT_DELETE_ACTIVE` (9006): Voucher đang hoạt động, cần tạm dừng trước khi xóa.
- `VOUCHER_CANNOT_DELETE_USED` (9007): Không thể xóa voucher đã phát sinh lượt sử dụng trong các đơn hàng (chỉ cho phép tạm dừng để bảo vệ đối soát tài chính).
