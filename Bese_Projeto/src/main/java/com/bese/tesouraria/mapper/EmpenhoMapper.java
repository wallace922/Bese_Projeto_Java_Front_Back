package com.bese.tesouraria.mapper;

import org.springframework.stereotype.Component;

import com.bese.tesouraria.dto.EmpenhoDto;
import com.bese.tesouraria.entity.Empenho;

@Component
public class EmpenhoMapper {

    public Empenho toEntity(EmpenhoDto dto) {
        if (dto == null) {
            return null;
        }
        
        return new Empenho(
                dto.getId(),
                dto.getNumero(),
                dto.getAno(),
                dto.getInternalPlan(),
                dto.getNature(),
                dto.getFontDeOrigin()
        );
    }

    public EmpenhoDto toDto(Empenho entity) {
        if (entity == null) {
            return null;
        }
        return new EmpenhoDto(entity);
    }
}