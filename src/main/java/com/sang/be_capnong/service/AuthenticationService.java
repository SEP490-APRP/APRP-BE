package com.sang.be_capnong.service;

import com.nimbusds.jose.JOSEException;
import com.sang.be_capnong.dto.request.AuthenticationRequest;
import com.sang.be_capnong.dto.request.IntrospectRequest;
import com.sang.be_capnong.dto.request.LogoutRequest;
import com.sang.be_capnong.dto.request.RefreshRequest;
import com.sang.be_capnong.dto.response.AuthenticationResponse;
import com.sang.be_capnong.dto.response.IntrospectResponse;

import java.text.ParseException;

public interface AuthenticationService {
    AuthenticationResponse authenticated(AuthenticationRequest request);

    IntrospectResponse introspect(IntrospectRequest request)
            throws ParseException, JOSEException;

    void logout(LogoutRequest request)
            throws ParseException, JOSEException;

    AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException;
}
