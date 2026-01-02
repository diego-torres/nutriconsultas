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
import org.springframework.lang.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamService;
import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/pacientes/{id}/examenes-clinicos")
@Slf4j
public class ClinicalExamRestController extends AbstractGridController<ClinicalExam> {

	@Autowired
	private ClinicalExamService clinicalExamService;

	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest,
			@PathVariable @NonNull final Long id) {
		log.info("starting getPageArray with pagingRequest: {} for paciente id: {}", pagingRequest, id);
		pagingRequest.setColumns(getColumns());
		final Page<ClinicalExam> page = getRows(pagingRequest, id);
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
		log.warn("getPageArray() called on ClinicalExamRestController without id. This should not be called.");
		return null;
	}

	@Override
	protected List<String> toStringList(final ClinicalExam row) {
		log.debug("converting ClinicalExam row {} to string list.", row);
		final DateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
		// Format the 5 most important indicators
		final String peso = row.getPeso() != null ? String.format("%.1f kg", row.getPeso()) : "-";
		final String imc = row.getImc() != null ? String.format("%.1f", row.getImc()) : "-";
		final String glucosa = row.getGlucosa() != null ? String.format("%.0f mg/dL", row.getGlucosa()) : "-";
		final String colesterolTotal = row.getColesterolTotal() != null
				? String.format("%.0f mg/dL", row.getColesterolTotal()) : "-";
		final String hemoglobina = row.getHemoglobina() != null ? String.format("%.1f g/dL", row.getHemoglobina())
				: "-";
		return Arrays.asList(row.getExamDateTime() != null ? dateTimeFormat.format(row.getExamDateTime()) : "",
				"<a href='/admin/pacientes/" + row.getPaciente().getId() + "/examen-clinico/" + row.getId() + "'>"
						+ row.getTitle() + "</a>",
				peso, imc, glucosa, colesterolTotal, hemoglobina,
				"<a href='#' class='btn action-btn btn-danger btn-sm delete-exam-btn' data-id='" + row.getId()
						+ "'><i class='fas fa-trash fa-sm fa-fw'></i> </a>");
	}

	@Override
	protected List<ClinicalExam> getData() {
		log.debug("getting ClinicalExam records for paciente.");
		// This method is not used when we override getRows
		return clinicalExamService.findAll();
	}

	protected Page<ClinicalExam> getRows(final PagingRequest pagingRequest, @NonNull final Long pacienteId) {
		log.debug("getting ClinicalExam rows filtered by pacienteId: {}", pacienteId);
		final List<ClinicalExam> exams = clinicalExamService.findByPacienteId(pacienteId);
		return getPage(pagingRequest, exams);
	}

	@Override
	protected Page<ClinicalExam> getRows(final PagingRequest pagingRequest) {
		log.debug("getting ClinicalExam rows (should not be called, use getRows with pacienteId)");
		return getPage(pagingRequest, clinicalExamService.findAll());
	}

	@Override
	protected Comparator<ClinicalExam> getComparator(final String column, final Direction dir) {
		log.debug("getting ClinicalExam comparator with column {} and direction {}.", column, dir);
		final Comparator<ClinicalExam> comparator;
		// Default comparator for examDateTime/fecha and unknown columns
		final Comparator<ClinicalExam> defaultComparator = Comparator.comparing(ClinicalExam::getExamDateTime,
				Comparator.nullsLast(Date::compareTo));
		switch (column) {
			case "title":
			case "titulo":
				comparator = Comparator.comparing(ClinicalExam::getTitle,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
				break;
			case "peso":
				comparator = Comparator.comparing(ClinicalExam::getPeso, Comparator.nullsLast(Double::compareTo));
				break;
			case "imc":
				comparator = Comparator.comparing(ClinicalExam::getImc, Comparator.nullsLast(Double::compareTo));
				break;
			case "glucosa":
				comparator = Comparator.comparing(ClinicalExam::getGlucosa, Comparator.nullsLast(Double::compareTo));
				break;
			case "colesterolTotal":
				comparator = Comparator.comparing(ClinicalExam::getColesterolTotal,
						Comparator.nullsLast(Double::compareTo));
				break;
			case "hemoglobina":
				comparator = Comparator.comparing(ClinicalExam::getHemoglobina,
						Comparator.nullsLast(Double::compareTo));
				break;
			case "examDateTime":
			case "fecha":
			default:
				comparator = defaultComparator;
				break;
		}
		return dir == Direction.desc ? comparator.reversed() : comparator;
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting ClinicalExam columns.");
		return Stream.of("fecha", "titulo", "peso", "imc", "glucosa", "colesterolTotal", "hemoglobina", "actions")
			.map(Column::new)
			.collect(Collectors.toList());
	}

	@Override
	protected Predicate<ClinicalExam> getPredicate(final String value) {
		log.debug("getting ClinicalExam predicate with value {}.", value);
		final String lowerValue = value.toLowerCase();
		return row -> (row.getTitle() != null && row.getTitle().toLowerCase().contains(lowerValue))
				|| (row.getDescription() != null && row.getDescription().toLowerCase().contains(lowerValue))
				|| (row.getExamDateTime() != null
						&& new SimpleDateFormat("dd MMM yyyy HH:mm").format(row.getExamDateTime())
							.toLowerCase()
							.contains(lowerValue));
	}

	@DeleteMapping("/{examId}")
	public ResponseEntity<Map<String, Object>> deleteExam(@PathVariable @NonNull final Long id,
			@PathVariable @NonNull final Long examId) {
		log.debug("Deleting clinical exam {} for paciente {}", examId, id);
		try {
			final ClinicalExam exam = clinicalExamService.findById(examId);
			if (exam == null) {
				final Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("error", "Examen clínico no encontrado");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			if (!exam.getPaciente().getId().equals(id)) {
				final Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("error", "El examen clínico no pertenece al paciente especificado");
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
			}
			clinicalExamService.deleteById(examId);
			final Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Examen clínico eliminado correctamente");
			return ResponseEntity.ok(response);
		}
		catch (final Exception e) {
			log.error("Error deleting clinical exam", e);
			final Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

}
