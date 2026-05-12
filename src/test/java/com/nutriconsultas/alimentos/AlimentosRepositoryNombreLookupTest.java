package com.nutriconsultas.alimentos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Smoke test for deterministic SMAE nome resolution used by platillo seeding (#84).
 */
@DataJpaTest
class AlimentosRepositoryNombreLookupTest {

	@Autowired
	private AlimentosRepository alimentosRepository;

	@Test
	void findFirstByNombreAlimentoIgnoreCaseOrderByIdAsc_findsInsertedRow() {
		final Alimento first = buildMinimalAlimento("Lookup Unique Alpha Case");
		final Alimento second = buildMinimalAlimento("Lookup Unique Beta Case");

		alimentosRepository.save(first);
		alimentosRepository.save(second);
		alimentosRepository.flush();

		assertThat(alimentosRepository.findFirstByNombreAlimentoIgnoreCaseOrderByIdAsc("lookup UNIQUE alpha CASE"))
			.map(Alimento::getNombreAlimento)
			.contains("Lookup Unique Alpha Case");
		assertThat(alimentosRepository.findFirstByNombreAlimentoIgnoreCaseOrderByIdAsc("Lookup Unique Beta Case"))
			.map(Alimento::getNombreAlimento)
			.contains("Lookup Unique Beta Case");
	}

	private static Alimento buildMinimalAlimento(final String nombre) {
		final Alimento alimento = new Alimento();
		alimento.setNombreAlimento(nombre);
		alimento.setClasificacion("TEST");
		alimento.setUnidad("g");
		alimento.setCantSugerida(1.0);
		return alimento;
	}

}
