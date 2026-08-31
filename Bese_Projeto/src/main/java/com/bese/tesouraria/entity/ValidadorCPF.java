package com.bese.tesouraria.entity;

import java.util.regex.Pattern;

public class ValidadorCPF {

    private static final int[] peso = { 11, 10, 9, 8, 7, 6, 5, 4, 3, 2 };

    public static boolean validarCPF(String cpf) {
        if (cpf == null) {
            return false;
        }

        // Remove caracteres não numéricos
        cpf = cpf.replaceAll("\\D", "");

        // Valida tamanho
        if (cpf.length() != 11) {
            return false;
        }

        // Valida números repetidos (ex: 111.111.111-11)
        if (Pattern.matches("(.)\\1{10}", cpf)) {
            return false;
        }

        // Calcula o primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (cpf.charAt(i) - '0') * peso[i + 1];
        }

        int digito1 = soma % 11;
        if (digito1 < 2) {
            digito1 = 0;
        } else {
            digito1 = 11 - digito1;
        }

        // Valida o primeiro dígito
        if ((cpf.charAt(9) - '0') != digito1) {
            return false;
        }

        // Calcula o segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (cpf.charAt(i) - '0') * peso[i];
        }

        int digito2 = soma % 11;
        if (digito2 < 2) {
            digito2 = 0;
        } else {
            digito2 = 11 - digito2;
        }

        // Valida o segundo dígito
        return (cpf.charAt(10) - '0') == digito2;
    }

}
