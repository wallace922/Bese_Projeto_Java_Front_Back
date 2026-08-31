package com.bese.tesouraria.dto;

import com.bese.tesouraria.entity.PaymentNote;
import com.bese.tesouraria.enun.StatusPaymentNote;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class PaymentNoteVinculacaoDto {

    private Long id;
    private Integer numeroNp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataLiquidacao;

    private EmpresaDto empresa;
    private String docOrigin;
    private BigDecimal value;
    private StatusPaymentNote status;

    private Integer vinculation;

    private List<PaymentNoteItemDto> items;

    public PaymentNoteVinculacaoDto() {}

    public PaymentNoteVinculacaoDto(PaymentNote paymentNote, Integer vinculation) {
        this.id = paymentNote.getId();
        this.numeroNp = paymentNote.getNumeroNp();
        this.dataLiquidacao = paymentNote.getDataLiquidacao();
        this.empresa = paymentNote.getEmpresa() != null
                ? new EmpresaDto(paymentNote.getEmpresa()) : null;
        this.docOrigin = paymentNote.getDocOrigin();
        this.value = paymentNote.getValue();
        this.status = paymentNote.getStatus();
        this.vinculation = vinculation;
        this.items = paymentNote.getItems() != null
                ? paymentNote.getItems().stream().map(item -> {
                    PaymentNoteItemDto dto = new PaymentNoteItemDto();
                    dto.setId(item.getId());
                    dto.setDescription(item.getDescription());
                    dto.setValue(item.getValue());

                    // Popula a lista completa de grupos de imposto (campo novo)
                    List<TaxDto> taxDtos = item.getTaxes() != null
                            ? item.getTaxes().stream()
                                .map(TaxDto::new)
                                .collect(Collectors.toList())
                            : null;
                    dto.setTaxes(taxDtos);

                    // Popula o campo legado (compatibilidade frontend antigo)
                    dto.setTax(item.getTax() != null ? new TaxDto(item.getTax()) : null);

                    dto.setManualAdjustment(item.isManualTaxAdjustment());
                    dto.setEmpresaBeneficiaria(item.getEmpresaBeneficiaria() != null
                            ? new EmpresaDto(item.getEmpresaBeneficiaria()) : null);
                    return dto;
                }).collect(Collectors.toList())
                : null;
    }
}
