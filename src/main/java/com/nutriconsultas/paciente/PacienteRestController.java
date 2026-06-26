package com.nutriconsultas.paciente;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.paciente.invitation.PatientMobileInvitationUiSupport;
import com.nutriconsultas.paciente.projection.PacienteListView;
import com.nutriconsultas.util.LogRedaction;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/pacientes")
@Slf4j
public class PacienteRestController extends AbstractGridController<PacienteListView> {

	@Autowired
	private PacienteService service;

	@Autowired
	private PacienteDeletionService pacienteDeletionService;

	private static final Map<String, String> COLUMN_TO_FIELD_MAP = new HashMap<>();

	static {
		COLUMN_TO_FIELD_MAP.put("nombre", "name");
		COLUMN_TO_FIELD_MAP.put("dob", "dob");
		COLUMN_TO_FIELD_MAP.put("email", "email");
		COLUMN_TO_FIELD_MAP.put("phone", "phone");
		COLUMN_TO_FIELD_MAP.put("gender", "gender");
		COLUMN_TO_FIELD_MAP.put("responsible", "responsibleName");
		COLUMN_TO_FIELD_MAP.put("mobileApp", "status");
	}

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
	 * Converts a PagingRequest to Spring Data Pageable.
	 * @param pagingRequest the paging request
	 * @return Spring Data Pageable
	 */
	private Pageable toPageable(final PagingRequest pagingRequest) {
		final int page = pagingRequest.getStart() / pagingRequest.getLength();
		final int size = pagingRequest.getLength();

		Sort sort = Sort.unsorted();
		if (pagingRequest.getOrder() != null && !pagingRequest.getOrder().isEmpty()) {
			final Order order = pagingRequest.getOrder().get(0);
			if (order.getColumn() != null && order.getColumn() < pagingRequest.getColumns().size()) {
				final String columnName = pagingRequest.getColumns().get(order.getColumn()).getData();
				final String fieldName = COLUMN_TO_FIELD_MAP.getOrDefault(columnName, columnName);
				final Sort.Direction direction = order.getDir() == Direction.asc ? Sort.Direction.ASC
						: Sort.Direction.DESC;
				sort = Sort.by(direction, fieldName);
			}
		}

		return PageRequest.of(page, size, sort);
	}

	/**
	 * Overrides base class method to add user filtering. Gets the authenticated user from
	 * SecurityContext and filters patients by userId. Uses server-side pagination.
	 * @param pagingRequest the paging request
	 * @return paginated patient data filtered by the authenticated user
	 */
	@Override
	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		log.info("starting getPageArray with pagingRequest: {}", pagingRequest);
		final OidcUser principal = (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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
		final com.nutriconsultas.dataTables.paging.Page<PacienteListView> page = getRows(pagingRequest, nonNullUserId);
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
	 * Gets rows filtered by userId using server-side pagination.
	 * @param pagingRequest the paging request
	 * @param userId the user ID to filter by (must not be null)
	 * @return the page of patients
	 */
	protected com.nutriconsultas.dataTables.paging.Page<PacienteListView> getRows(final PagingRequest pagingRequest,
			@org.springframework.lang.NonNull final String userId) {
		log.debug("starting getRows with pagingRequest: {} for userId: {}", pagingRequest, userId);
		final Pageable pageable = toPageable(pagingRequest);
		final String searchValue = pagingRequest.getSearch() != null && pagingRequest.getSearch().getValue() != null
				? pagingRequest.getSearch().getValue().trim() : null;

		final Page<PacienteListView> springPage;
		final long totalCount;
		final long filteredCount;

		if (searchValue != null && !searchValue.isEmpty()) {
			springPage = service.findListViewsByUserIdAndSearchTerm(userId, searchValue, pageable);
			filteredCount = service.countByUserIdAndSearchTerm(userId, searchValue);
		}
		else {
			springPage = service.findListViewsByUserId(userId, pageable);
			filteredCount = service.countByUserId(userId);
		}
		totalCount = service.countByUserId(userId);

		final com.nutriconsultas.dataTables.paging.Page<PacienteListView> result = new com.nutriconsultas.dataTables.paging.Page<>(
				springPage.getContent());
		result.setRecordsFiltered((int) filteredCount);
		result.setRecordsTotal((int) totalCount);
		result.setDraw(pagingRequest.getDraw());

		log.debug("returning data at getRows: recordsTotal={}, recordsFiltered={}", result.getRecordsTotal(),
				result.getRecordsFiltered());
		return result;
	}

	@Override
	protected List<String> toStringList(final PacienteListView row) {
		log.debug("converting Paciente list view row {} to string list.", LogRedaction.redactPaciente(row.getId()));
		final DateFormat dateFormat = new SimpleDateFormat(Paciente.DATE_OF_BIRTH_PATTERN);
		return Arrays.asList("<a href='/admin/pacientes/" + row.getId() + "'>" + row.getName() + "</a>",
				row.getDob() != null ? dateFormat.format(row.getDob()) : "", //
				row.getEmail(), //
				row.getPhone(), //
				row.getGender(), //
				row.getResponsibleName(), //
				PatientMobileInvitationUiSupport.gridBadgeHtml(row.getStatus(), row.getPatientAuthSub()),
				buildPacienteActions(row));
	}

	private String buildPacienteActions(final PacienteListView row) {
		final Long pacienteId = row.getId();
		final String inviteButton = PatientMobileInvitationUiSupport.canInviteFromGrid(row.getStatus(), row
			.getPatientAuthSub(), PatientMobileInvitationUiSupport.resolveRecipientEmailFromListRow(row))
					? "<button type='button' class='btn action-btn btn-outline-success btn-sm paciente-mobile-invite-btn' "
							+ "data-id='" + pacienteId
							+ "' title='Invitar a la app'><i class='fas fa-mobile-alt'></i></button> "
					: "";
		return inviteButton
				+ "<button type='button' class='btn action-btn btn-outline-primary btn-sm paciente-export-btn' "
				+ "data-id='" + pacienteId + "' title='Exportar registro'><i class='fas fa-download'></i></button> "
				+ "<button type='button' class='btn action-btn btn-danger btn-sm paciente-delete-btn' data-id='"
				+ pacienteId + "' title='Eliminar paciente'><i class='fas fa-trash'></i></button>";
	}

	@Override
	protected List<PacienteListView> getData() {
		log.warn("getData() called without userId filter. This should not happen in production.");
		return List.of();
	}

	/**
	 * Gets patient list views filtered by userId.
	 * @param userId the user ID to filter by (must not be null)
	 * @return list of patient list views for the user
	 */
	protected List<PacienteListView> getData(@org.springframework.lang.NonNull final String userId) {
		log.debug("getting Paciente list views for userId: {}", userId);
		return service.findListViewsByUserId(userId, Pageable.unpaged()).getContent();
	}

	@Override
	protected Predicate<PacienteListView> getPredicate(final String value) {
		return row -> row.getName().toLowerCase().contains(value) || row.getName().toLowerCase().startsWith(value)
				|| (row.getResponsibleName() != null && (row.getResponsibleName().toLowerCase().contains(value)
						|| row.getResponsibleName().toLowerCase().startsWith(value)));
	}

	@Override
	protected Comparator<PacienteListView> getComparator(final String column, final Direction dir) {
		log.debug("getting Paciente comparator with column {} and direction {}.", column, dir);
		return PacienteComparators.getComparator(column, dir);
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting Paciente columns.");
		return Stream.of("nombre", "dob", "email", "phone", "gender", "responsible", "mobileApp", "acciones")
			.map(Column::new)
			.collect(Collectors.toList());
	}

	@PostMapping("/{id}/assign-bmi")
	public ResponseEntity<java.util.Map<String, Object>> assignBmi(@PathVariable @NonNull final Long id,
			@RequestBody final java.util.Map<String, Object> body, @AuthenticationPrincipal final OidcUser principal) {
		final String userId = principal != null ? principal.getSubject() : null;
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(java.util.Map.of("success", false, "error", "Not authenticated"));
		}
		try {
			final Paciente paciente = service.findByIdAndUserId(id, userId);
			if (paciente == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(java.util.Map.of("success", false, "error", "Paciente no encontrado"));
			}
			final Double bmi = Double.parseDouble(body.get("bmi").toString());
			paciente.setImc(bmi);
			service.save(paciente);
			log.debug("Assigned BMI {} to patient {}", bmi, id);
			return ResponseEntity.ok(java.util.Map.of("success", true, "imc", bmi));
		}
		catch (Exception e) {
			log.error("Error assigning BMI to patient {}", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(java.util.Map.of("success", false, "error", e.getMessage()));
		}
	}

	@PostMapping("/{id}/assign-bmr")
	public ResponseEntity<java.util.Map<String, Object>> assignBmr(@PathVariable @NonNull final Long id,
			@RequestBody final java.util.Map<String, Object> body, @AuthenticationPrincipal final OidcUser principal) {
		final String userId = principal != null ? principal.getSubject() : null;
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(java.util.Map.of("success", false, "error", "Not authenticated"));
		}
		try {
			final Paciente paciente = service.findByIdAndUserId(id, userId);
			if (paciente == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(java.util.Map.of("success", false, "error", "Paciente no encontrado"));
			}
			final Double bmr = Double.parseDouble(body.get("bmr").toString());
			paciente.setBmr(bmr);
			service.save(paciente);
			log.debug("Assigned BMR {} to patient {}", bmr, id);
			return ResponseEntity.ok(java.util.Map.of("success", true, "bmr", bmr));
		}
		catch (Exception e) {
			log.error("Error assigning BMR to patient {}", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(java.util.Map.of("success", false, "error", e.getMessage()));
		}
	}

	@PutMapping("/{id}/avatar")
	public ResponseEntity<Map<String, Object>> updateAvatar(@PathVariable @NonNull final Long id,
			@Valid @RequestBody final PacienteAvatarUpdateRequest request,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = getUserId(principal);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("success", false, "error", "Not authenticated"));
		}
		try {
			final Paciente saved = service.updateAvatar(id, userId, request.getAvatarId());
			return ResponseEntity.ok(Map.of("success", true, "avatarId", saved.getAvatarId(), "avatarUrl",
					PacienteAvatarCatalog.resolveImagePath(saved)));
		}
		catch (final IllegalArgumentException ex) {
			final HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("no encontrado")
					? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
			return ResponseEntity.status(status).body(Map.of("success", false, "error", ex.getMessage()));
		}
	}

	/**
	 * Deletes a patient and all in-app clinical history for the authenticated
	 * nutritionist (#223).
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deletePaciente(@PathVariable @NonNull final Long id,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = getUserId(principal);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("success", false, "error", "Not authenticated"));
		}
		try {
			pacienteDeletionService.deletePatientWithHistory(id, userId);
			return ResponseEntity.ok(Map.of("success", true, "message", "Paciente eliminado correctamente"));
		}
		catch (final IllegalArgumentException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", ex.getMessage()));
		}
		catch (final Exception ex) {
			log.error("Error deleting patient {}", id, ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("success", false, "error", "Error al eliminar el paciente"));
		}
	}

}
