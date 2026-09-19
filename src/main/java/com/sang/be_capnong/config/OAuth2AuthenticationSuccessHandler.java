package com.sang.be_capnong.config;

import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.AuthenticationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    final UserRepository userRepository;
    final AuthenticationService authenticationService;

    @Value("${app.oauth2.frontend-url:http://localhost:3000}")
    String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        log.info("OAuth2 Google login success for email: {}, name: {}", email, name);

        if (email == null || email.isBlank()) {
            log.error("OAuth2 user has no email");
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=oauth2_email_missing");
            return;
        }

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Nếu có ảnh từ Google mà tài khoản chưa có logoUrl, cập nhật luôn
            if ((user.getLogoUrl() == null || user.getLogoUrl().isBlank()) && picture != null) {
                user.setLogoUrl(picture);
                userRepository.save(user);
            }

            String token = authenticationService.generateToken(user);
            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth-success")
                    .queryParam("token", token)
                    .build().toUriString();

            log.info("Redirecting existing user to: {}", targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } else {
            // User chưa có tài khoản trong DB -> chuyển hướng tới /oauth-register để hoàn tất thông tin
            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth-register")
                    .queryParam("email", email)
                    .queryParam("name", name != null ? name : "")
                    .queryParam("avatar", picture != null ? picture : "")
                    .queryParam("role", "BUYER")
                    .build().toUriString();

            log.info("Redirecting new user to registration: {}", targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}
