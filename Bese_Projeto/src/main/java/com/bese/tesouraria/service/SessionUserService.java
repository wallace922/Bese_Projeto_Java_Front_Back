package com.bese.tesouraria.service;

import com.bese.tesouraria.enun.Role;
import com.bese.tesouraria.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Foto atual do usuário para o filtro de segurança.
 *
 * <p>O JWT carrega {@code role}, mas ela pode estar defasada (rebaixamento)
 * e o usuário pode nem existir mais (exclusão). Por isso o
 * {@code SecurityFilter} reconfere no banco com cache curto (60s):
 * <ul>
 * <li>usuário inexistente → sessão rejeitada na hora;</li>
 * <li>role mudou → vale a do banco, não a do token.</li>
 * </ul>
 *
 * <p>O cache guarda só id + role (nunca o hash da senha) e é invalidado
 * em update/delete, então mudança crítica vale de imediato.
 */
@Service
public class SessionUserService {

    /** Foto mínima para autorização (sem dados sensíveis). */
    public record AuthSnapshot(Long id, Role role) {
    }

    private final UserRepository userRepository;
    private final Cache<Long, AuthSnapshot> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    public SessionUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<AuthSnapshot> resolve(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        AuthSnapshot cached = cache.getIfPresent(userId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return userRepository.findById(userId)
                .map(user -> {
                    AuthSnapshot snapshot = new AuthSnapshot(user.getId(), user.getRole());
                    cache.put(userId, snapshot);
                    return snapshot;
                });
    }

    public void invalidate(Long userId) {
        if (userId != null) {
            cache.invalidate(userId);
        }
    }
}
