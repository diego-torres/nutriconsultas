package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nutriconsultas.model.AbstractMacroNutrible;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Ingesta extends AbstractMacroNutrible {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	public Ingesta(String nombre) {
		this.nombre = nombre;
	}

	@ManyToOne
	@JoinColumn(name = "dieta_id")
	@JsonBackReference
	private Dieta dieta;

	@OneToMany(mappedBy = "ingesta", //
			cascade = CascadeType.ALL, //
			orphanRemoval = true, //
			targetEntity = PlatilloIngesta.class, //
			fetch = FetchType.LAZY)
	private List<PlatilloIngesta> platillos = new ArrayList<>();

	@OneToMany(mappedBy = "ingesta", //
			cascade = CascadeType.ALL, //
			orphanRemoval = true, //
			targetEntity = AlimentoIngesta.class, //
			fetch = FetchType.LAZY)
	private List<AlimentoIngesta> alimentos = new ArrayList<>();

}
