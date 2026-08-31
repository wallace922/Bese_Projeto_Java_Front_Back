package com.bese.tesouraria.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
public class PaymentNoteEmpenho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "EMPENHO_FK")
    @JsonBackReference
    private Empenho empenho;

    @ManyToOne
    @JoinColumn(name = "PAYMENTNOTE_FK")
    @JsonBackReference
    private PaymentNote paymentNote;

    @ManyToOne
    @JoinColumn(name = "FINACIALPLANNING_FK")
    @JsonBackReference
    private FinancialPlanning financialPlanning;

    private BigDecimal value;

    public PaymentNoteEmpenho() {
    }

    public void setValue(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NullPointerException();
        }
        this.value = value;
    }

}
