package com.bese.tesouraria.dto;

import com.bese.tesouraria.entity.Empenho;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpenhoDto {

    private Long id;

    @Positive(message = "o numero do empenho deve ser positivo")
    @NotNull(message = "Numero do empenho = null")
	private Integer numero;

    @Min(value = 1900, message = "O ano deve ter 4 dígitos e ser maior ou igual a 1900")
    @Max(value = 2050, message = "O ano não pode ser maior que 2050")
    @NotNull(message = "ano do empenho = null")
    private Integer ano;

    @Positive(message = "Fonte de Origem deve ser um número positivo")
    private Long fontDeOrigin;

    @Size(max = 11, message = "O Plano Interno não pode ter mais de 11 caracteres")
	private String internalPlan;

    @Positive(message = "A natureza deve ser positiva")
	private Integer nature;

    public EmpenhoDto() {
    }

	public EmpenhoDto(Empenho empenho){
	    if (empenho != null) {
	        this.id = empenho.getId();
            this.numero = empenho.getNumero();
            this.ano = empenho.getAno();
            this.fontDeOrigin = empenho.getFontDeOrigin();
            this.internalPlan = empenho.getInternalPlan();
            this.nature = empenho.getNature();
        }
	}
}