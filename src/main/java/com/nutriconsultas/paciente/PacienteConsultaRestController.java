package com.nutriconsultas.paciente;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.charts.ChartResponse;
import com.nutriconsultas.consulta.Consulta;
import com.nutriconsultas.consulta.ConsultaComparators;
import com.nutriconsultas.consulta.ConsultaRepository;
import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/pacientes/{id}/consultas")
@Slf4j
public class PacienteConsultaRestController extends AbstractGridController<Consulta> {

	@Autowired
	private ConsultaRepository repo;

	@GetMapping("charts/imc")
	public ChartResponse imcChart(@PathVariable Long id) {
		log.info("starting imcChart with id {}.", id);
		// Implement from repository
		ChartResponse response = new ChartResponse();
		List<String> labels = new ArrayList<>();
		List<String> imc = new ArrayList<>();
		List<Consulta> consultasPaciente = repo.findByPacienteId(id)
			.stream()
			.sorted(Comparator.comparing(Consulta::getFechaConsulta))
			.collect(Collectors.toList());
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		// TODO: Add niveles IMC
		// NivelPeso np = imc > 30.0d ? NivelPeso.SOBREPESO
		// : imc > 25.0d ? NivelPeso.ALTO : imc > 18.5d ? NivelPeso.NORMAL :
		// NivelPeso.BAJO;

		for (Consulta consulta : consultasPaciente) {
			labels.add(dateFormat.format(consulta.getFechaConsulta()));
			imc.add(String.format("%.2f", consulta.getImc()));
		}

		response.setLabels(labels);
		Map<String, Object> data = new HashMap<>();
		data.put("imc", imc);
		response.setData(data);
		log.info("finish imcChart with response {}.", response);
		return response;
	}

	@Override
	protected List<String> toStringList(Consulta row) {
		log.debug("converting Consulta row {} to string list.", row);
		DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
		return Arrays.asList(row.getFechaConsulta() != null ? dateFormat.format(row.getFechaConsulta()) : "", //
				row.getPeso().toString(), //
				row.getEstatura().toString(), //
				row.getImc() != null ? String.format("%.2f", row.getImc()) : "",
				row.getSistolica() != null && row.getDiastolica() != null
						? row.getSistolica().toString() + "/" + row.getDiastolica().toString() : "-", //
				row.getIndiceGlucemico() != null ? row.getIndiceGlucemico().toString() : "-", //
				"<a href='#'' class='btn action-btn btn-danger btn-sm delete-btn' data-id='" + row.getId()
						+ "'><i class='fas fa-trash fa-sm fa-fw'></i> </a>");
	}

	@Override
	protected List<Consulta> getData() {
		log.debug("getting all Consulta records.");
		return StreamSupport.stream(repo.findAll().spliterator(), false).collect(Collectors.toList());
	}

	@Override
	protected Comparator<Consulta> getComparator(String column, Direction dir) {
		log.debug("getting Consulta comparator with column {} and direction {}.", column, dir);
		return ConsultaComparators.getComparator(column, dir);
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting Consulta columns.");
		return Stream.of("fecha", "peso", "estatura", "imc", "presion", "indGluc", "actions")
			.map(Column::new)
			.collect(Collectors.toList());
	}

	@Override
	protected Predicate<Consulta> getPredicate(String value) {
		log.debug("getting Consulta predicate with value {}.", value);
		return row -> row.getFechaConsulta().toString().toLowerCase().contains(value)
				|| row.getPeso().toString().toLowerCase().contains(value)
				|| row.getEstatura().toString().toLowerCase().contains(value)
				|| row.getImc().toString().toLowerCase().contains(value)
				|| row.getSistolica().toString().toLowerCase().contains(value)
				|| row.getDiastolica().toString().toLowerCase().contains(value)
				|| row.getIndiceGlucemico().toString().toLowerCase().contains(value);
	}

}
