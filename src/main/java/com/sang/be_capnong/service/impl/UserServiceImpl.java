package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.BuyerCreationRequest;
import com.sang.be_capnong.dto.request.ResendOtpRequest;
import com.sang.be_capnong.dto.request.ShipperCreationRequest;
import com.sang.be_capnong.dto.request.ShopCreationRequest;
import com.sang.be_capnong.dto.request.UserCreationRequest;
import com.sang.be_capnong.dto.request.UserUpdateRequest;
import com.sang.be_capnong.dto.request.VerifyOtpRequest;
import com.sang.be_capnong.dto.response.UserResponse;
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

        Role defaultRole = roleRepository.findByName("BUYER")
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
        user.setStatus(UserStatus.INACTIVE);
        user.setLoyaltyPoints(BigDecimal.ZERO);
        user.setTotalRescuedWeight(BigDecimal.ZERO);
        user.setCreateAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        otpService.generateAndSendOtp(savedUser.getEmail());

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
        user.setStatus(UserStatus.INACTIVE);
        user.setShopActive(true);
        user.setRatingAverage(0.0);
        user.setCreateAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        otpService.generateAndSendOtp(savedUser.getEmail());

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
        user.setStatus(UserStatus.INACTIVE);
        user.setCreateAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        otpService.generateAndSendOtp(savedUser.getEmail());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse verifyAccount(VerifyOtpRequest request) {
        log.info("verifyAccount for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_ALREADY_ACTIVE);
        }

        otpService.verifyOtp(request.getEmail(), request.getOtp());

        user.setStatus(UserStatus.ACTIVE);
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

        userMapper.updateUser(user, request);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
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
        if (user.getRole() != null && ("SHOP".equalsIgnoreCase(user.getRole().getName()) || "SHOP_OWNER".equalsIgnoreCase(user.getRole().getName()))) {
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdateAt(LocalDateTime.now());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
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
}
