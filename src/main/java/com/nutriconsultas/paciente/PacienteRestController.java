package com.nutriconsultas.paciente;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/pacientes")
@Slf4j
public class PacienteRestController extends AbstractGridController<Paciente> {

	@Autowired
	private PacienteService service;

	/**
	 * Gets the user ID from the OAuth2 principal.
	 * @param principal the OAuth2 principal
	 * @return the user ID (sub claim) or null if not available
	 */
	private String getUserId(@AuthenticationPrincipal final OidcUser principal) {
		if (principal == null) {
			log.warn("OAuth2 principal is null, cannot get user ID");
			return null;
		}
		final String userId = principal.getSubject();
		log.debug("Retrieved user ID: {}", userId);
		return userId;
	}

	/**
	 * Overrides base class method to add user filtering.
	 * Gets the authenticated user from SecurityContext and filters patients by userId.
	 * @param pagingRequest the paging request
	 * @return paginated patient data filtered by the authenticated user
	 */
	@Override
	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		log.info("starting getPageArray with pagingRequest: {}", pagingRequest);
		final OidcUser principal = (OidcUser) SecurityContextHolder.getContext()
			.getAuthentication()
			.getPrincipal();
		final String userId = getUserId(principal);
		if (userId == null) {
			log.error("Cannot get patient data: user ID is null");
			final PageArray pageArray = new PageArray();
			pageArray.setData(List.of());
			pageArray.setDraw(pagingRequest.getDraw());
			pageArray.setRecordsFiltered(0);
			pageArray.setRecordsTotal(0);
			return pageArray;
		}
		pagingRequest.setColumns(getColumns());
		final String nonNullUserId = userId;
		final Page<Paciente> page = getRows(pagingRequest, nonNullUserId);
		log.debug("page with records: {}", page.getRecordsTotal());
		final PageArray pageArray = new PageArray();
		pageArray.setData(page.getData().stream().map(this::toStringList).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		log.info("returning data at getPageArray: {}", pageArray.getRecordsTotal());
		return pageArray;
	}

	/**
	 * Gets rows filtered by userId.
	 * @param pagingRequest the paging request
	 * @param userId the user ID to filter by (must not be null)
	 * @return the page of patients
	 */
	protected Page<Paciente> getRows(final PagingRequest pagingRequest,
			@org.springframework.lang.NonNull final String userId) {
		log.debug("starting getRows with pagingRequest: {} for userId: {}", pagingRequest, userId);
		return getPage(pagingRequest, getData(userId));
	}

	@Override
	protected List<String> toStringList(final Paciente row) {
		log.debug("converting Paciente row {} to string list.", row);
		final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return Arrays.asList("<a href='/admin/pacientes/" + row.getId() + "'>" + row.getName() + "</a>",
				row.getDob() != null ? dateFormat.format(row.getDob()) : "", //
				row.getEmail(), //
				row.getPhone(), //
				row.getGender(), //
				row.getResponsibleName());
	}

	@Override
	protected List<Paciente> getData() {
		log.warn("getData() called without userId filter. This should not happen in production.");
		return service.findAll();
	}

	/**
	 * Gets patient data filtered by userId.
	 * @param userId the user ID to filter by (must not be null)
	 * @return list of patients for the user
	 */
	protected List<Paciente> getData(@org.springframework.lang.NonNull final String userId) {
		log.debug("getting all Paciente records for userId: {}", userId);
		return service.findAllByUserId(userId);
	}

	@Override
	protected Predicate<Paciente> getPredicate(final String value) {
		return row -> row.getName().toLowerCase().contains(value) || row.getName().toLowerCase().startsWith(value)
				|| row.getResponsibleName().toLowerCase().contains(value)
				|| row.getResponsibleName().toLowerCase().startsWith(value);
	}

	@Override
	protected Comparator<Paciente> getComparator(final String column, final Direction dir) {
		log.debug("getting Paciente comparator with column {} and direction {}.", column, dir);
		return PacienteComparators.getComparator(column, dir);
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting Paciente columns.");
		return Stream.of("nombre", "dob", "email", "phone", "gender", "responsible")
			.map(Column::new)
			.collect(Collectors.toList());
	}

}
