package com.bese.tesouraria.converter;

import com.bese.tesouraria.security.CryptoService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Cifra o CNPJ em repouso (coluna {@code empresa.cnpj}).
 *
 * <p>Fora de contexto Spring (ex: testes sem container), repassa o valor.
 */
@Converter
public class CnpjConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String value) {
        if (value == null) {
            return null;
        }
        CryptoService crypto = CryptoService.get();
        return crypto == null ? value : crypto.encrypt(value);
    }

    @Override
    public String convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        CryptoService crypto = CryptoService.get();
        return crypto == null ? value : crypto.decrypt(value);
    }
}
