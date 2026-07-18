package com.inspecteam.shared.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }

    public static boolean isPlatformAdmin(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Boolean.TRUE.equals(jwt.getClaim("platform_admin"));
    }
}
