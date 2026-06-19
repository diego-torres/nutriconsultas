package com.nutriconsultas.paciente.mpx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

/**
 * Imports patient registration profiles from MPX v1 YAML files (#222).
 */
@Service
@Slf4j
public class PacienteMpxImportService {

	private final PacienteRepository pacienteRepository;

	private final PacienteService pacienteService;

	private final Validator validator;

	public PacienteMpxImportService(final PacienteRepository pacienteRepository, final PacienteService pacienteService,
			final Validator validator) {
		this.pacienteRepository = pacienteRepository;
		this.pacienteService = pacienteService;
		this.validator = validator;
	}

	@Transactional
	public MpxImportResult importRegistration(@NonNull final MultipartFile file, @NonNull final String userId) {
		validateFile(file);
		final MpxDocument document = parseDocument(readContent(file));
		final Paciente paciente = PacienteMpxMapper.toPaciente(document, userId);
		validatePaciente(paciente);
		final boolean duplicateWarning = detectDuplicateWarning(userId, paciente);
		final Paciente saved = pacienteService.save(paciente);
		log.info("Imported MPX registration as new paciente id {}", saved.getId());
		return new MpxImportResult(saved.getId(), duplicateWarning);
	}

	private void validateFile(final MultipartFile file) {
		final String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".mpx")) {
			throw new MpxImportException("El archivo debe tener extensión .mpx");
		}
		if (file.isEmpty()) {
			throw new MpxImportException("El archivo está vacío");
		}
	}

	private byte[] readContent(final MultipartFile file) {
		try {
			return file.getBytes();
		}
		catch (final IOException ex) {
			throw new MpxImportException("No se pudo leer el archivo MPX", ex);
		}
	}

	private MpxDocument parseDocument(final byte[] content) {
		try {
			final Constructor constructor = new Constructor(MpxDocument.class, new LoaderOptions());
			final Yaml yaml = new Yaml(constructor);
			final Object loaded = yaml.load(new String(content, StandardCharsets.UTF_8));
			if (!(loaded instanceof MpxDocument document)) {
				throw new MpxImportException("El archivo no es un MPX válido");
			}
			return document;
		}
		catch (final YAMLException ex) {
			throw new MpxImportException("El archivo no es un MPX válido", ex);
		}
	}

	private void validatePaciente(final Paciente paciente) {
		final String validationMessage = validator.validate(paciente)
			.stream()
			.map(ConstraintViolation::getMessage)
			.collect(Collectors.joining("; "));
		if (!validationMessage.isBlank()) {
			throw new MpxImportException("Datos de paciente inválidos: " + validationMessage);
		}
	}

	private boolean detectDuplicateWarning(final String userId, final Paciente paciente) {
		if (paciente.getName() == null || paciente.getDob() == null) {
			return false;
		}
		return pacienteRepository.existsByUserIdAndNameIgnoreCaseAndDob(userId, paciente.getName(), paciente.getDob());
	}

}
