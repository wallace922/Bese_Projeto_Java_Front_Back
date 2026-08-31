package com.bese.tesouraria.dto;

import com.bese.tesouraria.entity.FinancialPlanning;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FinancialPlanningBasicDto {

    private Long id;

    @NotNull(message = "O número não pode ser nulo")
    @Digits(integer = 6, fraction = 0, message = "O número deve ter no máximo 6 dígitos")
    private Integer numero;

    @NotNull(message = "Por favor informe a data!")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate data;

    @NotNull(message = "Informe o número da vinculação")
    @Min(value = 0, message = "Informe o número da vinculação")
    @Digits(integer = 3, fraction = 0)
    private Integer vinculation;

    @NotNull(message = "Informe a OM de origem")
    @Min(value = 0, message = "Informe a OM de origem")
    @Digits(integer = 6, fraction = 0)
    private Integer origin;

    public FinancialPlanningBasicDto() {
    }

    public FinancialPlanningBasicDto(FinancialPlanning financialPlanning){
        if (financialPlanning != null) {
            this.id = financialPlanning.getId();
            this.numero = financialPlanning.getNumero();
            this.data = financialPlanning.getData();
            this.vinculation = financialPlanning.getVinculation();
            this.origin = financialPlanning.getOrigin();
        }
    }
}