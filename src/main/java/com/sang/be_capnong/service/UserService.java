package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.BuyerCreationRequest;
import com.sang.be_capnong.dto.request.ResendOtpRequest;
import com.sang.be_capnong.dto.request.ShipperCreationRequest;
import com.sang.be_capnong.dto.request.ShopCreationRequest;
import com.sang.be_capnong.dto.request.UserCreationRequest;
import com.sang.be_capnong.dto.request.UserUpdateRequest;
import com.sang.be_capnong.dto.request.VerifyOtpRequest;
import com.sang.be_capnong.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserCreationRequest request);

    UserResponse createBuyer(BuyerCreationRequest request);

    UserResponse createShop(ShopCreationRequest request);

    UserResponse createShipper(ShipperCreationRequest request);

    UserResponse verifyAccount(VerifyOtpRequest request);

    void resendVerificationOtp(ResendOtpRequest request);

    UserResponse updateUser(Integer userId, UserUpdateRequest request);

    UserResponse getUserById(Integer userId);

    List<UserResponse> getAllUsers();

    void deleteUser(Integer userId);
}