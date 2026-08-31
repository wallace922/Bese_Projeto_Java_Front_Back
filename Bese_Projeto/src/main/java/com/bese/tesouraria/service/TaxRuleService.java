package com.bese.tesouraria.service;

import com.bese.tesouraria.dto.TaxRuleUpdateDto;
import com.bese.tesouraria.entity.TaxRule;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.bese.tesouraria.exception.EntityNotFoundException;
import com.bese.tesouraria.repository.TaxRuleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaxRuleService {

    private final TaxRuleRepository taxRuleRepository;

    public TaxRuleService(TaxRuleRepository taxRuleRepository) {
        this.taxRuleRepository = taxRuleRepository;
    }

    public List<TaxRule> findAll() {
        return taxRuleRepository.findAll();
    }

    public TaxRule findById(Long id) {
        return taxRuleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "TaxRule com ID=" + id + " não encontrada."));
    }

    /**
     * Salva uma nova regra de imposto, aplicando as seguintes validações:
     * <ol>
     * <li>Impede cadastrar uma regra com data de início igual ou posterior a uma já
     * existente
     * para o mesmo (codEfd, codigoReceita). Receitas diferentes no mesmo EFD são
     * permitidas.</li>
     * <li>Encerra automaticamente a vigência da regra anterior do mesmo (codEfd,
     * codigoReceita)
     * se ela estiver em aberto (dataFimVigencia == null).</li>
     * </ol>
     *
     * @param newRule A nova TaxRule a ser salva.
     * @return A TaxRule salva.
     */
    @Transactional
    public TaxRule save(TaxRule newRule) {
        if (newRule.getCodEfd() == null || newRule.getCodigoReceita() == null
                || newRule.getDataInicioVigencia() == null) {
            throw new BusinessRuleException(
                    "Código EFD, Código de Receita e Data de Início de Vigência são obrigatórios.");
        }

        // Encerra a vigência da regra anterior do mesmo (codEfd + codigoReceita), se
        // houver.
        taxRuleRepository.findVigenteByCodEfdAndCodigoReceitaAndDate(
                newRule.getCodEfd(), newRule.getCodigoReceita(), newRule.getDataInicioVigencia().minusDays(1))
                .ifPresent(previousRule -> {
                    if (previousRule.getDataFimVigencia() == null) {
                        previousRule.setDataFimVigencia(newRule.getDataInicioVigencia().minusDays(1));
                        taxRuleRepository.save(previousRule);
                    }
                });

        return taxRuleRepository.save(newRule);
    }

    /**
     * Retorna todas as regras vigentes para um EFD em uma data específica.
     * Pode retornar mais de uma quando o EFD possui múltiplos códigos de receita.
     * Usado pelo endpoint GET /API/TaxRule/opcoes.
     *
     * @param codEfd O código EFD.
     * @param data   A data de referência.
     * @return Lista de TaxRule vigentes.
     */
    public List<TaxRule> findOpcoesPorEfd(Integer codEfd, LocalDate data) {
        return taxRuleRepository.findAllVigenteByCodEfdAndDate(codEfd, data);
    }

    /**
     * Atualiza os detalhes de uma regra de imposto existente.
     * Apenas 'description', 'codigoReceita', 'items' e 'dataFimVigencia' podem ser alterados.
     * O {@code codEfd} nunca é alterado — permanece como está no banco de dados.
     * <p>
     * Validação de reabertura: se {@code dataFimVigencia} for removida (null) em uma regra
     * que estava encerrada, verifica se já existe uma versão mais recente ativa para o mesmo
     * par (codEfd + codigoReceita). Se houver, lança {@link BusinessRuleException}.
     * </p>
     *
     * @param id     ID da regra a ser atualizada.
     * @param update DTO com os campos editáveis (sem codEfd).
     * @return A regra atualizada.
     */
    public TaxRule update(Long id, TaxRuleUpdateDto update) {
        TaxRule existing = findById(id);

        // Valida reabertura: impede reabrir uma regra se já existe versão mais nova
        // ativa para o mesmo par (codEfd + codigoReceita)
        boolean estaReabrindo = update.getDataFimVigencia() == null
                && existing.getDataFimVigencia() != null;
        if (estaReabrindo) {
            boolean existeVersaoMaisNova = taxRuleRepository.existsNewerVersion(
                    existing.getCodEfd(),
                    existing.getCodigoReceita(),
                    existing.getId(),
                    existing.getDataInicioVigencia()
            );
            if (existeVersaoMaisNova) {
                throw new BusinessRuleException(
                        "Não é possível reabrir esta regra pois já existe uma versão mais " +
                        "recente ativa para o mesmo codEfd e código de receita.");
            }
        }

        existing.setDescription(update.getDescription());
        existing.setItems(update.getItems());
        existing.setCodigoReceita(update.getCodigoReceita());
        existing.setDataFimVigencia(update.getDataFimVigencia());
        return taxRuleRepository.save(existing);
    }
}
