package com.bese.tesouraria.controller;

import com.bese.tesouraria.dto.LoginRequestDto;
import com.bese.tesouraria.dto.PageDto;
import com.bese.tesouraria.dto.UserCreateDto;
import com.bese.tesouraria.dto.UserResponseDto;
import com.bese.tesouraria.dto.UserUpdateDto;
import com.bese.tesouraria.entity.User;
import com.bese.tesouraria.mapper.UserMapper;
import com.bese.tesouraria.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bese.tesouraria.security.CookieUtil;
import com.bese.tesouraria.security.TokenUtil;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/API/User")
public class UserController {

    private final TokenUtil tokenUtil;
    private final CookieUtil cookieUtil;
    private final UserService userService;
    private final UserMapper mapper;

    public UserController(UserService userService, UserMapper mapper, TokenUtil tokenUtil, CookieUtil cookieUtil) {
        this.userService = userService;
        this.mapper = mapper;
        this.tokenUtil = tokenUtil;
        this.cookieUtil = cookieUtil;
    }

    @GetMapping
    public ResponseEntity<PageDto<UserResponseDto>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> users = userService.findAll(page, size);
        Page<UserResponseDto> userDtos = users.map(mapper::toResponseDto);

        PageDto<UserResponseDto> pageDto = new PageDto<>(
                userDtos.getContent(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isLast());

        return ResponseEntity.ok(pageDto);
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<UserResponseDto> findByCpf(@PathVariable String cpf) {
        User user = userService.findByCpf(cpf);
        return ResponseEntity.ok(mapper.toResponseDto(user));
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> save(@Valid @RequestBody UserCreateDto dto) {
        User entity = mapper.toEntity(dto);
        User savedUser = userService.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponseDto(savedUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> update(
            @NotNull @PathVariable Long id,
            @Valid @RequestBody UserUpdateDto dto) {
        User userToUpdate = new User();
        userToUpdate.setName(dto.getName());
        userToUpdate.setRole(dto.getRole());
        userToUpdate.setPassword(dto.getPassword());

        User updatedUser = userService.update(id, userToUpdate);
        return ResponseEntity.ok(mapper.toResponseDto(updatedUser));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteById(@NotNull @PathVariable Long id) {
        userService.delete(id);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {

        String jwtToken = tokenUtil.generateRawToken(userService.autenticar(dto.getCpf(), dto.getPassword()));

        ResponseCookie cookie = cookieUtil.createJwtCookie(jwtToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(mapper.toResponseDto(userService.autenticar(dto.getCpf(), dto.getPassword())));
    }
}
