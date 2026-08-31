package com.bese.tesouraria.dto;

import com.bese.tesouraria.entity.Empresa;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpresaDto {

    private Long id;

    @NotNull
    private String nome;
    
    @NotNull
    @Size(max = 14, message = "O CNPJ não pode ter mais de 14 caracteres")
    private String cnpj;

    public EmpresaDto(){
    }

    public EmpresaDto(Empresa empresa){
        if (empresa != null) {
            this.id = empresa.getId();
            this.cnpj = empresa.getCnpj();
            this.nome = empresa.getNome();
        }
    }
}