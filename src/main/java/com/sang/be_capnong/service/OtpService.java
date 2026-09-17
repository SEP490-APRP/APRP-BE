package com.sang.be_capnong.service;

public interface OtpService {
    void generateAndSendOtp(String email);
    void verifyOtp(String email, int otp);
    void resendOtp(String email);
}
