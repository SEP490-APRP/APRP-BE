package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.ShipperStatusRequest;
import com.sang.be_capnong.dto.response.ShipperStatusResponse;
import com.sang.be_capnong.entity.ShipperLocation;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.ShipperLocationRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.ShipperService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShipperServiceImpl implements ShipperService {

    ShipperLocationRepository shipperLocationRepository;
    UserRepository userRepository;

    private User getCurrentUser() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        int userId = Integer.parseInt(subject);
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    @Transactional
    public void goOnline(Integer shipperId, ShipperStatusRequest request) {
        log.info("Shipper {} went online at {}, {}", shipperId, request.getLatitude(), request.getLongitude());
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!"SHIPPER".equalsIgnoreCase(shipper.getRole().getName())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ShipperLocation location = shipperLocationRepository.findByShipper(shipper)
                .orElse(ShipperLocation.builder().shipper(shipper).build());

        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setUpdatedAt(LocalDateTime.now());

        shipperLocationRepository.save(location);
    }

    @Override
    @Transactional
    public void goOffline(Integer shipperId) {
        log.info("Shipper {} went offline", shipperId);
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        shipperLocationRepository.deleteByShipper(shipper);
    }

    @Override
    @Transactional
    public void goOnlineCurrentShipper(ShipperStatusRequest request) {
        User shipper = getCurrentUser();
        goOnline(shipper.getId(), request);
    }

    @Override
    @Transactional
    public void goOfflineCurrentShipper() {
        User shipper = getCurrentUser();
        goOffline(shipper.getId());
    }

    @Override
    public ShipperStatusResponse getCurrentShipperStatus() {
        User shipper = getCurrentUser();
        return shipperLocationRepository.findByShipper(shipper)
                .map(loc -> ShipperStatusResponse.builder()
                        .isOnline(true)
                        .latitude(loc.getLatitude())
                        .longitude(loc.getLongitude())
                        .updatedAt(loc.getUpdatedAt())
                        .build())
                .orElse(ShipperStatusResponse.builder()
                        .isOnline(false)
                        .build());
    }
}
