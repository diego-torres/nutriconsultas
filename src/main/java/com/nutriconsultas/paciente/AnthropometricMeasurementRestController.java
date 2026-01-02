package com.nutriconsultas.paciente;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementService;
import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.util.LogRedaction;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/pacientes/{id}/antropometricos")
@Slf4j
public class AnthropometricMeasurementRestController extends AbstractGridController<AnthropometricMeasurement> {

	@Autowired
	private AnthropometricMeasurementService anthropometricMeasurementService;

	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest,
			@PathVariable @NonNull final Long id) {
		log.info("starting getPageArray with pagingRequest: {} for paciente id: {}", pagingRequest, id);
		pagingRequest.setColumns(getColumns());
		final Page<AnthropometricMeasurement> page = getRows(pagingRequest, id);
		log.debug("page with records: {}", page.getRecordsTotal());
		final PageArray pageArray = new PageArray();
		pageArray.setData(page.getData().stream().map(this::toStringList).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		log.info("returning data at getPageArray: {}", pageArray.getRecordsTotal());
		return pageArray;
	}

	@Override
	@PostMapping("not-implemented")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		log.warn(
				"getPageArray() called on AnthropometricMeasurementRestController without id. This should not be called.");
		return null;
	}

	@Override
	protected List<String> toStringList(final AnthropometricMeasurement row) {
		log.debug("converting AnthropometricMeasurement row {} to string list.",
				LogRedaction.redactAnthropometricMeasurement(row));
		final DateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
		final String peso = row.getPeso() != null ? String.format("%.1f kg", row.getPeso()) : "-";
		final String estatura = row.getEstatura() != null ? String.format("%.2f m", row.getEstatura()) : "-";
		final String imc = row.getImc() != null ? String.format("%.1f", row.getImc()) : "-";
		final String cintura = row.getCintura() != null ? String.format("%.1f cm", row.getCintura()) : "-";
		final String cadera = row.getCadera() != null ? String.format("%.1f cm", row.getCadera()) : "-";
		final String porcentajeGrasa = row.getPorcentajeGrasaCorporal() != null
				? String.format("%.1f%%", row.getPorcentajeGrasaCorporal()) : "-";
		return Arrays.asList(
				row.getMeasurementDateTime() != null ? dateTimeFormat.format(row.getMeasurementDateTime()) : "",
				"<a href='/admin/pacientes/" + row.getPaciente().getId() + "/antropometrico/" + row.getId() + "'>"
						+ row.getTitle() + "</a>",
				peso, estatura, imc, cintura, cadera, porcentajeGrasa,
				"<a href='#' class='btn action-btn btn-danger btn-sm delete-antropometrico-btn' data-id='" + row.getId()
						+ "'><i class='fas fa-trash fa-sm fa-fw'></i> </a>");
	}

	@Override
	protected List<AnthropometricMeasurement> getData() {
		log.debug("getting AnthropometricMeasurement records for paciente.");
		return anthropometricMeasurementService.findAll();
	}

	protected Page<AnthropometricMeasurement> getRows(final PagingRequest pagingRequest,
			@NonNull final Long pacienteId) {
		log.debug("getting AnthropometricMeasurement rows filtered by pacienteId: {}", pacienteId);
		final List<AnthropometricMeasurement> measurements = anthropometricMeasurementService
			.findByPacienteId(pacienteId);
		return getPage(pagingRequest, measurements);
	}

	@Override
	protected Page<AnthropometricMeasurement> getRows(final PagingRequest pagingRequest) {
		log.debug("getting AnthropometricMeasurement rows (should not be called, use getRows with pacienteId)");
		return getPage(pagingRequest, anthropometricMeasurementService.findAll());
	}

	@Override
	protected Comparator<AnthropometricMeasurement> getComparator(final String column, final Direction dir) {
		log.debug("getting AnthropometricMeasurement comparator with column {} and direction {}.", column, dir);
		final Comparator<AnthropometricMeasurement> comparator;
		// Default comparator for measurementDateTime/fecha and unknown columns
		final Comparator<AnthropometricMeasurement> defaultComparator = Comparator
				.comparing(AnthropometricMeasurement::getMeasurementDateTime, Comparator.nullsLast(Date::compareTo));
		switch (column) {
			case "title":
			case "titulo":
				comparator = Comparator.comparing(AnthropometricMeasurement::getTitle,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
				break;
			case "peso":
				comparator = Comparator.comparing(AnthropometricMeasurement::getPeso,
						Comparator.nullsLast(Double::compareTo));
				break;
			case "estatura":
				comparator = Comparator.comparing(AnthropometricMeasurement::getEstatura,
						Comparator.nullsLast(Double::compareTo));
				break;
			case "imc":
				comparator = Comparator.comparing(AnthropometricMeasurement::getImc,
						Comparator.nullsLast(Double::compareTo));
				break;
			case "cintura":
				comparator = Comparator.comparing(AnthropometricMeasurement::getCintura,
						Comparator.nullsLast(Double::compareTo));
				break;
			case "cadera":
				comparator = Comparator.comparing(AnthropometricMeasurement::getCadera,
						Comparator.nullsLast(Double::compareTo));
				break;
			case "porcentajeGrasaCorporal":
				comparator = Comparator.comparing(AnthropometricMeasurement::getPorcentajeGrasaCorporal,
						Comparator.nullsLast(Double::compareTo));
				break;
			case "measurementDateTime":
			case "fecha":
			default:
				comparator = defaultComparator;
				break;
		}
		return dir == Direction.desc ? comparator.reversed() : comparator;
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting AnthropometricMeasurement columns.");
		return Stream
			.of("fecha", "titulo", "peso", "estatura", "imc", "cintura", "cadera", "porcentajeGrasa", "actions")
			.map(Column::new)
			.collect(Collectors.toList());
	}

	@Override
	protected Predicate<AnthropometricMeasurement> getPredicate(final String value) {
		log.debug("getting AnthropometricMeasurement predicate with value {}.", value);
		final String lowerValue = value.toLowerCase();
		return row -> (row.getTitle() != null && row.getTitle().toLowerCase().contains(lowerValue))
				|| (row.getDescription() != null && row.getDescription().toLowerCase().contains(lowerValue))
				|| (row.getMeasurementDateTime() != null
						&& new SimpleDateFormat("dd MMM yyyy HH:mm").format(row.getMeasurementDateTime())
							.toLowerCase()
							.contains(lowerValue));
	}

	@DeleteMapping("/{measurementId}")
	public ResponseEntity<Map<String, Object>> deleteMeasurement(@PathVariable @NonNull final Long id,
			@PathVariable @NonNull final Long measurementId) {
		log.debug("Deleting anthropometric measurement {} for paciente {}", measurementId, id);
		try {
			final AnthropometricMeasurement measurement = anthropometricMeasurementService.findById(measurementId);
			if (measurement == null) {
				final Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("error", "Medición antropométrica no encontrada");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			if (!measurement.getPaciente().getId().equals(id)) {
				final Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("error", "La medición antropométrica no pertenece al paciente especificado");
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
			}
			anthropometricMeasurementService.deleteById(measurementId);
			final Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Medición antropométrica eliminada correctamente");
			return ResponseEntity.ok(response);
		}
		catch (final Exception e) {
			log.error("Error deleting anthropometric measurement", e);
			final Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

}
