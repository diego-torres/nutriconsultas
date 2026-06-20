package com.nutriconsultas.platillos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.controller.AbstractGridItemController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

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

	@DeleteMapping("/{ingredienteId}")
	public void delete(@NonNull @PathVariable final Long id, @NonNull @PathVariable final Long ingredienteId) {
		log.debug("deleting Ingrediente with id {}.", ingredienteId);
		final OidcUser principal = currentUser();
		final Platillo platillo = loadPlatilloForMutation(id, principal);
		platilloService.deleteIngrediente(id, ingredienteId);
		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, platillo, "platillos.ingredientes.delete");
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
		return Arrays.asList(row.getAlimento().getNombreAlimento(), row.getFractionalCantSugerida(), //
				row.getUnidad(), //
				row.getPesoNeto().toString(), //
				actions);
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
		return platilloService.findById(id).getIngredientes();
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
