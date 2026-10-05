# Tài Liệu Đặc Tả Luồng Xử Lý Voucher Dành Cho Frontend (Voucher Flow)

Tài liệu này cung cấp hướng dẫn chi tiết về logic Voucher để team Frontend (FE) có thể thiết kế giao diện (UI) và gọi API đồng bộ với Backend (BE).

---

## 1. Phân Loại Voucher & Giao Diện Hiển Thị (UI)

Hệ thống cung cấp 2 hình thức Voucher, được phân biệt bằng trường `isPublic` khi tạo. FE cần thiết kế 2 vùng UI khác nhau cho chúng:

### A. Voucher Công Khai (Public Voucher)
- **Bản chất:** Là các mã hiển thị công khai trên sàn hoặc trong trang chủ của Shop, ai cũng có thể thấy và lấy.
- **Tạo mã (Create API):** FE truyền `"isPublic": true` (Mặc định).
- **Lấy danh sách (GET API):** Gọi API `GET /vouchers/public`. BE sẽ chỉ trả về những mã public này.
- **Hành vi UI:**
  - FE render thành dạng thẻ (Card) có nút **Lưu (Claim)**.
  - Khi User bấm **Lưu**, gọi API `POST /vouchers/collect/{voucherId}`. Hệ thống sẽ cất mã này vào "Ví Voucher" của User.
  - Trong trang Checkout, FE hiển thị các mã đã lưu để User tick chọn.

### B. Voucher Ẩn (Hidden Voucher / Nhập mã thủ công)
- **Bản chất:** Là các mã đặc biệt (mã KOL, sự kiện độc quyền). Người dùng phải biết mã ký tự (VD: `VIP100K`) mới xài được. Mã này bị giấu khỏi mọi danh sách.
- **Tạo mã (Create API):** FE truyền `"isPublic": false`. BE sẽ tự động lưu `autoCreatedReason = "HIDDEN"`.
- **Hành vi UI:**
  - KHÔNG hiển thị dạng thẻ.
  - FE thiết kế một **Ô input (Textbox)** có nút **"Áp dụng"**.
  - Khi User gõ mã `VIP100K` và bấm Áp dụng:
    - **Cách 1 (Lưu vào ví trước):** FE gọi API `POST /vouchers/collect/code?code=VIP100K`. Sau khi báo thành công, mã xuất hiện trong ví.
    - **Cách 2 (Áp dụng trực tiếp lúc Checkout):** FE chỉ cần nhét đoạn String `"VIP100K"` vào mảng `voucherCodes` của API Checkout. BE sẽ tự động xác thực và áp dụng luôn mà không cần bước "Lưu".

---

## 2. Các Loại Giảm Giá (VoucherType) & Logic Tính Toán Tiền

FE cần nắm rõ 3 loại `VoucherType` để hiển thị đúng số tiền được giảm ở mục "Chi tiết thanh toán":

1. **`FIXED_AMOUNT` (Giảm tiền mặt)**
   - Trừ thẳng số tiền `discountValue` vào tổng tiền hàng (`goodsAmount`).
   - Nếu tiền giảm > tiền hàng => FE hiển thị tiền hàng = 0đ (Không để số âm).
2. **`PERCENTAGE` (Giảm phần trăm)**
   - Lấy tiền hàng × `discountValue` (%).
   - Nếu số tiền giảm tính ra vượt quá `maxDiscount`, FE hiển thị số tiền giảm bằng đúng `maxDiscount`.
3. **`FREE_SHIPPING` (Miễn phí vận chuyển)**
   - Trừ thẳng số tiền giảm vào **Phí vận chuyển (`shippingFee`)**, TUYỆT ĐỐI KHÔNG trừ vào tiền hàng.
   - Nếu tiền giảm > phí ship => Phí ship = 0đ.

---

## 3. Luồng Checkout (Xem Trước & Đặt Hàng)

### Bước 1: Preview Checkout (Xem trước đơn hàng)
Khi User thay đổi giỏ hàng hoặc tick chọn mã Voucher, FE không được tự tính tiền mà phải gọi BE để lấy con số chính xác nhất (vì BE có cơ chế chẻ tiền System Voucher siêu phức tạp).
- **API:** `POST /orders/preview`
- **Payload (Body):**
  ```json
  {
    "shippingAddress": "123 Đường ABC, HCM",
    "voucherCodes": ["FREESHIP50", "SHOP30K", "VIP100K"], // Mảng các mã code User muốn xài
    "items": [
      { "productId": 1, "quantity": 2 },
      { "mysteryBoxId": 5, "quantity": 1 }
    ]
  }
  ```
- **Response trả về:** BE sẽ trả về một Object `CheckoutPreviewResponse` chứa sẵn toàn bộ phép tính:
  - `totalGoodsAmount`: Tổng tiền hàng gốc.
  - `totalShippingFee`: Tổng phí ship gốc.
  - `totalVoucherDiscount`: Tổng tiền được giảm.
  - `finalTotalAmount`: Tổng tiền cuối cùng User phải trả.
  - **`shopOrders` (Mảng các đơn lẻ theo từng Shop):** Bên trong mỗi đơn lẻ, BE sẽ chia sẵn `voucherDiscount` và `totalAmount` của riêng Shop đó. FE chỉ việc bóc ra mà render UI.

### Bước 2: Đặt hàng (Create Order)
Sau khi User nhìn thấy số tiền ở bước Preview và bấm "Thanh toán":
- **API:** `POST /orders/create`
- **Payload (Body):** Giống hệt API Preview nhưng có thêm `paymentMethod`, `recipientName`, `recipientPhone` và mảng `cartItemIds` (danh sách ID của CartItem để BE tự xoá khỏi giỏ hàng).
- **Xử lý ngầm của BE (FE không cần lo):**
  - BE sẽ tự động kiểm tra xem mã Voucher đã hết lượt chưa (`usageLimit`).
  - BE đếm xem User này đã xài mã này bao nhiêu lần rồi (`perUserLimit`).
  - BE ghi lại lịch sử giảm giá chính xác đến từng đồng cho từng Voucher vào DB.

---

## 4. Bắt Lỗi & Cảnh Báo (Error Handling)

Khi User nhập sai mã hoặc mã không đủ điều kiện, BE sẽ trả về các mã lỗi sau để FE có thể bung Toast Message:
- `VOUCHER_NOT_FOUND`: Mã không tồn tại, hoặc nhập sai mã.
- Lỗi giới hạn (Giới hạn lượt dùng, Giới hạn cá nhân, Chưa đạt giá trị đơn tối thiểu): Sẽ bị loại bỏ (skip) âm thầm trong lúc tính toán. FE có thể so sánh mảng mã truyền đi và số tiền giảm trả về (nếu truyền mã mà tiền giảm = 0 tức là mã không thỏa mãn) hoặc thiết kế gọi API riêng để check.
