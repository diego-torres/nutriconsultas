package com.nutriconsultas.alimentos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/alimentos")
@Slf4j
public class AlimentoRestController extends AbstractGridController<Alimento> {

	@Autowired
	private AlimentoService service;

	@GetMapping("{id}")
	public Alimento get(@PathVariable @NonNull Long id) {
		log.info("starting get with id {}.", id);
		Alimento alimento = service.findById(id);
		log.info("finish get with alimento {}.", alimento);
		return alimento;
	}

	@Override
	protected List<String> toStringList(Alimento row) {
		log.debug("converting Alimento row {} to string list.", row);
		return Arrays.asList("<a href='/admin/alimentos/" + row.getId() + "'>" + row.getNombreAlimento() + "</a>",
				row.getClasificacion(), //
				row.getFractionalCantSugerida(), //
				row.getUnidad(), //
				row.getPesoBrutoRedondeado().toString(), //
				row.getPesoNeto().toString(), //
				row.getEnergia().toString(), //
				String.format("%.1f", row.getProteina()), //
				String.format("%.1f", row.getLipidos()), //
				String.format("%.1f", row.getHidratosDeCarbono()));
	}

	@Override
	protected List<Alimento> getData() {
		log.debug("getting all Alimento records.");
		return service.findAll();
	}

	@Override
	protected Predicate<Alimento> getPredicate(String value) {
		log.debug("getting Alimento predicate with value {}.", value);
		return row -> row.getNombreAlimento().toLowerCase().contains(value)
				|| row.getNombreAlimento().toLowerCase().startsWith(value)
				|| row.getClasificacion().toLowerCase().contains(value)
				|| row.getClasificacion().toLowerCase().startsWith(value);
	}

	@Override
	protected Comparator<Alimento> getComparator(String column, com.nutriconsultas.dataTables.paging.Direction dir) {
		log.debug("getting Alimento comparator with column {} and direction {}.", column, dir);
		return AlimentoComparators.getComparator(column, dir);
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting Alimento columns.");
		return Stream.of("alimento", "grupo", "cantidad", "unidad", "bruto", "neto", "kcal", "prot", "lip", "hc")
			.map(Column::new)
			.collect(Collectors.toList());
	}

}
