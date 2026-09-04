package com.bese.tesouraria.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final String COOKIE_NAME = "jwt_token";

    // 1. Monta o Cookie HttpOnly seguro contendo o JWT
    public ResponseCookie createJwtCookie(String jwtToken) {
        return ResponseCookie.from(COOKIE_NAME, jwtToken)
                .httpOnly(true) // Proteção XSS (JS não lê)
                .secure(false) // Em produção com HTTPS, alterar para true
                .path("/")
                .maxAge(2 * 60 * 60) // 2 horas de validade (em segundos)
                .sameSite("Lax") // Proteção CSRF
                .build();
    }

    public ResponseCookie createCleanJwtCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
