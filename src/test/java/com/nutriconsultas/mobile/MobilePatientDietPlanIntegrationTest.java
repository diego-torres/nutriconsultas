package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.dieta.AlimentoIngesta;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaPdfService;
import com.nutriconsultas.dieta.DietaRepository;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngestaRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobilePatientDietPlanIntegrationTest {

	private static final String LINKED_SUB = "auth0|mobile-diet-plan-integration";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private DietaRepository dietaRepository;

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	@Autowired
	private AlimentosRepository alimentosRepository;

	@MockBean
	private DietaPdfService dietaPdfService;

	private Paciente linkedPaciente;

	private PacienteDieta linkedAssignment;

	private Long linkedPlatilloId;

	@Autowired
	private PlatilloIngestaRepository platilloIngestaRepository;

	@BeforeEach
	void seedData() {
		linkedPaciente = pacienteRepository.findByPatientAuthSub(LINKED_SUB).orElseGet(() -> {
			final Paciente paciente = samplePaciente(LINKED_SUB);
			return pacienteRepository.saveAndFlush(paciente);
		});
		linkedAssignment = ensureAssignmentWithMealTree();
		linkedPlatilloId = platilloIngestaRepository
			.findByPatientAssignment(linkedAssignment.getId(), linkedPaciente.getId())
			.get(0)
			.getId();
	}

	private PacienteDieta ensureAssignmentWithMealTree() {
		for (final PacienteDieta assignment : pacienteDietaRepository.findByPacienteId(linkedPaciente.getId())) {
			if ("Plan activo de prueba con arbol".equals(assignment.getNotes())) {
				return assignment;
			}
		}
		return createAssignmentWithMealTree();
	}

	private PacienteDieta createAssignmentWithMealTree() {
		final Dieta dieta = new Dieta();
		dieta.setNombre("Dieta integración");
		dieta.setUserId("nutritionist-sub");
		dieta.setEnergia(1900);
		dieta.setProteina(95.0);
		dieta.setLipidos(65.0);
		dieta.setHidratosDeCarbono(210.0);

		final Ingesta ingesta = new Ingesta("Desayuno");
		ingesta.setDieta(dieta);
		ingesta.setEnergia(400);
		ingesta.setProteina(18.0);
		ingesta.setLipidos(10.0);
		ingesta.setHidratosDeCarbono(50.0);

		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setName("Huevos revueltos");
		platillo.setPortions(1);
		platillo.setEnergia(280);
		platillo.setProteina(18.0);
		platillo.setHidratosDeCarbono(4.0);
		platillo.setLipidos(20.0);
		platillo.setRecommendations("Con verduras");
		platillo.setVideoUrl("https://video.example/huevos");
		platillo.setPdfUrl("/uploads/recetas/huevos.pdf");
		platillo.setPdfUrl("/uploads/recetas/huevos.pdf");
		platillo.setIngesta(ingesta);

		final Alimento catalogAlimento = new Alimento();
		catalogAlimento.setNombreAlimento("Huevo");
		catalogAlimento.setClasificacion("Proteínas");
		catalogAlimento.setCantSugerida(1.0);
		catalogAlimento.setUnidad("pieza");
		final Alimento savedAlimento = alimentosRepository.saveAndFlush(catalogAlimento);

		final IngredientePlatilloIngesta ingrediente = new IngredientePlatilloIngesta();
		ingrediente.setAlimento(savedAlimento);
		ingrediente.setCantSugerida(2.0);
		ingrediente.setUnidad("pieza");
		ingrediente.setPlatillo(platillo);
		platillo.setIngredientes(new java.util.ArrayList<>(List.of(ingrediente)));

		final AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setName("Pan integral");
		alimento.setPortions(2);
		alimento.setEnergia(120);
		alimento.setUnidad("rebanada");
		alimento.setIngesta(ingesta);

		ingesta.setPlatillos(List.of(platillo));
		ingesta.setAlimentos(List.of(alimento));
		dieta.setIngestas(List.of(ingesta));

		final Dieta savedDieta = dietaRepository.saveAndFlush(dieta);

		final PacienteDieta assignment = new PacienteDieta();
		assignment.setPaciente(linkedPaciente);
		assignment.setDieta(savedDieta);
		assignment.setStatus(PacienteDietaStatus.ACTIVE);
		assignment
			.setStartDate(Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		assignment.setNotes("Plan activo de prueba con arbol");
		return pacienteDietaRepository.saveAndFlush(assignment);
	}

	@Test
	void listDietPlansWithLinkedJwtReturnsPagedSummaries() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/diet-plans").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].dietaName").value("Dieta integración"))
			.andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.content[0].totalKcal").value(1900))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void listDietPlansActiveOnlyFiltersResults() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/diet-plans").param("activeOnly", "true").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
	}

	@Test
	void getDietPlanDetailWithLinkedJwtReturnsStructuredMealTree() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/diet-plans/" + linkedAssignment.getId()).with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.assignmentId").value(linkedAssignment.getId()))
			.andExpect(jsonPath("$.data.dietaName").value("Dieta integración"))
			.andExpect(jsonPath("$.data.totalKcal").value(1900))
			.andExpect(jsonPath("$.data.ingestas[0].tipo").value("Desayuno"))
			.andExpect(jsonPath("$.data.ingestas[0].platillos[0].id").exists())
			.andExpect(jsonPath("$.data.ingestas[0].platillos[0].nombre").value("Huevos revueltos"))
			.andExpect(jsonPath("$.data.ingestas[0].platillos[0].porciones").value(1))
			.andExpect(jsonPath("$.data.ingestas[0].platillos[0].proteina").value(18.0))
			.andExpect(jsonPath("$.data.ingestas[0].platillos[0].carbohidratos").value(4.0))
			.andExpect(jsonPath("$.data.ingestas[0].platillos[0].grasas").value(20.0))
			.andExpect(jsonPath("$.data.ingestas[0].alimentos[0].nombre").value("Pan integral"))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void getGroceryListWithLinkedJwtReturnsAggregatedItems() throws Exception {
		mockMvc
			.perform(get("/rest/mobile/patient/diet-plans/" + linkedAssignment.getId() + "/grocery-list")
				.with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items").isArray())
			.andExpect(jsonPath("$.data.items[?(@.nombre == 'Huevo')].cantidad").value("2"))
			.andExpect(jsonPath("$.data.items[?(@.nombre == 'Huevo')].unidad").value("pieza"))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void getGroceryListForOtherPatientsAssignmentReturnsNotFound() throws Exception {
		final Paciente otherPatient = pacienteRepository.findByPatientAuthSub("auth0|mobile-diet-plan-other")
			.orElseGet(() -> {
				final Paciente paciente = samplePaciente("auth0|mobile-diet-plan-other");
				return pacienteRepository.saveAndFlush(paciente);
			});
		final PacienteDieta otherAssignment = pacienteDietaRepository.findByPacienteId(otherPatient.getId())
			.stream()
			.findFirst()
			.orElseGet(() -> {
				final Dieta dieta = new Dieta();
				dieta.setNombre("Dieta ajena");
				dieta.setUserId("nutritionist-sub");
				dieta.setEnergia(1500);
				final Dieta savedDieta = dietaRepository.saveAndFlush(dieta);

				final PacienteDieta assignment = new PacienteDieta();
				assignment.setPaciente(otherPatient);
				assignment.setDieta(savedDieta);
				assignment.setStatus(PacienteDietaStatus.ACTIVE);
				assignment.setStartDate(
						Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
				return pacienteDietaRepository.saveAndFlush(assignment);
			});

		mockMvc
			.perform(get("/rest/mobile/patient/diet-plans/" + otherAssignment.getId() + "/grocery-list")
				.with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNotFound());
	}

	@Test
	void getPlatilloDetailWithLinkedJwtReturnsIngredientsAndNutrition() throws Exception {
		mockMvc
			.perform(get(
					"/rest/mobile/patient/diet-plans/" + linkedAssignment.getId() + "/platillos/" + linkedPlatilloId)
				.with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(linkedPlatilloId))
			.andExpect(jsonPath("$.data.nombre").value("Huevos revueltos"))
			.andExpect(jsonPath("$.data.description").value("Con verduras"))
			.andExpect(jsonPath("$.data.videoUrl").value("https://video.example/huevos"))
			.andExpect(jsonPath("$.data.pdfUrl").value("/uploads/recetas/huevos.pdf"))
			.andExpect(jsonPath("$.data.ingredientes[0].nombre").value("Huevo"))
			.andExpect(jsonPath("$.data.ingredientes[0].cantidad").value("2"))
			.andExpect(jsonPath("$.data.ingredientes[0].unidad").value("pieza"))
			.andExpect(jsonPath("$.data.nutritionFacts.kcal").value(280))
			.andExpect(jsonPath("$.data.nutritionFacts.proteina").value(18.0))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void getPlatilloDetailForOtherPatientsAssignmentReturnsNotFound() throws Exception {
		final Paciente otherPatient = pacienteRepository.findByPatientAuthSub("auth0|mobile-diet-plan-other")
			.orElseGet(() -> {
				final Paciente paciente = samplePaciente("auth0|mobile-diet-plan-other");
				return pacienteRepository.saveAndFlush(paciente);
			});
		final PacienteDieta otherAssignment = pacienteDietaRepository.findByPacienteId(otherPatient.getId())
			.stream()
			.findFirst()
			.orElseGet(() -> {
				final Dieta dieta = new Dieta();
				dieta.setNombre("Dieta ajena");
				dieta.setUserId("nutritionist-sub");
				dieta.setEnergia(1500);
				final Dieta savedDieta = dietaRepository.saveAndFlush(dieta);

				final PacienteDieta assignment = new PacienteDieta();
				assignment.setPaciente(otherPatient);
				assignment.setDieta(savedDieta);
				assignment.setStatus(PacienteDietaStatus.ACTIVE);
				assignment.setStartDate(
						Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
				return pacienteDietaRepository.saveAndFlush(assignment);
			});

		mockMvc
			.perform(
					get("/rest/mobile/patient/diet-plans/" + otherAssignment.getId() + "/platillos/" + linkedPlatilloId)
						.with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNotFound());
	}

	@Test
	void getPlatilloDetailForMissingPlatilloReturnsNotFound() throws Exception {
		mockMvc
			.perform(get("/rest/mobile/patient/diet-plans/" + linkedAssignment.getId() + "/platillos/999999")
				.with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNotFound());
	}

	@Test
	void getDietPlanDetailForOtherPatientsAssignmentReturnsNotFound() throws Exception {
		final Paciente otherPatient = pacienteRepository.findByPatientAuthSub("auth0|mobile-diet-plan-other")
			.orElseGet(() -> {
				final Paciente paciente = samplePaciente("auth0|mobile-diet-plan-other");
				return pacienteRepository.saveAndFlush(paciente);
			});
		final PacienteDieta otherAssignment = pacienteDietaRepository.findByPacienteId(otherPatient.getId())
			.stream()
			.findFirst()
			.orElseGet(() -> {
				final Dieta dieta = new Dieta();
				dieta.setNombre("Dieta ajena");
				dieta.setUserId("nutritionist-sub");
				dieta.setEnergia(1500);
				final Dieta savedDieta = dietaRepository.saveAndFlush(dieta);

				final PacienteDieta assignment = new PacienteDieta();
				assignment.setPaciente(otherPatient);
				assignment.setDieta(savedDieta);
				assignment.setStatus(PacienteDietaStatus.ACTIVE);
				assignment.setStartDate(
						Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
				return pacienteDietaRepository.saveAndFlush(assignment);
			});

		mockMvc.perform(get("/rest/mobile/patient/diet-plans/" + otherAssignment.getId()).with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNotFound());
	}

	@Test
	void getDietPlanDetailForMissingAssignmentReturnsNotFound() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/diet-plans/999999").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNotFound());
	}

	@Test
	void getDietPlanPdfForMissingAssignmentReturnsNotFound() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/diet-plans/999999/pdf").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNotFound());
	}

	@Test
	void getDietPlanPdfWithLinkedJwtReturnsPdfAttachment() throws Exception {
		when(dietaPdfService.generatePdfForAssignment(any(PacienteDieta.class)))
			.thenReturn(new byte[] { 37, 80, 68, 70 });

		mockMvc
			.perform(get("/rest/mobile/patient/diet-plans/" + linkedAssignment.getId() + "/pdf")
				.with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF))
			.andExpect(
					header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Dieta integración.pdf\""));
	}

	@Test
	void getDietPlanPdfForOtherPatientsAssignmentReturnsNotFound() throws Exception {
		final Paciente otherPatient = pacienteRepository.findByPatientAuthSub("auth0|mobile-diet-plan-other")
			.orElseGet(() -> {
				final Paciente paciente = samplePaciente("auth0|mobile-diet-plan-other");
				return pacienteRepository.saveAndFlush(paciente);
			});
		final PacienteDieta otherAssignment = pacienteDietaRepository.findByPacienteId(otherPatient.getId())
			.stream()
			.findFirst()
			.orElseGet(() -> {
				final Dieta dieta = new Dieta();
				dieta.setNombre("Dieta ajena");
				dieta.setUserId("nutritionist-sub");
				dieta.setEnergia(1500);
				final Dieta savedDieta = dietaRepository.saveAndFlush(dieta);

				final PacienteDieta assignment = new PacienteDieta();
				assignment.setPaciente(otherPatient);
				assignment.setDieta(savedDieta);
				assignment.setStatus(PacienteDietaStatus.ACTIVE);
				assignment.setStartDate(
						Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
				return pacienteDietaRepository.saveAndFlush(assignment);
			});

		mockMvc
			.perform(get("/rest/mobile/patient/diet-plans/" + otherAssignment.getId() + "/pdf")
				.with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isNotFound());
	}

	private static Paciente samplePaciente(final String patientAuthSub) {
		final Paciente paciente = new Paciente();
		paciente.setName("Mobile Diet Plan Patient");
		paciente.setUserId("nutritionist-sub");
		paciente.setPatientAuthSub(patientAuthSub);
		final LocalDate dob = LocalDate.now().minusYears(28);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("M");
		return paciente;
	}

}
