package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PacientePhotoResponsesTest {

	@Mock
	private PacientePhotoService pacientePhotoService;

	@Test
	void buildPictureResponse_redirectsWhenNoCustomPhoto() {
		final Paciente paciente = new Paciente();
		paciente.setGender("F");

		final ResponseEntity<byte[]> response = PacientePhotoResponses.buildPictureResponse(paciente,
				pacientePhotoService);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(response.getHeaders().getLocation()).hasToString(PacienteAvatarCatalog.resolveImagePath(paciente));
	}

	@Test
	void buildPictureResponse_returnsBytesWhenCustomPhotoExists() {
		final Paciente paciente = new Paciente();
		paciente.setId(3L);
		paciente.setPhotoExtension("png");
		final byte[] bytes = new byte[] { 1, 2, 3 };
		when(pacientePhotoService.getPhotoBytes(3L)).thenReturn(bytes);

		final ResponseEntity<byte[]> response = PacientePhotoResponses.buildPictureResponse(paciente,
				pacientePhotoService);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(bytes);
		assertThat(response.getHeaders().getContentType()).isEqualTo(PacientePictureSupport.resolveMediaType("png"));
	}

}
