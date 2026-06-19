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
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.Subscription;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/platform/subscriptions")
@Slf4j
public class SubscriptionAdminRestController extends AbstractGridController<Subscription> {

	private static final Map<String, String> COLUMN_TO_FIELD_MAP = new HashMap<>();

	static {
		COLUMN_TO_FIELD_MAP.put("id", "id");
		COLUMN_TO_FIELD_MAP.put("planTier", "planTier");
		COLUMN_TO_FIELD_MAP.put("status", "status");
		COLUMN_TO_FIELD_MAP.put("periodEnd", "periodEnd");
		COLUMN_TO_FIELD_MAP.put("paymentExempt", "paymentExempt");
	}

	private final SubscriptionGridService gridService;

	private final ClinicRepository clinicRepository;

	private final SubscriptionOwnerResolver ownerResolver;

	private final PlatformAdminAuthorization platformAdminAuthorization;

	public SubscriptionAdminRestController(final SubscriptionGridService gridService,
			final ClinicRepository clinicRepository, final SubscriptionOwnerResolver ownerResolver,
			final PlatformAdminAuthorization platformAdminAuthorization) {
		this.gridService = gridService;
		this.clinicRepository = clinicRepository;
		this.ownerResolver = ownerResolver;
		this.platformAdminAuthorization = platformAdminAuthorization;
	}

	@Override
	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		final OidcUser principal = (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		platformAdminAuthorization.requirePlatformAdmin(principal, "subscriptions.list");
		return super.getPageArray(pagingRequest);
	}

	@Override
	protected com.nutriconsultas.dataTables.paging.Page<Subscription> getRows(final PagingRequest pagingRequest) {
		final Pageable pageable = toPageable(pagingRequest);
		final Page<Subscription> springPage = gridService.findPage(pageable);
		final com.nutriconsultas.dataTables.paging.Page<Subscription> result = new com.nutriconsultas.dataTables.paging.Page<>(
				springPage.getContent());
		result.setRecordsFiltered((int) gridService.countAll());
		result.setRecordsTotal((int) gridService.countAll());
		result.setDraw(pagingRequest.getDraw());
		return result;
	}

	@NonNull
	private Pageable toPageable(final PagingRequest pagingRequest) {
		final int length = pagingRequest.getLength() > 0 ? pagingRequest.getLength() : 25;
		final int page = pagingRequest.getStart() / length;
		return PageRequest.of(page, length, resolveSort(pagingRequest));
	}

	private Sort resolveSort(final PagingRequest pagingRequest) {
		if (pagingRequest.getOrder() == null || pagingRequest.getOrder().isEmpty()) {
			return Sort.by(Sort.Direction.DESC, "id");
		}
		final Order order = pagingRequest.getOrder().get(0);
		if (order.getColumn() == null || order.getColumn() >= pagingRequest.getColumns().size()) {
			return Sort.by(Sort.Direction.DESC, "id");
		}
		final String columnName = pagingRequest.getColumns().get(order.getColumn()).getData();
		if ("actions".equals(columnName) || "clinicName".equals(columnName) || "ownerEmail".equals(columnName)) {
			return Sort.by(Sort.Direction.DESC, "id");
		}
		final String fieldName = COLUMN_TO_FIELD_MAP.getOrDefault(columnName, columnName);
		final Sort.Direction direction = order.getDir() == Direction.asc ? Sort.Direction.ASC : Sort.Direction.DESC;
		return Sort.by(direction, fieldName);
	}

	@Override
	protected List<String> toStringList(final Subscription row) {
		final String clinicName = clinicRepository.findBySubscriptionId(row.getId())
			.map(clinic -> clinic.getName())
			.orElse("—");
		final SubscriptionOwnerView owner = ownerResolver.resolve(row.getId()).orElse(null);
		return Arrays.asList(String.valueOf(row.getId()), SubscriptionGridHtml.formatOwnerEmail(owner), clinicName,
				SubscriptionGridHtml.planTierLabel(row.getPlanTier()),
				SubscriptionGridHtml.statusBadge(row.getStatus()),
				SubscriptionGridHtml.formatInstant(row.getPeriodEnd()),
				SubscriptionGridHtml.paymentExemptBadge(row.isPaymentExempt()),
				SubscriptionGridHtml.editLink(row.getId(), row.getStatus()));
	}

	@Override
	protected List<Subscription> getData() {
		return List.of();
	}

	@Override
	protected Predicate<Subscription> getPredicate(final String value) {
		return row -> true;
	}

	@Override
	protected Comparator<Subscription> getComparator(final String column, final Direction dir) {
		return (left, right) -> 0;
	}

	@Override
	protected List<Column> getColumns() {
		return Stream
			.of("id", "ownerEmail", "clinicName", "planTier", "status", "periodEnd", "paymentExempt", "actions")
			.map(Column::new)
			.collect(Collectors.toList());
	}

}
