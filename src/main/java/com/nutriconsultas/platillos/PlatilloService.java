package com.nutriconsultas.platillos;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

public interface PlatilloService {

	List<Platillo> findAll();

	Page<Platillo> findAll(@NonNull Pageable pageable);

	Page<Platillo> findBySearchTerm(@NonNull String searchTerm, @NonNull Pageable pageable);

	long count();

	long countBySearchTerm(@NonNull String searchTerm);

	Platillo findById(@NonNull Long id);

	Platillo save(@NonNull Platillo platillo);

	void deleteIngrediente(@NonNull Long id, @NonNull Long ingredienteId);

	Ingrediente addIngrediente(@NonNull Long id, @NonNull Long alimentoId, @NonNull String cantidad,
			@NonNull Integer peso);

	void savePicture(@NonNull Long id, @NonNull byte[] bytes, @NonNull String fileExtension);

	byte[] getPicture(@NonNull Long id, @NonNull String fileName) throws IOException;

	void savePdf(@NonNull Long id, byte[] bytes);

}
