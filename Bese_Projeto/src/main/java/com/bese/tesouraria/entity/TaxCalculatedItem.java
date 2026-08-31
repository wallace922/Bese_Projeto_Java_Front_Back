package com.bese.tesouraria.entity;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * Snapshot de um imposto calculado no momento da nota.
 * Armazenado em tax.calculated_items como JSON.
 * Imutável após o cálculo — preserva a fotografia da época.
 */
@Getter
@Setter
public class TaxCalculatedItem {

    /** Tipo do imposto. Ex: "IR", "CSLL", "COFINS". */
    private String taxType;

    /** Alíquota usada no cálculo. Ex: 0.012 para 1,2%. */
    private BigDecimal rate;

    /** Valor monetário calculado. Ex: R$ 120,00. */
    private BigDecimal amount;

    public TaxCalculatedItem() {}

    public TaxCalculatedItem(String taxType, BigDecimal rate, BigDecimal amount) {
        this.taxType = taxType;
        this.rate = rate;
        this.amount = amount;
    }
}
