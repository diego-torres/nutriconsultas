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
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.paciente.calculation.PhysiologicalStressType;
import com.nutriconsultas.paciente.calculation.StressFormulaTable;
import com.nutriconsultas.paciente.calculation.StressIncrementMode;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

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

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private PhysicalActivityLevel physicalActivityLevel;

	@Column(precision = 4)
	private Double activityFactor;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private BmrFormulaType bmrFormula;

	@Column(precision = 7)
	private Double bmrUsed;

	@Column(precision = 7)
	private Double getKcal;

	@Column(precision = 7)
	private Double tefKcal;

	@Column(precision = 7)
	private Double totalAdjustedKcal;

	@Column(precision = 7)
	private Double stressKcal;

	@Column(precision = 7)
	private Double finalTotalKcal;

	private Boolean physiologicalStressActive = false;

	@Enumerated(EnumType.STRING)
	@Column(length = 40)
	private PhysiologicalStressType physiologicalStressType;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private StressFormulaTable stressFormulaTable;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private StressIncrementMode stressIncrementMode;

	@Column(precision = 5)
	private Double stressFactorValue;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Temporal(TemporalType.DATE)
	private Date stressValidFrom;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Temporal(TemporalType.DATE)
	private Date stressValidUntil;

	@Column(precision = 4)
	private Double stressFeverTemperature;

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

	public Double getEndomorphy() {
		return bodyComposition != null ? bodyComposition.getEndomorphy() : null;
	}

	public void setEndomorphy(final Double endomorphy) {
		if (bodyComposition == null) {
			bodyComposition = new BodyComposition();
		}
		bodyComposition.setEndomorphy(endomorphy);
	}

	public Double getMesomorphy() {
		return bodyComposition != null ? bodyComposition.getMesomorphy() : null;
	}

	public void setMesomorphy(final Double mesomorphy) {
		if (bodyComposition == null) {
			bodyComposition = new BodyComposition();
		}
		bodyComposition.setMesomorphy(mesomorphy);
	}

	public Double getEctomorphy() {
		return bodyComposition != null ? bodyComposition.getEctomorphy() : null;
	}

	public void setEctomorphy(final Double ectomorphy) {
		if (bodyComposition == null) {
			bodyComposition = new BodyComposition();
		}
		bodyComposition.setEctomorphy(ectomorphy);
	}

	public Double getSomatocartaX() {
		return bodyComposition != null ? bodyComposition.getSomatocartaX() : null;
	}

	public void setSomatocartaX(final Double somatocartaX) {
		if (bodyComposition == null) {
			bodyComposition = new BodyComposition();
		}
		bodyComposition.setSomatocartaX(somatocartaX);
	}

	public Double getSomatocartaY() {
		return bodyComposition != null ? bodyComposition.getSomatocartaY() : null;
	}

	public void setSomatocartaY(final Double somatocartaY) {
		if (bodyComposition == null) {
			bodyComposition = new BodyComposition();
		}
		bodyComposition.setSomatocartaY(somatocartaY);
	}

}
