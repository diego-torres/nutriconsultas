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

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalExam {

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
	private Date examDateTime;

	@NotBlank(message = "El título es requerido")
	@Column(nullable = false, length = 200)
	private String title = "Examen Clínico";

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String summaryNotes;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "vital_signs_id")
	private VitalSigns vitalSigns;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "lipid_profile_id")
	private LipidProfile lipidProfile;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "blood_chemistry_id")
	private BloodChemistry bloodChemistry;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "liver_function_id")
	private LiverFunction liverFunction;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "complete_blood_count_id")
	private CompleteBloodCount completeBloodCount;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "other_tests_id")
	private OtherTests otherTests;

	// Convenience methods for backward compatibility
	public Double getPeso() {
		return vitalSigns != null ? vitalSigns.getPeso() : null;
	}

	public void setPeso(final Double peso) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setPeso(peso);
	}

	public Double getEstatura() {
		return vitalSigns != null ? vitalSigns.getEstatura() : null;
	}

	public void setEstatura(final Double estatura) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setEstatura(estatura);
	}

	public Double getImc() {
		return vitalSigns != null ? vitalSigns.getImc() : null;
	}

	public void setImc(final Double imc) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setImc(imc);
	}

	public Double getIndiceGrasaCorporal() {
		return vitalSigns != null ? vitalSigns.getIndiceGrasaCorporal() : null;
	}

	public void setIndiceGrasaCorporal(final Double indiceGrasaCorporal) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setIndiceGrasaCorporal(indiceGrasaCorporal);
	}

	public NivelPeso getNivelPeso() {
		return vitalSigns != null ? vitalSigns.getNivelPeso() : null;
	}

	public void setNivelPeso(final NivelPeso nivelPeso) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setNivelPeso(nivelPeso);
	}

	public Integer getSistolica() {
		return vitalSigns != null ? vitalSigns.getSistolica() : null;
	}

	public void setSistolica(final Integer sistolica) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setSistolica(sistolica);
	}

	public Integer getDiastolica() {
		return vitalSigns != null ? vitalSigns.getDiastolica() : null;
	}

	public void setDiastolica(final Integer diastolica) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setDiastolica(diastolica);
	}

	public Integer getPulso() {
		return vitalSigns != null ? vitalSigns.getPulso() : null;
	}

	public void setPulso(final Integer pulso) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setPulso(pulso);
	}

	public Integer getIndiceGlucemico() {
		return vitalSigns != null ? vitalSigns.getIndiceGlucemico() : null;
	}

	public void setIndiceGlucemico(final Integer indiceGlucemico) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setIndiceGlucemico(indiceGlucemico);
	}

	public Double getSpo2() {
		return vitalSigns != null ? vitalSigns.getSpo2() : null;
	}

	public void setSpo2(final Double spo2) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setSpo2(spo2);
	}

	public Double getTemperatura() {
		return vitalSigns != null ? vitalSigns.getTemperatura() : null;
	}

	public void setTemperatura(final Double temperatura) {
		if (vitalSigns == null) {
			vitalSigns = new VitalSigns();
		}
		vitalSigns.setTemperatura(temperatura);
	}

	public Double getHdl() {
		return lipidProfile != null ? lipidProfile.getHdl() : null;
	}

	public void setHdl(final Double hdl) {
		if (lipidProfile == null) {
			lipidProfile = new LipidProfile();
		}
		lipidProfile.setHdl(hdl);
	}

	public Double getLdl() {
		return lipidProfile != null ? lipidProfile.getLdl() : null;
	}

	public void setLdl(final Double ldl) {
		if (lipidProfile == null) {
			lipidProfile = new LipidProfile();
		}
		lipidProfile.setLdl(ldl);
	}

	public Double getTrigliceridos() {
		return lipidProfile != null ? lipidProfile.getTrigliceridos() : null;
	}

	public void setTrigliceridos(final Double trigliceridos) {
		if (lipidProfile == null) {
			lipidProfile = new LipidProfile();
		}
		lipidProfile.setTrigliceridos(trigliceridos);
	}

	public Double getColesterolTotal() {
		return lipidProfile != null ? lipidProfile.getColesterolTotal() : null;
	}

	public void setColesterolTotal(final Double colesterolTotal) {
		if (lipidProfile == null) {
			lipidProfile = new LipidProfile();
		}
		lipidProfile.setColesterolTotal(colesterolTotal);
	}

	public Double getGlucosa() {
		return bloodChemistry != null ? bloodChemistry.getGlucosa() : null;
	}

	public void setGlucosa(final Double glucosa) {
		if (bloodChemistry == null) {
			bloodChemistry = new BloodChemistry();
		}
		bloodChemistry.setGlucosa(glucosa);
	}

	public Double getHba1c() {
		return bloodChemistry != null ? bloodChemistry.getHba1c() : null;
	}

	public void setHba1c(final Double hba1c) {
		if (bloodChemistry == null) {
			bloodChemistry = new BloodChemistry();
		}
		bloodChemistry.setHba1c(hba1c);
	}

	public Double getCreatinina() {
		return bloodChemistry != null ? bloodChemistry.getCreatinina() : null;
	}

	public void setCreatinina(final Double creatinina) {
		if (bloodChemistry == null) {
			bloodChemistry = new BloodChemistry();
		}
		bloodChemistry.setCreatinina(creatinina);
	}

	public Double getUrea() {
		return bloodChemistry != null ? bloodChemistry.getUrea() : null;
	}

	public void setUrea(final Double urea) {
		if (bloodChemistry == null) {
			bloodChemistry = new BloodChemistry();
		}
		bloodChemistry.setUrea(urea);
	}

	public Double getBun() {
		return bloodChemistry != null ? bloodChemistry.getBun() : null;
	}

	public void setBun(final Double bun) {
		if (bloodChemistry == null) {
			bloodChemistry = new BloodChemistry();
		}
		bloodChemistry.setBun(bun);
	}

	public Double getAlt() {
		return liverFunction != null ? liverFunction.getAlt() : null;
	}

	public void setAlt(final Double alt) {
		if (liverFunction == null) {
			liverFunction = new LiverFunction();
		}
		liverFunction.setAlt(alt);
	}

	public Double getAst() {
		return liverFunction != null ? liverFunction.getAst() : null;
	}

	public void setAst(final Double ast) {
		if (liverFunction == null) {
			liverFunction = new LiverFunction();
		}
		liverFunction.setAst(ast);
	}

	public Double getBilirrubina() {
		return liverFunction != null ? liverFunction.getBilirrubina() : null;
	}

	public void setBilirrubina(final Double bilirrubina) {
		if (liverFunction == null) {
			liverFunction = new LiverFunction();
		}
		liverFunction.setBilirrubina(bilirrubina);
	}

	public Double getHemoglobina() {
		return completeBloodCount != null ? completeBloodCount.getHemoglobina() : null;
	}

	public void setHemoglobina(final Double hemoglobina) {
		if (completeBloodCount == null) {
			completeBloodCount = new CompleteBloodCount();
		}
		completeBloodCount.setHemoglobina(hemoglobina);
	}

	public Double getHematocrito() {
		return completeBloodCount != null ? completeBloodCount.getHematocrito() : null;
	}

	public void setHematocrito(final Double hematocrito) {
		if (completeBloodCount == null) {
			completeBloodCount = new CompleteBloodCount();
		}
		completeBloodCount.setHematocrito(hematocrito);
	}

	public Double getLeucocitos() {
		return completeBloodCount != null ? completeBloodCount.getLeucocitos() : null;
	}

	public void setLeucocitos(final Double leucocitos) {
		if (completeBloodCount == null) {
			completeBloodCount = new CompleteBloodCount();
		}
		completeBloodCount.setLeucocitos(leucocitos);
	}

	public Double getPlaquetas() {
		return completeBloodCount != null ? completeBloodCount.getPlaquetas() : null;
	}

	public void setPlaquetas(final Double plaquetas) {
		if (completeBloodCount == null) {
			completeBloodCount = new CompleteBloodCount();
		}
		completeBloodCount.setPlaquetas(plaquetas);
	}

	public Double getVitaminaD() {
		return otherTests != null ? otherTests.getVitaminaD() : null;
	}

	public void setVitaminaD(final Double vitaminaD) {
		if (otherTests == null) {
			otherTests = new OtherTests();
		}
		otherTests.setVitaminaD(vitaminaD);
	}

	public Double getVitaminaB12() {
		return otherTests != null ? otherTests.getVitaminaB12() : null;
	}

	public void setVitaminaB12(final Double vitaminaB12) {
		if (otherTests == null) {
			otherTests = new OtherTests();
		}
		otherTests.setVitaminaB12(vitaminaB12);
	}

	public Double getHierro() {
		return otherTests != null ? otherTests.getHierro() : null;
	}

	public void setHierro(final Double hierro) {
		if (otherTests == null) {
			otherTests = new OtherTests();
		}
		otherTests.setHierro(hierro);
	}

	public Double getFerritina() {
		return otherTests != null ? otherTests.getFerritina() : null;
	}

	public void setFerritina(final Double ferritina) {
		if (otherTests == null) {
			otherTests = new OtherTests();
		}
		otherTests.setFerritina(ferritina);
	}

}

