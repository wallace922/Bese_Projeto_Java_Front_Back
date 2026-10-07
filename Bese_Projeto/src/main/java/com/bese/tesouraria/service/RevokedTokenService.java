package com.bese.tesouraria.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Denylist de tokens JWT revogados (logout / troca de senha).
 *
 * Guarda apenas o jti (nº de série do token). As entradas expiram
 * sozinhas após a vida máxima de um token, então a lista nunca
 * cresce de forma ilimitada. Tokens emitidos antes desta mudança
 * (sem jti) passam normalmente até expirarem.
 */
@Service
public class RevokedTokenService {

    private final Cache<String, Boolean> revoked = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    public void revoke(String jti) {
        if (jti != null && !jti.isBlank()) {
            revoked.put(jti, Boolean.TRUE);
        }
    }

    public boolean isRevoked(String jti) {
        return jti != null && revoked.getIfPresent(jti) != null;
    }
}
