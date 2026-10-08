package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.SavedAddressRequest;
import com.sang.be_capnong.dto.response.SavedAddressResponse;
import com.sang.be_capnong.entity.SavedAddress;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.SavedAddressRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.GoogleMapService;
import com.sang.be_capnong.service.SavedAddressService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SavedAddressServiceImpl implements SavedAddressService {

    static final int MAX_SAVED_ADDRESSES = 10;

    SavedAddressRepository savedAddressRepository;
    UserRepository userRepository;
    GoogleMapService googleMapService;

    @Override
    public List<SavedAddressResponse> getMyAddresses() {
        User user = getCurrentUser();
        List<SavedAddress> list = savedAddressRepository.findByUser_IdAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(user.getId());
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public SavedAddressResponse getAddressById(Long id) {
        User user = getCurrentUser();
        SavedAddress address = savedAddressRepository.findByIdAndUser_IdAndIsActiveTrue(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
        return toResponse(address);
    }

    @Transactional
    @Override
    public SavedAddressResponse createAddress(SavedAddressRequest request) {
        User user = getCurrentUser();

        long count = savedAddressRepository.countByUser_IdAndIsActiveTrue(user.getId());
        if (count >= MAX_SAVED_ADDRESSES) {
            log.warn("User {} exceeded max saved address limit of {}", user.getId(), MAX_SAVED_ADDRESSES);
            throw new AppException(ErrorCode.MAX_ADDRESSES_REACHED);
        }

        boolean makeDefault = (count == 0) || Boolean.TRUE.equals(request.getIsDefault());

        if (makeDefault) {
            clearOtherDefaults(user.getId());
        }

        Double lat = request.getLatitude();
        Double lng = request.getLongitude();

        // Geocode nếu chưa có tọa độ
        if ((lat == null || lng == null) && request.getAddress() != null && !request.getAddress().isBlank()) {
            try {
                double[] coords = googleMapService.geocodeAddress(request.getAddress());
                if (coords != null && coords.length == 2) {
                    lat = coords[0];
                    lng = coords[1];
                }
            } catch (Exception e) {
                log.warn("Geocoding failed for new address: {}", e.getMessage());
            }
        }

        SavedAddress address = SavedAddress.builder()
                .user(user)
                .label(request.getLabel() != null && !request.getLabel().isBlank() ? request.getLabel().trim() : "Nhà")
                .recipientName(request.getRecipientName().trim())
                .phone(request.getPhone().trim())
                .address(request.getAddress().trim())
                .latitude(lat)
                .longitude(lng)
                .isDefault(makeDefault)
                .isActive(true)
                .build();

        SavedAddress saved = savedAddressRepository.save(address);
        log.info("Saved new delivery address #{} for user {}", saved.getId(), user.getId());
        return toResponse(saved);
    }

    @Transactional
    @Override
    public SavedAddressResponse updateAddress(Long id, SavedAddressRequest request) {
        User user = getCurrentUser();
        SavedAddress address = savedAddressRepository.findByIdAndUser_IdAndIsActiveTrue(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefault()) {
            clearOtherDefaults(user.getId());
            address.setDefault(true);
        }

        if (request.getLabel() != null && !request.getLabel().isBlank()) {
            address.setLabel(request.getLabel().trim());
        }
        if (request.getRecipientName() != null && !request.getRecipientName().isBlank()) {
            address.setRecipientName(request.getRecipientName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            address.setPhone(request.getPhone().trim());
        }

        // Nếu địa chỉ text thay đổi, re-geocode nếu cần
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            String newAddress = request.getAddress().trim();
            if (!newAddress.equals(address.getAddress())) {
                address.setAddress(newAddress);
                Double lat = request.getLatitude();
                Double lng = request.getLongitude();
                if (lat == null || lng == null) {
                    try {
                        double[] coords = googleMapService.geocodeAddress(newAddress);
                        if (coords != null && coords.length == 2) {
                            lat = coords[0];
                            lng = coords[1];
                        }
                    } catch (Exception e) {
                        log.warn("Re-geocoding failed during update: {}", e.getMessage());
                    }
                }
                address.setLatitude(lat);
                address.setLongitude(lng);
            }
        }

        SavedAddress updated = savedAddressRepository.save(address);
        log.info("Updated delivery address #{} for user {}", updated.getId(), user.getId());
        return toResponse(updated);
    }

    @Transactional
    @Override
    public void deleteAddress(Long id) {
        User user = getCurrentUser();
        SavedAddress address = savedAddressRepository.findByIdAndUser_IdAndIsActiveTrue(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        boolean wasDefault = address.isDefault();
        address.setActive(false);
        address.setDefault(false);
        savedAddressRepository.save(address);

        // Nếu xóa địa chỉ mặc định, tự động gán địa chỉ còn lại gần nhất làm mặc định
        if (wasDefault) {
            List<SavedAddress> remaining = savedAddressRepository.findByUser_IdAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(user.getId());
            if (!remaining.isEmpty()) {
                SavedAddress newDefault = remaining.get(0);
                newDefault.setDefault(true);
                savedAddressRepository.save(newDefault);
            }
        }
        log.info("Soft-deleted delivery address #{} for user {}", id, user.getId());
    }

    @Transactional
    @Override
    public SavedAddressResponse setDefaultAddress(Long id) {
        User user = getCurrentUser();
        SavedAddress address = savedAddressRepository.findByIdAndUser_IdAndIsActiveTrue(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        clearOtherDefaults(user.getId());
        address.setDefault(true);
        SavedAddress saved = savedAddressRepository.save(address);
        log.info("Set address #{} as default for user {}", id, user.getId());
        return toResponse(saved);
    }

    @Override
    public SavedAddressResponse getDefaultAddress() {
        User user = getCurrentUser();
        return savedAddressRepository.findByUser_IdAndIsDefaultTrueAndIsActiveTrue(user.getId())
                .map(this::toResponse)
                .orElse(null);
    }

    private void clearOtherDefaults(int userId) {
        List<SavedAddress> all = savedAddressRepository.findByUser_IdAndIsActiveTrue(userId);
        for (SavedAddress a : all) {
            if (a.isDefault()) {
                a.setDefault(false);
                savedAddressRepository.save(a);
            }
        }
    }

    private User getCurrentUser() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        int userId = Integer.parseInt(subject);
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private SavedAddressResponse toResponse(SavedAddress a) {
        return SavedAddressResponse.builder()
                .id(a.getId())
                .label(a.getLabel())
                .recipientName(a.getRecipientName())
                .phone(a.getPhone())
                .address(a.getAddress())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .isDefault(a.isDefault())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
