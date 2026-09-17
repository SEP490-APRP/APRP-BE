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
import com.sang.be_capnong.service.OtpService;
import com.sang.be_capnong.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public UserResponse updateUser(Integer userId, UserUpdateRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public UserResponse getUserById(Integer userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<UserResponse> getAllUsers() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteUser(Integer userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
