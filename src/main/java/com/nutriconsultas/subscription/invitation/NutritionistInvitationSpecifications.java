package com.nutriconsultas.subscription.invitation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.PlanTier;

import jakarta.persistence.criteria.Predicate;

public final class NutritionistInvitationSpecifications {

	private NutritionistInvitationSpecifications() {
	}

	public static Specification<NutritionistInvitation> withFilters(final NutritionistInvitationGridFilters filters) {
		return (root, query, criteriaBuilder) -> {
			final List<Predicate> predicates = new ArrayList<>();
			if (StringUtils.hasText(filters.globalSearch())) {
				final String pattern = "%" + filters.globalSearch().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("planTier").as(String.class)), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("status").as(String.class)), pattern)));
			}
			if (StringUtils.hasText(filters.email())) {
				final String pattern = "%" + filters.email().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern));
			}
			parsePlanTier(filters.planTier())
				.ifPresent(planTier -> predicates.add(criteriaBuilder.equal(root.get("planTier"), planTier)));
			parseStatus(filters.status())
				.ifPresent(status -> predicates.add(criteriaBuilder.equal(root.get("status"), status)));
			parsePaymentExempt(filters.paymentExempt())
				.ifPresent(paymentExempt -> predicates.add(criteriaBuilder.equal(root.get("paymentExempt"), paymentExempt)));
			if (predicates.isEmpty()) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static Optional<PlanTier> parsePlanTier(final String value) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		try {
			return Optional.of(PlanTier.valueOf(value.trim().toUpperCase(Locale.ROOT)));
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	private static Optional<InvitationStatus> parseStatus(final String value) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		final String normalized = value.trim().toUpperCase(Locale.ROOT);
		return switch (normalized) {
			case "PENDIENTE" -> Optional.of(InvitationStatus.PENDING);
			case "ACEPTADA", "ACEPTADO" -> Optional.of(InvitationStatus.REDEEMED);
			case "EXPIRADA" -> Optional.of(InvitationStatus.EXPIRED);
			case "CANCELADA" -> Optional.of(InvitationStatus.CANCELLED);
			default -> {
				try {
					yield Optional.of(InvitationStatus.valueOf(normalized));
				}
				catch (IllegalArgumentException ex) {
					yield Optional.empty();
				}
			}
		};
	}

	private static Optional<Boolean> parsePaymentExempt(final String value) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		final String normalized = value.trim().toLowerCase(Locale.ROOT);
		if ("true".equals(normalized) || "1".equals(normalized) || "si".equals(normalized) || "sí".equals(normalized)
				|| "yes".equals(normalized)) {
			return Optional.of(Boolean.TRUE);
		}
		if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
			return Optional.of(Boolean.FALSE);
		}
		return Optional.empty();
	}

}
