# Luồng Shipper: điều phối, ký quỹ COD, bằng chứng giao hàng

Bổ sung cho [`order-and-tracking-flow.md`](order-and-tracking-flow.md) và [`wallet-settlement-flow.md`](wallet-settlement-flow.md). Nếu tài liệu khác code thì code là chuẩn.

```
READY ──(điều phối: quét shipper online quanh vườn, mời qua STOMP + thông báo)──► shipper bấm nhận
      ──(khoá đơn, kiểm tra online / 1 đơn / ví)──► SHIPPING ──(Đã lấy hàng)──► PICKED_UP
      ──(GPS /app/shipper/location → /topic/order/{id}/location, kèm distanceKm + estimatedMinutes)
      ├─► DELIVERED (bắt buộc ảnh) ──► COMPLETED ──► chia tiền
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

`GET /orders/ready?page&size&lat&lng`: bắt buộc online (7006). Trả đơn READY chưa có người nhận trong 15 km quanh vị trí gửi lên (hoặc vị trí gần nhất), gần nhất trước, kèm `distanceToShopKm`.

## 3. Nhận đơn `PUT /orders/{id}/ship`

Kiểm tra theo thứ tự (lỗi thì rollback toàn bộ):

| Điều kiện | Mã lỗi |
|---|---|
| Khoá dòng đơn (`SELECT ... FOR UPDATE`), đơn đã có shipper khác | 7005 `ORDER_ALREADY_TAKEN` |
| Đơn không ở READY | 7002 |
| Shipper chưa online | 7006 `SHIPPER_NOT_ONLINE` |
| Đang có đơn SHIPPING / PICKED_UP | 7004 `SHIPPER_HAS_ACTIVE_ORDER` |
| Ví âm (nợ COD luồng cũ) | 16014 `WALLET_HAS_DEBT` |
| Đơn COD, số dư khả dụng < `totalAmount` | 16013 `SHIPPER_COD_BALANCE_INSUFFICIENT`, `result = { required, available, missing }` |

Thành công: gán shipper, SHIPPING, giữ chỗ COD, tạm giữ cước (`SHIPPER_INCOME_HOLD`), báo Buyer + Shop.

## 4. Ký quỹ COD (chỉ nhận COD bằng số dư ví)

Mục tiêu: shipper không thể cầm tiền mặt đơn lớn rồi không nộp về sàn.

- Cột mới `wallets.cod_reserved`. **Số dư khả dụng = `total_balance − cod_reserved`**, dùng cho nhận đơn COD và **rút tiền**.
- Nhận đơn COD: `cod_reserved += totalAmount`, `orders.cod_reserved_amount = totalAmount`, bút toán `COD_RESERVED` (amount âm = khả dụng giảm, số dư không đổi).
- Giao thành công: `COD_RESERVE_RELEASED` + `COD_CASH_COLLECTED −totalAmount`. Tiền mặt shipper thu chính là tiền đã ứng, nên `cod_settled = true` ngay, **không cần Admin xác nhận nộp tiền**.
- Giao thất bại: `COD_RESERVE_RELEASED`, số dư như trước khi nhận.
- Đơn PayOS: không giữ chỗ, chỉ cần ví không âm.
- Đơn nhận trước khi có ký quỹ (`cod_reserved_amount` NULL) vẫn chạy luồng cũ: ghi nợ, Admin xác nhận qua `POST /wallets/cod/{orderId}/settle`.

Ví dụ: ví 500.000đ, đơn COD 215.000đ (200.000 hàng + 15.000 ship).
Nhận đơn: khả dụng 285.000đ. Giao xong: ví 285.000đ + 215.000đ tiền mặt. COMPLETED: +14.550đ cước → 299.550đ. Shipper lời đúng 14.550đ.

### Nạp ví

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/wallets/topups` `{amount, returnUrl, cancelUrl}` | SHIPPER, SHOP | Tạo link PayOS, `amount` từ `min-topup` (10.000) tới `max-topup` (20.000.000) |
| GET | `/wallets/topups/me` | SHIPPER, SHOP | 20 lệnh gần nhất |
| POST | `/wallets/topups/{orderCode}/confirm` | SHIPPER, SHOP, ADMIN | Hỏi PayOS, cộng ví nếu đã trả (idempotent) |

Webhook PayOS dùng chung `/payments/payos/webhook`: nếu `orderCode` thuộc `wallet_topups` thì cộng ví (`TOP_UP`), không thì xử lý như thanh toán đơn.

## 5. Bằng chứng giao hàng

Ảnh upload trước qua `POST /upload/image` (folder `delivery-proofs`), gửi URL.

- `PUT /orders/{id}/deliver { proofImageUrls }`: ít nhất 1 ảnh (7007).
- `PUT /orders/{id}/fail { reason, note, callAttempts, proofImageUrls }`:
  - `reason`: `KHONG_LIEN_LAC_DUOC | SAI_DIA_CHI | KHACH_TU_CHOI | KHAC` (thiếu: 7008);
  - `KHONG_LIEN_LAC_DUOC` cần `callAttempts ≥ 3` (7009); `KHAC` cần `note` (7010); ít nhất 1 ảnh (7007).
- Lưu ở `orders.delivery_proof_urls`, `fail_reason`, `fail_note`, `fail_call_attempts`, `fail_proof_urls`, `failed_at`; trả trong `OrderResponse`.

## 6. Lấy hàng: đối soát mã đơn

Không đổi API (`PUT /orders/{id}/pickup`). Đối soát làm ở giao diện, cùng định dạng mã `#{orderId}` ở cả hai phía:

- **Nhà vườn** (web `pages/farmer/Orders.tsx`, mobile `ShopOrderCard`, `shop/order/[id]`): đơn CONFIRMED / READY / SHIPPING hiện khung **"Mã dán lên túi hàng #123"** để ghi/dán lên túi.
- **Tài xế** (web Dashboard, bản đồ, FakeGPS; mobile tab Đang giao): thẻ đơn hiện mã to rõ. Bấm "Đã lấy hàng" mở hộp **"Đối soát mã đơn #123 — Kiểm tra mã trên túi hàng trùng #123"**, xác nhận rồi mới gọi API.

## 7. Theo dõi realtime

- STOMP `/app/shipper/location` chỉ nhận từ **shipper được gán** khi đơn SHIPPING / PICKED_UP (chặn giả mạo vị trí và `gpsOff`).
- `ShipperLocationResponse` có thêm `distanceKm` (quãng còn lại tới điểm của chặng hiện tại) và `estimatedMinutes` (25 km/h).
- Chặng mặc định: SHIPPING → `TO_SHOP`, PICKED_UP → `TO_BUYER`, DELIVERED/COMPLETED → `DELIVERED`.
- Bản đồ web (`GoogleMapTrackingModal`) và mobile (`ShipperTrackingView`) hiện dòng **"Còn ~2,3 km · khoảng 6 phút"** dưới trạng thái chặng (`utils/eta.ts`, `trackingRules.formatEta`). Dưới 1 km đổi sang mét, còn ≤ 1 phút hiện "sắp tới nơi", đã giao thì ẩn. Mobile ẩn dòng này khi Buyer chưa được xem vị trí xe (chặng tới vườn).

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

## 10. Test case (input / output)

Quy ước dữ liệu mẫu (dùng lại cho các case):

| Ký hiệu | Giá trị |
|---|---|
| Shop S | Nhà vườn tại (10.8500, 106.7720) |
| Shipper A | ACTIVE, online, vị trí (10.8600, 106.7720), cách S ~1,4 km. Ví 500.000đ, `cod_reserved` 0 |
| Shipper B | ACTIVE, online, vị trí cách S ~8 km. Ví 0đ |
| Đơn COD #1 | `paymentMethod = COD`, tiền hàng 200.000đ, ship 15.000đ, `totalAmount` 215.000đ, READY |
| Đơn PayOS #2 | `paymentMethod = PAYOS`, đã trả 215.000đ, READY |

Mã `1000` = thành công. Cột "Tự động" ghi test đã có trong code (BE: `WalletCodReserveTest`, `ShipperFlowRulesTest`; FE: `shipper.service.test.ts`, `eta.test.ts`, `orderCode.test.ts`; Mobile: `deliveryProof.test.ts`, `topup.test.ts`, `trackingRules.test.ts`). Ô trống = test tay qua Swagger / app.

### 10.1. Trực tuyến và heartbeat

| ID | Điều kiện | Input | Output mong đợi | Tự động |
|---|---|---|---|---|
| ON-01 | A offline | `POST /shippers/status/online {"latitude":10.86,"longitude":106.772}` | `1000`. `shipper_locations.online = true`, `last_seen_at` = bây giờ. `GET /shippers/status` → `isOnline: true` | |
| ON-02 | A online, app gọi lại sau 60s | `POST /shippers/status/online` (toạ độ mới) | `1000`, `last_seen_at` cập nhật, không tạo dòng mới | |
| ON-03 | A online, không gửi heartbeat quá 5 phút | Chờ scheduler `markStaleShippersOffline` | `online = false`, `GET /shippers/status` → `isOnline: false` | |
| ON-04 | A online | `POST /shippers/status/offline` | `1000`, `online = false`, dòng vẫn còn (vị trí cuối giữ lại) | |
| ON-05 | Bất kỳ | `POST /shippers/5/status/online` (API cũ) | 404/405: endpoint đã bị gỡ | |

### 10.2. Điều phối (Bước 1)

| ID | Điều kiện | Input | Output mong đợi | Tự động |
|---|---|---|---|---|
| DP-01 | A, B online; đơn PayOS #2 CONFIRMED | Shop gọi `PUT /orders/2/ready` | `1000`, đơn READY. Đợt 1 (3 km): A nhận thông báo `type = NEW_ORDER_OFFER, referenceId = 2` và tin `/user/queue/shipper-offers {type: NEW_ORDER, orderId: 2, distanceToShopKm: ~1.4}`. B **không** được mời. Buyer nhận "đang tìm tài xế" | |
| DP-02 | Tiếp DP-01, chưa ai nhận sau 60s và 120s | Scheduler chạy | Đợt 2 (6 km) rồi đợt 3 (10 km): B được mời ở đợt 3. A không bị mời lại (`dispatch_offers` unique) | |
| DP-03 | Đơn COD #1 READY, B ví 0đ, cách S 2 km | Đợt quét | B **không** được mời (không đủ ký quỹ 215.000đ), nhưng vẫn thấy đơn trong `GET /orders/ready` | |
| DP-04 | Đơn READY quá 15 phút chưa ai nhận | Scheduler | Shop và mọi Admin nhận 1 thông báo `DISPATCH_TIMEOUT`. `orders.dispatch_alerted_at` có giá trị, không gửi lặp | |
| DP-05 | A đã được mời đơn #2, C nhận đơn #2 | C gọi `PUT /orders/2/ship` | A nhận `/user/queue/shipper-offers {type: ORDER_TAKEN, orderId: 2}`, đơn biến mất khỏi danh sách trên app | |
| DP-06 | A offline | `GET /orders/ready` | Lỗi `7006 SHIPPER_NOT_ONLINE` | |
| DP-07 | A online | `GET /orders/ready?page=0&size=10&lat=10.86&lng=106.772` | `1000`, chỉ đơn READY chưa có shipper trong 15 km, sắp gần nhất trước, mỗi đơn có `distanceToShopKm` | FE ✓ (tham số) |

### 10.3. Nhận đơn và ký quỹ

| ID | Điều kiện | Input | Output mong đợi | Tự động |
|---|---|---|---|---|
| AC-01 | A online, ví 500.000đ | `PUT /orders/2/ship` (PayOS) | `1000`, đơn SHIPPING, `shipperId = A`. Ví: số dư 500.000đ, `cod_reserved` 0, `SHIPPER_INCOME_HOLD` +14.550đ vào tạm giữ. Buyer + Shop nhận thông báo | BE ✓ |
| AC-02 | A online, ví 500.000đ | `PUT /orders/1/ship` (COD) | `1000`. `orders.cod_reserved_amount = 215000`. Ví: số dư 500.000đ, `cod_reserved` 215.000đ, **khả dụng 285.000đ**. Bút toán `COD_RESERVED` amount −215.000 | BE ✓ |
| AC-03 | A ví 200.000đ | `PUT /orders/1/ship` | Lỗi `16013`, `result = {required: 215000, available: 200000, missing: 15000}`. Đơn vẫn READY, ví không đổi. App: "Nạp thêm 15.000đ để nhận đơn" + nút Nạp ví | BE ✓ FE ✓ Mobile ✓ |
| AC-04 | A ví −50.000đ (nợ COD cũ) | `PUT /orders/2/ship` (PayOS) | Lỗi `16014 WALLET_HAS_DEBT` | BE ✓ |
| AC-05 | A đang giao đơn khác | `PUT /orders/2/ship` | Lỗi `7004 SHIPPER_HAS_ACTIVE_ORDER`, không giữ chỗ ví | BE ✓ |
| AC-06 | Đơn #2 đã thuộc C | A gọi `PUT /orders/2/ship` | Lỗi `7005 ORDER_ALREADY_TAKEN` | BE ✓ FE ✓ |
| AC-07 | A offline | `PUT /orders/2/ship` | Lỗi `7006` | BE ✓ |
| AC-08 | A và C cùng bấm nhận #2 trong cùng 1 giây | 2 request song song | Đúng 1 request `1000`, request còn lại `7005`. Chỉ 1 ví có `SHIPPER_INCOME_HOLD` | |
| AC-09 | A khả dụng 285.000đ (đang ký quỹ 215.000đ) | `POST /wallets/withdrawals {amount: 300000, ...}` | Lỗi `16008`: tiền ký quỹ không được rút | |

### 10.4. Lấy hàng và theo dõi realtime (Bước 2)

| ID | Điều kiện | Input | Output mong đợi | Tự động |
|---|---|---|---|---|
| PK-01 | Đơn #1 SHIPPING, shipper A | A gọi `PUT /orders/1/pickup` | `1000`, PICKED_UP, `pickedUpAt` có giá trị. Buyer + Shop nhận thông báo | |
| PK-02 | Đơn #1 SHIPPING của A | C gọi `PUT /orders/1/pickup` | Lỗi `1004 UNAUTHORIZED` | |
| PK-03 | Đơn #1 READY | Nhà vườn mở danh sách đơn (web / mobile) | Thẻ đơn hiện khung "Mã dán lên túi hàng **#1**". Đơn PENDING hoặc PICKED_UP không hiện khung này | FE ✓ Mobile ✓ (`needsPickupCode`) |
| PK-04 | Đơn #1 SHIPPING của A | A bấm "Đã lấy hàng" | Hiện hộp "Đối soát mã đơn #1" với câu "Kiểm tra mã trên túi hàng trùng #1". Bấm huỷ: không gọi API, đơn vẫn SHIPPING. Bấm xác nhận: gọi `PUT /orders/1/pickup` → PICKED_UP | |
| GPS-01 | Đơn #1 PICKED_UP của A | A publish STOMP `/app/shipper/location {"orderId":1,"latitude":10.80,"longitude":106.70}` | Topic `/topic/order/1/location` nhận vị trí kèm `deliveryPhase: TO_BUYER`, `distanceKm` (quãng còn lại tới khách), `estimatedMinutes` | BE ✓ (công thức) |
| GPS-02 | Đơn #1 của A | C (hoặc Buyer) publish vị trí / `gpsOff` cho đơn #1 | Bị bỏ qua, topic không nhận gì | |
| GPS-03 | Đơn #1 SHIPPING, chưa có vị trí | `GET /shippers/location/order/1` | `deliveryPhase: TO_SHOP` (trước đây sai thành `TO_BUYER`) | BE ✓ |
| GPS-04 | Còn 5 km tới khách | Tính ETA | `estimatedMinutes = 12` (25 km/h, làm tròn lên) | BE ✓ |
| GPS-05 | Đơn #1 PICKED_UP, BE trả `distanceKm = 2.3`, `estimatedMinutes = 6` | Buyer / nhà vườn mở bản đồ | Hiện "Còn ~2,3 km · khoảng 6 phút", tự cập nhật theo mỗi lần nhận vị trí | FE ✓ Mobile ✓ (`formatEta`) |
| GPS-06 | `distanceKm = 0.42`, `estimatedMinutes = 1` | Hiển thị | "Còn ~400 m · sắp tới nơi" | FE ✓ Mobile ✓ |
| GPS-07 | Đơn DELIVERED (`distanceKm = 0`, `estimatedMinutes = 0`) | Hiển thị | Ẩn dòng km/phút | FE ✓ Mobile ✓ |

### 10.5. Giao hàng và phân nhánh (Bước 3)

| ID | Điều kiện | Input | Output mong đợi | Tự động |
|---|---|---|---|---|
| DL-01 | Đơn #1 PICKED_UP | `PUT /orders/1/deliver {}` hoặc `{"proofImageUrls":["abc"]}` | Lỗi `7007 DELIVERY_PROOF_REQUIRED` | BE ✓ Mobile ✓ |
| DL-02 | Đơn COD #1 PICKED_UP, A ký quỹ 215.000đ | `PUT /orders/1/deliver {"proofImageUrls":["https://res.cloudinary.com/.../1.jpg"]}` | `1000`, DELIVERED, `deliveryProofUrls` có 1 ảnh. Ví A: `COD_RESERVE_RELEASED` +215.000 rồi `COD_CASH_COLLECTED` −215.000. **Số dư 285.000đ, `cod_reserved` 0**, `codSettled = true`. Shop + Sàn có tạm giữ. Buyer nhận "Hãy bấm Đã nhận hàng" | BE ✓ FE ✓ |
| DL-03 | Đơn PayOS #2 PICKED_UP | `PUT /orders/2/deliver` (có ảnh) | `1000`, DELIVERED, ví A không đổi số dư | |
| DL-04 | Tiếp DL-02 | Buyer `PUT /orders/1/complete` | `1000`, COMPLETED. Ví A +14.550đ (299.550đ). Shop +180.000đ. Sàn +20.450đ | |
| FL-01 | Đơn #1 PICKED_UP | `PUT /orders/1/fail {"proofImageUrls":["https://..."]}` (không có reason) | Lỗi `7008 FAIL_REASON_REQUIRED` | |
| FL-02 | Đơn #1 PICKED_UP | `{"reason":"KHONG_LIEN_LAC_DUOC","callAttempts":2,"proofImageUrls":["https://..."]}` | Lỗi `7009` (cần ≥ 3 cuộc) | BE ✓ Mobile ✓ |
| FL-03 | Đơn #1 SHIPPING | `{"reason":"KHAC","proofImageUrls":["https://..."]}` | Lỗi `7010` (lý do khác phải ghi chú) | BE ✓ Mobile ✓ |
| FL-04 | Đơn COD #1 PICKED_UP, A ký quỹ 215.000đ, đơn có 2 sản phẩm + 1 Mystery Box + 1 Combo | `{"reason":"KHONG_LIEN_LAC_DUOC","callAttempts":3,"note":"Gọi 3 lần không nghe","proofImageUrls":["https://..."]}` | `1000`, FAILED, lưu `failReason`, `failCallAttempts = 3`, `failProofUrls`. Tồn kho cả 4 món được cộng lại (`increaseStockAtomic`). Ví A: `COD_RESERVE_RELEASED`, **khả dụng lại 500.000đ**, `SHIPPER_INCOME_HOLD` bị gỡ. Voucher trả lại. Buyer + Shop nhận thông báo | BE ✓ (luật + ví) |
| FL-05 | Đơn PayOS #2 PICKED_UP | `PUT /orders/2/fail` hợp lệ | `1000`, FAILED, `refundAmount = 215000`, `refundReason` "Giao hàng thất bại...", đơn hiện trong `GET /wallets/refunds` để Admin hoàn tiền | |

### 10.6. Nạp ví

| ID | Điều kiện | Input | Output mong đợi | Tự động |
|---|---|---|---|---|
| TU-01 | A đăng nhập | `POST /wallets/topups {"amount":5000}` | Lỗi `16015` (tối thiểu 10.000đ) | Mobile ✓ |
| TU-02 | A đăng nhập | `POST /wallets/topups {"amount":100000,"returnUrl":"http://localhost:3000/shipper?tab=wallet"}` | `1000`, `{status: PENDING, orderCode, checkoutUrl}`, có dòng `wallet_topups` | |
| TU-03 | Tiếp TU-02, đã trả trên PayOS | PayOS gọi webhook với `orderCode` đó | Ví A +100.000đ (`TOP_UP`), lệnh `PAID`. Không đi vào luồng thanh toán đơn | |
| TU-04 | Tiếp TU-03 | Gọi lại webhook hoặc `POST /wallets/topups/{orderCode}/confirm` | Không cộng thêm (idempotent), trả `status: PAID` | |
| TU-05 | Lệnh của A | C gọi `POST /wallets/topups/{orderCode}/confirm` | Lỗi `1034 FORBIDDEN` | |

## 11. Ngoài phạm vi 3 bước của tài liệu (chưa làm)

- Push khi app tắt (Expo push cần `expo-notifications` + development build). Hiện chỉ realtime khi app mở.
- Trả một phần cước cho shipper khi giao thất bại (hiện không trả).
- Shop xác nhận đã nhận lại hàng trước khi gỡ ký quỹ khi giao thất bại.

