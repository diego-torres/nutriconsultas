package com.nutriconsultas.paciente.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.paciente.Paciente;

class PhysiologicalStressCatalogTest {

	@Test
	void defaultLongMultiplierForMajorSurgery() {
		assertThat(PhysiologicalStressCatalog.defaultMultiplier(PhysiologicalStressType.MAJOR_SURGERY,
				StressFormulaTable.LONG))
			.isEqualTo(1.20);
	}

	@Test
	void defaultAspenMultiplierForBurns() {
		assertThat(
				PhysiologicalStressCatalog.defaultMultiplier(PhysiologicalStressType.BURNS, StressFormulaTable.ASPEN))
			.isEqualTo(1.80);
	}

	@Test
	void suggestFromPathologiesUsesPatientFlags() {
		final Paciente paciente = new Paciente();
		paciente.setDiabetes(true);
		paciente.setEnfermedadesHepaticas(true);
		paciente.setPregnancy(true);

		final List<PhysiologicalStressType> suggestions = PhysiologicalStressCatalog.suggestFromPathologies(paciente);

		assertThat(suggestions).contains(PhysiologicalStressType.MODERATE_INFECTION,
				PhysiologicalStressType.ORGAN_FAILURE, PhysiologicalStressType.PREGNANCY_COMPLICATION);
	}

	@Test
	void commonAndUncommonTypeListsExcludeNone() {
		assertThat(PhysiologicalStressCatalog.commonTypes()).contains(PhysiologicalStressType.FEVER)
			.doesNotContain(PhysiologicalStressType.NONE);
		assertThat(PhysiologicalStressCatalog.uncommonTypes()).contains(PhysiologicalStressType.HEAD_INJURY)
			.doesNotContain(PhysiologicalStressType.NONE);
	}

}
