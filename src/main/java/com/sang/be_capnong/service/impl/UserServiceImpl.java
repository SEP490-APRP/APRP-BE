package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.BuyerCreationRequest;
import com.sang.be_capnong.dto.request.ResendOtpRequest;
import com.sang.be_capnong.dto.request.ShipperCreationRequest;
import com.sang.be_capnong.dto.request.ShopCreationRequest;
import com.sang.be_capnong.dto.request.UserCreationRequest;
import com.sang.be_capnong.dto.request.UserUpdateRequest;
import com.sang.be_capnong.dto.request.VerifyOtpRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.HashSet;
import java.util.Set;
import com.sang.be_capnong.entity.Role;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.UserMapper;
import com.sang.be_capnong.repository.RoleRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.CloudinaryService;
import com.sang.be_capnong.service.OtpService;
import com.sang.be_capnong.service.UserService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    OtpService otpService;
    CloudinaryService cloudinaryService;

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
        return userMapper.toUserResponse(user);
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
        // Cập nhật các trường chung cho tất cả các Role
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String newPhone = request.getPhoneNumber().trim();
            if (!newPhone.equals(user.getPhoneNumber()) && userRepository.existsByPhoneNumberAndIdNot(newPhone, user.getId())) {
                throw new AppException(ErrorCode.PHONE_EXISTED);
            }
            user.setPhoneNumber(newPhone);
        }
        if (request.getAddress() != null) user.setAddress(request.getAddress());

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
            if (request.getBankAccount() != null) user.setBankAccount(request.getBankAccount());
            if (request.getBankAccountHolder() != null) user.setBankAccountHolder(request.getBankAccountHolder());
            if (request.getBankName() != null) user.setBankName(request.getBankName());
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
}
