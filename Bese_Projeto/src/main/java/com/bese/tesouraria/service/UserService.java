package com.bese.tesouraria.service;

import com.bese.tesouraria.entity.User;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.bese.tesouraria.exception.EntityNotFoundException;
import com.bese.tesouraria.repository.UserRepository;

import jakarta.persistence.EntityExistsException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Page<User> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAllByOrderByIdDesc(pageable);
    }

    public User findByCpf(String cpf) {
        return userRepository.findByCpf(cpf).orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    public User save(User user) {
        if (userRepository.findByCpf(user.getCpf()).isPresent()) {
            throw new EntityExistsException("Usuário já cadastrado com este CPF");
        }
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

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("Usuário não encontrado.");
        }
        userRepository.deleteById(id);
    }

    public User autenticar(String cpf, String rawPassword) {
        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new BusinessRuleException("CPF ou senha inválidos"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessRuleException("CPF ou senha inválidos");
        }

        return user;
    }
}
