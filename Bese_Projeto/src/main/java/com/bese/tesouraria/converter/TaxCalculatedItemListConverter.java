package com.bese.tesouraria.converter;

import com.bese.tesouraria.entity.TaxCalculatedItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class TaxCalculatedItemListConverter implements AttributeConverter<List<TaxCalculatedItem>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    /** Serializa List<TaxCalculatedItem> → JSON String para persistência. */
    @Override
    public String convertToDatabaseColumn(List<TaxCalculatedItem> items) {
        try {
            return (items == null || items.isEmpty()) ? null : mapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao serializar TaxCalculatedItems para JSON", e);
        }
    }

    /** Desserializa JSON String → List<TaxCalculatedItem> ao ler do banco. */
    @Override
    public List<TaxCalculatedItem> convertToEntityAttribute(String json) {
        try {
            return (json == null || json.isBlank()) ? null
                    : mapper.readValue(json, new TypeReference<List<TaxCalculatedItem>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao desserializar JSON para TaxCalculatedItems", e);
        }
    }
}
