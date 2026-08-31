package com.bese.tesouraria.mapper;

import com.bese.tesouraria.dto.TaxRuleDto;
import com.bese.tesouraria.entity.TaxRule;
import org.springframework.stereotype.Component;

@Component
public class TaxRuleMapper {

    public TaxRule toEntity(TaxRuleDto dto) {
        if (dto == null) return null;
        TaxRule rule = new TaxRule();
        // O ID não é mapeado do DTO para a entidade na criação
        rule.setCodEfd(dto.getCodEfd());
        rule.setCodigoReceita(dto.getCodigoReceita());
        rule.setDescription(dto.getDescription());
        rule.setItems(dto.getItems());
        rule.setDataInicioVigencia(dto.getDataInicioVigencia());
        rule.setDataFimVigencia(dto.getDataFimVigencia());
        return rule;
    }

    public TaxRuleDto toDto(TaxRule entity) {
        if (entity == null) return null;
        return new TaxRuleDto(entity);
    }
}
