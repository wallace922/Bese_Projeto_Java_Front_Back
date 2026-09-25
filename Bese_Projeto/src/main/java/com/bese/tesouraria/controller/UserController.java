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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/API/User")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

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
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<PageDto<UserResponseDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<User> users = userService.findAll(pageable.getPageNumber(), pageable.getPageSize());
        Page<UserResponseDto> userDtos = users.map(mapper::toResponseDto);

        return ResponseEntity.ok(PageDto.of(userDtos));
    }

    @GetMapping("/{cpf}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponseDto> findByCpf(@PathVariable String cpf) {
        User user = userService.findByCpf(cpf);
        return ResponseEntity.ok(mapper.toResponseDto(user));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserResponseDto> me(Authentication authentication) {
        Long id = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(mapper.toResponseDto(userService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponseDto> save(@Valid @RequestBody UserCreateDto dto, HttpServletRequest request) {
        User entity = mapper.toEntity(dto);
        User savedUser = userService.save(entity);
        log.info("AUDIT_USER_CREATED | newUserId={} | role={} | ip={}", savedUser.getId(), savedUser.getRole(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponseDto(savedUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponseDto> update(
            @NotNull @PathVariable Long id,
            @Valid @RequestBody UserUpdateDto dto,
            HttpServletRequest request) {
        User userToUpdate = new User();
        userToUpdate.setName(dto.getName());
        userToUpdate.setRole(dto.getRole());
        userToUpdate.setPassword(dto.getPassword());

        User updatedUser = userService.update(id, userToUpdate);
        log.info("AUDIT_USER_UPDATED | targetUserId={} | newRole={} | ip={}", id, dto.getRole(), request.getRemoteAddr());
        return ResponseEntity.ok(mapper.toResponseDto(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteById(@NotNull @PathVariable Long id, HttpServletRequest request) {
        userService.delete(id);
        log.warn("AUDIT_USER_DELETED | deletedUserId={} | ip={}", id, request.getRemoteAddr());
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@Valid @RequestBody LoginRequestDto dto, HttpServletRequest request) {
        try {
            User user = userService.autenticar(dto.getCpf(), dto.getPassword());
            String jwtToken = tokenUtil.generateRawToken(user);
            ResponseCookie cookie = cookieUtil.createJwtCookie(jwtToken);

            log.info("AUDIT_LOGIN_SUCCESS | userId={} | role={} | ip={}", user.getId(), user.getRole(), request.getRemoteAddr());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(mapper.toResponseDto(user));
        } catch (RuntimeException e) {
            log.warn("AUDIT_LOGIN_FAILED | cpf={} | ip={} | reason={}", dto.getCpf(), request.getRemoteAddr(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> logout(HttpServletRequest request, Authentication authentication) {
        ResponseCookie cleanCookie = cookieUtil.createCleanJwtCookie();
        String userId = authentication != null ? authentication.getName() : "ANONYMOUS";
        log.info("AUDIT_LOGOUT | userId={} | ip={}", userId, request.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .build();
    }
}
