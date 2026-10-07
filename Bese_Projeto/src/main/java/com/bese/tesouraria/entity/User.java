package com.bese.tesouraria.entity;

import com.bese.tesouraria.converter.CpfConverter;
import com.bese.tesouraria.enun.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, name = "name")
    private String name;

    // Cifrado em repouso (CpfConverter). Imutabilidade garantida na camada de
    // serviço (update nunca copia cpf); por isso sem updatable=false, o que
    // permite a migração automática de linhas legadas em claro.
    @Convert(converter = CpfConverter.class)
    @Column(nullable = false, unique = true, length = 255, name = "cpf")
    private String cpf;

    // Índice cego (HMAC dos dígitos) para busca exata e unicidade lógica.
    @Column(name = "cpf_hash", length = 64, unique = true)
    private String cpfHash;

    @Column(nullable = false, length = 255, name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role")
    private Role role;

    public User(Long id, String name, String cpf, String password, Role role) {
        this.id = id;
        this.name = name;
        setCPF(cpf);
        this.password = password;
        setRole(role);
    }

    public void setCPF(String cpf) {
        if (ValidadorCPF.validarCPF(cpf)) {
            this.cpf = cpf.replaceAll("\\D", "");
        } else {
            throw new IllegalArgumentException("CPF inválido");
        }
    }

    public void setRole(Role role) {
        this.role = (role != null) ? role : Role.USER;
    }

}
