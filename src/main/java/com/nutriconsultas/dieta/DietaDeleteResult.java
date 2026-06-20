package com.nutriconsultas.dieta;

import lombok.Getter;

@Getter
public final class DietaDeleteResult {

	public enum Outcome {

		DELETED, NOT_FOUND, FORBIDDEN, IN_USE

	}

	private final Outcome outcome;

	private final long assignedPatientCount;

	private DietaDeleteResult(final Outcome outcome, final long assignedPatientCount) {
		this.outcome = outcome;
		this.assignedPatientCount = assignedPatientCount;
	}

	public static DietaDeleteResult deleted() {
		return new DietaDeleteResult(Outcome.DELETED, 0L);
	}

	public static DietaDeleteResult notFound() {
		return new DietaDeleteResult(Outcome.NOT_FOUND, 0L);
	}

	public static DietaDeleteResult forbidden() {
		return new DietaDeleteResult(Outcome.FORBIDDEN, 0L);
	}

	public static DietaDeleteResult inUse(final long assignedPatientCount) {
		return new DietaDeleteResult(Outcome.IN_USE, assignedPatientCount);
	}

}
