package com.bese.tesouraria.mapper;

import org.springframework.stereotype.Component;

import com.bese.tesouraria.dto.PaymentNoteBasicDto;
import com.bese.tesouraria.entity.PaymentNote;
import com.bese.tesouraria.entity.PaymentNoteItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentNoteMapper {

    private final EmpresaMapper empresaMapper;
    private final PaymentNoteItemMapper paymentNoteItemMapper;

    public PaymentNoteMapper(EmpresaMapper empresaMapper, PaymentNoteItemMapper paymentNoteItemMapper) {
        this.empresaMapper = empresaMapper;
        this.paymentNoteItemMapper = paymentNoteItemMapper;
    }

    public PaymentNote toEntity(PaymentNoteBasicDto dto) {
        if (dto == null) {
            return null;
        }

        List<PaymentNoteItem> items = dto.getItems() != null
                ? dto.getItems().stream().map(paymentNoteItemMapper::toEntity).collect(Collectors.toList())
                : new ArrayList<>();

        BigDecimal total = items.stream()
                .filter(item -> item != null && item.getValue() != null)
                .map(item -> item.getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PaymentNote paymentNote = new PaymentNote(
                dto.getDataLiquidacao(),
                empresaMapper.toEntity(dto.getEmpresa()),
                dto.getDocOrigin(),
                dto.getStatus(),
                total,
                dto.getNumeroNp(),
                items,
                dto.getDatePayment()
        );
        paymentNote.setId(dto.getId());

        items.forEach(item -> item.setPaymentNote(paymentNote));

        return paymentNote;
    }

    public PaymentNoteBasicDto toDto(PaymentNote entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentNoteBasicDto(entity);
    }
}
