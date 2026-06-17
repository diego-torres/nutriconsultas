package com.nutriconsultas.admin;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationGridFilters;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationGridHtml;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationGridService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/platform/invitations")
@Slf4j
public class NutritionistInvitationRestController extends AbstractGridController<NutritionistInvitation> {

	private static final Map<String, String> COLUMN_TO_FIELD_MAP = new HashMap<>();

	static {
		COLUMN_TO_FIELD_MAP.put("createdAt", "createdAt");
		COLUMN_TO_FIELD_MAP.put("email", "email");
		COLUMN_TO_FIELD_MAP.put("planTier", "planTier");
		COLUMN_TO_FIELD_MAP.put("status", "status");
		COLUMN_TO_FIELD_MAP.put("paymentExempt", "paymentExempt");
		COLUMN_TO_FIELD_MAP.put("expiresAt", "expiresAt");
	}

	private final NutritionistInvitationGridService gridService;

	private final PlatformAdminAuthorization platformAdminAuthorization;

	public NutritionistInvitationRestController(final NutritionistInvitationGridService gridService,
			final PlatformAdminAuthorization platformAdminAuthorization) {
		this.gridService = gridService;
		this.platformAdminAuthorization = platformAdminAuthorization;
	}

	@Override
	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		final OidcUser principal = (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		platformAdminAuthorization.requirePlatformAdmin(principal, "invitations.list");
		log.info("starting invitations getPageArray with draw={}", pagingRequest.getDraw());
		pagingRequest.setColumns(getColumns());
		final com.nutriconsultas.dataTables.paging.Page<NutritionistInvitation> page = getRows(pagingRequest);
		final PageArray pageArray = new PageArray();
		pageArray.setData(page.getData().stream().map(this::toStringList).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		log.info("returning invitations data-table: recordsTotal={}, recordsFiltered={}", pageArray.getRecordsTotal(),
				pageArray.getRecordsFiltered());
		return pageArray;
	}

	@Override
	protected com.nutriconsultas.dataTables.paging.Page<NutritionistInvitation> getRows(
			final PagingRequest pagingRequest) {
		final Pageable pageable = toPageable(pagingRequest);
		final NutritionistInvitationGridFilters filters = NutritionistInvitationGridFilters
			.fromPagingRequest(pagingRequest);
		final Page<NutritionistInvitation> springPage = gridService.findPage(filters, pageable);
		final com.nutriconsultas.dataTables.paging.Page<NutritionistInvitation> result = new com.nutriconsultas.dataTables.paging.Page<>(
				springPage.getContent());
		result.setRecordsFiltered((int) gridService.countFiltered(filters));
		result.setRecordsTotal((int) gridService.countAll());
		result.setDraw(pagingRequest.getDraw());
		return result;
	}

	@NonNull
	private Pageable toPageable(final PagingRequest pagingRequest) {
		final int length = pagingRequest.getLength() > 0 ? pagingRequest.getLength() : 25;
		final int page = pagingRequest.getStart() / length;
		Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
		if (pagingRequest.getOrder() != null && !pagingRequest.getOrder().isEmpty()) {
			final Order order = pagingRequest.getOrder().get(0);
			if (order.getColumn() != null && order.getColumn() < pagingRequest.getColumns().size()) {
				final String columnName = pagingRequest.getColumns().get(order.getColumn()).getData();
				if (!"actions".equals(columnName)) {
					final String fieldName = COLUMN_TO_FIELD_MAP.getOrDefault(columnName, columnName);
					final Sort.Direction direction = order.getDir() == Direction.asc ? Sort.Direction.ASC
							: Sort.Direction.DESC;
					sort = Sort.by(direction, fieldName);
				}
			}
		}
		return PageRequest.of(page, length, sort);
	}

	@Override
	protected List<String> toStringList(final NutritionistInvitation row) {
		return Arrays.asList(
				"<span id=\"invitation-" + row.getId() + "\">" + NutritionistInvitationGridHtml.formatCreatedAt(row)
						+ "</span>",
				NutritionistInvitationGridHtml.escape(row.getEmail()),
				row.getPlanTier() != null ? row.getPlanTier().name() : "",
				NutritionistInvitationGridHtml.statusBadge(row.getStatus()),
				NutritionistInvitationGridHtml.paymentExemptBadge(row.isPaymentExempt()),
				NutritionistInvitationGridHtml.formatExpiresAt(row),
				NutritionistInvitationGridHtml.actionsHtml(row));
	}

	@Override
	protected List<NutritionistInvitation> getData() {
		log.warn("getData() called without server-side pagination for invitations");
		return List.of();
	}

	@Override
	protected Predicate<NutritionistInvitation> getPredicate(final String value) {
		return row -> true;
	}

	@Override
	protected Comparator<NutritionistInvitation> getComparator(final String column, final Direction dir) {
		return (left, right) -> 0;
	}

	@Override
	protected List<Column> getColumns() {
		return Stream
			.of("createdAt", "email", "planTier", "status", "paymentExempt", "expiresAt", "actions")
			.map(Column::new)
			.collect(Collectors.toList());
	}

}
