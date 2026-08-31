package com.bese.tesouraria.dto;

import com.bese.tesouraria.entity.Tax;
import com.bese.tesouraria.entity.TaxCalculatedItem;
import com.bese.tesouraria.enun.OptanteStatus;
import com.bese.tesouraria.enun.TaxStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TaxDto {

    private Long id;

    @NotNull(message = "tipo (optante/nao_optante) é obrigatório")
    private OptanteStatus tipo;

    private Integer codEfd;

    /**
     * Flag para indicar que os valores de imposto estão sendo fornecidos manualmente.
     * Se true, o backend não calculará os impostos e usará os valores de 'calculatedItems'.
     * Default é false.
     */
    private boolean manualAdjustment = false;

    /**
     * Lista de impostos.
     * - Na criação/edição com manualAdjustment=true, o usuário envia esta lista com os valores ajustados.
     * - Na resposta da API, este campo contém os impostos calculados ou os que foram enviados manualmente.
     */
    @Valid // Garante que as validações dentro de TaxCalculatedItem sejam acionadas
    private List<TaxCalculatedItem> calculatedItems;

    // --- Campos de resposta / snapshot ---

    private Integer codigoReceita;
    private String taxRuleDescription;
    private TaxStatus taxStatus;

    public TaxDto() {}

    public TaxDto(Tax tax) {
        if (tax != null) {
            this.id = tax.getId();
            this.tipo = tax.getTipo();
            this.codEfd = tax.getCodEfdUsed();
            this.codigoReceita = tax.getCodigoReceita();
            this.taxRuleDescription = tax.getTaxRuleDescription();
            this.taxStatus = tax.getTaxStatus();
            this.calculatedItems = tax.getCalculatedItems();
            // manualAdjustment não é persistido, é apenas um comando para a API
        }
    }
}
