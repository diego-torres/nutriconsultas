package com.nutriconsultas.paciente.mpx;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.embeddable.PacienteBodySnapshot;
import com.nutriconsultas.paciente.satellite.PacienteEnergyPreferences;
import com.nutriconsultas.paciente.satellite.PacienteMedicalHistory;

/**
 * Maps owned {@link Paciente} entities to MPX v1 DTOs (#221).
 */
public final class PacienteMpxMapper {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

	private PacienteMpxMapper() {
	}

	public static MpxDocument toDocument(final Paciente paciente, final Instant exportedAt) {
		final MpxDocument document = new MpxDocument();
		document.setMpxVersion(MpxConstants.MPX_VERSION);
		document.setExportedAt(ISO_INSTANT.format(exportedAt.atOffset(ZoneOffset.UTC)));
		document.setSourceApp(MpxConstants.SOURCE_APP);
		document.setPatient(toPatientRegistration(paciente));
		return document;
	}

	private static MpxPatientRegistration toPatientRegistration(final Paciente paciente) {
		final MpxPatientRegistration registration = new MpxPatientRegistration();
		registration.setName(paciente.getName());
		registration.setDob(formatDate(paciente.getDob()));
		registration.setEmail(paciente.getEmail());
		registration.setPhone(paciente.getPhone());
		registration.setGender(paciente.getGender());
		registration.setResponsibleName(paciente.getResponsibleName());
		registration.setParentesco(paciente.getParentesco());
		registration.setPregnancy(paciente.getPregnancy());
		registration.setBodySnapshot(toBodySnapshot(paciente.getBodySnapshot()));
		registration.setEnergyPreferences(toEnergyPreferences(paciente.getEnergyPreferences()));
		registration.setMedicalHistory(toMedicalHistory(paciente.getMedicalHistory()));
		return registration;
	}

	private static MpxBodySnapshot toBodySnapshot(final PacienteBodySnapshot snapshot) {
		if (snapshot == null) {
			return null;
		}
		final MpxBodySnapshot body = new MpxBodySnapshot();
		body.setPeso(snapshot.getPeso());
		body.setEstatura(snapshot.getEstatura());
		body.setImc(snapshot.getImc());
		body.setBmr(snapshot.getBmr());
		body.setGetKcal(snapshot.getGetKcal());
		body.setNivelPeso(formatEnum(snapshot.getNivelPeso()));
		body.setTefKcal(snapshot.getTefKcal());
		body.setTotalAdjustedKcal(snapshot.getTotalAdjustedKcal());
		body.setStressKcal(snapshot.getStressKcal());
		body.setFinalTotalKcal(snapshot.getFinalTotalKcal());
		return body;
	}

	private static MpxEnergyPreferences toEnergyPreferences(final PacienteEnergyPreferences preferences) {
		if (preferences == null) {
			return null;
		}
		final MpxEnergyPreferences energy = new MpxEnergyPreferences();
		energy.setActivityFactorScale(formatEnum(preferences.getActivityFactorScale()));
		energy.setPreferredBmrFormula(formatEnum(preferences.getPreferredBmrFormula()));
		energy.setPhysicalActivityLevel(formatEnum(preferences.getPhysicalActivityLevel()));
		energy.setActivityFactor(preferences.getActivityFactor());
		energy.setCustomFactorSedentary(preferences.getCustomFactorSedentary());
		energy.setCustomFactorLight(preferences.getCustomFactorLight());
		energy.setCustomFactorModerate(preferences.getCustomFactorModerate());
		energy.setCustomFactorIntense(preferences.getCustomFactorIntense());
		energy.setCustomFactorVeryIntense(preferences.getCustomFactorVeryIntense());
		energy.setPhysiologicalStressActive(preferences.getPhysiologicalStressActive());
		energy.setPhysiologicalStressType(formatEnum(preferences.getPhysiologicalStressType()));
		energy.setStressFormulaTable(formatEnum(preferences.getStressFormulaTable()));
		energy.setStressIncrementMode(formatEnum(preferences.getStressIncrementMode()));
		energy.setStressFactorValue(preferences.getStressFactorValue());
		energy.setStressValidFrom(formatDate(preferences.getStressValidFrom()));
		energy.setStressValidUntil(formatDate(preferences.getStressValidUntil()));
		energy.setStressFeverTemperature(preferences.getStressFeverTemperature());
		energy.setTefMethod(formatEnum(preferences.getTefMethod()));
		energy.setTefBase(formatEnum(preferences.getTefBase()));
		energy.setTefFixedPercent(preferences.getTefFixedPercent());
		energy.setTefMacroProteinPercent(preferences.getTefMacroProteinPercent());
		energy.setTefMacroCarbsPercent(preferences.getTefMacroCarbsPercent());
		energy.setTefMacroFatPercent(preferences.getTefMacroFatPercent());
		return energy;
	}

	private static MpxMedicalHistory toMedicalHistory(final PacienteMedicalHistory history) {
		if (history == null) {
			return null;
		}
		final MpxMedicalHistory medical = new MpxMedicalHistory();
		medical.setAntecedentesPrenatales(history.getAntecedentesPrenatales());
		medical.setAntecedentesNatales(history.getAntecedentesNatales());
		medical.setAntecedentesPatologicosPersonales(history.getAntecedentesPatologicosPersonales());
		medical.setAntecedentesPatologicosFamiliares(history.getAntecedentesPatologicosFamiliares());
		medical.setComplicaciones(history.getComplicaciones());
		medical.setTipoSanguineo(history.getTipoSanguineo());
		medical.setHistorialAlimenticio(history.getHistorialAlimenticio());
		medical.setDesarrolloPsicomotor(history.getDesarrolloPsicomotor());
		medical.setAlergias(history.getAlergias());
		medical.setHipertension(history.getHipertension());
		medical.setDiabetes(history.getDiabetes());
		medical.setHipotiroidismo(history.getHipotiroidismo());
		medical.setObesidad(history.getObesidad());
		medical.setAnemia(history.getAnemia());
		medical.setBulimia(history.getBulimia());
		medical.setAnorexia(history.getAnorexia());
		medical.setEnfermedadesHepaticas(history.getEnfermedadesHepaticas());
		return medical;
	}

	private static String formatDate(final Date date) {
		if (date == null) {
			return null;
		}
		return ISO_DATE.format(date.toInstant().atZone(ZoneOffset.UTC).toLocalDate());
	}

	private static String formatEnum(final Enum<?> value) {
		return value != null ? value.name() : null;
	}

}
