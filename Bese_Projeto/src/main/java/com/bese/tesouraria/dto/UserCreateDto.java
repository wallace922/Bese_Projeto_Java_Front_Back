package com.bese.tesouraria.dto;

import com.bese.tesouraria.enun.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserCreateDto {

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 50, message = "O nome não pode ter mais de 50 caracteres")
    private String name;

    @NotBlank(message = "O CPF é obrigatório")
    @Size(max = 14, message = "O CPF não pode ter mais de 14 caracteres")
    private String cpf;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    private String password;

    private Role role;
}
