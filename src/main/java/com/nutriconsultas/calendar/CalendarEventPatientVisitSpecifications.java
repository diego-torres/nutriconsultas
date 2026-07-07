package com.nutriconsultas.calendar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Dynamic filters for mobile patient visit listing (#91). Avoids nullable-parameter JPQL
 * that PostgreSQL cannot type-infer ({@code ? IS NULL OR column = ?}).
 */
public final class CalendarEventPatientVisitSpecifications {

	private CalendarEventPatientVisitSpecifications() {
	}

	public static Specification<CalendarEvent> forPatient(final Long pacienteId, final EventStatus status,
			final Date fromDate, final Date toDate) {
		return (root, query, criteriaBuilder) -> {
			final List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("paciente").get("id"), pacienteId));
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (fromDate != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventDateTime"), fromDate));
			}
			if (toDate != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("eventDateTime"), toDate));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
