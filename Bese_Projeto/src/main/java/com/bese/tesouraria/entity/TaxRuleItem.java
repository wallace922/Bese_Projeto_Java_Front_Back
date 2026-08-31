package com.bese.tesouraria.entity;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * Representa um item de imposto dentro de uma regra (TaxRule)
 * ou dentro de um snapshot calculado (TaxCalculatedItem).
 *
 * NÃO é uma entidade JPA — é um POJO serializado em JSON.
 */
@Getter
@Setter
public class TaxRuleItem {

    /**
     * Tipo do imposto.
     * Exemplos: "IR", "CSLL", "COFINS", "PIS_PASEP", "DARF", "ISS", "INSS".
     * É uma String para permitir extensão sem alterar enum.
     */
    private String taxType;

    /**
     * Alíquota decimal do imposto.
     * Exemplos: 0.012 = 1,2% | 0.03 = 3% | 0.0 = isento
     */
    private BigDecimal rate;

    public TaxRuleItem() {}

    public TaxRuleItem(String taxType, BigDecimal rate) {
        this.taxType = taxType;
        this.rate = rate;
    }
}
