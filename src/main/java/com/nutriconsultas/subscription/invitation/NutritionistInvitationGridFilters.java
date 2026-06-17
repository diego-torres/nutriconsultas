package com.nutriconsultas.subscription.invitation;

import org.springframework.util.StringUtils;

import com.nutriconsultas.dataTables.paging.PagingRequest;

/**
 * Server-side filter criteria for the platform-admin invitations grid.
 */
public record NutritionistInvitationGridFilters(String globalSearch, String email, String planTier, String status,
		String paymentExempt) {

	public static NutritionistInvitationGridFilters fromPagingRequest(final PagingRequest pagingRequest) {
		final String global = pagingRequest.getSearch() != null ? trim(pagingRequest.getSearch().getValue()) : null;
		return new NutritionistInvitationGridFilters(global, columnSearch(pagingRequest, "email"),
				columnSearch(pagingRequest, "planTier"), columnSearch(pagingRequest, "status"),
				columnSearch(pagingRequest, "paymentExempt"));
	}

	private static String columnSearch(final PagingRequest pagingRequest, final String data) {
		if (pagingRequest.getColumns() == null) {
			return null;
		}
		return pagingRequest.getColumns()
			.stream()
			.filter(column -> data.equals(column.getData()))
			.map(column -> column.getSearch() != null ? trim(column.getSearch().getValue()) : null)
			.filter(StringUtils::hasText)
			.findFirst()
			.orElse(null);
	}

	private static String trim(final String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		final String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
