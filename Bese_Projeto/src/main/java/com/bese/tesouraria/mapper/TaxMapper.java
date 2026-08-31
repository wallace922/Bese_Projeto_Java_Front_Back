package com.bese.tesouraria.mapper;

import com.bese.tesouraria.dto.TaxDto;
import com.bese.tesouraria.entity.Tax;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TaxMapper {

    /**
     * Converte um único {@link TaxDto} para entidade {@link Tax}.
     * O flag {@code manualAdjustment} é propagado para o campo {@code manualTaxAdjustment}
     * da entidade (agora por grupo de imposto).
     */
    public Tax toEntity(TaxDto dto) {
        if (dto == null) return null;
        Tax tax = new Tax();
        tax.setId(dto.getId());
        tax.setTipo(dto.getTipo());
        tax.setCodEfdUsed(dto.getCodEfd());
        tax.setCodigoReceita(dto.getCodigoReceita());
        tax.setManualTaxAdjustment(dto.isManualAdjustment());

        // Se for ajuste manual, já popula os calculatedItems
        if (dto.isManualAdjustment()) {
            tax.setCalculatedItems(dto.getCalculatedItems());
        }

        return tax;
    }

    /** Converte uma lista de {@link TaxDto} para uma lista de entidades {@link Tax}. */
    public List<Tax> toEntityList(List<TaxDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return new ArrayList<>();
        List<Tax> result = new ArrayList<>();
        for (TaxDto dto : dtos) {
            result.add(toEntity(dto));
        }
        return result;
    }

    /**
     * Normaliza o payload de entrada: retorna a lista de Tax para um item.
     *
     * <p>Suporta os dois formatos:
     * <ul>
     *   <li>Novo: {@code taxes} (lista) — usado pelo frontend atualizado.</li>
     *   <li>Legado: {@code tax} (singular) — usado pelo frontend atual.
     *       Convertido para lista de 1 elemento internamente, com o flag
     *       {@code manualAdjustment} do item propagado para o grupo.</li>
     * </ul>
     *
     * @param taxesDtos   campo {@code taxes} do DTO (pode ser null no payload legado)
     * @param taxDto      campo {@code tax} do DTO (pode ser null no payload novo)
     * @param itemManual  flag {@code manualAdjustment} no nível do item (legado)
     */
    public List<Tax> resolveTaxesFromDto(List<TaxDto> taxesDtos, TaxDto taxDto, boolean itemManual) {
        if (taxesDtos != null && !taxesDtos.isEmpty()) {
            // Formato NOVO — usa a lista diretamente
            return toEntityList(taxesDtos);
        }
        if (taxDto != null) {
            // Formato LEGADO — converte o singular para lista de 1 elemento,
            // propagando o manualAdjustment do item para o grupo.
            if (itemManual && !taxDto.isManualAdjustment()) {
                taxDto.setManualAdjustment(true);
            }
            return Collections.singletonList(toEntity(taxDto));
        }
        // Nenhum imposto informado — retorna lista vazia (será validado pelo service)
        return new ArrayList<>();
    }

    public TaxDto toDto(Tax entity) {
        if (entity == null) return null;
        return new TaxDto(entity);
    }

    /** Converte uma lista de entidades {@link Tax} para lista de {@link TaxDto}. */
    public List<TaxDto> toDtoList(List<Tax> entities) {
        if (entities == null || entities.isEmpty()) return new ArrayList<>();
        List<TaxDto> result = new ArrayList<>();
        for (Tax tax : entities) {
            result.add(toDto(tax));
        }
        return result;
    }
}
