package com.nutriconsultas.platillos;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nutriconsultas.model.AbstractNutrible;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false)
public class Platillo extends AbstractNutrible {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String description;

	private String imageUrl;

	private String videoUrl;

	private String pdfUrl;

	private String ingestasSugeridas;

	@OneToMany(mappedBy = "platillo", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = Ingrediente.class,
			fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@JsonManagedReference
	private List<Ingrediente> ingredientes = new ArrayList<>();

}
