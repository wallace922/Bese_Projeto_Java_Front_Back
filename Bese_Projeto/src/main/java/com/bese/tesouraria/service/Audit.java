package com.bese.tesouraria.service;

import com.bese.tesouraria.exception.BusinessRuleException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilidades transversais de auditoria e consistência de IDs.
 *
 * <p>Centraliza duas regras usadas pelos services/controllers para que o
 * comportamento seja idêntico em todas as entidades:
 * <ul>
 * <li>{@link #currentUserId()} — id do usuário autenticado (subject do JWT),
 * usado para preencher {@code createdBy}/{@code updatedBy}. Retorna
 * {@code null} fora de um contexto autenticado (ex: jobs, testes).</li>
 * <li>{@link #requireIdMatch(Long, Long, String)} — garante que o registro
 * resolvido pela chave natural (número+ano, CNPJ...) é o mesmo da URL do
 * PUT, impedindo atualização cruzada por troca de chave no corpo.</li>
 * </ul>
 */
public final class Audit {

    private Audit() {
    }

    public static Long currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return Long.parseLong(auth.getName());
            }
        } catch (RuntimeException e) {
            // Sem usuário autenticado ou subject não numérico: sem autoria.
        }
        return null;
    }

    public static void requireIdMatch(Long resolvedId, Long pathId, String what) {
        if (pathId == null) {
            throw new BusinessRuleException("O ID do recurso deve ser informado na URL.");
        }
        if (resolvedId == null || !resolvedId.equals(pathId)) {
            throw new BusinessRuleException(
                    "O ID da URL (" + pathId + ") diverge do registro informado no corpo para " + what + ".");
        }
    }
}
