package com.sang.be_capnong.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sang.be_capnong.dto.request.*;
import com.sang.be_capnong.dto.response.AuthenticationResponse;
import com.sang.be_capnong.dto.response.IntrospectResponse;
import com.sang.be_capnong.entity.InvalidatedToken;
import com.sang.be_capnong.entity.Role;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.InvalidatedTokenRepository;
import com.sang.be_capnong.repository.RoleRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.sang.be_capnong.service.OtpService;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    InvalidatedTokenRepository invalidatedTokenRepository;
    OtpService otpService;
    RestTemplate restTemplate;

    @Value("${jwt.signer-key}")
    @NonFinal
    String SIGNER_KEY;

    @Value("${jwt.valid-duration}")
    @NonFinal
    long VALID_DURATION;

    @Value("${jwt.refreshable-duration}")
    @NonFinal
    long REFRESHABLE_DURATION;

    // ================= LOGIN =================
    @Override
    public AuthenticationResponse authenticated(AuthenticationRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new AppException(ErrorCode.USER_PENDING_APPROVAL);
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        String token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    // ================= GENERATE JWT =================
    @Override
    public String generateToken(User user) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + VALID_DURATION * 1000);

            String jti = UUID.randomUUID().toString();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(user.getId()))
                    .claim("email", user.getEmail())
                    .claim("roles",
                            user.getRole() != null ? List.of(user.getRole().getName()) : List.of())
                    .issueTime(now)
                    .expirationTime(expiry)
                    .jwtID(jti)
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claimsSet
            );

            jwt.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwt.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Cannot generate token", e);
        }
    }

    // ================= INTROSPECT =================
    @Override
    public IntrospectResponse introspect(IntrospectRequest request)
            throws ParseException, JOSEException {

        SignedJWT jwt = SignedJWT.parse(request.getToken());

        // verify signature
        boolean signatureValid = jwt.verify(
                new MACVerifier(SIGNER_KEY.getBytes())
        );

        if (!signatureValid) {
            return IntrospectResponse.builder().valid(false).build();
        }

        Date expiry = jwt.getJWTClaimsSet().getExpirationTime();
        if (expiry.before(new Date())) {
            return IntrospectResponse.builder().valid(false).build();
        }

        String jti = jwt.getJWTClaimsSet().getJWTID();

        // 🔥 check revoked
        if (invalidatedTokenRepository.existsById(jti)) {
            return IntrospectResponse.builder().valid(false).build();
        }

        return IntrospectResponse.builder()
                .valid(true)
                .build();
    }

    // ================= LOGOUT =================
    @Override
    public void logout(LogoutRequest request)
            throws ParseException, JOSEException {

        SignedJWT jwt = SignedJWT.parse(request.getToken());

        String jti = jwt.getJWTClaimsSet().getJWTID();
        Date expiry = jwt.getJWTClaimsSet().getExpirationTime();

        // đã logout rồi → bỏ qua
        if (invalidatedTokenRepository.existsById(jti)) {
            return;
        }

        invalidatedTokenRepository.save(
                InvalidatedToken.builder()
                        .id(jti)
                        .expiryTime(expiry)
                        .build()
        );

        log.info("Logout success - token revoked: {}", jti);
    }

    // ================= REFRESH TOKEN =================
    @Override
    public AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException {

        SignedJWT jwt = SignedJWT.parse(request.getToken());

        String jti = jwt.getJWTClaimsSet().getJWTID();
        Date issuedAt = jwt.getJWTClaimsSet().getIssueTime();
        Date expiry = jwt.getJWTClaimsSet().getExpirationTime();

        // ❌ token đã logout
        if (invalidatedTokenRepository.existsById(jti)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // ❌ quá hạn refresh
        long refreshDeadline =
                issuedAt.getTime() + REFRESHABLE_DURATION * 1000;

        if (System.currentTimeMillis() > refreshDeadline) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String email = jwt.getJWTClaimsSet().getStringClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        // 🔥 revoke token cũ
        invalidatedTokenRepository.save(
                InvalidatedToken.builder()
                        .id(jti)
                        .expiryTime(expiry)
                        .build()
        );

        String newToken = generateToken(user);

        return AuthenticationResponse.builder()
                .token(newToken)
                .authenticated(true)
                .build();
    }

    // ================= FORGOT PASSWORD =================
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Request forgot password for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        otpService.generateAndSendForgotPasswordOtp(user.getEmail());
    }

    // ================= RESET PASSWORD =================
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Request reset password for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()
                && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_NOT_MATCH);
        }

        otpService.verifyOtp(request.getEmail(), request.getOtp());

        // Nếu tài khoản đang INACTIVE (chưa xác thực OTP khi đăng ký), việc nhập đúng OTP reset mật khẩu
        // chứng minh người dùng sở hữu email -> kích hoạt tài khoản
        if (user.getStatus() == UserStatus.INACTIVE) {
            boolean isShopOrShipper = user.getRole() != null && (
                    "SHOP".equalsIgnoreCase(user.getRole().getName())
                    || "SHIPPER".equalsIgnoreCase(user.getRole().getName())
            );
            user.setStatus(isShopOrShipper ? UserStatus.PENDING : UserStatus.ACTIVE);
            log.info("Reset password verified OTP. Activated user status to {} for email: {}", user.getStatus(), request.getEmail());
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Reset password successfully for email: {}", request.getEmail());
    }

    // ================= SOCIAL LOGIN (GOOGLE) =================
    @Override
    @Transactional
    public AuthenticationResponse socialLoginGoogle(SocialLoginRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Map<String, Object> userInfo = fetchGoogleUserInfo(request.getToken().trim());
        if (userInfo == null || !userInfo.containsKey("email")) {
            log.error("Google userinfo verification failed or missing email");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String googleId = (String) userInfo.get("sub");
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String picture = (String) userInfo.get("picture");

        User user = findOrCreateGoogleUser(googleId, email, name, picture);

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Google login failed: Account is inactive for email: {}", email);
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }
        if (user.getStatus() == UserStatus.PENDING) {
            log.warn("Google login failed: Account is pending approval for email: {}", email);
            throw new AppException(ErrorCode.USER_PENDING_APPROVAL);
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            log.warn("Google login failed: Account is locked for email: {}", email);
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        String token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    private Map<String, Object> fetchGoogleUserInfo(String token) {
        // Cách 1: Thử gọi UserInfo endpoint với Bearer access token
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Google Bearer access_token verify failed: {}", e.getMessage());
        }

        // Cách 2: Thử gọi tokeninfo endpoint với id_token
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Google id_token verify failed: {}", e.getMessage());
        }

        return null;
    }

    private User findOrCreateGoogleUser(String googleId, String email, String name, String picture) {
        Optional<User> userOpt = Optional.empty();
        if (googleId != null && !googleId.isBlank()) {
            userOpt = userRepository.findByGoogleId(googleId);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if ((user.getLogoUrl() == null || user.getLogoUrl().isBlank()) && picture != null) {
                user.setLogoUrl(picture);
            }
            if (user.getStatus() == UserStatus.INACTIVE) {
                if (user.getRole() != null && ("SHOP".equals(user.getRole().getName()) || "SHIPPER".equals(user.getRole().getName()))) {
                    user.setStatus(UserStatus.PENDING);
                } else {
                    user.setStatus(UserStatus.ACTIVE);
                }
                log.info("Google login automatically verified unverified account for googleId: {}", googleId);
            }
            return userRepository.save(user);
        }

        // Không tìm thấy theo googleId, tìm theo email
        return userRepository.findByEmail(email).map(user -> {
            if (googleId != null && !googleId.isBlank()) {
                user.setGoogleId(googleId);
            }
            if ((user.getLogoUrl() == null || user.getLogoUrl().isBlank()) && picture != null) {
                user.setLogoUrl(picture);
            }
            // Nếu người dùng đã đăng ký thủ công nhưng chưa xác thực OTP:
            // Đăng nhập Google thành công chứng minh sở hữu email, tự động kích hoạt tài khoản
            if (user.getStatus() == UserStatus.INACTIVE) {
                if (user.getRole() != null && ("SHOP".equals(user.getRole().getName()) || "SHIPPER".equals(user.getRole().getName()))) {
                    user.setStatus(UserStatus.PENDING);
                } else {
                    user.setStatus(UserStatus.ACTIVE);
                }
                log.info("Google login automatically verified unverified account for email: {}", email);
            }
            return userRepository.save(user);
        }).orElseGet(() -> {
            Role buyerRole = roleRepository.findByName("BUYER")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

            User newUser = User.builder()
                    .email(email)
                    .fullName(name != null && !name.isBlank() ? name : email.split("@")[0])
                    .googleId(googleId)
                    .logoUrl(picture)
                    .role(buyerRole)
                    .status(UserStatus.ACTIVE)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .loyaltyPoints(BigDecimal.ZERO)
                    .totalRescuedWeight(BigDecimal.ZERO)
                    .createAt(LocalDateTime.now())
                    .updateAt(LocalDateTime.now())
                    .build();

            User savedUser = userRepository.save(newUser);
            log.info("Automatically registered new BUYER via Google login: {}", email);
            return savedUser;
        });
    }
}
