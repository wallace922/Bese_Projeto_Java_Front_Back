package com.bese.tesouraria.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
public class Empresa {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private String nome;

    @Column(length = 14)
    private String cnpj;

    public Empresa() {
    }

    public Empresa(String cnpj, String nome) {
        setCnpj(cnpj);
        this.nome = nome;
    }

    public void setCnpj(String cnpj) {
        if(!isValidCnpj(cnpj)){
            throw new IllegalArgumentException();
        }
        this.cnpj= cnpj;
    }

    public Boolean isValidCnpj(String value) {

        String cnpj = value.replaceAll("\\D", "");

        if (cnpj.length() != 14 || cnpj.matches("(\\d)\\1{13}")) return false;

        try {
            int d1 = calcularCnpj(cnpj.substring(0, 12), new int[]{5,4,3,2,9,8,7,6,5,4,3,2});
            int d2 = calcularCnpj(cnpj.substring(0, 12) + d1, new int[]{6,5,4,3,2,9,8,7,6,5,4,3,2});
            return cnpj.equals(cnpj.substring(0, 12) + d1 + d2);
        } catch (Exception e) {
            return false;
        }
    }

    private int calcularCnpj(String s, int[] p) {
        int soma = 0;
        for (int i = 0; i < s.length(); i++)
            soma += Integer.parseInt(s.substring(i, i+1)) * p[i];
        int r = soma % 11;
        return (r < 2) ? 0 : 11 - r;
    }

    @PrePersist
    @PreUpdate
    private void toUpperCaseBeforeSave() {
        if (this.nome != null) {
            this.nome = this.nome.toUpperCase();
        }
    }
}