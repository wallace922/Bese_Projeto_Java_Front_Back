package com.bese.tesouraria.converter;

import com.bese.tesouraria.entity.TaxRuleItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class TaxRuleItemListConverter implements AttributeConverter<List<TaxRuleItem>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Converte List<TaxRuleItem> → String JSON para salvar no banco.
     * @param items lista de itens de regra de imposto
     * @return string JSON ou null se lista nula/vazia
     */
    @Override
    public String convertToDatabaseColumn(List<TaxRuleItem> items) {
        try {
            return (items == null || items.isEmpty()) ? null : mapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao serializar TaxRuleItems para JSON", e);
        }
    }

    /**
     * Converte String JSON → List<TaxRuleItem> ao ler do banco.
     * @param json string JSON da coluna
     * @return lista de TaxRuleItem ou null se JSON nulo/vazio
     */
    @Override
    public List<TaxRuleItem> convertToEntityAttribute(String json) {
        try {
            return (json == null || json.isBlank()) ? null
                    : mapper.readValue(json, new TypeReference<List<TaxRuleItem>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao desserializar JSON para TaxRuleItems", e);
        }
    }
}
