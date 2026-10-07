package com.bese.tesouraria.service;

import com.bese.tesouraria.entity.User;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.bese.tesouraria.exception.EntityNotFoundException;
import com.bese.tesouraria.repository.UserRepository;
import com.bese.tesouraria.security.CryptoService;

import jakarta.persistence.EntityExistsException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CryptoService crypto;

    public UserService(UserRepository userRepository, CryptoService crypto) {
        this.userRepository = userRepository;
        this.crypto = crypto;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Page<User> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAllByOrderByIdDesc(pageable);
    }

    // Busca por índice cego (HMAC); com fallback para linha legada em claro,
    // que é migrada (cifrada + hash) no primeiro uso.
    @Transactional
    public User findByCpf(String cpf) {
        String digits = CryptoService.digits(cpf);
        if (digits == null || digits.isBlank()) {
            throw new EntityNotFoundException("Usuário não encontrado");
        }
        String hash = crypto.hmacHex(digits);
        return userRepository.findByCpfHash(hash)
                .orElseGet(() -> upgradeLegacyRow(digits, hash));
    }

    private User upgradeLegacyRow(String digits, String hash) {
        User legacy = userRepository.findByCpfLegacy(digits)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        legacy.setCpfHash(hash);
        return userRepository.save(legacy);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    public User save(User user) {
        String digits = CryptoService.digits(user.getCpf());
        if (digits == null || digits.isBlank()) {
            throw new BusinessRuleException("CPF inválido ou não fornecido.");
        }
        String hash = crypto.hmacHex(digits);
        if (userRepository.existsByCpfHash(hash) || userRepository.findByCpfLegacy(digits).isPresent()) {
            throw new EntityExistsException("Usuário já cadastrado com este CPF");
        }
        user.setCpfHash(hash);
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }

    public User update(Long id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        existingUser.setName(user.getName());

        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            existingUser.setPassword(encodedPassword);
        }

        return userRepository.save(existingUser);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("Usuário não encontrado.");
        }
        userRepository.deleteById(id);
    }

    public User autenticar(String cpf, String rawPassword) {
        final User user;
        try {
            user = findByCpf(cpf);
        } catch (EntityNotFoundException e) {
            // Mensagem genérica de propósito: não revelar se o CPF existe.
            throw new BusinessRuleException("CPF ou senha inválidos");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessRuleException("CPF ou senha inválidos");
        }

        return user;
    }
}
