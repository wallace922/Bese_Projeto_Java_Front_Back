package com.bese.tesouraria.security;

import com.bese.tesouraria.service.RevokedTokenService;
import com.bese.tesouraria.service.SessionUserService;

import java.io.IOException;
import java.util.Collections;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenUtil tokenUtil;
    private final RevokedTokenService revokedTokenService;
    private final SessionUserService sessionUserService;

    public SecurityFilter(TokenUtil tokenUtil, RevokedTokenService revokedTokenService,
            SessionUserService sessionUserService) {
        this.tokenUtil = tokenUtil;
        this.revokedTokenService = revokedTokenService;
        this.sessionUserService = sessionUserService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromCookie(request);

        if (token != null) {
            // Valida o token e constrói o objeto de Autenticação do Spring Security
            Authentication auth = tokenUtil.validateTokenString(token);
            if (auth != null) {
                // Barreira 1: token revogado (logout/troca de senha) não autentica
                String jti = tokenUtil.extractJti(token);
                if (revokedTokenService.isRevoked(jti)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                // Barreira 2: confere o usuário no banco (cache de 60s).
                // Deletado -> rejeita; role mudou -> vale a atual do banco.
                Authentication fresh = refreshFromDatabase(auth);
                if (fresh != null) {
                    // Registra o usuário autenticado na sessão atual da requisição
                    SecurityContextHolder.getContext().setAuthentication(fresh);
                }
            }
        }

        // Passa a requisição para o próximo filtro na cadeia do Spring Security
        filterChain.doFilter(request, response);
    }

    private Authentication refreshFromDatabase(Authentication auth) {
        final Long userId;
        try {
            userId = Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            System.out.println("DEBUG filter: subject não numérico: " + auth.getName());
            return null;
        }
        var resolved = sessionUserService.resolve(userId);
        return resolved
                .map(snapshot -> (Authentication) new UsernamePasswordAuthenticationToken(
                        snapshot.id().toString(),
                        null,
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + snapshot.role().name()))))
                .orElse(null);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;

        for (Cookie cookie : request.getCookies()) {
            if ("jwt_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
