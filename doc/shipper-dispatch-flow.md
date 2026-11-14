# Luồng Shipper: điều phối, ký quỹ COD, bằng chứng giao hàng

Bổ sung cho [`order-and-tracking-flow.md`](order-and-tracking-flow.md) và [`wallet-settlement-flow.md`](wallet-settlement-flow.md). Nếu tài liệu khác code thì code là chuẩn.

```
READY ──(điều phối: quét shipper online quanh vườn, mời qua STOMP + thông báo)──► shipper bấm nhận
      ──(khoá đơn, kiểm tra online / 1 đơn [giao xuôi + hoàn] / ví)──► SHIPPING ──(Đã lấy hàng)──► PICKED_UP
      ──(GPS /app/shipper/location → /topic/order/{id}/location, kèm distanceKm + estimatedMinutes)
      ├─► DELIVERED (bắt buộc ảnh PoD) ──► CỘNG NGAY CƯỚC GIAO vào ví khả dụng Shipper (không cần chờ 24h)
      │                               ──► COMPLETED (khách bấm nhận hoặc hết 24h) ──► Quyết toán tiền hàng Shop & hoa hồng Affiliate
      └─► FAILED (lý do + số cuộc gọi + ảnh) ──► hoàn kho, gỡ ký quỹ, ghi hoàn tiền nếu đã trả online
```

## 1. Trực tuyến và heartbeat

- `POST /shippers/status/online {latitude, longitude}`: bật nhận đơn. App gọi lại **mỗi 60s** khi đang nhận đơn (heartbeat).
- `POST /shippers/status/offline`: tắt nhận đơn. Không xoá dòng `shipper_locations` để vẫn theo dõi được đơn đang giao.
- Online = `shipper_locations.online = true` và `last_seen_at` trong `capnong.dispatch.heartbeat-timeout-seconds` (mặc định 300s). Scheduler tự chuyển offline nếu mất heartbeat.
- Gửi vị trí qua STOMP cũng cập nhật `last_seen_at`, nhưng không tự bật online.
- Đã bỏ `POST /shippers/{shipperId}/status/online|offline` vì không có phân quyền.

## 2. Điều phối (Assign delivery task)

- Khi Shop báo READY (`PUT /orders/{id}/ready`), sau khi commit chạy **đợt 1**.
- Mỗi đợt quét shipper **đang online, ACTIVE, không có đơn SHIPPING/PICKED_UP, chưa được mời**, trong bán kính `capnong.dispatch.radii-km` (mặc định `3,6,10` km, tính quãng đường ước lượng = chim bay × 1,25 từ shipper tới vườn).
- Đơn COD chỉ mời shipper có **số dư khả dụng ≥ tổng tiền đơn**. Shipper khác vẫn thấy đơn trong danh sách.
- Mỗi lời mời lưu vào `dispatch_offers` (unique đơn + shipper), gửi:
  - thông báo DB + `/user/queue/notifications` (`type = NEW_ORDER_OFFER`, `referenceId = orderId`);
  - `/user/queue/shipper-offers` với `ShipperOfferMessage { type: NEW_ORDER, orderId, shopName, distanceToShopKm, codAmount, ... }`.
- Scheduler (`capnong.scheduler.dispatch-delay-ms`, 20s) mở đợt tiếp theo sau `wave-interval-seconds` (60s). Hết đợt thì quét lại bán kính lớn nhất (mời shipper mới online).
- Quá `no-taker-alert-minutes` (15 phút) chưa ai nhận: báo Shop + Admin 1 lần (`DISPATCH_TIMEOUT`).
- Có người nhận: các shipper đã được mời nhận `ORDER_TAKEN` để gỡ đơn khỏi danh sách.
- Mô hình mời: phát cho nhiều shipper gần, **ai bấm nhận trước được đơn** (kiểu Ahamove/Lalamove).

`GET /orders/ready?page&size&lat&lng`: bắt buộc online (7006). Trả đơn READY chưa có người nhận trong 15 km quanh vị trí gửi lên (hoặc vị trí gần nhất), gần nhất trước, kèm `distanceToShopKm`. Trên Mobile App, vị trí GPS được lắng nghe liên tục (`watchPositionAsync`), tự động tải lại danh sách đơn khi shipper di chuyển (>100m) và cập nhật toạ độ vệ tinh mới nhất khi vuốt làm mới (pull-to-refresh).

## 3. Nhận đơn `PUT /orders/{id}/ship`

Kiểm tra theo thứ tự (lỗi thì rollback toàn bộ):

| Điều kiện | Mã lỗi |
|---|---|
| Khoá dòng đơn (`SELECT ... FOR UPDATE`), đơn đã có shipper khác | 7005 `ORDER_ALREADY_TAKEN` |
| Đơn không ở READY | 7002 |
| Shipper chưa online | 7006 `SHIPPER_NOT_ONLINE` |
| Đang có đơn giao xuôi (SHIPPING/PICKED_UP) HOẶC đơn hoàn trả (RETURN_SHIPPING/RETURN_PICKED_UP) | 7004 `SHIPPER_HAS_ACTIVE_ORDER` |
| Ví âm (nợ COD luồng cũ) | 16014 `WALLET_HAS_DEBT` |
| Đơn COD, số dư khả dụng < `totalAmount` | 16013 `SHIPPER_COD_BALANCE_INSUFFICIENT`, `result = { required, available, missing }` |

> **Ràng buộc "1 tài xế - 1 đơn tại 1 thời điểm" (Cross-task checking):**
> Shipper chỉ được phép thực thi **duy nhất 1 nhiệm vụ giao vận** tại một thời điểm trên toàn hệ thống.
> Kiểm tra chéo đồng thời 2 bảng:
> 1. `orders`: `existsByShipper_IdAndStatusIn(shipperId, [SHIPPING, PICKED_UP])`
> 2. `return_requests`: `existsByShipper_IdAndStatusIn(shipperId, [RETURN_SHIPPING, RETURN_PICKED_UP])`
> - Khi nhận đơn giao hàng mới (`PUT /orders/{id}/ship`): chặn nếu đang có đơn giao xuôi hoặc đơn hoàn trả đang thực thi.
> - Khi nhận đơn hoàn hàng mới (`POST /return-requests/{id}/shipper-accept`): cũng áp dụng cùng kiểm tra chéo, chặn nếu đang giao đơn hàng thông thường hoặc đơn hoàn khác.

Thành công: gán shipper, SHIPPING, giữ chỗ COD, tạm giữ cước (`SHIPPER_INCOME_HOLD`), báo Buyer + Shop.

## 4. Ký quỹ COD & Giải phóng cước giao ngay khi hoàn thành (DELIVERED)

Mục tiêu:
1. Shipper không thể cầm tiền mặt đơn lớn rồi không nộp về sàn (ký quỹ COD).
2. Công sức lao động của Shipper được bảo vệ và thanh toán ngay khi hoàn thành chặng giao hàng (`DELIVERED`), không bị giữ lại 24h chờ đối soát đơn như doanh thu hàng của Shop.

- Cột mới `wallets.cod_reserved`. **Số dư khả dụng = `total_balance − cod_reserved`**, dùng cho nhận đơn COD và **rút tiền**.
- Nhận đơn COD: `cod_reserved += totalAmount`, `orders.cod_reserved_amount = totalAmount`, bút toán `COD_RESERVED` (amount âm = khả dụng giảm, số dư không đổi).
- Giao thành công (`DELIVERED`):
  1. `COD_RESERVE_RELEASED` + `COD_CASH_COLLECTED −totalAmount`. Tiền mặt shipper thu chính là tiền đã ứng, nên `cod_settled = true` ngay, **không cần Admin xác nhận nộp tiền**.
  2. **Giải phóng cước giao ngay (`releaseShipperFeeOnDelivered`)**: Hệ thống tự động gỡ khoản cước tạm giữ (`HOLD_RELEASED −s.shipperReceive`) và cộng cước thực nhận thẳng vào số dư khả dụng của Shipper (`SHIPPER_INCOME +s.shipperReceive`), đánh dấu `order.shipperPaidAt = now()`.
  3. Tiền hàng của Shop và hoa hồng Người mua (Affiliate) vẫn giữ nguyên trạng thái tạm giữ / chờ đối soát 24h để đảm bảo quyền khiếu nại trả hàng của Buyer. Khi Buyer xác nhận hoặc hết 24h cron `settleOrder()` chạy, hệ thống kiểm tra `order.getShipperPaidAt() == null` để đảm bảo **tính duy nhất (idempotent), không cộng tiền cước 2 lần**.
- Giao thất bại: `COD_RESERVE_RELEASED`, số dư như trước khi nhận.
- Đơn PayOS: không giữ chỗ, chỉ cần ví không âm.
- Đơn nhận trước khi có ký quỹ (`cod_reserved_amount` NULL) vẫn chạy luồng cũ: ghi nợ, Admin xác nhận qua `POST /wallets/cod/{orderId}/settle`.

Ví dụ: ví 500.000đ, đơn COD 215.000đ (200.000 hàng + 15.000 ship, shipper nhận 14.550đ sau phí sàn).
- Nhận đơn: khả dụng 285.000đ (tạm giữ 14.550đ cước ở mục frozen).
- Giao xong (`DELIVERED`): thu 215.000đ tiền mặt, gỡ ký quỹ COD, **cộng ngay 14.550đ cước vào số dư khả dụng** → ví hiển thị 299.550đ khả dụng + 215.000đ tiền mặt = tổng tài sản 514.550đ. Shipper lời đúng 14.550đ ngay khi giao xong!
- Đơn hoàn tất (`COMPLETED` sau 24h): Tiền hàng 200.000đ quyết toán cho Shop, hoa hồng cộng cho Buyer, Shipper không bị tác động thêm.

### Nạp ví

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/wallets/topups` `{amount, returnUrl, cancelUrl}` | SHIPPER, SHOP | Tạo link PayOS, `amount` từ `min-topup` (10.000) tới `max-topup` (20.000.000) |
| GET | `/wallets/topups/me` | SHIPPER, SHOP | 20 lệnh gần nhất |
| POST | `/wallets/topups/{orderCode}/confirm` | SHIPPER, SHOP, ADMIN | Hỏi PayOS, cộng ví nếu đã trả (idempotent) |

Webhook PayOS dùng chung `/payments/payos/webhook`: nếu `orderCode` thuộc `wallet_topups` thì cộng ví (`TOP_UP`), không thì xử lý như thanh toán đơn.

## 5. Bằng chứng giao hàng & Giao diện thu tiền (PoD & Payment Display)

Ảnh upload trước qua `POST /upload/image` (folder `delivery-proofs`), gửi URL.

- `PUT /orders/{id}/deliver { proofImageUrls }`: ít nhất 1 ảnh (7007).
- `PUT /orders/{id}/fail { reason, note, callAttempts, proofImageUrls }`:
  - `reason`: `KHONG_LIEN_LAC_DUOC | SAI_DIA_CHI | KHACH_TU_CHOI | KHAC` (thiếu: 7008);
  - `KHONG_LIEN_LAC_DUOC` cần `callAttempts ≥ 3` (7009); `KHAC` cần `note` (7010); ít nhất 1 ảnh (7007).
- Lưu ở `orders.delivery_proof_urls`, `fail_reason`, `fail_note`, `fail_call_attempts`, `fail_proof_urls`, `failed_at`; trả trong `OrderResponse`.

### Giao diện kết quả giao hàng & Xác nhận tiền thu (Web + Mobile):
- Endpoint `GET /shippers/location/order/{orderId}` bổ sung trường: `paymentMethod`, `codAmount`, `totalAmount`.
- **Màn hình giao hàng (`DeliveryResultModal` / `DeliveryResultSheet`):**
  - **Đơn COD**: Hiển thị thẻ màu vàng/hổ phách (amber) nổi bật: **"🏷️ ĐƠN THU TIỀN MẶT (COD): Thu đủ [XXX.XXX đ] tiền mặt của khách"**. Nhắc nhở số tiền này đã được ký quỹ trong ví tài xế.
  - **Đơn thanh toán online (PayOS / VietQR / Ví)**: Hiển thị banner màu xanh lá (emerald) nổi bật: **"✓ ĐÃ THANH TOÁN ONLINE (VIETQR): Không thu tiền mặt của khách (0 đ)"**. Giúp tài xế an tâm bàn giao hàng mà không lo nhầm lẫn việc thu tiền mặt.

## 6. Lấy hàng: đối soát mã đơn

Không đổi API (`PUT /orders/{id}/pickup`). Đối soát làm ở giao diện, cùng định dạng mã `#{orderId}` ở cả hai phía:

- **Nhà vườn** (web `pages/farmer/Orders.tsx`, mobile `ShopOrderCard`, `shop/order/[id]`): đơn CONFIRMED / READY / SHIPPING hiện khung **"Mã dán lên túi hàng #123"** để ghi/dán lên túi.
- **Tài xế** (web Dashboard, bản đồ, FakeGPS; mobile tab Đang giao): thẻ đơn hiện mã to rõ. Bấm "Đã lấy hàng" mở hộp **"Đối soát mã đơn #123 — Kiểm tra mã trên túi hàng trùng #123"**, xác nhận rồi mới gọi API.

## 7. Theo dõi realtime và bảo mật vị trí

- STOMP `/app/shipper/location` chỉ nhận từ **shipper được gán** khi đơn SHIPPING / PICKED_UP (chặn giả mạo vị trí và `gpsOff`).
- `ShipperLocationResponse` có thêm `distanceKm` (quãng còn lại tới điểm của chặng hiện tại), `estimatedMinutes` (25 km/h), `trackingActive` (cờ báo tracking đang chạy hay đã chốt), và `proofImageUrls` (danh sách ảnh PoD).
- Chặng mặc định: SHIPPING → `TO_SHOP`, PICKED_UP → `TO_BUYER`, DELIVERED/COMPLETED → `DELIVERED`, FAILED → `FAILED`.
- **Bảo mật và chống rò rỉ GPS đơn cũ:**
  - Endpoint `GET /shippers/location/order/{orderId}` được bảo vệ chặt chẽ: chỉ Buyer của đơn, Shop của đơn, chính Shipper được gán hoặc Admin mới được xem toạ độ.
  - Khi đơn đã kết thúc (`DELIVERED`, `COMPLETED`, `CANCELLED`, `FAILED`): BE **không bao giờ** trả toạ độ realtime của tài xế (tránh lộ toạ độ khi shipper bắt đầu chạy đơn khác). Thay vào đó, BE trả về toạ độ điểm nhận hàng của khách (ghim tĩnh), `distanceKm = 0`, `estimatedMinutes = 0`, `deliveryPhase = "DELIVERED"`, `trackingActive = false` và kèm danh sách ảnh PoD.
  - Khi shipper hoàn thành đơn (`markOrderAsDelivered`) hoặc báo thất bại (`markOrderAsFailed`), BE tự động gỡ `order_id` khỏi `shipper_locations` (`detachOrderFromShipperLocation`) và broadcast frame WebSocket mang `{ trackingActive: false, orderStatus: ... }` lên `/topic/order/{id}/location` để client ngắt kết nối STOMP và ẩn icon xe.
- Bản đồ mobile (`ShipperTrackingView`) hiện dòng **"Còn ~2,3 km · khoảng 6 phút"** dưới trạng thái chặng (`trackingRules.formatEta`). Dưới 1 km đổi sang mét, còn ≤ 1 phút hiện "sắp tới nơi", đã giao thì ẩn dòng km/phút và ẩn xe di chuyển. Mobile ẩn dòng này khi Buyer chưa được xem vị trí xe (chặng tới vườn).
- **Quy hoạch nền tảng:** Toàn bộ nghiệp vụ tác nghiệp của Shipper (Bật/tắt trực tuyến, GPS Tracking di chuyển, Nhận đơn theo bán kính, Đối soát mã lấy hàng, Chụp ảnh bằng chứng giao hàng, Quản lý ví tiền) được triển khai độc quyền trên ứng dụng di động **Mobile App** (`APRP-Mobile`). Web (`APRP-FE`) không dùng cho shipper tác nghiệp.

## 8. Thông báo trạng thái

Sau commit, Buyer và Shop nhận thông báo (`type = ORDER_STATUS`) khi đơn READY (Buyer), SHIPPING, PICKED_UP, DELIVERED, FAILED.

## 9. Cách tính (giải thích các con số)

Mọi phép tính khoảng cách và thời gian nằm trong `util/GeoUtils.java` ở BE. Client (web, mobile) **không tự tính**, chỉ định dạng số BE gửi về.

### 9.1. Toạ độ đầu vào

| Điểm | Lấy từ | Khi thiếu |
|---|---|---|
| Tài xế | GPS của thiết bị: heartbeat `POST /shippers/status/online` hoặc STOMP `/app/shipper/location` | Dùng vị trí cuối đã lưu trong `shipper_locations` |
| Nhà vườn | `orders.shop_latitude / shop_longitude` | Gọi **Google Geocoding** với địa chỉ shop **một lần**, lưu lại vào đơn. Geocode lỗi thì dùng toạ độ mặc định (Linh Chiểu, Thủ Đức) |
| Khách | `orders.shipping_latitude / shipping_longitude` | Như trên, toạ độ mặc định là 268 Lý Thường Kiệt, Q.10 |

Google chỉ dùng để **đổi địa chỉ thành toạ độ**. Km và số phút **không** gọi Google.

### 9.2. Khoảng cách

**Bước 1: đường chim bay (công thức haversine)**, tính khoảng cách giữa 2 điểm trên mặt cầu Trái Đất:

```
a = sin²(Δlat/2) + cos(lat1) · cos(lat2) · sin²(Δlng/2)
d_chim_bay = 2 · R · atan2(√a, √(1−a))        R = 6.371 km, góc đổi sang radian
```

**Bước 2: quy ra quãng đường thực tế**

```
km = làm_tròn_1_chữ_số(d_chim_bay × 1,25)
```

Hệ số **1,25** là ước lượng độ vòng vèo của đường phố nội thành so với đường thẳng. Thực tế thường từ 1,2 đến 1,4.

Ví dụ: tài xế (10.8600, 106.7720), vườn (10.8500, 106.7720). Chênh 0,01° vĩ độ ≈ 1,11 km chim bay → 1,11 × 1,25 ≈ **1,4 km**.

> Riêng `OrderResponse.distanceKm` (quãng nhà vườn → khách, hiện ở thẻ đơn) dùng cùng công thức nhưng **tối thiểu 0,4 km** để không hiện số quá nhỏ.

### 9.3. Số phút dự kiến (ETA)

```
phút = làm_tròn_lên(km ÷ 25 × 60)       nếu km > 0, tối thiểu 1 phút
phút = 0                                 nếu km = 0 (đã tới / đã giao)
```

**25 km/h** là tốc độ trung bình giả định của xe máy giao hàng trong thành phố, đã tính cả đèn đỏ và dừng đỗ.

| km còn lại | Phép tính | Phút |
|---|---|---|
| 0,1 | 0,1 ÷ 25 × 60 = 0,24 → lên 1 | 1 |
| 2,3 | 2,3 ÷ 25 × 60 = 5,52 → lên 6 | 6 |
| 5,0 | 5 ÷ 25 × 60 = 12 | 12 |
| 10,0 | 10 ÷ 25 × 60 = 24 | 24 |

**Điểm đến theo chặng:** đơn `SHIPPING` thì tính tới **nhà vườn**, đơn `PICKED_UP` thì tính tới **nhà khách**, đơn `DELIVERED`/`COMPLETED` thì km = 0, phút = 0.

**Tần suất:** tính lại mỗi lần tài xế gửi vị trí (mobile 4 giây một lần, web 3 giây), kèm trong tin `/topic/order/{id}/location`, nên con số giảm dần khi xe chạy.

### 9.4. Hiển thị trên bản đồ

Web `utils/eta.ts` và mobile `trackingRules.formatEta`:

| Dữ liệu từ BE | Hiển thị |
|---|---|
| `distanceKm = 2.3`, `estimatedMinutes = 6` | Còn ~2,3 km · khoảng 6 phút |
| `distanceKm = 0.42`, `estimatedMinutes = 1` | Còn ~400 m · sắp tới nơi (dưới 1 km đổi sang mét, làm tròn 50 m) |
| `distanceKm = 0` | Ẩn dòng |

### 9.5. Bán kính điều phối và danh sách đơn

Cùng công thức km ở 9.2, đo **từ vị trí tài xế tới nhà vườn**:

| Dùng ở đâu | Ngưỡng | Cấu hình |
|---|---|---|
| Đợt 1 / 2 / 3 mời tài xế | ≤ 3 / 6 / 10 km | `capnong.dispatch.radii-km` |
| Danh sách `GET /orders/ready` | ≤ 15 km, gần nhất trước | hằng số `READY_LIST_MAX_KM` |

**Mốc thời gian điều phối:**
- Scheduler chạy **20 giây** một lần (`dispatch-delay-ms`), mở đợt mới khi đợt trước đã qua **60 giây** (`wave-interval-seconds`).
- Vì vậy đợt 2 chạy khoảng giây 60–80, đợt 3 khoảng giây 120–140 sau khi đơn READY. Sau đó cứ mỗi 60–80 giây lại quét lại bán kính 10 km để mời tài xế mới online.
- Cảnh báo "chưa có tài xế" gửi khi `readyAt` đã quá **15 phút** (`no-taker-alert-minutes`), chậm nhất thêm 20 giây.

### 9.6. Trực tuyến (heartbeat)

- App gửi heartbeat **mỗi 60 giây**. BE coi là offline nếu `last_seen_at` cũ hơn **300 giây** (`heartbeat-timeout-seconds`).
- Scheduler kiểm tra mỗi 60 giây, nên tài xế tắt app ngang sẽ bị chuyển offline sau **5 đến 6 phút**.
- Cần chờ 300 giây (thay vì 60) để chịu được mạng chập chờn: mất 1–4 nhịp heartbeat vẫn chưa bị coi là offline.

### 9.7. Tiền: ký quỹ COD và chia tiền

**Ký quỹ khi nhận đơn COD:**

```
cần_ký_quỹ   = orders.total_amount              (đúng số tiền mặt sẽ thu của khách)
khả_dụng     = wallets.total_balance − wallets.cod_reserved
nhận được    ⇔ khả_dụng ≥ cần_ký_quỹ
còn_thiếu    = cần_ký_quỹ − khả_dụng            (trả trong result của lỗi 16013)
```

**Chia tiền khi đơn COMPLETED** (`capnong.wallet.*`, xem `wallet-settlement-flow.md`):

```
phí_nông_dân = 10% × (tiền_hàng − voucher_shop)
Shop nhận    = tiền_hàng − voucher_shop − phí_nông_dân
phí_shipper  = 3% × cước_ship
Shipper nhận = cước_ship − phí_shipper
Sàn nhận     = tiền_khách_trả − Shop nhận − Shipper nhận
```

Ví dụ đơn COD 215.000đ (hàng 200.000đ, ship 15.000đ, không voucher), ví tài xế ban đầu 500.000đ:

| Thời điểm | Số dư | Đang ký quỹ | Khả dụng | Tiền mặt tài xế cầm |
|---|---|---|---|---|
| Trước khi nhận | 500.000 | 0 | 500.000 | 0 |
| Nhận đơn | 500.000 | 215.000 | 285.000 | 0 |
| Giao xong (thu tiền khách) | 285.000 | 0 | 285.000 | 215.000 |
| Khách xác nhận (COMPLETED) | 299.550 | 0 | 299.550 | 215.000 |

Tài xế lời 299.550 + 215.000 − 500.000 = **14.550đ** = 15.000 − 3%. Shop nhận 180.000đ, Sàn nhận 20.000 + 450 = **20.450đ**. Kiểm tra lại: 180.000 + 14.550 + 20.450 = 215.000 = tiền khách trả.

### 9.8. Câu hỏi thường gặp

**Vì sao không dùng Google Maps để tính km và phút?**
Google Routes / Distance Matrix tính phí theo từng lần gọi, mà vị trí cập nhật 3–4 giây một lần. Một đơn 20 phút sẽ tốn khoảng 300 lần gọi. Công thức haversine chạy ngay trên server, không tốn phí, không phụ thuộc quota, sai số chấp nhận được cho phạm vi nội thành.

**Sai số khoảng bao nhiêu?**
Km lệch khoảng ±20% so với đường thật, lệch nhiều hơn khi phải vòng qua sông, cầu hoặc đường một chiều. Phút không tính kẹt xe nên giờ cao điểm có thể chậm hơn dự kiến.

**Muốn chính xác hơn thì làm thế nào?**
Gọi Google Routes API **2 lần mỗi đơn**: lúc nhận đơn (đường tới vườn) và lúc lấy hàng (đường tới khách). Lưu quãng đường và thời gian Google trả về, giữa 2 lần thì tính phần còn lại theo tỷ lệ quãng đã đi. Google lỗi thì quay về công thức hiện tại.

**Vì sao hệ số 1,25 và tốc độ 25 km/h?**
Đây là giá trị ước lượng hợp lý cho xe máy trong nội thành TP.HCM: đường thực tế thường dài hơn đường thẳng 20–40%, tốc độ xe máy nội thành thường 20–30 km/h. Hai giá trị nằm trong `GeoUtils` (`ROAD_FACTOR`, `AVG_SPEED_KMH`), đổi được khi có dữ liệu thực tế.

**Vì sao ký quỹ đủ tổng tiền đơn, không trừ phần cước của tài xế?**
Tiền mặt tài xế thu của khách chính là `total_amount`. Giữ chỗ đúng số đó thì rủi ro của sàn bằng 0 và công thức đơn giản. Cước của tài xế vẫn được cộng vào ví khi đơn hoàn tất như mọi đơn khác.

**Hai tài xế bấm nhận cùng lúc thì sao?**
BE khoá dòng đơn bằng `SELECT … FOR UPDATE` trước khi kiểm tra. Người đến sau thấy đơn đã có chủ và nhận lỗi 7005, không ai bị trừ tiền hai lần.

## 10. Test case trên giao diện (web + mobile)

Mỗi case ghi **thao tác trên màn hình (input)** và **những gì màn hình phải hiện (output)**, dùng đúng câu chữ trong app. Cột "Nền tảng": **W** = web (`APRP-FE`), **M** = app mobile (`APRP-Mobile`).

Phần kiểm tra ở mức API và công thức đã có unit test (BE: `WalletCodReserveTest`, `ShipperFlowRulesTest`; FE: `shipper.service.test.ts`, `eta.test.ts`, `orderCode.test.ts`; Mobile: `deliveryProof.test.ts`, `topup.test.ts`, `trackingRules.test.ts`).

### 10.0. Chuẩn bị

| Mục | Cách chuẩn bị |
|---|---|
| Tài khoản | 1 Buyer, 1 Shop **S**, 2 Shipper: **A** (dùng web) và **B** (dùng mobile). Shipper đều đã được duyệt |
| Địa chỉ | Shop S và Buyer có địa chỉ thật ở TP.HCM để geocode được (xem 9.1) |
| Vị trí shipper | **Bắt buộc giả lập**, vì Chrome trên máy tính đoán vị trí theo IP nên thường lệch hàng chục km. Muốn được **mời** thì thẻ đơn phải hiện **"Cách bạn" ≤ 10 km** (≤ 3 km để được mời ngay đợt 1); danh sách thì hiện tới 15 km. Web: Chrome DevTools → **More tools → Sensors → Location**, đặt gần S (vd. cách ~1–2 km). Mobile máy ảo: **Extended controls → Location**. Máy thật: đứng gần địa chỉ S hoặc chọn địa chỉ S gần mình |
| Đơn COD #1 | Buyer đặt đơn COD ở shop S, tổng **215.000đ**. Shop xác nhận để đơn ở **Đã xác nhận** |
| Đơn PayOS #2 | Buyer đặt đơn PayOS ở shop S và thanh toán xong. Shop xác nhận |
| Ví shipper | Dùng chức năng **Nạp ví** (10.6) để đặt số dư theo từng case |
| Đường dẫn | Web shipper: `/shipper`. Ví web: `/shipper?tab=wallet`. Web nhà vườn: cổng nhà vườn → tab Đơn hàng. Mobile: đăng nhập tài khoản shipper / shop |

### 10.1. Trực tuyến

| ID | Nền tảng | Input (thao tác) | Output (màn hình) |
|---|---|---|---|
| UI-ON-01 | W | A mở `/shipper`, đang Ngoại tuyến | Tab Đơn hàng mới hiện khối **"Bạn đang ở trạng thái Ngoại tuyến"** và nút **"Bật Trực Tuyến Để Nhận Đơn Ngay"**, không hiện đơn nào |
| UI-ON-02 | W | Bấm **"Bật Trực Tuyến Để Nhận Đơn Ngay"**, cho phép trình duyệt lấy vị trí | Popup **"Đã bật Trực tuyến! Đang quét các đơn hàng mới sẵn sàng quanh bạn."**. Nút trạng thái đổi thành **"🟢 Đang Trực Tuyến"**, danh sách đơn tải ra |
| UI-ON-03 | M | B mở tab **Đơn mới** khi đang nghỉ | Thẻ **"Bạn đang nghỉ"**, nút **"Bật nhận đơn"** |
| UI-ON-04 | M | Bấm **"Bật nhận đơn"**, cho phép quyền vị trí | Toast **"Đã bật nhận đơn. Đơn sẵn sàng sẽ hiện ngay bên dưới."**. Header đổi thành **"Đang nhận đơn"** |
| UI-ON-05 | M | Đang nhận đơn, từ chối quyền vị trí rồi bật lại | Toast **"Đã bật nhận đơn (vị trí mặc định). Hãy bật GPS để tính quãng đường chính xác."** |
| UI-ON-06 | W | Đang online, đóng hẳn tab `/shipper` khoảng **6 phút**, rồi mở lại | Trạng thái hiện **Ngoại tuyến** (hệ thống tự tắt do mất tín hiệu) |
| UI-ON-07 | W | Đang online, bấm nút **"🟢 Đang Trực Tuyến"** | Popup **"Đã chuyển sang Ngoại tuyến. Bạn đang tạm dừng nhận đơn mới."**, danh sách đơn ẩn |

### 10.2. Đơn mới và thông báo (Bước 1)

| ID | Nền tảng | Input (thao tác) | Output (màn hình) |
|---|---|---|---|
| UI-DP-01 | W (Shop) | Shop S mở đơn #2 (đã xác nhận), bấm **"📦 Đã Chuẩn Bị Xong (Sẵn Sàng Giao)"** | Thông báo **"Đơn hàng #2 đã chuyển sang SẴN SÀNG!..."**. Thẻ đơn có khung **"Mã dán lên túi hàng"** với mã **#2** chữ to |
| UI-DP-02 | W (A online, thẻ đơn hiện "Cách bạn" ≤ 3 km) | Ngay sau UI-DP-01, để yên màn `/shipper` | Đơn #2 **tự xuất hiện** trong danh sách sau vài giây, không cần bấm tải lại. Chuông thông báo (tải lại mỗi 30 giây) có **"Có đơn mới gần bạn"**: **"Đơn #2 tại {tên shop} cách bạn … km. Đơn đã thanh toán online"** |
| UI-DP-03 | M (B online, cách S ≤ 3 km) | Như UI-DP-02, B đang mở app | Toast **"Có đơn mới #2 tại {tên shop} cách bạn … km"**, danh sách **Đơn mới** tự thêm đơn #2 |
| UI-DP-04 | M | B đặt vị trí **cách S khoảng 8 km**, Shop báo sẵn sàng một đơn mới | Không có toast ngay. Khoảng **2 phút** sau (đợt quét 10 km) mới có toast mời |
| UI-DP-05 | W | A xem thẻ đơn #2 trong danh sách | Thẻ hiện **"Cách bạn ~x km"** (tới kho), **"Giao hàng ~y km"**, tổng quãng đường, **"Tiền ship nhận"**. Đơn gần hơn đứng trước |
| UI-DP-06 | M | B xem danh sách | Dòng **"N đơn chờ giao · gần bạn trước"**, đơn gần hơn đứng trước |
| UI-DP-07 | W + M | A đang thấy đơn #2 trên web. B bấm nhận đơn #2 trên mobile thành công | Trên web của A, đơn #2 **tự biến mất** khỏi danh sách (không cần tải lại) |
| UI-DP-08 | W (Shop) | Đơn sẵn sàng **quá 15 phút** không ai nhận | Shop nhận thông báo **"Chưa có tài xế nhận đơn"**, nội dung **"Đơn #… đã sẵn sàng hơn 15 phút nhưng chưa có tài xế nhận..."**. Admin cũng nhận **"Đơn chưa có tài xế"** |
| UI-DP-10 | M | B online nhưng thẻ đơn hiện **"Cách bạn 12 km"**, Shop báo sẵn sàng một đơn | Đơn **có trong danh sách** (≤ 15 km) nhưng **không có toast / thông báo mời**, vì ngoài bán kính mời 10 km |
| UI-DP-09 | W (Buyer) | Shop báo sẵn sàng đơn của Buyer | Buyer nhận thông báo **"Cập nhật đơn hàng"**: **"Đơn #… đã được nhà vườn đóng gói, đang tìm tài xế."** |

### 10.3. Nhận đơn và ký quỹ COD

| ID | Nền tảng | Input (thao tác) | Output (màn hình) |
|---|---|---|---|
| UI-AC-01 | W | A (ví bất kỳ ≥ 0) bấm **"Nhận đơn này"** ở đơn PayOS #2 | Popup **"Nhận đơn thành công"**: **"Đã nhận đơn #2 thành công! Đang chuyển đến bảng "Đơn đang giao"..."**, tự chuyển sang tab Đơn đang giao |
| UI-AC-02 | W | A ví khả dụng **500.000đ** bấm **"Nhận đơn này"** ở đơn COD #1 | Mở hộp **"Nhận đơn COD #1"**: ô **Cần ký quỹ 215.000đ**, ô **Số dư khả dụng 500.000đ** (màu xanh), nút **"Để sau"** và **"Ký quỹ và nhận đơn"** |
| UI-AC-03 | W | Tiếp UI-AC-02, bấm **"Ký quỹ và nhận đơn"** | Popup nhận đơn thành công. Vào `/shipper?tab=wallet`: **SỐ DƯ KHẢ DỤNG 285.000đ**, dòng dưới **"Tổng số dư 500.000đ"**, ô **ĐANG KÝ QUỸ COD 215.000đ**. Lịch sử có **"Ký quỹ đơn COD · Đơn #1"** −215.000đ |
| UI-AC-04 | W | A ví khả dụng **200.000đ** bấm **"Nhận đơn này"** ở đơn COD #1 | Hộp **"Nhận đơn COD #1"**: số dư khả dụng **200.000đ** màu đỏ, dòng đỏ **"Ví còn thiếu 15.000đ. Nạp thêm để nhận đơn này."**, nút **"Đóng"** và **"Nạp ví"** (không có nút nhận đơn) |
| UI-AC-05 | W | Tiếp UI-AC-04, bấm **"Nạp ví"** | Mở hộp **"Nạp tiền vào ví"**, ô số tiền điền sẵn **15.000** |
| UI-AC-06 | M | B ví khả dụng **500.000đ** bấm **"Nhận đơn này"** ở đơn COD #1 | Tấm **"Nhận đơn #1?"**: các dòng Lấy hàng tại / Giao cho / Quãng đường / Phí ship / Thu của khách **215.000đ**. Khung vàng **"Đơn COD: ký quỹ 215.000đ · ví khả dụng 500.000đ"**. Nút **"Để sau"** và **"Ký quỹ và nhận"** |
| UI-AC-07 | M | Tiếp UI-AC-06, bấm **"Ký quỹ và nhận"** | Toast **"Đã nhận đơn #1. Đến nhà vườn lấy hàng nhé!"**, chuyển sang tab **Đang giao** |
| UI-AC-08 | M | B ví khả dụng **200.000đ** mở đơn COD #1 | Khung đỏ **"Ví còn thiếu 15.000đ. Nạp thêm để nhận đơn này."**, nút bên phải là **"Nạp ví"** |
| UI-AC-09 | W | A đang giao 1 đơn, xem danh sách đơn mới | Mỗi thẻ hiện cảnh báo **"Bạn đang có 1 đơn đang giao..."**, nút nhận đổi thành **"Đang bận giao 1 đơn khác"** và bị khoá |
| UI-AC-10 | M | B đang giao 1 đơn | Banner **"Bạn đang giao đơn #…"**, nút trên thẻ đơn là **"Giao xong đơn hiện tại để nhận thêm"** và bị mờ |
| UI-AC-11 | W + M | A (web) và B (mobile) cùng mở đơn #2, **bấm nhận gần như cùng lúc** | Một người thấy nhận thành công. Người còn lại thấy **"Đơn này đã được tài xế khác nhận."** và đơn biến mất khỏi danh sách |
| UI-AC-12 | W | A đang ký quỹ 215.000đ, khả dụng 285.000đ. Vào ví, bấm **"Rút tiền"**, nhập **300.000** | Hộp rút tiền chỉ cho rút tối đa **285.000đ**, gửi 300.000đ thì báo không đủ số dư |

### 10.4. Lấy hàng và đối soát mã (Bước 2)

| ID | Nền tảng | Input (thao tác) | Output (màn hình) |
|---|---|---|---|
| UI-PK-01 | W (Shop) | Shop mở danh sách đơn, đơn #1 đang **Đã xác nhận / Sẵn sàng giao / Shipper đang tới** | Thẻ có khung nét đứt **"Mã dán lên túi hàng"**, mã **#1** chữ to, dòng **"Tài xế sẽ so mã này trước khi bấm "Đã lấy hàng""**. Đơn **Chờ xác nhận** hoặc **Đã lấy hàng** không có khung này |
| UI-PK-02 | M (Shop) | Shop mở tab **Đơn hàng** và màn chi tiết đơn #1 | Cùng khung **"Mã dán lên túi hàng" · #1** ở thẻ và ở màn chi tiết |
| UI-PK-03 | W | A mở tab **Đơn đang giao**, đơn #1 đang tới vườn | Thẻ hiện mã **#1** chữ to và dòng **"Mã trên túi hàng phải trùng mã này"**, nhãn **"Chặng 1: Đang đến Shop lấy hàng"** |
| UI-PK-04 | W | Bấm **"📦 Xác nhận đã lấy hàng tại Shop"** | Hộp xác nhận tiêu đề **"Đối soát mã đơn #1"**, nội dung **"Kiểm tra mã trên túi hàng trùng #1 rồi mới xác nhận đã lấy hàng."** |
| UI-PK-05 | W | Ở hộp UI-PK-04 bấm **Huỷ** | Hộp đóng, đơn vẫn **Chặng 1**, không có thông báo nào |
| UI-PK-06 | W | Ở hộp UI-PK-04 bấm **Xác nhận** | Popup **"Đã xác nhận lấy hàng tại shop thành công! Bắt đầu chặng giao đến người mua."**, nhãn đổi thành **"Chặng 2: Đã lấy hàng · Đang giao khách"** |
| UI-PK-07 | W | Mở **"🗺️ Xem Bản Đồ Map"** lúc đang tới vườn, bấm nút xác nhận lấy hàng trên bản đồ | Cũng hiện hộp **"Đối soát mã đơn #1"** trước khi gọi |
| UI-PK-08 | M | B mở tab **Đang giao**, đơn #1 đang tới vườn | Thanh chặng **"1. Đến vườn lấy hàng"** sáng, khung **"Mã đối soát" · #1** với dòng **"Mã trên túi hàng phải trùng mã này"** |
| UI-PK-09 | M | Bấm **"Đã lấy hàng"** | Hộp **"Đối soát mã đơn"**, nội dung **"Kiểm tra mã trên túi hàng trùng #1, rồi bấm khi đã nhận đủ hàng của {tên shop}."**, nút **"Chưa"** và **"Mã trùng, đã lấy hàng"** |
| UI-PK-10 | M | Bấm **"Mã trùng, đã lấy hàng"** | Toast **"Đã lấy hàng. Giao cho khách nhé!"**, thanh chặng chuyển sang **"2. Giao cho khách"**, khung mã đối soát ẩn đi |
| UI-PK-11 | W + M (Buyer, Shop) | Sau khi tài xế nhận đơn, rồi lấy hàng | Buyer nhận **"{Tài xế} đã nhận đơn #1 và đang tới nhà vườn lấy hàng."**, sau đó **"Đơn #1 đã được lấy hàng và đang giao tới bạn. Mở bản đồ để theo dõi."**. Shop nhận **"{Tài xế} đã lấy hàng đơn #1."** |

### 10.5. Theo dõi bản đồ: km và số phút (Bước 2)

| ID | Nền tảng | Input (thao tác) | Output (màn hình) |
|---|---|---|---|
| UI-MAP-01 | W (Buyer / Shop) | Đơn #1 đã lấy hàng, tài xế đang chạy (hoặc bật **🎭 Demo GPS**). Mở bản đồ theo dõi | Dưới dòng trạng thái chặng có dòng xanh đậm **"Còn ~x,x km · khoảng y phút"**, con số giảm dần khi xe di chuyển |
| UI-MAP-02 | W | Kiểm tra số liệu: còn **5,0 km** | Hiện **"Còn ~5,0 km · khoảng 12 phút"** (công thức ở 9.3) |
| UI-MAP-03 | W / M | Xe tới gần khách, còn dưới 1 km | Đổi sang mét, ví dụ **"Còn ~400 m · sắp tới nơi"** |
| UI-MAP-04 | W / M | Đơn đã giao thành công | Dòng km/phút **không còn hiện** |
| UI-MAP-05 | M (Buyer) | Mở **Theo dõi** khi tài xế còn đang tới vườn | Không hiện dòng km/phút (Buyer chưa được xem xe ở chặng 1). Sau khi tài xế lấy hàng thì dòng xuất hiện |
| UI-MAP-06 | M (Shop / Shipper) | Mở màn theo dõi đơn đang giao | Hiện **"Còn ~… · khoảng … phút"** dưới thẻ trạng thái |

### 10.6. Giao hàng và phân nhánh (Bước 3)

| ID | Nền tảng | Input (thao tác) | Output (màn hình) |
|---|---|---|---|
| UI-DL-01 | W | Đơn #1 ở chặng 2, A bấm **"Giao thành công"** | Hộp **"Xác nhận giao hàng · Đơn #1"**, có tên + số khách và nút **Gọi**. Khung vàng **"Thu tiền mặt của khách: 215.000đ..."** |
| UI-DL-02 | W | Không chọn ảnh, bấm **"Xác nhận đã giao"** | Dòng đỏ **"Chụp ít nhất 1 ảnh bằng chứng."**, hộp không đóng |
| UI-DL-03 | W | Bấm **"Thêm ảnh"**, chọn 1 ảnh, bấm **"Xác nhận đã giao"** | Ảnh hiện thu nhỏ. Sau khi gửi: popup **"Giao hàng thành công! Cước giao vào ví khi khách xác nhận đã nhận hàng."**, đơn biến mất khỏi Đơn đang giao |
| UI-DL-04 | W | Sau UI-DL-03, mở ví | **SỐ DƯ KHẢ DỤNG 285.000đ**, **ĐANG KÝ QUỸ COD 0đ**. Lịch sử có **"Gỡ ký quỹ đơn COD · Đơn #1"** và **"Thu tiền mặt COD · Đơn #1"** −215.000đ |
| UI-DL-05 | W (Buyer) | Buyer mở đơn #1, bấm **Đã nhận hàng** | Đơn **Hoàn thành**. Ví A (tải lại) tăng **14.550đ** thành **299.550đ**, lịch sử có **"Cước giao hàng về ví"** |
| UI-DL-06 | W | Thử chọn **6 ảnh** | Chỉ giữ **5 ảnh**, ô **"Thêm ảnh"** ẩn đi |
| UI-DL-07 | M | Đơn #1 chặng 2, B bấm **"Giao thành công"** | Tấm **"Xác nhận đã giao · #1"**, khung **"Thu đủ 215.000đ tiền mặt của khách..."**, nút **"Chụp ảnh"** và **"Chọn ảnh"** |
| UI-DL-08 | M | Không chụp ảnh, bấm **"Đã giao"** | Toast **"Kiểm tra lại các mục được đánh dấu đỏ nhé."**, dòng đỏ **"Chụp ít nhất 1 ảnh bằng chứng."** |
| UI-DL-09 | M | Chụp 1 ảnh, bấm **"Đã giao"** | Toast **"Giao thành công! Cước giao vào ví khi khách xác nhận đã nhận hàng."**, đơn rời tab Đang giao |
| UI-FL-01 | W | Chặng 2, bấm **"Giao thất bại"** | Hộp **"Báo giao thất bại · Đơn #1"** với 4 lựa chọn lý do: **Không liên lạc được**, **Sai địa chỉ**, **Khách từ chối nhận**, **Lý do khác**; bộ đếm **"Số cuộc đã gọi khách"** |
| UI-FL-02 | W | Có ảnh nhưng chưa chọn lý do, bấm **"Báo thất bại"** | Dòng đỏ **"Chọn lý do giao thất bại."** |
| UI-FL-03 | W | Chọn **Không liên lạc được**, bấm **+** 2 lần (số cuộc = 2), bấm **"Báo thất bại"** | Dòng đỏ **"Cần gọi khách tối thiểu 3 cuộc."** |
| UI-FL-04 | W | Chọn **Lý do khác**, để trống ghi chú | Dòng đỏ **"Ghi rõ lý do."** |
| UI-FL-05 | W | Chọn **Không liên lạc được**, số cuộc **3**, 1 ảnh, bấm **"Báo thất bại"** | Popup **"Đã báo giao thất bại. Hàng được hoàn kho, tiền ký quỹ COD (nếu có) đã trả lại ví."**. Ví: **SỐ DƯ KHẢ DỤNG 500.000đ**, **ĐANG KÝ QUỸ COD 0đ**, lịch sử có **"Gỡ ký quỹ đơn COD"** |
| UI-FL-06 | W (Shop) | Sau UI-FL-05, Shop mở danh sách sản phẩm / túi mù / combo của đơn | Tồn kho các món trong đơn **tăng lại** đúng số lượng đã đặt. Shop nhận **"Đơn #1 giao thất bại, hàng được hoàn về kho."** |
| UI-FL-07 | W (Buyer) | Đơn PayOS #2 bị báo giao thất bại | Buyer nhận **"Đơn #2 giao không thành công. Đơn đã được huỷ, sàn sẽ hoàn 215.000đ cho bạn."**. Admin thấy đơn trong danh sách cần hoàn tiền |
| UI-FL-08 | W | Chặng 1 (chưa lấy hàng), bấm **"Không lấy được"** | Mở cùng hộp **"Báo giao thất bại · Đơn #1"** |
| UI-FL-09 | M | Chặng 2, bấm **"Thất bại"** | Tấm **"Báo giao thất bại · #1"**, nhóm chọn **"Lý do"**, bộ đếm cuộc gọi, ô **"Ghi chú"** |
| UI-FL-10 | M | Chọn **Không liên lạc được**, số cuộc 2, có ảnh, bấm **"Báo thất bại"** | Toast **"Kiểm tra lại các mục được đánh dấu đỏ nhé."**, dòng đỏ **"Cần gọi khách tối thiểu 3 cuộc."** |
| UI-FL-11 | M | Chặng 1, bấm **"Không lấy được"** | Tấm tiêu đề **"Không lấy được hàng · #1"** |
| UI-FL-12 | M | Điền hợp lệ, bấm **"Báo thất bại"** | Toast **"Đã báo giao thất bại. Hàng được hoàn kho, tiền ký quỹ COD đã trả lại ví."** |

### 10.7. Ví và nạp ví

| ID | Nền tảng | Input (thao tác) | Output (màn hình) |
|---|---|---|---|
| UI-W-01 | W | A mở `/shipper?tab=wallet` | 4 ô: **SỐ DƯ KHẢ DỤNG** (kèm "Tổng số dư …"), **ĐANG KÝ QUỸ COD**, **CƯỚC ĐANG TẠM GIỮ**, **TỔNG THU NHẬP**. Nút **"Nạp ví"**, **"Rút tiền"** |
| UI-W-02 | W | Bấm **"Nạp ví"**, nhập **5.000**, bấm **"Thanh toán"** | Dòng đỏ **"Tối thiểu 10.000đ"**, không chuyển trang |
| UI-W-03 | W | Bấm nút nhanh **100.000đ**, bấm **"Thanh toán 100.000đ"** | Chuyển sang trang thanh toán PayOS |
| UI-W-04 | W | Thanh toán xong trên PayOS | Quay về tab ví, khung xanh **"Đã nạp 100.000đ vào ví."**. Số dư tăng 100.000đ, lịch sử có **"Nạp ví qua PayOS"** +100.000đ |
| UI-W-05 | W | Trên PayOS bấm **Huỷ** | Quay về tab ví, khung đỏ **"Lệnh nạp đã huỷ, ví không thay đổi."** |
| UI-W-06 | W | Sau UI-W-04, tải lại trang (F5) | Không hiện lại thông báo, số dư **không cộng thêm lần nữa** |
| UI-W-07 | M | B vào **Tài khoản → "Ví tài xế"** | Thẻ xanh **"Số dư khả dụng"** kèm **"Tổng số dư …"** và nút **"Nạp ví"**. Hai ô **"Đang ký quỹ COD"**, **"Cước tạm giữ"**. Mục **"Lịch sử biến động"** |
| UI-W-08 | M | Bấm **"Nạp ví"**, nhập 5.000, bấm **"Nạp 5.000đ"** | Ô số tiền báo đỏ **"Tối thiểu 10.000đ"** |
| UI-W-09 | M | Chọn **100.000đ**, bấm **"Nạp 100.000đ"**, thanh toán trong trình duyệt của app | Quay về app, toast **"Đã nạp tiền vào ví."**, số dư tăng |
| UI-W-10 | M | Mở PayOS rồi đóng trình duyệt không trả tiền | Toast **"Đã huỷ nạp tiền, ví không thay đổi."** hoặc **"Đang chờ PayOS xác nhận. Kéo xuống để tải lại sau ít phút."** |
| UI-W-11 | M | Từ tấm nhận đơn COD thiếu tiền (UI-AC-08), bấm **"Nạp ví"** và nạp đủ | Ô số tiền điền sẵn số còn thiếu (làm tròn lên nghìn, tối thiểu 10.000đ). Nạp xong, tấm nhận đơn đổi nút thành **"Ký quỹ và nhận"** |

## 11. Ngoài phạm vi 3 bước của tài liệu (chưa làm)

- Push khi app tắt (Expo push cần `expo-notifications` + development build). Hiện chỉ realtime khi app mở.
- Trả một phần cước cho shipper khi giao thất bại (hiện không trả).
- Shop xác nhận đã nhận lại hàng trước khi gỡ ký quỹ khi giao thất bại.

