package com.bese.tesouraria.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Empenho {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "Empenho")
	private Integer numero;

	@Column(name = "ano")
	private Integer ano;

	@Column(name = "FontDeOrigin")
	private Long fontDeOrigin;

	@Column(name = "InternalPlan", length = 11)
	private String internalPlan;

	@Column(name = "Nature")
	private Integer nature;

	public Empenho() {
	}

	public Empenho(Long id, Integer Numero, Integer ano, String internalPlan, Integer nature, Long fontDeOrigin) {
		this.id = id;
		this.numero = Numero;
		this.ano = ano;
		this.internalPlan = internalPlan;
		this.nature = nature;
		this.fontDeOrigin = fontDeOrigin;
	}

	public Empenho(Integer ano, String internalPlan, Integer nature, Integer numero, Long fontDeOrigin) {
		this.ano = ano;
		this.internalPlan = internalPlan;
		this.nature = nature;
		this.numero = numero;
		this.fontDeOrigin = fontDeOrigin;
	}

	@PrePersist
	@PreUpdate
	private void toUpperCaseBeforeSave() {
		if (this.internalPlan != null) {
			this.internalPlan = this.internalPlan.toUpperCase();
		}
	}
}