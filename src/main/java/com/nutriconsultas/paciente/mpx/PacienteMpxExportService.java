package com.nutriconsultas.paciente.mpx;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Builds MPX v1 YAML exports for patient registration profiles (#221).
 */
@Service
@Slf4j
public class PacienteMpxExportService {

	private static final DateTimeFormatter FILENAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
		.withZone(ZoneOffset.UTC);

	private final PacienteRepository pacienteRepository;

	private final Clock clock;

	public PacienteMpxExportService(final PacienteRepository pacienteRepository,
			@Autowired(required = false) final Clock clock) {
		this.pacienteRepository = pacienteRepository;
		this.clock = clock != null ? clock : Clock.systemUTC();
	}

	@Transactional(readOnly = true)
	public MpxExportResult exportRegistration(@NonNull final Long pacienteId, @NonNull final String userId) {
		final Paciente paciente = pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
		initializeSatellites(paciente);
		final Instant exportedAt = clock.instant();
		final MpxDocument document = PacienteMpxMapper.toDocument(paciente, exportedAt);
		final byte[] content = serialize(document);
		final String filename = buildFilename(paciente, exportedAt);
		log.info("Exported MPX registration for paciente id {}", pacienteId);
		return new MpxExportResult(content, filename);
	}

	private void initializeSatellites(final Paciente paciente) {
		paciente.getBodySnapshot();
		paciente.getEnergyPreferences();
		paciente.getMedicalHistory();
	}

	private byte[] serialize(final MpxDocument document) {
		final DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		final Representer representer = new Representer(options);
		representer.addClassTag(MpxDocument.class, Tag.MAP);
		representer.addClassTag(MpxPatientRegistration.class, Tag.MAP);
		representer.addClassTag(MpxBodySnapshot.class, Tag.MAP);
		representer.addClassTag(MpxEnergyPreferences.class, Tag.MAP);
		representer.addClassTag(MpxMedicalHistory.class, Tag.MAP);
		final Yaml yaml = new Yaml(representer, options);
		return yaml.dump(document).getBytes(StandardCharsets.UTF_8);
	}

	private String buildFilename(final Paciente paciente, final Instant exportedAt) {
		final String slugSource = paciente.getAssignedId() != null && !paciente.getAssignedId().isBlank()
				? paciente.getAssignedId() : paciente.getName();
		final String slug = sanitizeFilename(slugSource);
		final String timestamp = FILENAME_TIMESTAMP.format(exportedAt);
		return slug + "-" + timestamp + ".mpx";
	}

	private String sanitizeFilename(final String value) {
		if (value == null || value.isBlank()) {
			return "paciente";
		}
		final String sanitized = value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
		if (sanitized.isBlank()) {
			return "paciente";
		}
		return sanitized.length() > 60 ? sanitized.substring(0, 60) : sanitized;
	}

}
