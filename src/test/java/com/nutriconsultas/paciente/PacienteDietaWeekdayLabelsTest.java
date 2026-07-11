package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.dieta.Dieta;

class PacienteDietaWeekdayLabelsTest {

	@Test
	void slotsByDay_mapsSlotsByIsoDay() {
		final Dieta dieta = new Dieta();
		dieta.setId(10L);
		dieta.setNombre("Lunes");

		final PacienteDietaWeekday monday = new PacienteDietaWeekday();
		monday.setDayOfWeek(1);
		monday.setDieta(dieta);

		final PacienteDietaWeekday wednesday = new PacienteDietaWeekday();
		wednesday.setDayOfWeek(3);
		wednesday.setDieta(dieta);

		assertThat(PacienteDietaWeekdayLabels.slotsByDay(List.of(monday, wednesday)))
			.containsEntry(1, monday)
			.containsEntry(3, wednesday)
			.hasSize(2);
	}

	@Test
	void slotsByDay_returnsEmptyMapWhenNull() {
		assertThat(PacienteDietaWeekdayLabels.slotsByDay(null)).isEmpty();
	}

}
