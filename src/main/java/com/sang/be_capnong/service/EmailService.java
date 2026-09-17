package com.sang.be_capnong.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, int otp);
}
