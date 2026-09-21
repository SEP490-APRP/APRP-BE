# Báo Cáo Tính Năng: Cấu Hình PayOS & Gemini AI Keys (Branch: `feature/Config-PayOs,-AI-keys`)

Tài liệu này tổng hợp toàn bộ các công việc đã thực hiện trên nhánh `feature/Config-PayOs,-AI-keys` nhằm thiết lập hạ tầng thanh toán trực tuyến qua **PayOS** và chuẩn bị sẵn danh sách khóa API **Google Gemini** cho AI Gateway.

---

## 1. Mục Tiêu Thực Hiện
1. Tích hợp thư viện SDK chính thức của **PayOS** vào dự án Spring Boot.
2. Cấu hình các biến môi trường cho PayOS (Client ID, API Key, Checksum Key, Return/Cancel URLs).
3. Đăng ký Bean `PayOS` vào Spring Application Context để sẵn sàng tiêm `@Autowired` cho các module thanh toán đơn hàng.
4. Cấu hình danh sách 13 API Keys của **Google Gemini** vào biến môi trường `.env` nhằm phục vụ cơ chế xoay vòng khóa (API Key Rotation) khi gọi AI sau này.

---

## 2. Chi Tiết Các Công Việc Đã Thực Hiện

### 2.1. Cập nhật biến môi trường ([`.env`](file:///d:/Desktop/SP26/Finaldoan/Code/BE/BE_CapNong/.env))
Đã thêm vào file `.env` các khóa cấu hình sau:

```env
# ===============================
# PAYOS PAYMENT GATEWAY
# ===============================
PAYOS_CLIENT_ID=f7a9ffab-552f-4940-a7b0-10cb6993b071
PAYOS_API_KEY=f9d8e4bc-75cc-4533-bec2-83125b6d1535
PAYOS_CHECKSUM_KEY=22eb8ca119e9fa9bb913517b535217a6739ee0e727bec2a82d809f79e0a35eb6
# URL FE để PayOS redirect sau khi thanh toán
PAYOS_RETURN_URL=http://localhost:3000/payment/result
PAYOS_CANCEL_URL=http://localhost:3000/payment/result

# ===============================
# AI GATEWAY — GEMINI API KEYS
# ===============================
# Lấy tại https://aistudio.google.com/app/apikey
# Thêm nhiều key để tự động rotate khi hết quota
GEMINI_KEY_1=AQ.Ab8RN6Ko1UC0QqeCnFkVyYlegoFNYCtcfdSI7Vu5-IeEzps-bQ
GEMINI_KEY_2=AQ.Ab8RN6KCDCTMsv3xekY5r8Muf4-tR7GyUv7oJuZLGDLSMBowDg
GEMINI_KEY_3=AQ.Ab8RN6K8EQ_6X0SzZuhakaMRn14mvpMo32y2gHWAt3Vswa3bSA
GEMINI_KEY_4=AQ.Ab8RN6IBQKwEhOrBGqHmUqW038dqKswHUYBW1xKUN-WHFM-ipQ
GEMINI_KEY_5=AQ.Ab8RN6KPZtw413uHJFsR-YS-2_ATX6ecswTloJqWILuIcWMW0A
GEMINI_KEY_6=AQ.Ab8RN6KF4r7hZkx6SsbTL6BaExaAWL9CZxw5FyFnPtD31n0-aA
GEMINI_KEY_7=AQ.Ab8RN6I10JulVGpQspw-mM6-
GEMINI_KEY_8=AIwSrdAkcBaqraZlCPiR0YrITQ
GEMINI_KEY_9=AQ.Ab8RN6IQDaCV8RnR0utG3mvadgAcvW-gdnhFROTwxqrMj2_qxQ
GEMINI_KEY_10=AQ.Ab8RN6K5EIduWVFMK2sqigkcNJ1WPN-Io8UhaErai86iP5MsUA
GEMINI_KEY_11=AQ.Ab8RN6IsQcx6jmX7Y8_LH0ri600zIv1Tut-vJewJ18qL0FceZg
GEMINI_KEY_12=AQ.Ab8RN6LcMGH6rGynrQH-6J9z45DGaAqGOh-tr985HCNdyAxqiw
GEMINI_KEY_13=AQ.Ab8RN6IQDaCV8RnR0utG3mvadgAcvW-gdnhFROTwxqrMj2_qxQ
```

---

### 2.2. Thêm PayOS Java SDK vào [`pom.xml`](file:///d:/Desktop/SP26/Finaldoan/Code/BE/BE_CapNong/pom.xml#L133)
Đã bổ sung dependency chính thức của PayOS:

```xml
<!-- PayOS SDK -->
<dependency>
    <groupId>vn.payos</groupId>
    <artifactId>payos-java</artifactId>
    <version>2.0.1</version>
</dependency>
```

---

### 2.3. Ánh xạ cấu hình trong Spring Boot ([`application-local.yml`](file:///d:/Desktop/SP26/Finaldoan/Code/BE/BE_CapNong/src/main/resources/application-local.yml#L88) & [`application-server.yml`](file:///d:/Desktop/SP26/Finaldoan/Code/BE/BE_CapNong/src/main/resources/application-server.yml#L50))
Đã cấu hình khối `payos` lấy trực tiếp các giá trị từ file `.env` mà không dùng giá trị mặc định hardcode phía sau:

```yaml
# ===============================
# PAYOS CONFIGURATION
# ===============================
payos:
  client-id: ${PAYOS_CLIENT_ID}
  api-key: ${PAYOS_API_KEY}
  checksum-key: ${PAYOS_CHECKSUM_KEY}
  return-url: ${PAYOS_RETURN_URL}
  cancel-url: ${PAYOS_CANCEL_URL}
```

---

### 2.4. Khởi tạo Bean cấu hình ([`PayOSConfig.java`](file:///d:/Desktop/SP26/Finaldoan/Code/BE/BE_CapNong/src/main/java/com/sang/be_capnong/config/PayOSConfig.java))
Đã tạo file cấu hình Spring `@Configuration` tại package `com.sang.be_capnong.config`:

```java
package com.sang.be_capnong.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
```

---

## 3. [Tham Khảo] Hướng Dẫn Mẫu Cho Các Task Triển Khai Nghiệp Vụ Tiếp Theo

> ⚠️ **LƯU Ý QUAN TRỌNG:**
> Hiện tại nhánh `feature/Config-PayOs,-AI-keys` **CHỈ thực hiện cấu hình nền tảng** (khai báo biến môi trường `.env`, thêm dependency SDK và tạo Bean `PayOSConfig`).
> Dự án **CHƯA có Controller hay Service thanh toán này**. Đoạn code dưới đây chỉ là **code mẫu tham khảo cú pháp của PayOS SDK** để bạn hoặc đội ngũ phát triển dễ dàng viết API ở các task nghiệp vụ tiếp theo.

### 3.1. Gợi ý tiêm Bean `PayOS` vào Service (Khi viết PaymentService)
Bất kỳ Service thanh toán nào sau này (ví dụ `PaymentServiceImpl` hoặc `CheckoutServiceImpl`) đều có thể tiêm trực tiếp:

```java
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PayOS payOS;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;
}
```

### 3.2. Ví dụ tạo liên kết thanh toán (Create Payment Link)
```java
// 1. Tạo đối tượng ItemData
ItemData item = ItemData.builder()
        .name("Nông sản giải cứu")
        .quantity(1)
        .price(50000)
        .build();

// 2. Tạo PaymentData
long orderCode = System.currentTimeMillis() / 1000; // Mã đơn số nguyên duy nhất
PaymentData paymentData = PaymentData.builder()
        .orderCode(orderCode)
        .amount(50000)
        .description("Thanh toan don " + orderCode)
        .returnUrl(returnUrl)
        .cancelUrl(cancelUrl)
        .item(item)
        .build();

// 3. Gọi SDK tạo link thanh toán
CheckoutResponseData data = payOS.createPaymentLink(paymentData);
String checkoutUrl = data.getCheckoutUrl();
String qrCode = data.getQrCode();
```

### 3.3. Xác thực Webhook nhận từ PayOS
```java
@PostMapping("/webhook")
public ApiResponse<WebhookData> handlePayOSWebhook(@RequestBody Webhook webhookBody) {
    // PayOS SDK tự động kiểm tra chữ ký checksum với checksumKey
    WebhookData data = payOS.verifyPaymentWebhookData(webhookBody);
    
    // Cập nhật trạng thái đơn hàng / thanh toán
    return ApiResponse.<WebhookData>builder().result(data).build();
}
```

---

## 4. Kết Quả Kiểm Tra (Verification)
- Đã chạy kiểm tra cú pháp và build bằng Maven:
  ```powershell
  .\mvnw.cmd test-compile
  ```
- Kết quả: **`BUILD SUCCESS`** (Toàn bộ 143 file mã nguồn được biên dịch thành công, không có lỗi xung đột thư viện).
