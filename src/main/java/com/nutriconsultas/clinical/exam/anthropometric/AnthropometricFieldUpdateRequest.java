package com.nutriconsultas.clinical.exam.anthropometric;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for correcting one anthropometric field (#242).
 */
public class AnthropometricFieldUpdateRequest {

	@NotBlank(message = "El campo es requerido")
	private String fieldKey;

	@NotNull(message = "El valor es requerido")
	private Double value;

	private String correctionNote;

	private boolean confirmDerivedRecalc;

	public String getFieldKey() {
		return fieldKey;
	}

	public void setFieldKey(final String fieldKey) {
		this.fieldKey = fieldKey;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(final Double value) {
		this.value = value;
	}

	public String getCorrectionNote() {
		return correctionNote;
	}

	public void setCorrectionNote(final String correctionNote) {
		this.correctionNote = correctionNote;
	}

	public boolean isConfirmDerivedRecalc() {
		return confirmDerivedRecalc;
	}

	public void setConfirmDerivedRecalc(final boolean confirmDerivedRecalc) {
		this.confirmDerivedRecalc = confirmDerivedRecalc;
	}

}
