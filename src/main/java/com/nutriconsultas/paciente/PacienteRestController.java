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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/pacientes")
@Slf4j
public class PacienteRestController extends AbstractGridController<Paciente> {

	@Autowired
	private PacienteService service;

	@Override
	protected List<String> toStringList(Paciente row) {
		log.debug("converting Paciente row {} to string list.", row);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return Arrays.asList("<a href='/admin/pacientes/" + row.getId() + "'>" + row.getName() + "</a>",
				row.getDob() != null ? dateFormat.format(row.getDob()) : "", //
				row.getEmail(), //
				row.getPhone(), //
				row.getGender(), //
				row.getResponsibleName());
	}

	@Override
	protected List<Paciente> getData() {
		log.debug("getting all Paciente records.");
		return service.findAll();
	}

	@Override
	protected Predicate<Paciente> getPredicate(String value) {
		return row -> row.getName().toLowerCase().contains(value) || row.getName().toLowerCase().startsWith(value)
				|| row.getResponsibleName().toLowerCase().contains(value)
				|| row.getResponsibleName().toLowerCase().startsWith(value);
	}

	@Override
	protected Comparator<Paciente> getComparator(String column, Direction dir) {
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
