package com.nutriconsultas.platillos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.controller.AbstractGridItemController;
import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("rest/platillos/{id}/ingredientes")
@Slf4j
public class IngredienteRestController extends AbstractGridItemController<Ingrediente> {

	@Autowired
	private PlatilloService platilloService;

	@Autowired
	private PlatilloAuthorization platilloAuthorization;

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

	private Platillo loadPlatilloForMutation(@NonNull final Long id, final OidcUser principal) {
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se pudo identificar al usuario");
		}
		final Platillo platillo = platilloAuthorization.resolveForMutation(id, userId, principal, platilloService);
		if (platillo == null) {
			if (platilloService.findById(id) == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Platillo no encontrado");
			}
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para modificar este platillo");
		}
		platilloAuthorization.verifyCanModify(platillo, userId, principal);
		return platillo;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<IngredienteListItemDto>>> list(@NonNull @PathVariable final Long id) {
		log.debug("listing ingredientes for platillo {}.", id);
		final Platillo platillo = platilloService.findById(id);
		if (platillo == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Platillo no encontrado");
		}
		final List<IngredienteListItemDto> items = platilloService.listIngredientes(id)
			.stream()
			.map(IngredienteListItemDto::fromEntity)
			.toList();
		return ResponseEntity.ok(new ApiResponse<>(items));
	}

	@DeleteMapping("/{ingredienteId}")
	public void delete(@NonNull @PathVariable final Long id, @NonNull @PathVariable final Long ingredienteId) {
		log.debug("deleting Ingrediente with id {}.", ingredienteId);
		final OidcUser principal = currentUser();
		final Platillo platillo = loadPlatilloForMutation(id, principal);
		platilloService.deleteIngrediente(id, ingredienteId);
		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, platillo, "platillos.ingredientes.delete");
	}

	@PutMapping("/reorder")
	public ResponseEntity<ApiResponse<Void>> reorder(@NonNull @PathVariable final Long id,
			@RequestBody @NonNull final List<Long> orderedIngredienteIds) {
		log.debug("reordering ingredientes for platillo {}.", id);
		final OidcUser principal = currentUser();
		loadPlatilloForMutation(id, principal);
		try {
			platilloService.reorderIngredientes(id, orderedIngredienteIds);
			return ResponseEntity.ok(new ApiResponse<>(null));
		}
		catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
		}
	}

	@PutMapping("/{ingredienteId}")
	public ResponseEntity<ApiResponse<Platillo>> update(@NonNull @PathVariable final Long id,
			@NonNull @PathVariable final Long ingredienteId, @RequestBody @NonNull final IngredienteFormModel form) {
		log.debug("updating Ingrediente with id {} on platillo {}.", ingredienteId, id);
		final OidcUser principal = currentUser();
		loadPlatilloForMutation(id, principal);
		final String cantidad = form.getCantidad();
		final Integer peso = form.getPeso();
		if (cantidad == null || cantidad.isBlank() || peso == null) {
			return ResponseEntity.badRequest().build();
		}
		final Platillo saved = platilloService.updateIngrediente(id, ingredienteId, cantidad.trim(), peso);
		if (saved == null) {
			return ResponseEntity.notFound().build();
		}
		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, saved, "platillos.ingredientes.update");
		return ResponseEntity.ok(new ApiResponse<>(saved));
	}

	@Override
	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest,
			@NonNull @PathVariable final Long id) {
		log.info("starting getPageArray with pagingRequest: {} for id {}", pagingRequest, id);
		final OidcUser principal = currentUser();
		final Platillo platillo = platilloService.findById(id);
		final boolean canModify = platilloAuthorization.canModify(platillo, getUserId(principal), principal);
		pagingRequest.setColumns(getColumns());
		pagingRequest.setStart(0);
		pagingRequest.setLength(PlatilloIngredientLimits.LIST_PAGE_SIZE);
		if (pagingRequest.getSearch() == null) {
			pagingRequest.setSearch(new com.nutriconsultas.dataTables.paging.Search("", "false"));
		}
		else {
			pagingRequest.getSearch().setValue("");
		}
		final Page<Ingrediente> page = getRows(pagingRequest, id);
		log.debug("page with records: {}", page.getRecordsTotal());
		final PageArray pageArray = new PageArray();
		pageArray
			.setData(page.getData().stream().map(row -> toStringList(row, canModify)).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		log.info("returning data at getPageArray: {}", pageArray.getRecordsTotal());
		return pageArray;
	}

	protected List<String> toStringList(final Ingrediente row, final boolean canModify) {
		log.debug("converting Ingrediente row {} to string list.", row);
		final String actions = canModify ? "<a href='#'' class='btn action-btn btn-danger btn-sm delete-btn' data-id='"
				+ row.getId() + "'><i class='fas fa-trash fa-sm fa-fw'></i> </a>" : "";
		return Arrays.asList(row.getAlimento().getNombreAlimento(), buildCantidadCell(row, canModify), //
				row.getUnidad(), //
				row.getPesoNeto().toString(), //
				actions);
	}

	private String buildCantidadCell(final Ingrediente row, final boolean canModify) {
		if (!canModify) {
			return row.getDisplayCantSugerida(row.getUnidad());
		}
		final Alimento alimento = row.getAlimento();
		return "<input type='text' class='form-control form-control-sm inline-cantidad-input' data-id='" + row.getId()
				+ "' data-alimento-cant='" + alimento.getCantSugerida() + "' data-alimento-peso='"
				+ alimento.getPesoNeto() + "' value='" + row.getFractionalCantSugerida() + "' aria-label='Cantidad' />";
	}

	@Override
	protected List<String> toStringList(final Ingrediente row) {
		return toStringList(row, true);
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting Platillo columns.");
		return Arrays.asList(new Column("ingrediente"), //
				new Column("cantidad"), //
				new Column("unidad"), //
				new Column("peso"), //
				new Column("acciones"));
	}

	@Override
	protected List<Ingrediente> getData(@NonNull final Long id) {
		log.debug("getting Ingrediente rows for Platillo id {}.", id);
		return platilloService.listIngredientes(id);
	}

	@Override
	protected Predicate<Ingrediente> getPredicate(final String value) {
		log.debug("getting predicate for value {}.", value);
		return ingrediente -> ingrediente.getAlimento().getNombreAlimento().toLowerCase().contains(value.toLowerCase());
	}

	@Override
	protected Comparator<Ingrediente> getComparator(final String column, final Direction dir) {
		log.debug("getting comparator for column {} and direction {}.", column, dir);
		return IngredienteComparators.getComparator(column, dir);
	}

}
