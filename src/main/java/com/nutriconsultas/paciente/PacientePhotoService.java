package com.nutriconsultas.paciente;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface PacientePhotoService {

	void savePhotoForNutritionist(@NonNull Long pacienteId, @NonNull String userId, @NonNull byte[] bytes,
			@NonNull String fileExtension);

	void savePhotoForPatient(@NonNull Long pacienteId, @NonNull byte[] bytes, @NonNull String fileExtension);

	void deletePhotoForNutritionist(@NonNull Long pacienteId, @NonNull String userId);

	void deletePhotoForPatient(@NonNull Long pacienteId);

	@Nullable
	byte[] getPhotoBytes(@NonNull Long pacienteId);

	@Nullable
	String getPhotoExtension(@NonNull Long pacienteId);

	void deletePhotoFromStorage(@NonNull Long pacienteId, @Nullable String extension);

}
