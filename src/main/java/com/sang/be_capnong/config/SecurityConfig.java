package com.sang.be_capnong.config;
import com.sang.be_capnong.config.CustomJwtDecoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ANY_ENDPOINTS = {
            "/login/**",
            "/error",
            "/ws/**",
            "/api/v1/ws/**"
    };

    /**
     * Các endpoint POST công khai dành riêng cho Guest hoặc Webhook bên thứ 3.
     */
    private static final String[] PUBLIC_POST_ENDPOINTS = {
            // Auth & Token
            "/auth/login",
            "/auth/google",
            "/auth/introspect",
            "/auth/logout",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/reset-password",

            // Đăng ký tài khoản (Buyer, Shop, Shipper) & xác thực OTP
            "/users",
            "/users/shops",
            "/users/shippers",
            "/users/verify-otp",
            "/users/resend-otp",

            // Upload ảnh (đính kèm khi đăng ký shop/tài khoản)
            "/upload/**",

            // Tính trước phí ship cho khách chưa đăng nhập
            "/shipping/preview",

            // Webhook và xác nhận thanh toán PayOS
            "/payments/payos/webhook",
            "/payments/webhook",
            "/payments/*/confirm",
            "/payments/*/cancel",
            "/payments/cancel"
    };

    /**
     * Các endpoint GET công khai dành riêng cho Guest xem thông tin catalog/sản phẩm.
     */
    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/categories",
            "/categories/**",
            "/products",
            "/products/{id:[0-9]+}",
            "/products/{id:[0-9]+}/images/**",
            "/mystery-boxes",
            "/mystery-boxes/{id:[0-9]+}",
            "/combos",
            "/combos/{id:[0-9]+}",
            "/v1/vouchers",
            "/vouchers",
            "/blogs",
            "/blogs/**",
            "/v1/blogs",
            "/v1/blogs/**",
            "/v1/public/blogs",
            "/v1/public/blogs/**",
            "/shipping/address-suggestions",
            "/payments/*",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Autowired
    @Lazy
    private CustomJwtDecoder customJwtDecoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                // Trả về JSON 401 khi chưa đăng nhập, không redirect 302 sang Google OAuth gây lỗi CORS
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                )

                .authorizeHttpRequests(req -> req
                        // Các route công khai chung
                        .requestMatchers(PUBLIC_ANY_ENDPOINTS).permitAll()

                        // Route công khai cho Guest
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()

                        // Tất cả các request còn lại yêu cầu đăng nhập (bao gồm /products/my-shop, /cart/**, /orders/**)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(customJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object rolesClaim = jwt.getClaims().get("roles");
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (rolesClaim instanceof Collection<?> roles) {
                for (Object r : roles) {
                    if (r != null) {
                        String role = r.toString();
                        authorities.add(new SimpleGrantedAuthority(role));
                        if (!role.startsWith("ROLE_")) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                    }
                }
            }
            return authorities;
        });
        return jwtConverter;
    }
}
