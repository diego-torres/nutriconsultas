package com.nutriconsultas.alimentos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

/**
 * Ensures catalog search JPQL works without optional null string filters (PostgreSQL
 * {@code lower(bytea)} regression for Mina food search).
 */
@DataJpaTest
class AlimentosRepositoryCatalogSearchTest {

	@Autowired
	private AlimentosRepository alimentosRepository;

	@Test
	void searchWithoutClasificacionFilterUsesSearchTermQueries() {
		alimentosRepository.save(buildMinimalAlimento("Avena cocida", "Cereales"));
		alimentosRepository.save(buildMinimalAlimento("Leche descremada", "Lácteos"));
		alimentosRepository.flush();

		assertThat(alimentosRepository.countBySearchTerm("%avena%")).isEqualTo(1L);
		assertThat(alimentosRepository.findBySearchTerm("%avena%", PageRequest.of(0, 10)).getContent())
			.extracting(Alimento::getNombreAlimento)
			.containsExactly("Avena cocida");
	}

	@Test
	void searchWithClasificacionFilterRequiresNonNullPattern() {
		alimentosRepository.save(buildMinimalAlimento("Avena cocida", "Cereales"));
		alimentosRepository.save(buildMinimalAlimento("Avena con leche", "Lácteos"));
		alimentosRepository.flush();

		assertThat(alimentosRepository.countForCatalogSearch("%avena%", "%cereales%")).isEqualTo(1L);
		assertThat(
				alimentosRepository.findForCatalogSearch("%avena%", "%cereales%", PageRequest.of(0, 10)).getContent())
			.extracting(Alimento::getNombreAlimento)
			.containsExactly("Avena cocida");
	}

	private static Alimento buildMinimalAlimento(final String nombre, final String clasificacion) {
		final Alimento alimento = new Alimento();
		alimento.setNombreAlimento(nombre);
		alimento.setClasificacion(clasificacion);
		alimento.setUnidad("g");
		alimento.setCantSugerida(1.0);
		return alimento;
	}

}
