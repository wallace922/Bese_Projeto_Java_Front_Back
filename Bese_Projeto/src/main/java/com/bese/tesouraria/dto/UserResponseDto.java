package com.bese.tesouraria.dto;

import com.bese.tesouraria.entity.User;
import com.bese.tesouraria.enun.Role;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserResponseDto {

    private Long id;
    private String name;
    private String cpf;
    private Role role;

    public UserResponseDto(User user) {
        if (user != null) {
            this.id = user.getId();
            this.name = user.getName();
            this.cpf = user.getCpf();
            this.role = user.getRole();
        }
    }
}
