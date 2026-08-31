package com.bese.tesouraria.entity;

import com.bese.tesouraria.enun.StatusPaymentNote;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
public class PaymentNote {

	@Setter
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(name = "NumeroPaymentNote")
    private Integer numeroNp;

	@Column(name = "Dataliquidação")
	@JsonFormat(pattern = "dd/MM/yyyy")
	private LocalDate dataLiquidacao;

	@Setter
	@ManyToOne
	@JoinColumn(name = "empresa", referencedColumnName = "id")
	private Empresa empresa;

	@JsonFormat(pattern = "dd/MM/yyyy")
	private LocalDate datePayment;

	@Setter
    @Column(name = "DocumentoOrigin", length = 25)
	private String docOrigin;

	@Setter
    @Column(name = "valor")
	private BigDecimal value;

	@Enumerated(EnumType.STRING)
    @Column(name = "status")
	private StatusPaymentNote status;

	@OneToMany(mappedBy = "paymentNote", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<PaymentNoteItem> items = new ArrayList<>();

	public PaymentNote() {
	}

	public PaymentNote(LocalDate dataLiquidacao, Empresa empresa,
					   String docOrigin, StatusPaymentNote status, BigDecimal value, Integer numeroNp, List<PaymentNoteItem> items, LocalDate datePayment) {
		setDataLiquidacao(dataLiquidacao);
		this.empresa = empresa;
		this.docOrigin = docOrigin;
		this.value = value;
        this.numeroNp = numeroNp;
		this.items = items;
		setStatus(status);
		setDatePayment(datePayment);
	}

	public void setDataLiquidacao(LocalDate dataLiquidacao){
		if (dataLiquidacao != null && dataLiquidacao.isAfter(LocalDate.now())){
			throw new BusinessRuleException("A data de liquidação não pode ser no futuro.");
		}
		this.dataLiquidacao = dataLiquidacao;
	}

	public void setStatus(StatusPaymentNote status) {
		if (status != StatusPaymentNote.PAGA) {
			this.datePayment = null;
		}
		this.status = status;
	}

	public void setDatePayment(LocalDate datePayment) {
		if (datePayment != null && this.status != StatusPaymentNote.PAGA) {
			throw new BusinessRuleException("Não é possível definir uma data de pagamento para uma nota de pagamento que não está com o status PAGA.");
		}
		this.datePayment = datePayment;
	}

    @PrePersist
    @PreUpdate
    private void validateBeforeSave() {
        if (this.docOrigin != null) {
            this.docOrigin = this.docOrigin.toUpperCase();
        }
        if (this.status == StatusPaymentNote.PAGA && this.datePayment == null) {
            throw new BusinessRuleException("A data de pagamento é obrigatória quando o status da Nota de Pagamento é PAGA.");
        }
    }
}
