package com.bese.tesouraria.dto;

import com.bese.tesouraria.entity.TaxRule;
import com.bese.tesouraria.entity.TaxRuleItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class TaxRuleDto {

    private Long id;

    @NotNull(message = "codEfd é obrigatório")
    private Integer codEfd;

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

    public TaxRuleDto() {}

    public TaxRuleDto(TaxRule rule) {
        this.id = rule.getId();
        this.codEfd = rule.getCodEfd();
        this.codigoReceita = rule.getCodigoReceita();
        this.description = rule.getDescription();
        this.dataInicioVigencia = rule.getDataInicioVigencia();
        this.dataFimVigencia = rule.getDataFimVigencia();
        this.items = rule.getItems();
    }
}
