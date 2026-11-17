package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.BuyerCreationRequest;
import com.sang.be_capnong.dto.request.ChangePasswordRequest;
import com.sang.be_capnong.dto.request.ResendOtpRequest;
import com.sang.be_capnong.dto.request.ShipperCreationRequest;
import com.sang.be_capnong.dto.request.ShopCreationRequest;
import com.sang.be_capnong.dto.request.UserCreationRequest;
import com.sang.be_capnong.dto.request.UserUpdateRequest;
import com.sang.be_capnong.dto.request.VerifyOtpRequest;
import com.sang.be_capnong.dto.response.AdminUserReportItemResponse;
import com.sang.be_capnong.dto.response.AdminUserReportResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ShopRiskFlagResponse;
import com.sang.be_capnong.dto.response.UserResponse;
import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.entity.ReturnRequest;
import com.sang.be_capnong.entity.Role;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.DeliveryFailReason;
import com.sang.be_capnong.enums.ReturnStatus;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.UserMapper;
import com.sang.be_capnong.repository.OrderRepository;
import com.sang.be_capnong.repository.ReturnRequestRepository;
import com.sang.be_capnong.repository.ReviewRepository;
import com.sang.be_capnong.repository.RoleRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.CloudinaryService;
import com.sang.be_capnong.service.NotificationService;
import com.sang.be_capnong.service.OtpService;
import com.sang.be_capnong.service.UserService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    OrderRepository orderRepository;
    ReturnRequestRepository returnRequestRepository;
    ReviewRepository reviewRepository;
    NotificationService notificationService;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    OtpService otpService;
    CloudinaryService cloudinaryService;

    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    static final List<DeliveryFailReason> SUBJECTIVE_FAIL_REASONS = List.of(
            DeliveryFailReason.KHONG_LIEN_LAC_DUOC,
            DeliveryFailReason.KHACH_TU_CHOI
    );

    static final List<ReturnStatus> ACTIVE_RISK_STATUSES = List.of(
            ReturnStatus.ADMIN_APPROVED,
            ReturnStatus.RETURN_SHIPPING,
            ReturnStatus.RETURN_PICKED_UP,
            ReturnStatus.RETURN_DELIVERED,
            ReturnStatus.COMPLETED
    );

    @Override
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        log.info("createUser with email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        String requestedRole = request.getRoleName() != null ? request.getRoleName().toUpperCase() : "BUYER";
        Role defaultRole = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        user.setRole(defaultRole);
        user.setStatus(UserStatus.INACTIVE);
        user.setCreateAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        otpService.generateAndSendOtp(savedUser.getEmail());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse createBuyer(BuyerCreationRequest request) {
        log.info("createBuyer with email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = roleRepository.findByName("BUYER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        user.setRole(role);
        boolean isEmailVerified = Boolean.TRUE.equals(request.getEmailVerified());
        user.setStatus(isEmailVerified ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        user.setLoyaltyPoints(BigDecimal.ZERO);
        user.setTotalRescuedWeight(BigDecimal.ZERO);
        user.setCreateAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        if (!isEmailVerified) {
            otpService.generateAndSendOtp(savedUser.getEmail());
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse createShop(ShopCreationRequest request) {
        log.info("createShop with email: {}, shopName: {}", request.getEmail(), request.getShopName());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }
        if (request.getCccd() != null && userRepository.existsByCccd(request.getCccd())) {
            throw new AppException(ErrorCode.CCCD_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = roleRepository.findByName("SHOP")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        user.setRole(role);
        boolean isEmailVerified = Boolean.TRUE.equals(request.getEmailVerified());
        user.setStatus(isEmailVerified ? UserStatus.PENDING : UserStatus.INACTIVE);
        user.setShopActive(true);
        user.setRatingAverage(0.0);
        user.setCreateAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        if (!isEmailVerified) {
            otpService.generateAndSendOtp(savedUser.getEmail());
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse createShipper(ShipperCreationRequest request) {
        log.info("createShipper with email: {}, vehicleNumber: {}", request.getEmail(), request.getVehicleNumber());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }
        if (request.getCccd() != null && userRepository.existsByCccd(request.getCccd())) {
            throw new AppException(ErrorCode.CCCD_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = roleRepository.findByName("SHIPPER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        user.setRole(role);
        boolean isEmailVerified = Boolean.TRUE.equals(request.getEmailVerified());
        user.setStatus(isEmailVerified ? UserStatus.PENDING : UserStatus.INACTIVE);
        user.setCreateAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        if (!isEmailVerified) {
            otpService.generateAndSendOtp(savedUser.getEmail());
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse verifyAccount(VerifyOtpRequest request) {
        log.info("verifyAccount for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.PENDING) {
            throw new AppException(ErrorCode.USER_ALREADY_ACTIVE);
        }

        otpService.verifyOtp(request.getEmail(), request.getOtp());

        boolean isShopOrShipper = user.getRole() != null && (
                "SHOP".equalsIgnoreCase(user.getRole().getName())
                || "SHIPPER".equalsIgnoreCase(user.getRole().getName())
        );

        user.setStatus(isShopOrShipper ? UserStatus.PENDING : UserStatus.ACTIVE);
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public void resendVerificationOtp(ResendOtpRequest request) {
        log.info("resendVerificationOtp for email: {}", request.getEmail());
        otpService.resendOtp(request.getEmail());
    }

    @Override
    public UserResponse getMyInfo() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("getMyInfo for user id: {}", currentUserId);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        UserResponse response = userMapper.toUserResponse(user);
        return enrichPenaltyAndRiskInfo(user, response);
    }

    @Override
    @Transactional
    public UserResponse updateMyInfo(UserUpdateRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("updateMyInfo for user id: {}", currentUserId);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        updateUserFieldsByRole(user, request);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
                throw new AppException(ErrorCode.OLD_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setUpdateAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("changePassword for user id: {}", currentUserId);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }

        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()
                && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_NOT_MATCH);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password successfully changed for user id: {}", currentUserId);
    }

    @Override
    @Transactional
    public UserResponse updateAvatar(MultipartFile file) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("updateAvatar for user id: {}", currentUserId);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String folder = "users";
        if (user.getRole() != null && "SHOP".equalsIgnoreCase(user.getRole().getName())) {
            folder = "shops";
        }

        String imageUrl = cloudinaryService.uploadImage(file, folder);
        user.setLogoUrl(imageUrl);
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Integer userId, UserUpdateRequest request) {
        log.info("updateUser for user id: {}", userId);
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (!SecurityUtils.isAdmin() && (currentUserId == null || !currentUserId.equals(userId))) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        updateUserFieldsByRole(user, request);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setUpdateAt(LocalDateTime.now());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    /**
     * Helper cập nhật thông tin User dựa trên Role của tài khoản:
     * - Common fields (fullName, phoneNumber, address) áp dụng cho mọi role.
     * - Role-specific fields chỉ cập nhật nếu user sở hữu Role tương ứng.
     */
    private void updateUserFieldsByRole(User user, UserUpdateRequest request) {
        // Cập nhật các trường chung cho tất cả các Role (bao gồm thông tin cá nhân & tài khoản ngân hàng liên kết ví)
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String newPhone = request.getPhoneNumber().trim();
            if (!newPhone.equals(user.getPhoneNumber()) && userRepository.existsByPhoneNumberAndIdNot(newPhone, user.getId())) {
                throw new AppException(ErrorCode.PHONE_EXISTED);
            }
            user.setPhoneNumber(newPhone);
        }
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getCccd() != null) user.setCccd(request.getCccd());
        if (request.getBankAccount() != null) user.setBankAccount(request.getBankAccount());
        if (request.getBankAccountHolder() != null) user.setBankAccountHolder(request.getBankAccountHolder());
        if (request.getBankName() != null) user.setBankName(request.getBankName());

        String roleName = (user.getRole() != null && user.getRole().getName() != null)
                ? user.getRole().getName().toUpperCase()
                : "";

        // Cập nhật thông tin đặc thù của BUYER
        if ("BUYER".equals(roleName)) {
            if (request.getDietPreference() != null) user.setDietPreference(request.getDietPreference());
        }

        // Cập nhật thông tin đặc thù của SHOP
        if ("SHOP".equals(roleName)) {
            if (request.getShopName() != null) user.setShopName(request.getShopName());
            if (request.getDescription() != null) user.setDescription(request.getDescription());
            if (request.getLogoUrl() != null) user.setLogoUrl(request.getLogoUrl());
            if (request.getAchievement() != null) user.setAchievement(request.getAchievement());
            if (request.getLicense() != null) user.setLicense(request.getLicense());
            if (request.getLicenseImageUrl() != null) user.setLicenseImageUrl(request.getLicenseImageUrl());
        }

        // Cập nhật thông tin đặc thù của SHIPPER
        if ("SHIPPER".equals(roleName)) {
            if (request.getVehicleNumber() != null) user.setVehicleNumber(request.getVehicleNumber());
            if (request.getVehicleDocImageUrl() != null) user.setVehicleDocImageUrl(request.getVehicleDocImageUrl());
            if (request.getLicense() != null) user.setLicense(request.getLicense());
            if (request.getLicenseImageUrl() != null) user.setLicenseImageUrl(request.getLicenseImageUrl());
        }
    }

    @Override
    public UserResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        UserResponse response = userMapper.toUserResponse(user);

        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        boolean isOwnerOrAdmin = (currentUserId != null && currentUserId.equals(userId)) || SecurityUtils.isAdmin();
        if (!isOwnerOrAdmin) {
            // Bảo mật: Ẩn thông tin tài khoản ngân hàng và tài liệu pháp lý khi xem công khai
            response.setBankAccount(null);
            response.setBankAccountHolder(null);
            response.setBankName(null);
            response.setLicense(null);
            response.setLicenseImageUrl(null);
            response.setVehicleDocImageUrl(null);
        } else {
            enrichPenaltyAndRiskInfo(user, response);
        }
        return response;
    }

    private UserResponse enrichPenaltyAndRiskInfo(User user, UserResponse response) {
        if (response == null || user == null) return response;
        long fails = countDeliveryFailures(user.getId());
        response.setFailedDeliveryCount((int) fails);
        response.setCodBlocked(fails >= 3);
        if (user.getLockedAt() != null) {
            response.setSuspendedUntil(user.getLockedAt().plusDays(7));
        }
        if (user.getRole() != null && "SHOP".equalsIgnoreCase(user.getRole().getName())) {
            response.setActiveRiskFlags((int) countActiveRiskFlags(user.getId()));
        }
        return response;
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    public void deleteUser(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        userRepository.deleteById(userId);
    }

    @Override
    @Transactional
    public UserResponse approveShopOwner(Integer userId) {
        log.info("approveShopOwner for user id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(UserStatus.ACTIVE);
        user.setShopActive(true);
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse approveShipper(Integer userId) {
        log.info("approveShipper for user id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(UserStatus.ACTIVE);
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public void activateUser(Integer userId) {
        log.info("activateUser for user id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(UserStatus.ACTIVE);
        user.setUpdateAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Integer userId) {
        log.info("deactivateUser for user id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(UserStatus.INACTIVE);
        user.setUpdateAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public PageResponse<UserResponse> getUsersByRolePaged(List<String> roles, String statusStr, int page, int size) {
        log.info("getUsersByRolePaged roles: {}, status: {}, page: {}, size: {}", roles, statusStr, page, size);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("id").descending());

        Set<String> normalizedRoles = new HashSet<>();
        if (roles != null && !roles.isEmpty()) {
            for (String r : roles) {
                if (r == null || r.isBlank()) continue;
                normalizedRoles.add(r.trim().toUpperCase());
            }
        }

        UserStatus userStatus = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                userStatus = UserStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid user status: {}", statusStr);
            }
        }

        Page<User> userPage;
        if (!normalizedRoles.isEmpty() && userStatus != null) {
            userPage = userRepository.findByRole_NameInAndStatus(normalizedRoles, userStatus, pageable);
        } else if (!normalizedRoles.isEmpty()) {
            userPage = userRepository.findByRole_NameIn(normalizedRoles, pageable);
        } else if (userStatus != null) {
            userPage = userRepository.findByStatus(userStatus, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        Page<UserResponse> responsePage = userPage.map(userMapper::toUserResponse);
        return PageResponse.from(responsePage);
    }

    // =========================================================================
    // ===== CUSTOMER PENALTY SYSTEM (CHỐNG BOM HÀNG) ==========================
    // =========================================================================

    public static boolean isCustomerFault(DeliveryFailReason reason) {
        return reason == DeliveryFailReason.KHONG_LIEN_LAC_DUOC || reason == DeliveryFailReason.KHACH_TU_CHOI;
    }

    @Override
    public long countDeliveryFailures(int buyerId) {
        return orderRepository.countByBuyer_IdAndStatusAndFailReasonIn(
                buyerId, com.sang.be_capnong.enums.OrderStatus.FAILED, SUBJECTIVE_FAIL_REASONS
        );
    }

    @Override
    public boolean isCodBlocked(int buyerId) {
        return countDeliveryFailures(buyerId) >= 3;
    }

    @Override
    @Transactional
    public void handleDeliveryFailed(Order order, DeliveryFailReason reason) {
        if (order == null || order.getBuyer() == null || !isCustomerFault(reason)) {
            return;
        }

        User buyer = userRepository.findById(order.getBuyer().getId()).orElse(null);
        if (buyer == null) {
            return;
        }

        long currentFails = countDeliveryFailures(buyer.getId());
        log.warn("[CustomerPenalty] Buyer id={}, orderId={}, reason={}, currentFails={}",
                buyer.getId(), order.getOrderId(), reason, currentFails);

        if (currentFails >= 5) {
            // Ngưỡng 2: Khách bom hàng 5 lần -> Tự động tạm khóa tài khoản (suspended) trong 7 ngày
            buyer.setStatus(UserStatus.LOCKED);
            buyer.setLockedAt(LocalDateTime.now());
            userRepository.save(buyer);

            String untilStr = LocalDateTime.now().plusDays(7).format(DATE_FORMATTER);
            notificationService.sendNotificationToUser(
                    buyer,
                    "Tài khoản bị tạm khóa 7 ngày do bom hàng",
                    "Tài khoản của bạn đã tích lũy 5 lần giao hàng không thành công do lỗi chủ quan (bom hàng). "
                            + "Hệ thống đã tự động tạm khóa tài khoản trong 7 ngày (đến " + untilStr + "). "
                            + "Vui lòng liên hệ CSKH CapNong nếu bạn cần hỗ trợ khiếu nại.",
                    "ACCOUNT_SUSPENDED",
                    order.getOrderId()
            );
            log.warn("[CustomerPenalty] Buyer id={} SUSPENDED until {} due to {} failed deliveries",
                    buyer.getId(), untilStr, currentFails);

        } else if (currentFails >= 3) {
            // Ngưỡng 1: Khách bị giao hàng thất bại 3 lần do lỗi chủ quan -> Cảnh báo tài khoản + Tước quyền thanh toán COD
            if (currentFails == 3) {
                notificationService.sendNotificationToUser(
                        buyer,
                        "Cảnh báo tài khoản: Tước quyền thanh toán COD",
                        "Bạn đã có 3 lần giao hàng không thành công do lỗi chủ quan (từ chối nhận hoặc không nghe máy). "
                                + "Tài khoản của bạn đã bị tước quyền thanh toán khi nhận hàng (COD). "
                                + "Từ bây giờ, bạn chỉ có thể chọn hình thức thanh toán trực tuyến qua PayOS.",
                        "ACCOUNT_PENALTY",
                        order.getOrderId()
                );
                log.warn("[CustomerPenalty] Buyer id={} COD BLOCKED due to {} failed deliveries",
                        buyer.getId(), currentFails);
            } else if (currentFails == 4) {
                notificationService.sendNotificationToUser(
                        buyer,
                        "Cảnh báo nghiêm trọng: Nguy cơ tạm khóa tài khoản",
                        "Bạn đã có 4 lần giao hàng không thành công (bom hàng). "
                                + "Nếu tiếp tục xảy ra lần thứ 5, tài khoản của bạn sẽ tự động bị tạm khóa 7 ngày.",
                        "ACCOUNT_PENALTY",
                        order.getOrderId()
                );
            }
        } else {
            // Lần 1 hoặc 2: Nhắc nhở tiến trình
            notificationService.sendNotificationToUser(
                    buyer,
                    "Nhắc nhở nhận hàng (Vi phạm lần " + currentFails + "/3)",
                    "Đơn hàng #" + order.getOrderId() + " đã giao thất bại do không liên lạc được hoặc bị từ chối nhận. "
                            + "Nếu tích lũy đủ 3 lần vi phạm, tài khoản sẽ bị tước quyền thanh toán COD.",
                    "ORDER_STATUS",
                    order.getOrderId()
            );
        }
    }

    @Override
    public void checkCodAllowed(User buyer) {
        if (buyer != null && isCodBlocked(buyer.getId())) {
            throw new AppException(ErrorCode.COD_NOT_ALLOWED,
                    "Tài khoản của bạn đã bị tước quyền thanh toán khi nhận hàng (COD) do có từ 3 lần giao hàng thất bại (bom hàng). "
                            + "Vui lòng chọn hình thức thanh toán trực tuyến qua PayOS.");
        }
    }

    @Override
    @Transactional
    public void autoUnsuspendAccounts() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<User> dueUsers = userRepository.findByStatusAndLockedAtBefore(UserStatus.LOCKED, sevenDaysAgo);
        for (User u : dueUsers) {
            u.setStatus(UserStatus.ACTIVE);
            u.setLockedAt(null);
            userRepository.save(u);
            notificationService.sendNotificationToUser(
                    u,
                    "Tài khoản đã được mở khóa",
                    "Thời hạn tạm khóa 7 ngày do bom hàng đã kết thúc. Tài khoản của bạn đã được mở khóa và có thể mua sắm trở lại. "
                            + "Vui lòng lưu ý quy tắc nhận hàng để tránh bị khóa vĩnh viễn.",
                    "ACCOUNT_UNLOCKED",
                    u.getId()
            );
            log.info("[CustomerPenalty] Auto-unsuspended user id={}", u.getId());
        }
    }

    @Override
    @Transactional
    public void resetCustomerPenalties(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setLockedAt(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
        notificationService.sendNotificationToUser(
                user,
                "Khôi phục quyền tài khoản",
                "Quản trị viên đã xem xét và gỡ bỏ các cảnh báo bom hàng cho tài khoản của bạn.",
                "ACCOUNT_RESTORED",
                user.getId()
        );
        log.info("[CustomerPenalty] Admin reset customer penalties for user id={}", userId);
    }

    // =========================================================================
    // ===== SELLER RISK FLAGGING (CỜ RỦI RO SHOP) =============================
    // =========================================================================

    @Override
    public long countActiveRiskFlags(int shopId) {
        return returnRequestRepository.countByShop_IdAndStatusInAndAdminRemarkIsNotNull(shopId, ACTIVE_RISK_STATUSES);
    }

    @Override
    @Transactional
    public void addRiskFlagForAdminApprovedReturn(ReturnRequest returnRequest, String adminRemark) {
        if (returnRequest == null || returnRequest.getShop() == null) {
            return;
        }

        User shop = userRepository.findById(returnRequest.getShop().getId()).orElse(null);
        if (shop == null) {
            return;
        }

        Order order = returnRequest.getOrderDetail() != null ? returnRequest.getOrderDetail().getOrder() : null;
        Integer orderId = order != null ? order.getOrderId() : null;

        long activeFlags = countActiveRiskFlags(shop.getId());
        log.warn("[ShopRiskFlag] Shop id={}, shopName={}, returnId={}, activeFlags={}",
                shop.getId(), shop.getShopName(), returnRequest.getReturnId(), activeFlags);

        if (activeFlags >= 3) {
            Double rawAvg = reviewRepository.getAverageRatingByShopId(shop.getId());
            double baseRating = (rawAvg != null && rawAvg > 0) ? Math.round(rawAvg * 10.0) / 10.0 : 5.0;
            double penalizedRating = Math.max(1.0, Math.round((baseRating - 1.0) * 10.0) / 10.0);
            shop.setRatingAverage(penalizedRating);
            userRepository.save(shop);

            notificationService.sendNotificationToUser(
                    shop,
                    "Cảnh báo chất lượng: Bị áp dụng hình phạt vi phạm",
                    "Cửa hàng của bạn đã tích lũy " + activeFlags + " khiếu nại giao hàng hỏng được Admin xác nhận (>= 3 Risk Flags). "
                            + "Hệ thống đã tự động trừ 1 sao trên điểm đánh giá công khai (còn " + penalizedRating + " sao) "
                            + "và giảm độ ưu tiên hiển thị sản phẩm của shop trên sàn. Vui lòng cải thiện chất lượng đóng gói và nông sản.",
                    "SHOP_PENALTY",
                    returnRequest.getReturnId()
            );
            log.warn("[ShopRiskFlag] Shop id={} PENALIZED with -1.0 star rating (now {}) and deprioritized listing",
                    shop.getId(), penalizedRating);

        } else {
            notificationService.sendNotificationToUser(
                    shop,
                    "Cảnh báo vi phạm giao hàng hỏng (Cờ " + activeFlags + "/3)",
                    "Khiếu nại giao hàng hỏng cho đơn hàng " + (orderId != null ? "#" + orderId : "")
                            + " đã được Admin chấp thuận (Gắn 1 Risk Flag). "
                            + "Nếu tích lũy đủ 3 Risk Flags, shop sẽ bị tự động trừ 1 sao đánh giá công khai và giảm độ ưu tiên hiển thị sản phẩm trên sàn.",
                    "SHOP_RISK_FLAG",
                    returnRequest.getReturnId()
            );
        }
    }

    @Override
    public List<ShopRiskFlagResponse> getMyRiskFlags() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return getShopRiskFlags(currentUserId);
    }

    @Override
    public List<ShopRiskFlagResponse> getShopRiskFlags(int shopId) {
        List<ReturnRequest> requests = returnRequestRepository
                .findByShop_IdAndStatusInAndAdminRemarkIsNotNullOrderByUpdatedAtDesc(shopId, ACTIVE_RISK_STATUSES);
        return requests.stream()
                .map(r -> {
                    Order order = r.getOrderDetail() != null ? r.getOrderDetail().getOrder() : null;
                    return ShopRiskFlagResponse.builder()
                            .id(r.getReturnId())
                            .shopId(r.getShop().getId())
                            .shopName(r.getShop().getShopName())
                            .returnId(r.getReturnId())
                            .orderId(order != null ? order.getOrderId() : null)
                            .reason(r.getReason())
                            .isActive(true)
                            .createdAt(r.getCreatedAt())
                            .resolvedAt(null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void resolveRiskFlag(int flagId, String resolutionNote) {
        ReturnRequest request = returnRequestRepository.findById(flagId)
                .orElseThrow(() -> new AppException(ErrorCode.RISK_FLAG_NOT_FOUND));

        User shop = request.getShop();
        request.setAdminRemark("Đã giải quyết cờ rủi ro: " + (resolutionNote != null ? resolutionNote : "Khắc phục vi phạm"));
        request.setStatus(ReturnStatus.REJECTED);
        returnRequestRepository.save(request);

        if (shop != null) {
            long remaining = countActiveRiskFlags(shop.getId());
            if (remaining < 3) {
                Double rawAvg = reviewRepository.getAverageRatingByShopId(shop.getId());
                double baseRating = (rawAvg != null && rawAvg > 0) ? Math.round(rawAvg * 10.0) / 10.0 : 5.0;
                shop.setRatingAverage(baseRating);
                userRepository.save(shop);

                notificationService.sendNotificationToUser(
                        shop,
                        "Khôi phục điểm đánh giá Shop",
                        "Quản trị viên đã giải quyết cờ rủi ro #" + flagId + ". "
                                + "Số cờ rủi ro hiện tại còn " + remaining + "/3. Điểm đánh giá công khai của shop đã được khôi phục về "
                                + baseRating + " sao.",
                        "SHOP_RESTORED",
                        flagId
                );
            }
        }
        log.info("[ShopRiskFlag] Resolved risk flag/return #{} with note: {}", flagId, resolutionNote);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserReportResponse getUserReport(String type, String from, String to) {
        long totalUsers = userRepository.count();
        long totalBuyers = userRepository.countByRole_Name("BUYER");
        long totalShops = userRepository.countByRole_Name("SHOP");
        long totalShipper = userRepository.countByRole_Name("SHIPPER");
        long totalActiveUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long totalInactiveUsers = userRepository.countByStatus(UserStatus.INACTIVE);
        long totalPending = userRepository.countByStatus(UserStatus.PENDING);
        long totalShopSelling = userRepository.countByRole_NameAndShopActive("SHOP", true);
        long totalShopNotSelling = Math.max(0, totalShops - totalShopSelling);
        long totalUserLock = userRepository.countByStatus(UserStatus.LOCKED);

        List<AdminUserReportItemResponse> items = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime rangeEnd = today.atTime(LocalTime.MAX);

        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            rangeEnd = toDate.atTime(LocalTime.MAX);

            long daysDiff = ChronoUnit.DAYS.between(fromDate, toDate);
            if (daysDiff <= 31) {
                LocalDate curr = fromDate;
                while (!curr.isAfter(toDate)) {
                    LocalDateTime bStart = curr.atStartOfDay();
                    LocalDateTime bEnd = curr.atTime(LocalTime.MAX);
                    String dayLabel = formatDayOfWeek(curr.getDayOfWeek()) + " (" + curr.format(DateTimeFormatter.ofPattern("dd/MM")) + ")";
                    items.add(buildUserReportItem(bStart, bEnd, curr.toString(), formatDayOfWeek(curr.getDayOfWeek()), dayLabel));
                    curr = curr.plusDays(1);
                }
            } else {
                LocalDate curr = fromDate.withDayOfMonth(1);
                while (!curr.isAfter(toDate)) {
                    LocalDateTime bStart = curr.atStartOfDay();
                    LocalDate nextMonth = curr.plusMonths(1).withDayOfMonth(1);
                    LocalDateTime bEnd = nextMonth.minusDays(1).atTime(LocalTime.MAX);
                    if (bEnd.isAfter(rangeEnd)) bEnd = rangeEnd;
                    String monthLabel = "Thg " + curr.getMonthValue() + "/" + curr.getYear();
                    items.add(buildUserReportItem(bStart, bEnd, curr.toString(), "T" + curr.getMonthValue(), monthLabel));
                    curr = nextMonth;
                }
            }
        } else if ("year".equalsIgnoreCase(type)) {
            int currentYear = today.getYear();
            for (int m = 1; m <= 12; m++) {
                LocalDate mStart = LocalDate.of(currentYear, m, 1);
                LocalDate mEnd = mStart.plusMonths(1).minusDays(1);
                LocalDateTime bStart = mStart.atStartOfDay();
                LocalDateTime bEnd = mEnd.atTime(LocalTime.MAX);
                String monthLabel = "Thg " + m;
                items.add(buildUserReportItem(bStart, bEnd, mStart.toString(), "T" + m, monthLabel));
            }
        } else if ("all".equalsIgnoreCase(type)) {
            LocalDate startMonth = today.minusMonths(11).withDayOfMonth(1);
            LocalDate curr = startMonth;
            for (int i = 0; i < 12; i++) {
                LocalDate mEnd = curr.plusMonths(1).minusDays(1);
                LocalDateTime bStart = curr.atStartOfDay();
                LocalDateTime bEnd = mEnd.atTime(LocalTime.MAX);
                String monthLabel = curr.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                items.add(buildUserReportItem(bStart, bEnd, curr.toString(), "T" + curr.getMonthValue(), monthLabel));
                curr = curr.plusMonths(1);
            }
        } else if ("month".equalsIgnoreCase(type)) {
            for (int i = 29; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                LocalDateTime bStart = d.atStartOfDay();
                LocalDateTime bEnd = d.atTime(LocalTime.MAX);
                String label = d.format(DateTimeFormatter.ofPattern("dd/MM"));
                items.add(buildUserReportItem(bStart, bEnd, d.toString(), formatDayOfWeek(d.getDayOfWeek()), label));
            }
        } else {
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                LocalDateTime bStart = d.atStartOfDay();
                LocalDateTime bEnd = d.atTime(LocalTime.MAX);
                String period = formatDayOfWeek(d.getDayOfWeek());
                String label = period + " (" + d.format(DateTimeFormatter.ofPattern("dd/MM")) + ")";
                items.add(buildUserReportItem(bStart, bEnd, d.toString(), period, label));
            }
        }

        return AdminUserReportResponse.builder()
                .totalUsers(totalUsers)
                .totalBuyers(totalBuyers)
                .totalShops(totalShops)
                .totalShipper(totalShipper)
                .totalActiveUsers(totalActiveUsers)
                .totalInactiveUsers(totalInactiveUsers)
                .totalShopPending(totalPending)
                .totalShopSelling(totalShopSelling)
                .totalShopNotSelling(totalShopNotSelling)
                .totalUserLock(totalUserLock)
                .userReports(items)
                .build();
    }

    private AdminUserReportItemResponse buildUserReportItem(LocalDateTime bStart, LocalDateTime bEnd, String dateStr, String periodStr, String labelStr) {
        List<User> createdUsers = userRepository.findByCreateAtBetween(bStart, bEnd);
        List<User> lockedUsers = userRepository.findByLockedAtBetween(bStart, bEnd);

        long buyers = 0;
        long shops = 0;
        long shippers = 0;

        for (User u : createdUsers) {
            if (u.getRole() != null) {
                String roleName = u.getRole().getName();
                if ("BUYER".equalsIgnoreCase(roleName)) buyers++;
                else if ("SHOP".equalsIgnoreCase(roleName)) shops++;
                else if ("SHIPPER".equalsIgnoreCase(roleName)) shippers++;
            }
        }

        long locked = lockedUsers != null ? lockedUsers.size() : 0;
        long total = buyers + shops + shippers;

        return AdminUserReportItemResponse.builder()
                .date(dateStr)
                .period(periodStr)
                .label(labelStr)
                .buyers(buyers)
                .shops(shops)
                .shippers(shippers)
                .locked(locked)
                .total(total)
                .newUsers(total)
                .activeUsers(total)
                .totalBuyers(buyers)
                .totalShops(shops)
                .totalShipper(shippers)
                .totalUserLock(locked)
                .build();
    }

    private String formatDayOfWeek(DayOfWeek dow) {
        switch (dow) {
            case MONDAY: return "T2";
            case TUESDAY: return "T3";
            case WEDNESDAY: return "T4";
            case THURSDAY: return "T5";
            case FRIDAY: return "T6";
            case SATURDAY: return "T7";
            case SUNDAY: return "CN";
            default: return "";
        }
    }
}
