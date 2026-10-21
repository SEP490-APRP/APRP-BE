package com.sang.be_capnong.service;

import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.entity.User;
import java.math.BigDecimal;
import java.util.List;

public interface EmailService {
    void sendOtpEmail(String toEmail, int otp);

    void sendForgotPasswordOtpEmail(String toEmail, int otp);

    void sendPaymentSuccessInvoiceEmail(User buyer, List<Order> orders, String orderCode, BigDecimal totalAmount);
}
