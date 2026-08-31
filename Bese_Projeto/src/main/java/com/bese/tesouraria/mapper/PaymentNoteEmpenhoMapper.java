package com.bese.tesouraria.mapper;

import com.bese.tesouraria.dto.PaymentNoteEmpenhoBasicDto;
import com.bese.tesouraria.entity.PaymentNoteEmpenho;
import org.springframework.stereotype.Component;

@Component
public class PaymentNoteEmpenhoMapper {

    private final EmpenhoMapper empenhoMapper;
    private final PaymentNoteMapper paymentNoteMapper;
    private final FinancialPlanningMapper financialPlanningMapper;

    public PaymentNoteEmpenhoMapper(EmpenhoMapper empenhoMapper, PaymentNoteMapper paymentNoteMapper, FinancialPlanningMapper financialPlanningMapper) {
        this.empenhoMapper = empenhoMapper;
        this.paymentNoteMapper = paymentNoteMapper;
        this.financialPlanningMapper = financialPlanningMapper;
    }

    public PaymentNoteEmpenho toEntity(PaymentNoteEmpenhoBasicDto dto) {
        if (dto == null) {
            return null;
        }

        PaymentNoteEmpenho entity = new PaymentNoteEmpenho();
        entity.setId(dto.getId());
        entity.setEmpenho(empenhoMapper.toEntity(dto.getEmpenhoDto()));
        entity.setPaymentNote(paymentNoteMapper.toEntity(dto.getPaymentNoteBasicDto()));
        entity.setFinancialPlanning(financialPlanningMapper.toEntity(dto.getFinancialPlanningBasicDto()));
        if (dto.getValue() != null) {
            entity.setValue(dto.getValue());
        }

        return entity;
    }

    public PaymentNoteEmpenhoBasicDto toDto(PaymentNoteEmpenho entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentNoteEmpenhoBasicDto(entity);
    }
}