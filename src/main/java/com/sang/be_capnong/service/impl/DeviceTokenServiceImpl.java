package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.DeviceTokenRegisterRequest;
import com.sang.be_capnong.dto.request.DeviceTokenUnregisterRequest;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.entity.UserDeviceToken;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.UserDeviceTokenRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.DeviceTokenService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceTokenServiceImpl implements DeviceTokenService {

    UserDeviceTokenRepository userDeviceTokenRepository;
    UserRepository userRepository;

    @Override
    @Transactional
    public void registerDeviceToken(DeviceTokenRegisterRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String token = request.getDeviceToken().trim();

        // 1. Vô hiệu hóa token cũ của cùng 1 thiết bị vật lý (nếu token string thay đổi)
        if (request.getDeviceName() != null && !request.getDeviceName().isBlank()) {
            java.util.List<UserDeviceToken> userTokens = userDeviceTokenRepository.findByUserAndIsActiveTrue(user);
            for (UserDeviceToken ut : userTokens) {
                if (request.getDeviceName().equalsIgnoreCase(ut.getDeviceName()) && !token.equals(ut.getDeviceToken())) {
                    ut.setIsActive(false);
                    ut.setUpdatedAt(LocalDateTime.now());
                    userDeviceTokenRepository.save(ut);
                }
            }
        }

        Optional<UserDeviceToken> existingOpt = userDeviceTokenRepository.findByDeviceToken(token);

        if (existingOpt.isPresent()) {
            UserDeviceToken existing = existingOpt.get();
            existing.setUser(user);
            existing.setIsActive(true);
            existing.setLastUsedAt(LocalDateTime.now());
            if (request.getPlatform() != null) {
                existing.setPlatform(request.getPlatform().trim().toUpperCase());
            }
            if (request.getDeviceName() != null) {
                existing.setDeviceName(request.getDeviceName());
            }
            if (request.getTokenType() != null) {
                existing.setTokenType(request.getTokenType());
            }
            userDeviceTokenRepository.save(existing);
            log.info("[DeviceToken] Reactivated device token for userId={}, platform={}", userId, existing.getPlatform());
        } else {
            UserDeviceToken newToken = UserDeviceToken.builder()
                    .user(user)
                    .deviceToken(token)
                    .platform(request.getPlatform() != null ? request.getPlatform().trim().toUpperCase() : "UNKNOWN")
                    .deviceName(request.getDeviceName())
                    .tokenType(request.getTokenType() != null ? request.getTokenType() : "EXPO")
                    .isActive(true)
                    .lastUsedAt(LocalDateTime.now())
                    .build();
            userDeviceTokenRepository.save(newToken);
            log.info("[DeviceToken] Registered new device token for userId={}, platform={}", userId, newToken.getPlatform());
        }
    }

    @Override
    @Transactional
    public void unregisterDeviceToken(DeviceTokenUnregisterRequest request) {
        if (request == null || request.getDeviceToken() == null) return;
        String token = request.getDeviceToken().trim();
        int updated = userDeviceTokenRepository.deactivateToken(token, LocalDateTime.now());
        log.info("[DeviceToken] Unregistered device token (count={})", updated);
    }
}

