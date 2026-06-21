package com.nutriconsultas.dieta;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.model.ApiResponse;
import com.nutriconsultas.platillos.IngredienteFormModel;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/dietas/{dietaId}/ingestas/{ingestaId}/platillos/{platilloIngestaId}/ingredientes")
@Slf4j
public class IngredientePlatilloIngestaRestController extends AbstractGridController<IngredientePlatilloIngesta> {

	@Autowired
	private DietaService dietaService;

	@Autowired
	private DietaAuthorization dietaAuthorization;

	private record PlatilloIngestaContext(Dieta dieta, PlatilloIngesta platilloIngesta) {
	}

	private PlatilloIngestaContext loadPlatilloIngestaForMutation(@NonNull final Long dietaId,
			@NonNull final Long ingestaId, @NonNull final Long platilloIngestaId, final OidcUser principal) {
		if (principal == null) {
			return null;
		}
		final String userId = principal.getSubject();
		if (userId == null) {
			return null;
		}
		final Dieta dieta = dietaAuthorization.resolveForMutation(dietaId, userId, principal, dietaService);
		if (dieta == null) {
			return null;
		}
		dietaAuthorization.verifyCanModify(dieta, userId, principal);
		final PlatilloIngesta platilloIngesta = dieta.getIngestas()
			.stream()
			.filter(ingesta -> ingesta.getId().equals(ingestaId))
			.findFirst()
			.flatMap(ingesta -> ingesta.getPlatillos()
				.stream()
				.filter(platillo -> platillo.getId().equals(platilloIngestaId))
				.findFirst())
			.orElse(null);
		if (platilloIngesta == null) {
			return null;
		}
		return new PlatilloIngestaContext(dieta, platilloIngesta);
	}

	@PostMapping("add")
	public ResponseEntity<ApiResponse<Dieta>> addIngrediente(@PathVariable @NonNull final Long dietaId,
			@PathVariable @NonNull final Long ingestaId, @PathVariable @NonNull final Long platilloIngestaId,
			@RequestBody @NonNull final IngredienteFormModel ingrediente,
			@AuthenticationPrincipal final OidcUser principal) {
		log.info("Adding ingredient to platillo ingesta {} in dieta {}", platilloIngestaId, dietaId);
		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		final PlatilloIngestaContext context = loadPlatilloIngestaForMutation(dietaId, ingestaId, platilloIngestaId,
				principal);
		if (context == null) {
			return ResponseEntity.notFound().build();
		}
		final Long alimentoId = ingrediente.getAlimentoId();
		final String cantidad = ingrediente.getCantidad();
		final Integer peso = ingrediente.getPeso();
		if (alimentoId == null || cantidad == null || peso == null) {
			return ResponseEntity.badRequest().build();
		}
		final IngredientePlatilloIngesta result = dietaService.addIngredientePlatilloIngesta(context.platilloIngesta(),
				alimentoId, cantidad, peso);
		if (result == null) {
			return ResponseEntity.notFound().build();
		}
		final Dieta saved = dietaService.saveDieta(context.dieta());
		dietaAuthorization.auditSystemDietMutationIfNeeded(principal, saved, "dietas.platillos.ingredientes.add");
		return ResponseEntity.ok(new ApiResponse<>(saved));
	}

	@PutMapping("{ingredienteId}")
	public ResponseEntity<ApiResponse<Dieta>> updateIngrediente(@PathVariable @NonNull final Long dietaId,
			@PathVariable @NonNull final Long ingestaId, @PathVariable @NonNull final Long platilloIngestaId,
			@PathVariable @NonNull final Long ingredienteId,
			@RequestBody @NonNull final IngredienteFormModel ingrediente,
			@AuthenticationPrincipal final OidcUser principal) {
		log.info("Updating ingredient {} on platillo ingesta {} in dieta {}", ingredienteId, platilloIngestaId,
				dietaId);
		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		final PlatilloIngestaContext context = loadPlatilloIngestaForMutation(dietaId, ingestaId, platilloIngestaId,
				principal);
		if (context == null) {
			return ResponseEntity.notFound().build();
		}
		final String cantidad = ingrediente.getCantidad();
		final Integer peso = ingrediente.getPeso();
		if (cantidad == null || cantidad.isBlank() || peso == null) {
			return ResponseEntity.badRequest().build();
		}
		final boolean exists = context.platilloIngesta()
			.getIngredientes()
			.stream()
			.anyMatch(ing -> ing.getId().equals(ingredienteId));
		if (!exists) {
			return ResponseEntity.notFound().build();
		}
		dietaService.updateIngredientePlatilloIngesta(context.platilloIngesta(), ingredienteId, cantidad, peso);
		final Dieta saved = dietaService.saveDieta(context.dieta());
		dietaAuthorization.auditSystemDietMutationIfNeeded(principal, saved, "dietas.platillos.ingredientes.update");
		return ResponseEntity.ok(new ApiResponse<>(saved));
	}

	@DeleteMapping("{ingredienteId}")
	public ResponseEntity<ApiResponse<Dieta>> deleteIngrediente(@PathVariable @NonNull final Long dietaId,
			@PathVariable @NonNull final Long ingestaId, @PathVariable @NonNull final Long platilloIngestaId,
			@PathVariable @NonNull final Long ingredienteId, @AuthenticationPrincipal final OidcUser principal) {
		log.info("Deleting ingredient {} from platillo ingesta {} in dieta {}", ingredienteId, platilloIngestaId,
				dietaId);
		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		final PlatilloIngestaContext context = loadPlatilloIngestaForMutation(dietaId, ingestaId, platilloIngestaId,
				principal);
		if (context == null) {
			return ResponseEntity.notFound().build();
		}
		final boolean exists = context.platilloIngesta()
			.getIngredientes()
			.stream()
			.anyMatch(ing -> ing.getId().equals(ingredienteId));
		if (!exists) {
			return ResponseEntity.notFound().build();
		}
		dietaService.deleteIngredientePlatilloIngesta(context.platilloIngesta(), ingredienteId);
		final Dieta saved = dietaService.saveDieta(context.dieta());
		dietaAuthorization.auditSystemDietMutationIfNeeded(principal, saved, "dietas.platillos.ingredientes.delete");
		return ResponseEntity.ok(new ApiResponse<>(saved));
	}

	@Override
	@PostMapping("not-implemented")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		log.warn("Single-arg getPageArray should not be called on IngredientePlatilloIngestaRestController");
		return emptyPageArray(pagingRequest);
	}

	@PostMapping("data-table")
	public PageArray getIngredientPageArray(@RequestBody final PagingRequest pagingRequest,
			@PathVariable @NonNull final Long dietaId, @PathVariable @NonNull final Long ingestaId,
			@PathVariable @NonNull final Long platilloIngestaId, @AuthenticationPrincipal final OidcUser principal) {
		log.info("Ingredient data-table for platillo ingesta {} in dieta {}", platilloIngestaId, dietaId);
		final PlatilloIngestaContext context = loadPlatilloIngestaForMutation(dietaId, ingestaId, platilloIngestaId,
				principal);
		if (context == null) {
			return emptyPageArray(pagingRequest);
		}
		pagingRequest.setColumns(getColumns());
		final Page<IngredientePlatilloIngesta> page = getPage(pagingRequest,
				context.platilloIngesta().getIngredientes());
		final PageArray pageArray = new PageArray();
		final boolean canModify = dietaAuthorization.canModify(context.dieta(), principal.getSubject(), principal);
		pageArray
			.setData(page.getData().stream().map(row -> toStringList(row, canModify)).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		return pageArray;
	}

	private PageArray emptyPageArray(final PagingRequest pagingRequest) {
		final PageArray pageArray = new PageArray();
		pageArray.setData(List.of());
		pageArray.setDraw(pagingRequest.getDraw());
		pageArray.setRecordsFiltered(0);
		pageArray.setRecordsTotal(0);
		return pageArray;
	}

	@Override
	protected List<IngredientePlatilloIngesta> getData() {
		return List.of();
	}

	@Override
	protected Page<IngredientePlatilloIngesta> getRows(final PagingRequest pagingRequest) {
		log.warn("getRows without platillo context should not be called");
		return getPage(pagingRequest, List.of());
	}

	@Override
	protected List<String> toStringList(final IngredientePlatilloIngesta row) {
		return toStringList(row, true);
	}

	private List<String> toStringList(final IngredientePlatilloIngesta row, final boolean canModify) {
		final String actions = canModify
				? "<button type='button' class='btn action-btn btn-primary btn-sm edit-ingrediente-btn' " + "data-id='"
						+ row.getId() + "' data-cantidad='" + row.getFractionalCantSugerida() + "' data-peso='"
						+ row.getPesoNeto() + "' data-alimento-id='" + row.getAlimento().getId()
						+ "' data-alimento-nombre='" + row.getAlimento().getNombreAlimento()
						+ "' title='Editar'><i class='fas fa-edit fa-sm fa-fw'></i></button> "
						+ "<button type='button' class='btn action-btn btn-danger btn-sm delete-ingrediente-btn' "
						+ "data-id='" + row.getId() + "' title='Eliminar'>"
						+ "<i class='fas fa-trash fa-sm fa-fw'></i></button>"
				: "";
		return Arrays.asList(row.getAlimento().getNombreAlimento(), buildCantidadCell(row, canModify), row.getUnidad(),
				row.getPesoNeto().toString(), actions);
	}

	private String buildCantidadCell(final IngredientePlatilloIngesta row, final boolean canModify) {
		if (!canModify) {
			return row.getDisplayCantSugerida(row.getUnidad());
		}
		final Alimento alimento = row.getAlimento();
		return "<input type='text' class='form-control form-control-sm inline-platillo-ingesta-cantidad-input' "
				+ "data-id='" + row.getId() + "' data-alimento-cant='" + alimento.getCantSugerida()
				+ "' data-alimento-peso='" + alimento.getPesoNeto() + "' value='" + row.getFractionalCantSugerida()
				+ "' aria-label='Cantidad' />";
	}

	@Override
	protected List<Column> getColumns() {
		return Arrays.asList(new Column("ingrediente"), new Column("cantidad"), new Column("unidad"),
				new Column("peso"), new Column("acciones"));
	}

	@Override
	protected Predicate<IngredientePlatilloIngesta> getPredicate(final String value) {
		return ingrediente -> ingrediente.getAlimento().getNombreAlimento().toLowerCase().contains(value.toLowerCase());
	}

	@Override
	protected Comparator<IngredientePlatilloIngesta> getComparator(final String column, final Direction dir) {
		return IngredientePlatilloIngestaComparators.getComparator(column, dir);
	}

}
