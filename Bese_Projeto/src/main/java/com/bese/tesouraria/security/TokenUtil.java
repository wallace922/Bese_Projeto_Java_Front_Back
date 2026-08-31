package com.bese.tesouraria.security;

import com.bese.tesouraria.entity.User;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;
import jakarta.servlet.http.HttpServletRequest;
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

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final long EXPIRATION = 2 * 60 * 60 * 1000;
    private static final String EMISSOR = "TesourariaBESE__WM";

    @Value("${jwt.secret}")
    private String secretKey;

    public String generateToken(User User) {
        Key secretKey = Keys.hmacShaKeyFor(this.secretKey.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setSubject(User.getName())
                .claim("role", User.getRole().name())
                .setIssuer(EMISSOR)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(secretKey)
                .compact();

        return PREFIX + token;
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

    public Authentication validate(HttpServletRequest request) {
        String token = request.getHeader(HEADER);
        if (token == null || !token.startsWith(PREFIX)) {
            return null;
        }
        token = token.replace(PREFIX, "");

        try {
            Jws<Claims> jwsClaims = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes())
                    .build()
                    .parseClaimsJws(token);

            String username = jwsClaims.getBody().getSubject();
            Date expiration = jwsClaims.getBody().getExpiration();
            String emissor = jwsClaims.getBody().getIssuer();
            String role = jwsClaims.getBody().get("role", String.class);

            if (isSubjectValid(username) && isExpirrationValid(expiration) && isEmissorValid(emissor) && isRoleValid(role)) {
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
        if (role == null) return false;
        try {
            com.bese.tesouraria.enun.Role.valueOf(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
