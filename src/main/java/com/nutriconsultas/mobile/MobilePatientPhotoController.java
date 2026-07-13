package com.nutriconsultas.mobile;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacientePhotoResponses;
import com.nutriconsultas.paciente.PacientePhotoService;
import com.nutriconsultas.paciente.PacientePictureSupport;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.util.LogRedaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/profile/photo")
@Tag(name = "Mobile", description = "Patient mobile API")
@Slf4j
public class MobilePatientPhotoController extends AbstractMobilePatientController {

	private final PacientePhotoService pacientePhotoService;

	private final PacienteRepository pacienteRepository;

	public MobilePatientPhotoController(final PatientAuthService patientAuthService,
			final PacientePhotoService pacientePhotoService, final PacienteRepository pacienteRepository) {
		super(patientAuthService);
		this.pacientePhotoService = pacientePhotoService;
		this.pacienteRepository = pacienteRepository;
	}

	@GetMapping
	@Operation(summary = "Get patient profile photo",
			description = "Returns custom photo bytes or redirects to catalog avatar when no custom photo is set.")
	@MobileOpenApiResponses.AuthenticatedPatient
	public ResponseEntity<byte[]> getPhoto(@AuthenticationPrincipal final Jwt jwt) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		final Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		return PacientePhotoResponses.buildPictureResponse(paciente, pacientePhotoService);
	}

	@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload patient profile photo", description = "Stores a custom profile photo in S3.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.WriteEndpoint
	public ApiResponse<MobilePatientPhotoResponse> uploadPhoto(@AuthenticationPrincipal final Jwt jwt,
			@RequestParam("photoFile") final MultipartFile photoFile) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile profile photo upload for patient {}", LogRedaction.redactPaciente(pacienteId));
		}
		final byte[] bytes = readBytes(photoFile);
		final String extension = extractExtension(photoFile);
		pacientePhotoService.savePhotoForPatient(pacienteId, bytes, extension);
		final Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		return ApiResponse.ok(new MobilePatientPhotoResponse(
				PacientePictureSupport.resolveDisplayUrlForMobile(paciente), paciente.getPhotoExtension()));
	}

	private byte[] readBytes(final MultipartFile photoFile) {
		if (photoFile == null || photoFile.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
		}
		try {
			return photoFile.getBytes();
		}
		catch (final IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen");
		}
	}

	private String extractExtension(final MultipartFile photoFile) {
		final String originalName = photoFile.getOriginalFilename();
		if (originalName == null) {
			return "png";
		}
		final int dotIndex = originalName.lastIndexOf('.');
		if (dotIndex <= 0 || dotIndex >= originalName.length() - 1) {
			return "png";
		}
		return originalName.substring(dotIndex + 1);
	}

	record MobilePatientPhotoResponse(String photoUrl, String photoExtension) {
	}

}
