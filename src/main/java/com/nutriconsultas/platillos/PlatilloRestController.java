package com.nutriconsultas.platillos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("rest/platillos")
@Slf4j
public class PlatilloRestController extends AbstractGridController<Platillo> {

	@Autowired
	private PlatilloService service;

	@Autowired
	private PlatilloAuthorization platilloAuthorization;

	@Autowired
	private PlatilloDeletionService platilloDeletionService;

	private String getUserId(final OidcUser principal) {
		if (principal == null) {
			return null;
		}
		return principal.getSubject();
	}

	private OidcUser currentUser() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
			return oidcUser;
		}
		return null;
	}

	private Platillo loadPlatilloForMutation(@NonNull final Long id) {
		final OidcUser principal = currentUser();
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se pudo identificar al usuario");
		}
		final Platillo platillo = platilloAuthorization.resolveForMutation(id, userId, principal, service);
		if (platillo == null) {
			if (service.findById(id) == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Platillo no encontrado");
			}
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para modificar este platillo");
		}
		platilloAuthorization.verifyCanModify(platillo, userId, principal);
		return platillo;
	}

	@Override
	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		log.info("starting getPageArray with pagingRequest: {}", pagingRequest);
		final OidcUser principal = resolveGridPrincipal();
		pagingRequest.setColumns(getColumns());
		final Page<Platillo> page = getRows(pagingRequest, principal);
		log.debug("page with records: {}", page.getRecordsTotal());
		final PageArray pageArray = new PageArray();
		pageArray
			.setData(page.getData().stream().map(row -> toStringList(row, principal)).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		log.info("returning data at getPageArray: {}", pageArray.getRecordsTotal());
		return pageArray;
	}

	protected Page<Platillo> getRows(final PagingRequest pagingRequest, final OidcUser principal) {
		log.debug("starting getRows with pagingRequest: {}", pagingRequest);
		final PlatilloCatalogFilter catalogFilter = PlatilloCatalogFilter
			.fromRequestValue(pagingRequest.getOwnershipFilter());
		final String userId = principal != null ? principal.getSubject() : null;
		final List<Platillo> data = service.getPlatillosForCatalogFilter(catalogFilter, userId);
		return getPage(pagingRequest, data);
	}

	private OidcUser resolveGridPrincipal() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
			return oidcUser;
		}
		return null;
	}

	@PostMapping("{id}/ingredientes/add")
	public ResponseEntity<ApiResponse<Ingrediente>> addIngrediente(@PathVariable @NonNull final Long id,
			@RequestBody @NonNull final IngredienteFormModel ingrediente) {
		log.info("starting addIngrediente with id {} and ingrediente {}.", id, ingrediente);
		loadPlatilloForMutation(id);
		final OidcUser principal = currentUser();
		final Long alimentoId = ingrediente.getAlimentoId();
		final String cantidad = ingrediente.getCantidad();
		final Integer peso = ingrediente.getPeso();
		ResponseEntity<ApiResponse<Ingrediente>> result;
		if (alimentoId == null || cantidad == null || peso == null) {
			log.error("Missing required fields: alimentoId={}, cantidad={}, peso={}", alimentoId, cantidad, peso);
			result = ResponseEntity.badRequest().build();
		}
		else {
			final Ingrediente ingredienteResult = service.addIngrediente(id, alimentoId, cantidad, peso);
			final Platillo platillo = service.findById(id);
			platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, platillo,
					"platillos.ingredientes.add");
			log.info("finish addIngrediente with id {} and ingrediente {}.", id, ingrediente);
			result = ResponseEntity.ok(new ApiResponse<Ingrediente>(ingredienteResult));
		}
		return result;
	}

	@PostMapping("{id}/ingestas/add")
	public ResponseEntity<ApiResponse<Platillo>> addIngesta(@PathVariable @NonNull final Long id,
			@RequestBody @NonNull final IngestaFormModel ingesta) {
		log.info("starting addIngesta with id {} and ingesta {}.", id, ingesta);
		final Platillo platillo = loadPlatilloForMutation(id);
		final OidcUser principal = currentUser();
		String ingestas = platillo.getIngestasSugeridas();

		ResponseEntity<ApiResponse<Platillo>> result;
		if (ingestas != null && ingestas.contains(ingesta.getIngesta())) {
			log.info("finish addIngesta with id {} and ingesta {} - DUPLICATE requested.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(platillo));
		}
		else {
			if (ingestas == null || ingestas.isEmpty()) {
				ingestas = ingesta.getIngesta();
			}
			else {
				ingestas += ", " + ingesta.getIngesta();
			}
			platillo.setIngestasSugeridas(ingestas);
			final Platillo saved = service.save(platillo);
			platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, saved, "platillos.ingestas.add");
			log.info("finish addIngesta with id {} and ingesta {}.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(saved));
		}
		return result;
	}

	@DeleteMapping("{id}/ingestas/{ingesta}")
	public ResponseEntity<ApiResponse<Platillo>> deleteIngesta(@PathVariable @NonNull final Long id,
			@PathVariable @NonNull final String ingesta) {
		log.info("starting deleteIngesta with id {} and ingesta {}.", id, ingesta);
		final Platillo platillo = loadPlatilloForMutation(id);
		final OidcUser principal = currentUser();
		final String ingestas = platillo.getIngestasSugeridas();
		ResponseEntity<ApiResponse<Platillo>> result;
		if (ingestas != null) {
			String updatedIngestas = ingestas.replace(ingesta, "").replace(",,", ",").trim();
			if (updatedIngestas.startsWith(",")) {
				updatedIngestas = updatedIngestas.substring(1);
			}
			if (updatedIngestas.endsWith(",")) {
				updatedIngestas = updatedIngestas.substring(0, updatedIngestas.length() - 1);
			}
			platillo.setIngestasSugeridas(updatedIngestas);
			log.debug("Ingestas after delete: [{}]", updatedIngestas);
			final Platillo saved = service.save(platillo);
			platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, saved, "platillos.ingestas.delete");
			log.info("finish deleteIngesta with id {} and ingesta {}.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(saved));
		}
		else {
			log.info("finish deleteIngesta with id {} and ingesta {}.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(platillo));
		}
		return result;
	}

	@PostMapping("{id}/video/add")
	public ResponseEntity<ApiResponse<Platillo>> addVideo(@PathVariable @NonNull final Long id,
			@RequestBody @NonNull final VideoFormModel video) {
		log.info("starting addVideo with id {} and video {}.", id, video);
		final Platillo platillo = loadPlatilloForMutation(id);
		final OidcUser principal = currentUser();
		platillo.setVideoUrl(video.getVideoUrl());
		final Platillo saved = service.save(platillo);
		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, saved, "platillos.video.add");
		log.info("finish addVideo with id {} and video {}.", id, video);
		return ResponseEntity.ok(new ApiResponse<Platillo>(saved));
	}

	@PostMapping("add")
	public Platillo add(@RequestBody @NonNull final Platillo platillo,
			@AuthenticationPrincipal final OidcUser principal) {
		log.info("starting add with platillo {}.", platillo);
		if (principal == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final String userId = principal.getSubject();
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		platillo.setUserId(platilloAuthorization.resolveCreateUserId(principal, userId));
		final Platillo saved = service.save(platillo);
		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, saved, "platillos.create");
		log.info("finish add with platillo {}.", saved);
		return saved;
	}

	@PostMapping("{id}/duplicate")
	public ResponseEntity<ApiResponse<Platillo>> duplicatePlatillo(@PathVariable @NonNull final Long id,
			@AuthenticationPrincipal final OidcUser principal) {
		log.info("starting duplicatePlatillo with id {}.", id);
		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		final String userId = principal.getSubject();
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		final Platillo original = service.findById(id);
		if (original == null) {
			log.warn("Platillo with id {} not found for duplication", id);
			return ResponseEntity.notFound().build();
		}
		if (!platilloAuthorization.canCopy(original, userId)) {
			log.warn("User forbidden to duplicate platillo {}", id);
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(buildDuplicateErrorResponse("No tiene permiso para copiar este platillo"));
		}
		final Platillo duplicated = service.duplicatePlatillo(id, userId);
		if (duplicated == null) {
			log.warn("Failed to duplicate platillo with id {}", id);
			return ResponseEntity.notFound().build();
		}
		log.info("finish duplicatePlatillo with id {}, new platillo id {} owned by user {}.", id, duplicated.getId(),
				userId);
		return ResponseEntity.ok(new ApiResponse<>(duplicated));
	}

	private ApiResponse<Platillo> buildDuplicateErrorResponse(final String message) {
		final ApiResponse<Platillo> apiResponse = new ApiResponse<>();
		apiResponse.setStatus(HttpStatus.FORBIDDEN.value());
		apiResponse.setMessage(message);
		apiResponse.setData(null);
		return apiResponse;
	}

	@DeleteMapping("{id}")
	public ResponseEntity<ApiResponse<Void>> deletePlatillo(@PathVariable @NonNull final Long id,
			@AuthenticationPrincipal final OidcUser principal) {
		log.info("starting deletePlatillo with id {}.", id);
		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		final String userId = principal.getSubject();
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		final PlatilloDeleteResult result = platilloDeletionService.deletePlatillo(id, userId, principal);
		return switch (result.getOutcome()) {
			case DELETED -> {
				log.info("finish deletePlatillo with id {}.", id);
				yield ResponseEntity.ok(new ApiResponse<>(null));
			}
			case NOT_FOUND -> {
				log.warn("Platillo with id {} not found for deletion", id);
				yield ResponseEntity.notFound().build();
			}
			case FORBIDDEN -> {
				log.warn("User {} forbidden to delete platillo {}", userId, id);
				yield ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(buildDeleteErrorResponse(HttpStatus.FORBIDDEN.value(),
							"No tiene permiso para eliminar este platillo"));
			}
			case IN_USE -> {
				final String message = buildInUseMessage(result.getDietReferenceCount());
				log.info("Delete blocked for platillo {}: {}", id, message);
				yield ResponseEntity.status(HttpStatus.CONFLICT)
					.body(buildDeleteErrorResponse(HttpStatus.CONFLICT.value(), message));
			}
		};
	}

	private ApiResponse<Void> buildDeleteErrorResponse(final int status, final String message) {
		final ApiResponse<Void> apiResponse = new ApiResponse<>();
		apiResponse.setStatus(status);
		apiResponse.setMessage(message);
		apiResponse.setData(null);
		return apiResponse;
	}

	private String buildInUseMessage(final long dietReferenceCount) {
		return "Este platillo está referenciado en " + dietReferenceCount
				+ (dietReferenceCount == 1 ? " dieta y no puede eliminarse" : " dieta(s) y no puede eliminarse");
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting Platillo columns.");
		return Stream.of("acciones", "platillo", "ingestas", "kcal", "prot", "lip", "hc")
			.map(Column::new)
			.collect(Collectors.toList());
	}

	@Override
	protected List<String> toStringList(final Platillo row) {
		return toStringList(row, null);
	}

	private List<String> toStringList(final Platillo row, final OidcUser principal) {
		log.debug("converting Platillo row {} to string list.", row);
		return Arrays.asList(buildActionsColumn(row, principal),
				"<a href='/admin/platillos/" + row.getId() + "'>" + row.getName() + "</a>",
				row.getIngestasSugeridas() == null ? "" : row.getIngestasSugeridas(),
				row.getEnergia() == null ? "" : row.getEnergia().toString(), String.format("%.1f", row.getProteina()),
				String.format("%.1f", row.getLipidos()), String.format("%.1f", row.getHidratosDeCarbono()));
	}

	private String buildActionsColumn(final Platillo row, final OidcUser principal) {
		if (principal == null) {
			return "";
		}
		final String userId = principal.getSubject();
		final String copyButton = platilloAuthorization.canCopy(row, userId)
				? "<button onclick='duplicatePlatillo(" + row.getId()
						+ ")' class='btn btn-sm btn-info' title='Copiar Platillo'><i class='fas fa-copy'></i></button> "
				: "";
		final String editButton = platilloAuthorization.canModify(row, userId, principal)
				? "<a href='/admin/platillos/" + row.getId()
						+ "' class='btn btn-sm btn-warning' title='Editar Platillo'><i class='fas fa-edit'></i></a> "
				: "";
		final String deleteButton = platilloAuthorization.canModify(row, userId, principal)
				? "<button onclick='deletePlatillo(" + row.getId()
						+ ")' class='btn btn-sm btn-danger' title='Eliminar Platillo'><i class='fas fa-trash'></i></button>"
				: "";
		return copyButton + editButton + deleteButton;
	}

	@Override
	protected List<Platillo> getData() {
		log.warn("getData() called without pagination. This should not happen in production.");
		return service.findAll();
	}

	@Override
	protected Page<Platillo> getRows(final PagingRequest pagingRequest) {
		return getRows(pagingRequest, resolveGridPrincipal());
	}

	@Override
	protected Predicate<Platillo> getPredicate(final String value) {
		log.debug("getting Platillo predicate with value {}.", value);
		return platillo -> platillo.getName().toLowerCase().contains(value.toLowerCase())
				|| (platillo.getIngestasSugeridas() != null
						&& platillo.getIngestasSugeridas().toLowerCase().contains(value.toLowerCase()));
	}

	@Override
	protected Comparator<Platillo> getComparator(final String column,
			final com.nutriconsultas.dataTables.paging.Direction dir) {
		log.debug("getting Platillo comparator with column {} and direction {}.", column, dir);
		return PlatilloComparators.getComparator(column, dir);
	}

}
