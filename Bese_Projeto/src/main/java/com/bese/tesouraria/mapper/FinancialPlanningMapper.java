package com.bese.tesouraria.mapper;

import org.springframework.stereotype.Component;

import com.bese.tesouraria.dto.FinancialPlanningBasicDto;
import com.bese.tesouraria.entity.FinancialPlanning;

@Component
public class FinancialPlanningMapper {

    public FinancialPlanning toEntity(FinancialPlanningBasicDto dto) {
        if (dto == null) {
            return null;
        }
        
        FinancialPlanning fp = new FinancialPlanning(dto.getData(), dto.getVinculation(), dto.getOrigin(), dto.getNumero());
        fp.setId(dto.getId());
        return fp;
    }

    public FinancialPlanningBasicDto toDto(FinancialPlanning entity) {
        if (entity == null) {
            return null;
        }
        return new FinancialPlanningBasicDto(entity);
    }
}