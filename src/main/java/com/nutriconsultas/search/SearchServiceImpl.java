package com.nutriconsultas.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

	private static final int PAGE_SIZE = 20;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private AlimentosRepository alimentosRepository;

	@Autowired
	private PlatilloRepository platilloRepository;

	@Autowired
	private CalendarEventRepository calendarEventRepository;

	@Autowired
	private ClinicalExamRepository clinicalExamRepository;

	@Override
	@Transactional(readOnly = true)
	public SearchResponse search(@NonNull final String query, @NonNull final String userId,
			@NonNull final String category, final int page) {
		log.info("Searching for '{}' for userId {}, category: {}, page: {}", query, userId, category, page);
		final String searchTerm = "%" + query.toLowerCase() + "%";
		final int pageNumber = Math.max(1, page);

		// Search Pacientes (filtered by userId)
		final List<Paciente> allPacientes = pacienteRepository.findByUserIdAndSearchTerm(userId, searchTerm);
		final List<SearchResult> allPacienteResults = allPacientes.stream()
			.map(p -> new SearchResult(SearchResultType.PACIENTE, p.getId(), p.getName(),
					buildPacienteDescription(p), "/admin/pacientes/" + p.getId()))
			.collect(Collectors.toList());
		final PaginatedSearchResults pacientes = paginateResults(allPacienteResults, pageNumber, "pacientes"
				.equals(category));

		// Search Alimentos
		final List<Alimento> allAlimentos = alimentosRepository.findByNombreAlimentoContainingIgnoreCase(query);
		final List<SearchResult> allAlimentoResults = allAlimentos.stream()
			.map(a -> new SearchResult(SearchResultType.ALIMENTO, a.getId(), a.getNombreAlimento(),
					"Clasificación: " + a.getClasificacion(), "/admin/alimentos"))
			.collect(Collectors.toList());
		final PaginatedSearchResults alimentos = paginateResults(allAlimentoResults, pageNumber, "alimentos"
				.equals(category));

		// Search Platillos
		final List<Platillo> allPlatillos = platilloRepository.findByNameContainingIgnoreCase(query);
		final List<SearchResult> allPlatilloResults = allPlatillos.stream()
			.map(p -> new SearchResult(SearchResultType.PLATILLO, p.getId(), p.getName(),
					p.getDescription() != null ? p.getDescription() : "", "/admin/platillos"))
			.collect(Collectors.toList());
		final PaginatedSearchResults platillos = paginateResults(allPlatilloResults, pageNumber, "platillos"
				.equals(category));

		// Search Calendar Events (filtered by userId through paciente)
		final List<CalendarEvent> allCalendarEvents = calendarEventRepository
			.findByUserIdAndSearchTerm(userId, searchTerm);
		final List<SearchResult> allCalendarEventResults = allCalendarEvents.stream()
			.map(e -> new SearchResult(SearchResultType.CALENDAR_EVENT, e.getId(), e.getTitle(),
					buildCalendarEventDescription(e), "/admin/calendar"))
			.collect(Collectors.toList());
		final PaginatedSearchResults calendarEvents = paginateResults(allCalendarEventResults, pageNumber,
				"calendarevents".equals(category));

		// Search Clinical Exams (filtered by userId through paciente)
		final List<ClinicalExam> allClinicalExams = clinicalExamRepository.findByUserIdAndSearchTerm(userId, searchTerm);
		final List<SearchResult> allClinicalExamResults = allClinicalExams.stream()
			.map(e -> new SearchResult(SearchResultType.CLINICAL_EXAM, e.getId(), e.getTitle(),
					buildClinicalExamDescription(e), "/admin/pacientes/" + e.getPaciente().getId() + "/examenes"))
			.collect(Collectors.toList());
		final PaginatedSearchResults clinicalExams = paginateResults(allClinicalExamResults, pageNumber,
				"clinicalexams".equals(category));

		final int totalResults = pacientes.getTotalCount() + alimentos.getTotalCount() + platillos.getTotalCount()
				+ calendarEvents.getTotalCount() + clinicalExams.getTotalCount();

		log.info("Search completed. Found {} total results", totalResults);

		return new SearchResponse(query, pacientes, alimentos, platillos, calendarEvents, clinicalExams, totalResults);
	}

	private PaginatedSearchResults paginateResults(final List<SearchResult> allResults, final int page,
			final boolean isActiveCategory) {
		final int totalCount = allResults.size();
		final int pageNumber = isActiveCategory ? page : 1;
		final int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
		final int startIndex = (pageNumber - 1) * PAGE_SIZE;
		final int endIndex = Math.min(startIndex + PAGE_SIZE, totalCount);
		final List<SearchResult> paginatedResults = startIndex < totalCount
				? allResults.subList(startIndex, endIndex)
				: new ArrayList<>();

		return new PaginatedSearchResults(paginatedResults, totalCount, pageNumber, PAGE_SIZE, totalPages);
	}

	private String buildPacienteDescription(final Paciente paciente) {
		final List<String> parts = new ArrayList<>();
		if (paciente.getEmail() != null && !paciente.getEmail().isEmpty()) {
			parts.add("Email: " + paciente.getEmail());
		}
		if (paciente.getPhone() != null && !paciente.getPhone().isEmpty()) {
			parts.add("Tel: " + paciente.getPhone());
		}
		return String.join(" | ", parts);
	}

	private String buildCalendarEventDescription(final CalendarEvent event) {
		final List<String> parts = new ArrayList<>();
		if (event.getPaciente() != null) {
			parts.add("Paciente: " + event.getPaciente().getName());
		}
		if (event.getDescription() != null && !event.getDescription().isEmpty()) {
			final String desc = event.getDescription().length() > 100
					? event.getDescription().substring(0, 100) + "..."
					: event.getDescription();
			parts.add(desc);
		}
		return String.join(" | ", parts);
	}

	private String buildClinicalExamDescription(final ClinicalExam exam) {
		final List<String> parts = new ArrayList<>();
		if (exam.getPaciente() != null) {
			parts.add("Paciente: " + exam.getPaciente().getName());
		}
		if (exam.getSummaryNotes() != null && !exam.getSummaryNotes().isEmpty()) {
			final String notes = exam.getSummaryNotes().length() > 100
					? exam.getSummaryNotes().substring(0, 100) + "..."
					: exam.getSummaryNotes();
			parts.add(notes);
		}
		else if (exam.getDescription() != null && !exam.getDescription().isEmpty()) {
			final String desc = exam.getDescription().length() > 100
					? exam.getDescription().substring(0, 100) + "..."
					: exam.getDescription();
			parts.add(desc);
		}
		return String.join(" | ", parts);
	}

}
