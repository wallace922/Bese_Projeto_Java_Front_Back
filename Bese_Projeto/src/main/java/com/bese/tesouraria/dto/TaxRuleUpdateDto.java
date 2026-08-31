package com.bese.tesouraria.dto;

import com.bese.tesouraria.entity.TaxRuleItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO exclusivo para o endpoint PUT /API/TaxRule/{id}.
 * <p>
 * Separa as validações de criação (POST) das de atualização (PUT):
 * no PUT, {@code codEfd} não é enviado pelo frontend e não deve ser
 * alterado — ele permanece como está no banco de dados.
 * Apenas {@code description}, {@code codigoReceita}, {@code items}
 * e {@code dataFimVigencia} podem ser atualizados.
 * </p>
 */
@Getter
@Setter
public class TaxRuleUpdateDto {

    @NotNull(message = "Código da Receita é obrigatório")
    private Integer codigoReceita;

    private String description;

    @NotNull(message = "Data de início de vigência é obrigatória")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataInicioVigencia;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataFimVigencia;

    @NotNull(message = "items é obrigatório")
    private List<TaxRuleItem> items;
}
