package com.bese.tesouraria.mapper;

import org.springframework.stereotype.Component;

import com.bese.tesouraria.dto.LoginRequestDto;
import com.bese.tesouraria.dto.UserCreateDto;
import com.bese.tesouraria.dto.UserResponseDto;
import com.bese.tesouraria.entity.User;

@Component
public class UserMapper {

    public UserResponseDto toResponseDto(User entity) {
        if (entity == null) {
            return null;
        }
        return new UserResponseDto(entity);
    }

    public User toEntity(UserCreateDto dto) {
        if (dto == null) {
            return null;
        }
        return new User(null, dto.getName(), dto.getCpf(), dto.getPassword(), dto.getRole());
    }

    public User toEntity(LoginRequestDto dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setCPF(dto.getCpf());
        user.setPassword(dto.getPassword());
        return user;
    }
}
