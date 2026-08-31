package com.bese.tesouraria.entity;

import com.bese.tesouraria.converter.TaxRuleItemListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "tax_rule", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tax_rule_cod_efd_receita_inicio",
        columnNames = {"cod_efd", "codigo_receita", "data_inicio_vigencia"})
})
@Getter
@Setter
public class TaxRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_efd", nullable = false)
    private Integer codEfd;

    /**
     * Novo campo para agrupar códigos EFD.
     */
    @Column(name = "codigo_receita", nullable = false)
    private Integer codigoReceita;

    /**
     * Descrição legível da regra, agora com até 300 caracteres.
     */
    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "data_inicio_vigencia", nullable = false)
    private LocalDate dataInicioVigencia;

    @Column(name = "data_fim_vigencia")
    private LocalDate dataFimVigencia;

    @Convert(converter = TaxRuleItemListConverter.class)
    @Column(name = "items", nullable = false, columnDefinition = "JSON")
    private List<TaxRuleItem> items;

    public TaxRule() {}

    public TaxRule(Integer codEfd, Integer codigoReceita, String description, List<TaxRuleItem> items, LocalDate dataInicioVigencia) {
        this.codEfd = codEfd;
        this.codigoReceita = codigoReceita;
        this.description = description;
        this.items = items;
        this.dataInicioVigencia = dataInicioVigencia;
    }
}