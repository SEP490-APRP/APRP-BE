package com.sang.be_capnong.util;

import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            try {
                return Integer.parseInt(jwt.getSubject());
            } catch (NumberFormatException e) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
        }

        try {
            return Integer.parseInt(authentication.getName());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    public static Integer getCurrentUserIdOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt) {
                return Integer.parseInt(jwt.getSubject());
            }

            return Integer.parseInt(authentication.getName());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasRole(String role) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }
            String targetRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equalsIgnoreCase(targetRole) || a.getAuthority().equalsIgnoreCase(role));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
