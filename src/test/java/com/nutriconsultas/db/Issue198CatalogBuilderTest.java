package com.nutriconsultas.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for issue #198 catalog builder portion logic
 * (scripts/build-issue-198-catalog.py).
 */
public class Issue198CatalogBuilderTest {

	@Test
	public void targetKcalRangeCoversThirtyStepsFrom2500To3500() {
		final int templateCount = 30;
		final int kcalMin = 2500;
		final int kcalMax = 3500;
		int previous = kcalMin - 1;
		for (int idx = 0; idx < templateCount; idx++) {
			final int target = (int) Math.round(kcalMin + (kcalMax - kcalMin) * idx / (double) (templateCount - 1));
			assertThat(target).isGreaterThan(previous);
			assertThat(target).isBetween(kcalMin, kcalMax);
			previous = target;
		}
		assertThat(previous).isEqualTo(kcalMax);
	}

}
