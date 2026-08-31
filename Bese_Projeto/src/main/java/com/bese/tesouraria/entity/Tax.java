package com.bese.tesouraria.entity;

import com.bese.tesouraria.converter.TaxCalculatedItemListConverter;
import com.bese.tesouraria.enun.OptanteStatus;
import com.bese.tesouraria.enun.TaxStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
public class Tax implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private OptanteStatus tipo;

    @Column(name = "cod_efd_used")
    private Integer codEfdUsed;

    @Column(name = "codigo_receita")
    private Integer codigoReceita;

    @Column(name = "tax_rule_description", length = 300)
    private String taxRuleDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_status", nullable = false)
    private TaxStatus taxStatus;

    @Convert(converter = TaxCalculatedItemListConverter.class)
    @Column(name = "calculated_items", columnDefinition = "JSON")
    private List<TaxCalculatedItem> calculatedItems;

    /**
     * Relacionamento com o item de NP ao qual este Tax pertence.
     * Usa a nova FK payment_note_item_id (criada pela migration V7).
     * ManyToOne — permite que um item tenha N grupos de imposto (1:N).
     *
     * O campo payment_note_item.tax_id (OneToOne legado) ainda existe no banco
     * mas NÃO está mais mapeado aqui — lido apenas pelo backend legado durante
     * a transição (Fase 3). Será removido na Fase 6 (contract).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_note_item_id", referencedColumnName = "id")
    @JsonBackReference
    private PaymentNoteItem paymentNoteItem;

    /**
     * Flag de ajuste manual POR GRUPO de imposto.
     * Migrado de PaymentNoteItem para Tax, permitindo que cada grupo
     * tenha seu próprio estado de ajuste manual sem afetar os demais
     * grupos do mesmo item.
     *
     * Não persistido — é um comando de entrada da API (igual ao campo
     * homônimo que existia em PaymentNoteItemDto).
     */
    @Transient
    private boolean manualTaxAdjustment;

    public Tax() {}

    public Tax(OptanteStatus tipo, Integer codEfdUsed) {
        this.tipo = tipo;
        this.codEfdUsed = codEfdUsed;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(id, ((Tax) obj).id);
    }
}
