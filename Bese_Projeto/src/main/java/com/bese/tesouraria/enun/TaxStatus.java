package com.bese.tesouraria.enun;

public enum TaxStatus {
    /** Impostos calculados com sucesso — snapshot salvo. */
    CALCULATED,

    /** Nota não-optante sem codEfd — aguarda inclusão futura de impostos. */
    PENDING,

    /** Nota optante — isenta, sem impostos. */
    EXEMPT
}
