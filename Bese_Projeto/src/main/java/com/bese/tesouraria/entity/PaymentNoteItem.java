package com.bese.tesouraria.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
public class PaymentNoteItem {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne
    @JoinColumn(name = "payment_note_id", referencedColumnName = "id")
    @JsonBackReference
    private PaymentNote paymentNote;

    @Setter
    @Column(name = "description", length = 255)
    private String description;

    @Setter
    @Column(name = "value", nullable = false)
    private BigDecimal value;

    /**
     * Lista de grupos de imposto deste item.
     * Um item pode ter N grupos, todos calculados sobre o mesmo {@code value}.
     *
     * Aditivo — a coluna legada {@code tax_id} ainda existe no banco (mantida
     * pela Fase 6/contract do plano). A relação abaixo usa a nova FK
     * {@code payment_note_item_id} em {@code tax}, criada pela migration V7.
     *
     * @OrderBy garante ordem determinística para que {@link #getTax()} retorne
     * sempre o mesmo grupo como "primeiro" (compatibilidade com frontend legado).
     */
    @OneToMany(mappedBy = "paymentNoteItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @JsonManagedReference
    private List<Tax> taxes = new ArrayList<>();

    /**
     * Empresa beneficiária do imposto retido neste item.
     * Nullable — quando null, herda a empresa da própria NP (comportamento padrão).
     * Corresponde à coluna empresa_beneficiaria adicionada pela migration V5.
     */
    @Setter
    @ManyToOne
    @JoinColumn(name = "empresa_beneficiaria", referencedColumnName = "id")
    private Empresa empresaBeneficiaria;

    public PaymentNoteItem() {}

    public PaymentNoteItem(String description, BigDecimal value, Tax tax) {
        this.description = description;
        this.value = value;
        if (tax != null) {
            tax.setPaymentNoteItem(this);
            this.taxes.add(tax);
        }
    }

    // ── Acessores de compatibilidade retroativa ────────────────────────────────

    /**
     * Atalho legado: retorna o PRIMEIRO grupo de imposto da lista.
     *
     * <p>Mantido para compatibilidade com o frontend antigo e com
     * código interno que ainda acessa {@code item.getTax()}.
     * Não persiste nenhuma coluna — puramente calculado em memória.
     *
     * @deprecated Usar {@link #getTaxes()} para acessar todos os grupos.
     */
    @Deprecated
    public Tax getTax() {
        return taxes.isEmpty() ? null : taxes.get(0);
    }

    /**
     * Atalho legado: define o imposto do item substituindo todos os grupos
     * existentes por uma lista de um único elemento.
     *
     * <p>Mantido para que código interno e mapeadores legados continuem
     * funcionando sem alteração durante a transição.
     *
     * @deprecated Usar {@link #getTaxes()} e manipular a lista diretamente.
     */
    @Deprecated
    public void setTax(Tax tax) {
        this.taxes.clear();
        if (tax != null) {
            tax.setPaymentNoteItem(this);
            this.taxes.add(tax);
        }
    }

    /**
     * Mantido para compatibilidade com PaymentNoteService durante a transição.
     * Lê o flag {@code manualTaxAdjustment} do primeiro grupo de imposto da lista.
     *
     * <p>O flag real agora está em {@link Tax#isManualTaxAdjustment()} (por grupo).
     * Este método existe apenas para que o código do service não precise ser
     * alterado de uma vez — será removido na Fase 6.
     *
     * @deprecated O ajuste manual agora é por grupo de imposto ({@link Tax}).
     */
    @Deprecated
    public boolean isManualTaxAdjustment() {
        Tax first = getTax();
        return first != null && first.isManualTaxAdjustment();
    }

    /**
     * @deprecated O ajuste manual agora é por grupo de imposto ({@link Tax}).
     */
    @Deprecated
    public void setManualTaxAdjustment(boolean manual) {
        Tax first = getTax();
        if (first != null) {
            first.setManualTaxAdjustment(manual);
        }
    }

    /** Adiciona um grupo de imposto a este item, configurando o relacionamento bidirecional. */
    public void addTax(Tax tax) {
        if (tax != null) {
            tax.setPaymentNoteItem(this);
            this.taxes.add(tax);
        }
    }
}
