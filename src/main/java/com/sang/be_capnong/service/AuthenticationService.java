package com.sang.be_capnong.service;

import com.nimbusds.jose.JOSEException;
import com.sang.be_capnong.dto.request.AuthenticationRequest;
import com.sang.be_capnong.dto.request.ForgotPasswordRequest;
import com.sang.be_capnong.dto.request.IntrospectRequest;
import com.sang.be_capnong.dto.request.LogoutRequest;
import com.sang.be_capnong.dto.request.RefreshRequest;
import com.sang.be_capnong.dto.request.ResetPasswordRequest;
import com.sang.be_capnong.dto.request.SocialLoginRequest;
import com.sang.be_capnong.dto.response.AuthenticationResponse;
import com.sang.be_capnong.dto.response.IntrospectResponse;
import com.sang.be_capnong.entity.User;

import java.text.ParseException;

public interface AuthenticationService {
    AuthenticationResponse authenticated(AuthenticationRequest request);

    AuthenticationResponse socialLoginGoogle(SocialLoginRequest request);

    IntrospectResponse introspect(IntrospectRequest request)
            throws ParseException, JOSEException;

    void logout(LogoutRequest request)
            throws ParseException, JOSEException;

    AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException;

    String generateToken(User user);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
