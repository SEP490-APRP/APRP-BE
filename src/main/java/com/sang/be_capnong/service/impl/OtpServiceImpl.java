package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.entity.OtpVerification;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.OtpVerificationRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.EmailService;
import com.sang.be_capnong.service.OtpService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OtpServiceImpl implements OtpService {

    OtpVerificationRepository otpVerificationRepository;
    UserRepository userRepository;
    EmailService emailService;
    SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void generateAndSendOtp(String email) {
        int otp = 100000 + secureRandom.nextInt(900000);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plusMinutes(5);

        OtpVerification otpVerification = otpVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElse(OtpVerification.builder().email(email).build());

        otpVerification.setOtp(otp);
        otpVerification.setCreatedAt(now);
        otpVerification.setExpirationTime(expirationTime);

        otpVerificationRepository.save(otpVerification);
        log.info("Saved OTP for email: {}", email);

        emailService.sendOtpEmail(email, otp);
    }

    @Override
    @Transactional
    public void verifyOtp(String email, int otp) {
        log.info("Verifying OTP for email: {}", email);

        OtpVerification otpVerification = otpVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_NOT_FOUND));

        if (otpVerification.getExpirationTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (otpVerification.getOtp() != otp) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        otpVerificationRepository.delete(otpVerification);
        log.info("OTP verified successfully and consumed for email: {}", email);
    }

    @Override
    @Transactional
    public void resendOtp(String email) {
        log.info("Requesting resend OTP for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() != UserStatus.INACTIVE) {
            throw new AppException(ErrorCode.USER_ALREADY_ACTIVE);
        }

        generateAndSendOtp(email);
    }
}
