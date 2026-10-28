package com.bese.tesouraria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PaymentNoteItemDto {

    private Long id;

    @Size(max = 255, message = "descrição do item não pode ter mais de 255 caracteres")
    private String description;

    @NotNull(message = "valor do item não pode ser null")
    @DecimalMin(value = "0.00", inclusive = true, message = "valor do item não pode ser negativo")
    private BigDecimal value;

    /**
     * Campo legado — retorna o primeiro grupo de imposto da lista {@code taxes}.
     *
     * <p>
     * Mantido para compatibilidade retroativa com o frontend antigo.
     * O frontend novo deve usar {@link #taxes} para ler e enviar múltiplos grupos.
     *
     * @deprecated Usar {@link #taxes}.
     */
    @Deprecated
    @Valid
    private TaxDto tax;

    /**
     * Lista de grupos de imposto deste item.
     * Cada grupo pode vir de um {@code codEfd}/{@code codigoReceita} diferente,
     * todos calculados sobre o mesmo {@code value}.
     *
     * <p>
     * O frontend novo usa este campo. O frontend legado usa {@link #tax}
     * (singular) — o mapper converte automaticamente entre os dois formatos.
     */
    @Valid
    @Size(max = 10, message = "item não pode ter mais de 10 grupos de imposto")
    private List<TaxDto> taxes;

    /**
     * Flag de ajuste manual — DEPRECATED.
     * O ajuste manual agora é por grupo de imposto (campo {@code manualAdjustment}
     * dentro de cada {@link TaxDto} em {@link #taxes}).
     *
     * <p>
     * Mantido para compatibilidade com payloads do frontend legado que ainda
     * enviam este campo no nível do item. O mapper propagará o valor para o
     * único grupo da lista.
     *
     * @deprecated Usar {@link TaxDto#isManualAdjustment()} em cada grupo.
     */
    @Deprecated
    private boolean manualAdjustment;

    /**
     * Empresa beneficiária do imposto retido neste item.
     * Opcional — se null, o backend herda a empresa da própria NP.
     */
    private EmpresaDto empresaBeneficiaria;
}
