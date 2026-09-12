package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.UserCreationRequest;
import com.sang.be_capnong.dto.request.UserUpdateRequest;
import com.sang.be_capnong.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserCreationRequest request);

    UserResponse updateUser(Integer userId, UserUpdateRequest request);

    UserResponse getUserById(Integer userId);

    List<UserResponse> getAllUsers();

    void deleteUser(Integer userId);
}