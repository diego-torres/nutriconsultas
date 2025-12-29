package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nutriconsultas.model.AbstractMacroNutrible;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Dieta extends AbstractMacroNutrible {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	@OneToMany(mappedBy = "dieta", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true,
			targetEntity = Ingesta.class, fetch = jakarta.persistence.FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@JsonManagedReference
	private List<Ingesta> ingestas = new ArrayList<>();

}
