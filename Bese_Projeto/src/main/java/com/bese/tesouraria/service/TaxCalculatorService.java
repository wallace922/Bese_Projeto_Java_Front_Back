package com.bese.tesouraria.service;

import com.bese.tesouraria.entity.Tax;
import com.bese.tesouraria.entity.TaxCalculatedItem;
import com.bese.tesouraria.entity.TaxRule;
import com.bese.tesouraria.enun.OptanteStatus;
import com.bese.tesouraria.enun.TaxStatus;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.bese.tesouraria.repository.TaxRuleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaxCalculatorService implements TaxCalculator {

    private final TaxRuleRepository taxRuleRepository;

    public TaxCalculatorService(TaxRuleRepository taxRuleRepository) {
        this.taxRuleRepository = taxRuleRepository;
    }

    @Override
    public Tax calculate(Tax tax, BigDecimal value, LocalDate dataLiquidacao, boolean isManualAdjustment) {

        // ── Regra 0: Ajuste Manual ──────────────────────────────────────────────
        if (isManualAdjustment) {
            // Se for ajuste manual, os itens já foram setados pelo mapper.
            // Apenas garantimos que o status esteja correto.
            tax.setTaxStatus(TaxStatus.CALCULATED);
            // Opcional: buscar a regra para popular codigoReceita e description
            if (tax.getCodEfdUsed() != null) {
                // Em ajuste manual, já sabemos o codigoReceita — buscamos a regra correspondente
                // para popular description. Se não achar, não é erro (valores foram informados manualmente).
                taxRuleRepository.findAllVigenteByCodEfdAndDate(tax.getCodEfdUsed(), dataLiquidacao)
                    .stream()
                    .filter(r -> tax.getCodigoReceita() == null || r.getCodigoReceita().equals(tax.getCodigoReceita()))
                    .findFirst()
                    .ifPresent(rule -> {
                        tax.setCodigoReceita(rule.getCodigoReceita());
                        tax.setTaxRuleDescription(rule.getDescription());
                    });
            }
            return tax;
        }

        // ── Regra 1: Optante ──────────────────────────────────────────────────────
        if (OptanteStatus.OPTANTE.equals(tax.getTipo())) {
            tax.setTaxStatus(TaxStatus.EXEMPT);
            tax.setCalculatedItems(null);
            return tax;
        }

        // ── Regra 2: Não-optante sem codEfd (pendente) ────────────────────────────
        if (tax.getCodEfdUsed() == null) {
            tax.setTaxStatus(TaxStatus.PENDING);
            tax.setCalculatedItems(null);
            return tax;
        }

        // ── Regra 3: Não-optante com codEfd (cálculo automático) ────────────────
        List<TaxRule> opcoes = taxRuleRepository.findAllVigenteByCodEfdAndDate(tax.getCodEfdUsed(), dataLiquidacao);

        final TaxRule rule;
        if (opcoes.isEmpty()) {
            throw new BusinessRuleException(
                "Nenhuma regra de imposto ativa encontrada para o código EFD " + tax.getCodEfdUsed() +
                " na data " + dataLiquidacao.format(DateTimeFormatter.ISO_LOCAL_DATE));
        } else if (opcoes.size() == 1) {
            // Comportamento atual mantido 100% compatível: calcula automaticamente
            rule = opcoes.get(0);
        } else {
            // Múltiplos códigos de receita: o usuário deve informar qual usar
            if (tax.getCodigoReceita() == null) {
                throw new BusinessRuleException(
                    "O código EFD " + tax.getCodEfdUsed() + " possui mais de um código de receita vigente. " +
                    "Informe qual código de receita deve ser usado.");
            }
            rule = opcoes.stream()
                .filter(r -> r.getCodigoReceita().equals(tax.getCodigoReceita()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                    "Código de receita " + tax.getCodigoReceita() + " não é válido para o EFD " +
                    tax.getCodEfdUsed() + " na data informada."));
        }

        tax.setCodigoReceita(rule.getCodigoReceita());
        tax.setTaxRuleDescription(rule.getDescription());

        List<TaxCalculatedItem> snapshot = calculateSnapshot(rule, value);

        tax.setCalculatedItems(snapshot);
        tax.setTaxStatus(TaxStatus.CALCULATED);
        return tax;
    }

    private List<TaxCalculatedItem> calculateSnapshot(TaxRule rule, BigDecimal value) {
        return rule.getItems().stream()
                .map(item -> {
                    BigDecimal amount = value
                            .multiply(item.getRate())
                            .setScale(2, RoundingMode.HALF_UP);
                    return new TaxCalculatedItem(item.getTaxType(), item.getRate(), amount);
                })
                .collect(Collectors.toList());
    }
}
