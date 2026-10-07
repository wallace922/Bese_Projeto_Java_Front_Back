package com.bese.tesouraria.security;

import com.bese.tesouraria.entity.User;
import com.bese.tesouraria.enun.Role;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Component
public class TokenUtil {

    private static final long EXPIRATION = 2 * 60 * 60 * 1000;
    private static final String EMISSOR = "TesourariaBESE__WM";

    @Value("${jwt.secret}")
    private String secretKey;

    public String generateRawToken(User user) {
        Key secretKey = Keys.hmacShaKeyFor(this.secretKey.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setSubject(user.getId().toString())
                .setId(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .setIssuer(EMISSOR)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(secretKey)
                .compact();

        return token;
    }

    private boolean isExpirrationValid(Date expiration) {
        return expiration.after(new Date(System.currentTimeMillis()));
    }

    private boolean isEmissorValid(String emissor) {
        return EMISSOR.equals(emissor);
    }

    private boolean isSubjectValid(String username) {
        return username != null && username.length() > 0;
    }

    // 2. Validar a string pura do token extraída do Cookie
    public Authentication validateTokenString(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            Jws<Claims> jwsClaims = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token);

            String username = jwsClaims.getBody().getSubject();
            Date expiration = jwsClaims.getBody().getExpiration();
            String emissor = jwsClaims.getBody().getIssuer();
            String role = jwsClaims.getBody().get("role", String.class);

            if (isSubjectValid(username) && isExpirrationValid(expiration)
                    && isEmissorValid(emissor) && isRoleValid(role)) {
                List<GrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role));
                return new UsernamePasswordAuthenticationToken(username, null, authorities);
            }
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return null;
        }

        return null;
    }

    private boolean isRoleValid(String role) {
        if (role == null)
            return false;
        try {
            Role.valueOf(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Extrai o jti (nº de série) do token. Retorna null se inválido.
    public String extractJti(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getId();
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    // Quanto tempo (ms) falta para o token expirar. 0 se inválido/expirado.
    public long remainingMillis(String token) {
        if (token == null || token.isBlank()) {
            return 0;
        }
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            if (expiration == null) {
                return 0;
            }
            return Math.max(0, expiration.getTime() - System.currentTimeMillis());
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return 0;
        }
    }
}
