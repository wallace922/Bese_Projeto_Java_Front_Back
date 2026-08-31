package com.bese.tesouraria.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class FinancialPlanning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @Digits(integer = 6, fraction = 0)
    private Integer numero;

    @Column(name = "DataDeliquidação")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate data;

    @Digits(integer = 3, fraction = 0)
    private Integer vinculation;

    @Digits(integer = 6, fraction = 0)
    private Integer origin;

    public FinancialPlanning() {
    }

    public FinancialPlanning(LocalDate data, Integer vinculation, Integer origin, Integer numero) {
        this.data = data;
        this.vinculation = vinculation;
        this.origin = origin;
        this.numero = numero;
    }

}