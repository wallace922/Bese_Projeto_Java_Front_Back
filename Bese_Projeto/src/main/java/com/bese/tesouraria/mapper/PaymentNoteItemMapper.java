package com.bese.tesouraria.mapper;

import com.bese.tesouraria.dto.EmpresaDto;
import com.bese.tesouraria.dto.PaymentNoteItemDto;
import com.bese.tesouraria.dto.TaxDto;
import com.bese.tesouraria.entity.Empresa;
import com.bese.tesouraria.entity.PaymentNoteItem;
import com.bese.tesouraria.entity.Tax;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentNoteItemMapper {

    private final TaxMapper taxMapper;

    public PaymentNoteItemMapper(TaxMapper taxMapper) {
        this.taxMapper = taxMapper;
    }

    /**
     * Converte um {@link PaymentNoteItemDto} para entidade {@link PaymentNoteItem}.
     *
     * <p>Suporta dois formatos de payload:
     * <ul>
     *   <li><b>Novo</b>: {@code taxes} (lista de grupos) — frontend atualizado.</li>
     *   <li><b>Legado</b>: {@code tax} (singular) — frontend atual.
     *       Convertido internamente para lista de 1 elemento.</li>
     * </ul>
     */
    public PaymentNoteItem toEntity(PaymentNoteItemDto dto) {
        if (dto == null) {
            return null;
        }

        // Resolve a lista de impostos (suporta formato legado e novo)
        List<Tax> taxes = taxMapper.resolveTaxesFromDto(
                dto.getTaxes(),
                dto.getTax(),
                dto.isManualAdjustment()
        );

        PaymentNoteItem item = new PaymentNoteItem();
        item.setId(dto.getId());
        item.setDescription(dto.getDescription());
        item.setValue(dto.getValue());

        // Adiciona cada grupo de imposto, configurando o relacionamento bidirecional
        for (Tax tax : taxes) {
            item.addTax(tax);
        }

        // Pré-popula o CNPJ para que o serviço faça o lookup completo.
        // O serviço resolverá a empresa real via empresaService.findyByCnpj().
        if (dto.getEmpresaBeneficiaria() != null && dto.getEmpresaBeneficiaria().getCnpj() != null) {
            Empresa beneficiaria = new Empresa();
            beneficiaria.setCnpj(dto.getEmpresaBeneficiaria().getCnpj());
            item.setEmpresaBeneficiaria(beneficiaria);
        }

        return item;
    }

    /**
     * Converte uma entidade {@link PaymentNoteItem} para {@link PaymentNoteItemDto}.
     *
     * <p>Popula tanto o campo novo ({@code taxes}) quanto o legado ({@code tax}),
     * para que o frontend antigo continue lendo {@code tax} normalmente.
     */
    public PaymentNoteItemDto toDto(PaymentNoteItem entity) {
        if (entity == null) {
            return null;
        }
        PaymentNoteItemDto dto = new PaymentNoteItemDto();
        dto.setId(entity.getId());
        dto.setDescription(entity.getDescription());
        dto.setValue(entity.getValue());

        // Campo novo — lista completa de grupos de imposto
        List<TaxDto> taxDtos = taxMapper.toDtoList(entity.getTaxes());
        dto.setTaxes(taxDtos);

        // Campo legado — primeiro grupo (para frontend antigo)
        dto.setTax(taxDtos.isEmpty() ? null : taxDtos.get(0));

        // manualAdjustment no nível do item — reflete o primeiro grupo (legado)
        dto.setManualAdjustment(
                entity.getTax() != null && entity.getTax().isManualTaxAdjustment()
        );

        dto.setEmpresaBeneficiaria(
                entity.getEmpresaBeneficiaria() != null
                        ? new EmpresaDto(entity.getEmpresaBeneficiaria())
                        : null
        );
        return dto;
    }
}
