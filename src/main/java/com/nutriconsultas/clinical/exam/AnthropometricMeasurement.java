package com.nutriconsultas.clinical.exam;

import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

import com.nutriconsultas.clinical.exam.anthropometric.BodyComposition;
import com.nutriconsultas.clinical.exam.anthropometric.BodyMass;
import com.nutriconsultas.clinical.exam.anthropometric.Bioimpedance;
import com.nutriconsultas.clinical.exam.anthropometric.Circumferences;
import com.nutriconsultas.clinical.exam.anthropometric.Diameters;
import com.nutriconsultas.clinical.exam.anthropometric.Skinfolds;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnthropometricMeasurement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "paciente_id")
	@NotNull(message = "El paciente es requerido")
	private Paciente paciente;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	@Temporal(TemporalType.TIMESTAMP)
	@NotNull(message = "La fecha y hora son requeridas")
	private Date measurementDateTime;

	@NotBlank(message = "El título es requerido")
	@Column(nullable = false, length = 200)
	private String title = "Medición Antropométrica";

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "body_mass_id")
	private BodyMass bodyMass;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "bioimpedance_id")
	private Bioimpedance bioimpedance;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "skinfolds_id")
	private Skinfolds skinfolds;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "circumferences_id")
	private Circumferences circumferences;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "diameters_id")
	private Diameters diameters;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "body_composition_id")
	private BodyComposition bodyComposition;

	// Convenience methods for backward compatibility
	public Double getPeso() {
		return bodyMass != null ? bodyMass.getWeight() : null;
	}

	public void setPeso(final Double peso) {
		if (bodyMass == null) {
			bodyMass = new BodyMass();
		}
		bodyMass.setWeight(peso);
	}

	public Double getEstatura() {
		return bodyMass != null ? bodyMass.getHeight() : null;
	}

	public void setEstatura(final Double estatura) {
		if (bodyMass == null) {
			bodyMass = new BodyMass();
		}
		bodyMass.setHeight(estatura);
	}

	public Double getImc() {
		return bodyMass != null ? bodyMass.getImc() : null;
	}

	public void setImc(final Double imc) {
		if (bodyMass == null) {
			bodyMass = new BodyMass();
		}
		bodyMass.setImc(imc);
	}

	public Double getIndiceGrasaCorporal() {
		return bodyComposition != null ? bodyComposition.getIndiceGrasaCorporal() : null;
	}

	public void setIndiceGrasaCorporal(final Double indiceGrasaCorporal) {
		if (bodyComposition == null) {
			bodyComposition = new BodyComposition();
		}
		bodyComposition.setIndiceGrasaCorporal(indiceGrasaCorporal);
	}

	public NivelPeso getNivelPeso() {
		return bodyMass != null ? bodyMass.getNivelPeso() : null;
	}

	public void setNivelPeso(final NivelPeso nivelPeso) {
		if (bodyMass == null) {
			bodyMass = new BodyMass();
		}
		bodyMass.setNivelPeso(nivelPeso);
	}

	public Double getCintura() {
		return circumferences != null ? circumferences.getWaistCircumference() : null;
	}

	public void setCintura(final Double cintura) {
		if (circumferences == null) {
			circumferences = new Circumferences();
		}
		circumferences.setWaistCircumference(cintura);
	}

	public Double getCadera() {
		return circumferences != null ? circumferences.getHipCircumference() : null;
	}

	public void setCadera(final Double cadera) {
		if (circumferences == null) {
			circumferences = new Circumferences();
		}
		circumferences.setHipCircumference(cadera);
	}

	public Double getCuello() {
		return circumferences != null ? circumferences.getNeckCircumference() : null;
	}

	public void setCuello(final Double cuello) {
		if (circumferences == null) {
			circumferences = new Circumferences();
		}
		circumferences.setNeckCircumference(cuello);
	}

	public Double getBrazo() {
		return circumferences != null ? circumferences.getMidUpperArmCircumferenceRelaxed() : null;
	}

	public void setBrazo(final Double brazo) {
		if (circumferences == null) {
			circumferences = new Circumferences();
		}
		circumferences.setMidUpperArmCircumferenceRelaxed(brazo);
	}

	public Double getMuslo() {
		return circumferences != null ? circumferences.getThighCircumference() : null;
	}

	public void setMuslo(final Double muslo) {
		if (circumferences == null) {
			circumferences = new Circumferences();
		}
		circumferences.setThighCircumference(muslo);
	}

	public Double getPorcentajeGrasaCorporal() {
		return bodyComposition != null ? bodyComposition.getPorcentajeGrasaCorporal() : null;
	}

	public void setPorcentajeGrasaCorporal(final Double porcentajeGrasaCorporal) {
		if (bodyComposition == null) {
			bodyComposition = new BodyComposition();
		}
		bodyComposition.setPorcentajeGrasaCorporal(porcentajeGrasaCorporal);
	}

	public Double getPorcentajeMasaMuscular() {
		return bodyComposition != null ? bodyComposition.getPorcentajeMasaMuscular() : null;
	}

	public void setPorcentajeMasaMuscular(final Double porcentajeMasaMuscular) {
		if (bodyComposition == null) {
			bodyComposition = new BodyComposition();
		}
		bodyComposition.setPorcentajeMasaMuscular(porcentajeMasaMuscular);
	}

}
