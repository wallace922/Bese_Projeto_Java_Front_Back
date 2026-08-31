package com.bese.tesouraria.service;

import com.bese.tesouraria.entity.Tax;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface TaxCalculator {
    Tax calculate(Tax tax, BigDecimal value, LocalDate dataLiquidacao, boolean isManualAdjustment);
}
