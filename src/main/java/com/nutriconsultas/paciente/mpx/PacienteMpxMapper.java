package com.nutriconsultas.paciente.mpx;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.calculation.ActivityFactorScale;
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.paciente.calculation.PhysiologicalStressType;
import com.nutriconsultas.paciente.calculation.StressFormulaTable;
import com.nutriconsultas.paciente.calculation.StressIncrementMode;
import com.nutriconsultas.paciente.calculation.TefBase;
import com.nutriconsultas.paciente.calculation.TefMethod;
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

	public static Paciente toPaciente(final MpxDocument document, final String userId) {
		if (document == null || document.getPatient() == null) {
			throw new MpxImportException("El archivo MPX no contiene datos de paciente");
		}
		if (document.getMpxVersion() != MpxConstants.MPX_VERSION) {
			throw new MpxImportException("Versión MPX no compatible. Solo se admite la versión 1.");
		}
		if (document.getSourceApp() != null && !MpxConstants.SOURCE_APP.equals(document.getSourceApp())) {
			throw new MpxImportException("El archivo MPX no proviene de Minutriporción");
		}
		final Paciente paciente = fromPatientRegistration(document.getPatient());
		paciente.setUserId(userId);
		paciente.setStatus(PacienteStatus.ACTIVE);
		paciente.setRegistro(new Date());
		return paciente;
	}

	private static Paciente fromPatientRegistration(final MpxPatientRegistration registration) {
		final Paciente paciente = new Paciente();
		paciente.setName(registration.getName());
		paciente.setDob(parseDate(registration.getDob(), "fecha de nacimiento"));
		paciente.setEmail(registration.getEmail());
		paciente.setPhone(registration.getPhone());
		paciente.setGender(registration.getGender());
		paciente.setResponsibleName(registration.getResponsibleName());
		paciente.setParentesco(registration.getParentesco());
		paciente.setPregnancy(registration.getPregnancy() != null ? registration.getPregnancy() : false);
		applyBodySnapshot(paciente, registration.getBodySnapshot());
		applyEnergyPreferences(paciente, registration.getEnergyPreferences());
		applyMedicalHistory(paciente, registration.getMedicalHistory());
		return paciente;
	}

	private static void applyBodySnapshot(final Paciente paciente, final MpxBodySnapshot snapshot) {
		if (snapshot == null) {
			return;
		}
		paciente.setPeso(snapshot.getPeso());
		paciente.setEstatura(snapshot.getEstatura());
		paciente.setImc(snapshot.getImc());
		paciente.setBmr(snapshot.getBmr());
		paciente.setGetKcal(snapshot.getGetKcal());
		paciente.setNivelPeso(parseEnum(snapshot.getNivelPeso(), NivelPeso.class, "nivel de peso"));
		paciente.setTefKcal(snapshot.getTefKcal());
		paciente.setTotalAdjustedKcal(snapshot.getTotalAdjustedKcal());
		paciente.setStressKcal(snapshot.getStressKcal());
		paciente.setFinalTotalKcal(snapshot.getFinalTotalKcal());
	}

	private static void applyEnergyPreferences(final Paciente paciente, final MpxEnergyPreferences energy) {
		if (energy == null) {
			return;
		}
		final PacienteEnergyPreferences preferences = paciente.getEnergyPreferences();
		preferences.setActivityFactorScale(
				parseEnum(energy.getActivityFactorScale(), ActivityFactorScale.class, "escala de factor de actividad"));
		preferences.setPreferredBmrFormula(
				parseEnum(energy.getPreferredBmrFormula(), BmrFormulaType.class, "fórmula de TMB preferida"));
		preferences.setPhysicalActivityLevel(
				parseEnum(energy.getPhysicalActivityLevel(), PhysicalActivityLevel.class, "nivel de actividad física"));
		preferences.setActivityFactor(energy.getActivityFactor());
		preferences.setCustomFactorSedentary(energy.getCustomFactorSedentary());
		preferences.setCustomFactorLight(energy.getCustomFactorLight());
		preferences.setCustomFactorModerate(energy.getCustomFactorModerate());
		preferences.setCustomFactorIntense(energy.getCustomFactorIntense());
		preferences.setCustomFactorVeryIntense(energy.getCustomFactorVeryIntense());
		preferences.setPhysiologicalStressActive(energy.getPhysiologicalStressActive());
		preferences.setPhysiologicalStressType(parseEnum(energy.getPhysiologicalStressType(),
				PhysiologicalStressType.class, "tipo de estrés fisiológico"));
		preferences.setStressFormulaTable(
				parseEnum(energy.getStressFormulaTable(), StressFormulaTable.class, "tabla de estrés"));
		preferences.setStressIncrementMode(
				parseEnum(energy.getStressIncrementMode(), StressIncrementMode.class, "modo de incremento de estrés"));
		preferences.setStressFactorValue(energy.getStressFactorValue());
		preferences.setStressValidFrom(parseDate(energy.getStressValidFrom(), "inicio de vigencia de estrés"));
		preferences.setStressValidUntil(parseDate(energy.getStressValidUntil(), "fin de vigencia de estrés"));
		preferences.setStressFeverTemperature(energy.getStressFeverTemperature());
		preferences.setTefMethod(parseEnum(energy.getTefMethod(), TefMethod.class, "método de TEF"));
		preferences.setTefBase(parseEnum(energy.getTefBase(), TefBase.class, "base de TEF"));
		preferences.setTefFixedPercent(energy.getTefFixedPercent());
		preferences.setTefMacroProteinPercent(energy.getTefMacroProteinPercent());
		preferences.setTefMacroCarbsPercent(energy.getTefMacroCarbsPercent());
		preferences.setTefMacroFatPercent(energy.getTefMacroFatPercent());
	}

	private static void applyMedicalHistory(final Paciente paciente, final MpxMedicalHistory history) {
		if (history == null) {
			return;
		}
		final PacienteMedicalHistory medical = paciente.getMedicalHistory();
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
	}

	private static Date parseDate(final String value, final String fieldLabel) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			final LocalDate localDate = LocalDate.parse(value, ISO_DATE);
			return Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
		}
		catch (final DateTimeParseException ex) {
			throw new MpxImportException("Fecha inválida en " + fieldLabel + ": " + value, ex);
		}
	}

	private static <E extends Enum<E>> E parseEnum(final String value, final Class<E> enumType,
			final String fieldLabel) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Enum.valueOf(enumType, value);
		}
		catch (final IllegalArgumentException ex) {
			throw new MpxImportException("Valor inválido en " + fieldLabel + ": " + value, ex);
		}
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
		return ISO_DATE.format(toLocalDate(date));
	}

	private static LocalDate toLocalDate(final Date date) {
		if (date instanceof java.sql.Date sqlDate) {
			return sqlDate.toLocalDate();
		}
		return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
	}

	private static String formatEnum(final Enum<?> value) {
		return value != null ? value.name() : null;
	}

}
