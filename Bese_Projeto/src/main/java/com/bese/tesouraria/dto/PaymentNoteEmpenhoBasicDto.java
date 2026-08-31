package com.bese.tesouraria.dto;

import com.bese.tesouraria.entity.PaymentNoteEmpenho;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentNoteEmpenhoBasicDto {

    private Long id;
    private EmpenhoDto empenhoDto;
    private PaymentNoteBasicDto paymentNoteBasicDto;
    private FinancialPlanningBasicDto financialPlanningBasicDto;
    private BigDecimal value;

    public PaymentNoteEmpenhoBasicDto() {
    }

    public PaymentNoteEmpenhoBasicDto(PaymentNoteEmpenho paymentNoteEmpenho){
        if (paymentNoteEmpenho != null) {
            this.id = paymentNoteEmpenho.getId();
            this.empenhoDto = paymentNoteEmpenho.getEmpenho() != null ? new EmpenhoDto(paymentNoteEmpenho.getEmpenho()) : null;
            this.paymentNoteBasicDto = paymentNoteEmpenho.getPaymentNote() != null ? new PaymentNoteBasicDto(paymentNoteEmpenho.getPaymentNote()) : null;
            this.financialPlanningBasicDto = paymentNoteEmpenho.getFinancialPlanning() != null ? new FinancialPlanningBasicDto(paymentNoteEmpenho.getFinancialPlanning()) : null;
            this.value = paymentNoteEmpenho.getValue();
        }
    }

}