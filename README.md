# APRP-BE (Agricultural Produce Rescue Platform - Backend)

> **Dự án:** Nền tảng Giải cứu Nông sản Việt Nam (Agricultural Produce Rescue Platform - APRP)  
> **Đồ án tốt nghiệp / Capstone Project:** SEP490 (Học kỳ SP26 - Group 3)

---

## 📌 1. Giới thiệu Dự án

**BE_CapNong** là hệ thống Backend được xây dựng dưới dạng **RESTful API** sử dụng Java & Spring Boot. Dự án đóng vai trò làm máy chủ trung tâm quản lý toàn bộ dữ liệu, nghiệp vụ xác thực người dùng, phân quyền bảo mật, quản lý tài khoản (Nông dân, Thương lái, Admin...) và kết nối hỗ trợ tiêu thụ/giải cứu nông sản.

### 🛠️ Công nghệ & Thư viện sử dụng
- **Ngôn ngữ & SDK:** Java 21
- **Framework chính:** Spring Boot 3.5.6 (Spring Web, Spring Data JPA, Spring Security, Spring Validation)
- **Cơ sở dữ liệu:** MySQL 8.0 (kết nối qua `mysql-connector-j`, quản lý ORM bằng Hibernate JPA)
- **Xác thực & Bảo mật:**
  - JWT (JSON Web Token) với thư viện `nimbus-jose-jwt`
  - Spring Security OAuth2 Resource Server
  - Mã hóa mật khẩu bằng BCrypt (`PasswordEncoder`)
  - Cơ chế thu hồi Token khi đăng xuất (`InvalidatedToken`)
- **Mapping & Utilities:** Lombok, MapStruct 1.5.5
- **Tài liệu API:** OpenAPI 3.0 / Swagger UI (`springdoc-openapi-starter-webmvc-ui` v2.8.5)
- **Containerization & Config:** Docker Compose (khởi tạo MySQL), Dotenv (`.env` file)

---

## 📂 2. Cấu trúc Dự án & Nhiệm vụ của từng Package

Mã nguồn chính của ứng dụng nằm tại thư mục: `src/main/java/com/sang/be_capnong/`

```text
com.sang.be_capnong/
├── BE_CapNong.java             # Entrypoint chính khởi chạy Spring Boot Application
├── config/                     # Cấu hình hệ thống (Security, CORS, JWT, Swagger)
├── controller/                 # Tiếp nhận & Xử lý HTTP Request (REST APIs)
├── dto/                        # Data Transfer Objects (Data Request & Response)
│   ├── request/                # Chứa các DTO dữ liệu đầu vào từ Client
│   └── response/               # Chứa các DTO dữ liệu trả về cho Client
├── entity/                     # Ánh xạ bảng trong CSDL (JPA Entities)
├── exception/                  # Quản lý & Xử lý lỗi tập trung (Global Exception Handler)
├── intializer/                 # Tự động nạp dữ liệu mặc định khi ứng dụng khởi động
├── mapper/                     # Chuyển đổi dữ liệu Entity <-> DTO (MapStruct)
├── repository/                 # Thao tác dữ liệu với CSDL (Spring Data JPA)
└── service/                    # Xử lý logic nghiệp vụ (Business Logic Layer)
    └── impl/                   # Lớp triển khai cụ thể (Implementation) của các Service
```

### 📋 Chi tiết Nhiệm vụ của từng Package

| STT | Package | Nhiệm vụ & Chức năng | Các File tiêu biểu |
|---|---|---|---|
| **1** | **`config/`** | **Cấu hình hệ thống:** Quản lý luồng bảo mật (Spring Security), cấu hình giải mã/kiểm tra Token JWT (`CustomJwtDecoder`), xử lý lỗi 401 Unauthorized (`JwtAuthenticationEntryPoint`), và cấu hình tài liệu Swagger UI (`OpenApiConfig`). | `SecurityConfig.java`<br>`CustomJwtDecoder.java`<br>`JwtAuthenticationEntryPoint.java`<br>`OpenApiConfig.java` |
| **2** | **`controller/`** | **Tầng REST API Controller:** Định nghĩa các Endpoint HTTP URL, nhận các Request từ Client, gọi xuống tầng `Service` xử lý và trả về dữ liệu chuẩn hóa `ApiResponse`. | `AuthenticationController.java`<br>`UserController.java` |
| **3** | **`dto/`** | **Tầng Chuyển đổi Dữ liệu (DTO):** Định nghĩa cấu trúc dữ liệu giao tiếp với Client, tránh lộ các trường thông tin nhạy cảm của Entity. Phân tách thành `request/` (nhận dữ liệu) và `response/` (trả dữ liệu). | `AuthenticationRequest.java`<br>`UserCreationRequest.java`<br>`ApiResponse.java`<br>`UserResponse.java` |
| **4** | **`entity/`** | **Tầng Ánh xạ Cơ sở dữ liệu (ORM Entity):** Khai báo các đối tượng ánh xạ trực tiếp thành các bảng trong MySQL thông qua Spring Data JPA/Hibernate. | `User.java` *(Bảng người dùng)*<br>`Role.java` *(Bảng vai trò)*<br>`InvalidatedToken.java` *(Bảng lưu JWT đã logout)* |
| **5** | **`exception/`** | **Quản lý Lỗi tập trung (Global Exception Handling):** Bắt toàn bộ ngoại lệ trong ứng dụng, ánh xạ sang các `ErrorCode` tương ứng và trả về định dạng lỗi chuẩn hóa cho Client. | `AppException.java`<br>`ErrorCode.java`<br>`GlobalExceptionHandler.java` |
| **6** | **`intializer/`** | **Khởi tạo dữ liệu mặc định:** Thực thi các đoạn mã khi ứng dụng vừa khởi động (VD: Tạo tài khoản Admin mặc định, khởi tạo các Role hệ thống nếu chưa có). | `DataInitializer.java` |
| **7** | **`mapper/`** | **Chuyển đổi Dữ liệu (Object Mapping):** Sử dụng MapStruct để tự động map dữ liệu giữa Entity và DTO một cách an toàn và tối ưu hiệu năng. | `UserMapper.java` |
| **8** | **`repository/`** | **Tầng Truy xuất Dữ liệu (DAO):** Chứa các Interface kế thừa `JpaRepository` để thực hiện các thao tác CRUD và các câu truy vấn cơ sở dữ liệu MySQL. | `UserRepository.java`<br>`RoleRepository.java`<br>`InvalidatedTokenRepository.java` |
| **9** | **`service/` & `service/impl/`** | **Tầng Xử lý Logic Nghiệp vụ (Business Logic):** Chứa toàn bộ logic xử lý chính của ứng dụng như đăng nhập, mã hóa mật khẩu, kiểm tra quyền hạn, tạo/hủy JWT token, quản lý thông tin người dùng. | `AuthenticationService.java` / `AuthenticationServiceImpl.java`<br>`UserService.java` / `UserServiceImpl.java` |

---

## 🚀 3. Hướng dẫn Khởi chạy Dự án (Getting Started)

### 📋 Yêu cầu môi trường
- **Java JDK:** 21 trở lên
- **Maven:** 3.8+ (hoặc dùng trực tiếp `./mvnw` đi kèm dự án)
- **Database:** MySQL 8.0 hoặc Docker Desktop

### 🛠️ Các bước thực hiện

#### Bước 1: Khởi động Cơ sở dữ liệu (MySQL)
Bạn có thể khởi động nhanh MySQL container thông qua **Docker Compose**:
```bash
docker-compose up -d
```
*Lưu ý:* Mặc định MySQL sẽ chạy trên port `3306`, Database name: `DB_CapNong`, User: `root`, Password: `123456789`.

#### Bước 2: Cấu hình Môi trường (`.env`)
Tạo hoặc kiểm tra file `.env` tại thư mục gốc của dự án với các cấu hình bí mật như JWT:
```env
JWT_SIGNER_KEY=QArxkDscJw8ogDUNNKhwlvtbLR96CtEIgDbUzkkeN+iqXg6ucLvwU/LPkDVxHpzM
JWT_VALID_DURATION=3600
JWT_REFRESHABLE_DURATION=360000
```

#### Bước 3: Chạy ứng dụng Spring Boot
Chạy lệnh Maven Wrapper sau tại thư mục gốc dự án:
- Trên Linux/macOS:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
  ```
- Trên Windows (PowerShell/CMD):
  ```powershell
  .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
  ```

---

## 📖 4. Tài liệu API (Swagger UI / OpenAPI)

Sau khi ứng dụng khởi chạy thành công trên cổng `8080`, bạn có thể xem và thực nghiệm toàn bộ danh sách API thông qua giao diện Swagger UI tại đường dẫn:

👉 **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
👉 **OpenAPI Docs (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
