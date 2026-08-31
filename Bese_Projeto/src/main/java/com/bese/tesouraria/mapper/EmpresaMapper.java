package com.bese.tesouraria.mapper;

import org.springframework.stereotype.Component;

import com.bese.tesouraria.dto.EmpresaDto;
import com.bese.tesouraria.entity.Empresa;

@Component
public class EmpresaMapper {

    public Empresa toEntity(EmpresaDto dto) {
        if (dto == null) {
            return null;
        }

        Empresa emp = new Empresa(dto.getCnpj(), dto.getNome());
        emp.setId(dto.getId());
        return emp;
    }

    public EmpresaDto toDto(Empresa entity) {
        if (entity == null) {
            return null;
        }
        return new EmpresaDto(entity);
    }
}
