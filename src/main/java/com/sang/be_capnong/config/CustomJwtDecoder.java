package com.sang.be_capnong.config;

import com.sang.be_capnong.repository.InvalidatedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signer-key}")
    private String signerKey;

    @Autowired
    @Lazy
    private InvalidatedTokenRepository invalidatedTokenRepository;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        if (token.startsWith("ya29.")) {
            throw new JwtException("Google access token - skip local JWT decode");
        }

        try {
            //  Tạo Nimbus decoder (nếu chưa có)
            if (Objects.isNull(nimbusJwtDecoder)) {
                SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HmacSHA512");
                nimbusJwtDecoder = NimbusJwtDecoder
                        .withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();
            }

            //  Decode và verify chữ ký token
            Jwt jwt = nimbusJwtDecoder.decode(token);

            //  Kiểm tra token có nằm trong blacklist (đã logout / revoked) không
            String jti = jwt.getId();
            if (jti != null && invalidatedTokenRepository.existsById(jti)) {
                throw new JwtException("Token has been revoked");
            }

            return jwt;

        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("JWT decode failed: " + e.getMessage());
        }
    }
}
