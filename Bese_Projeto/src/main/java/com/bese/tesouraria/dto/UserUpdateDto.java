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
public class UserUpdateDto {

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 50, message = "O nome não pode ter mais de 50 caracteres")
    private String name;

    private Role role;

    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    private String password;
}
