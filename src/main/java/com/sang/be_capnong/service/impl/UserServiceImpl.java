package com.sang.be_capnong.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.sang.be_capnong.dto.request.UserCreationRequest;
import com.sang.be_capnong.dto.request.UserUpdateRequest;
import com.sang.be_capnong.dto.response.UserResponse;
import com.sang.be_capnong.entity.Role;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.UserMapper;
import com.sang.be_capnong.repository.RoleRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(UserCreationRequest request){
        log.info("createUser with email: {}", request.getEmail());
        if (userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        String requestedRole = request.getRoleName() != null ? request.getRoleName().toUpperCase() : "BUYER";
        Role role = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        user.setRole(role);

        // Shipper fields are automatically mapped by UserMapper because field names match
        // But we want to ensure only specific roles get these. For now, mapstruct handles it cleanly.
        
        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(Integer userId, UserUpdateRequest request) {
        log.info("updateUser with id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        
        // Shipper Profile update
        if (request.getVehicleNumber() != null) user.setVehicleNumber(request.getVehicleNumber());
        if (request.getVehicleDocImageUrl() != null) user.setVehicleDocImageUrl(request.getVehicleDocImageUrl());
        if (request.getLicense() != null) user.setLicense(request.getLicense());
        if (request.getLicenseImageUrl() != null) user.setLicenseImageUrl(request.getLicenseImageUrl());
        
        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
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
