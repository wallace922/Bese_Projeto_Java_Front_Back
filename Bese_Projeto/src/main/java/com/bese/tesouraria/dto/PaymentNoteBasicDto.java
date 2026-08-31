package com.bese.tesouraria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.bese.tesouraria.entity.PaymentNote;
import com.bese.tesouraria.enun.StatusPaymentNote;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentNoteBasicDto {

    private Long id;

    @NotNull(message = "numero da Np não pode ser null")
    private Integer numeroNp;

    @NotNull(message = "data de liquidação não pode ser null")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
	private LocalDate dataLiquidacao;

	@NotNull
	private EmpresaDto empresa;

    @NotBlank @NotNull(message = "documento de origem não pode ser null")
    @Size(max = 25, message = "O Documento de Origem não pode ter mais de 25 caracteres")
	private String docOrigin;

	private BigDecimal value;

    @NotNull @NotNull(message = "status da Np não pode ser null")
    private StatusPaymentNote status;

    @NotNull @Valid
    private List<PaymentNoteItemDto> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate datePayment;

	public PaymentNoteBasicDto() {
	}

	public PaymentNoteBasicDto(PaymentNote paymentNote){
		this.id = paymentNote.getId();
		this.dataLiquidacao = paymentNote.getDataLiquidacao();
		this.empresa = paymentNote.getEmpresa() != null ? new EmpresaDto(paymentNote.getEmpresa()) : null;
		this.docOrigin = paymentNote.getDocOrigin();
		this.value = paymentNote.getValue();
		this.status = paymentNote.getStatus();
		this.numeroNp = paymentNote.getNumeroNp();
		this.datePayment = paymentNote.getDatePayment();
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

					// Popula o campo legado com o primeiro grupo (compatibilidade frontend antigo)
					dto.setTax(item.getTax() != null ? new TaxDto(item.getTax()) : null);

					dto.setManualAdjustment(item.isManualTaxAdjustment());
					dto.setEmpresaBeneficiaria(item.getEmpresaBeneficiaria() != null
							? new EmpresaDto(item.getEmpresaBeneficiaria()) : null);
					return dto;
				}).collect(Collectors.toList())
				: null;
	}

}
