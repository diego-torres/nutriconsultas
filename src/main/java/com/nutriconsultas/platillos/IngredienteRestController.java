package com.nutriconsultas.platillos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridItemController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("rest/platillos/{id}/ingredientes")
@Slf4j
public class IngredienteRestController extends AbstractGridItemController<Ingrediente> {

	@Autowired
	private PlatilloService platilloService;

	@DeleteMapping("/{ingredienteId}")
	public void delete(@NonNull @PathVariable final Long id, @NonNull @PathVariable final Long ingredienteId) {
		log.debug("deleting Ingrediente with id {}.", ingredienteId);
		platilloService.deleteIngrediente(id, ingredienteId);
	}

	@Override
	protected List<String> toStringList(final Ingrediente row) {
		log.debug("converting Ingrediente row {} to string list.", row);
		return Arrays.asList(row.getAlimento().getNombreAlimento(), row.getFractionalCantSugerida(), //
				row.getUnidad(), //
				row.getPesoNeto().toString(), //
				"<a href='#'' class='btn action-btn btn-danger btn-sm delete-btn' data-id='" + row.getId()
						+ "'><i class='fas fa-trash fa-sm fa-fw'></i> </a>");
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
