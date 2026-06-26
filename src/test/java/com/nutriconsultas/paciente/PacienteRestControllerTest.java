package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.ResponseEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;
import com.nutriconsultas.paciente.projection.PacienteListView;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class PacienteRestControllerTest {

	@InjectMocks
	private PacienteRestController controller;

	@Mock
	private PacienteService service;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private OidcUser principal;

	private Paciente paciente1;

	private Paciente paciente2;

	private static final String TEST_USER_ID = "test-user-id-123";

	@BeforeEach
	public void setup() {
		log.info("setting up PacienteRestController test");

		paciente1 = new Paciente();
		paciente1.setId(1L);
		paciente1.setName("Juan Perez");
		paciente1.setEmail("juan@example.com");
		paciente1.setPhone("1234567890");
		paciente1.setUserId(TEST_USER_ID);
		final LocalDate dob1 = LocalDate.now().minusYears(30);
		paciente1.setDob(Date.from(dob1.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente1.setGender("M");
		paciente1.setResponsibleName("Maria Perez");

		paciente2 = new Paciente();
		paciente2.setId(2L);
		paciente2.setName("Maria Garcia");
		paciente2.setEmail("maria@example.com");
		paciente2.setPhone("0987654321");
		paciente2.setUserId(TEST_USER_ID);
		final LocalDate dob2 = LocalDate.now().minusYears(25);
		paciente2.setDob(Date.from(dob2.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente2.setGender("F");
		paciente2.setResponsibleName("Carlos Garcia");

		// Setup SecurityContext
		org.mockito.Mockito.lenient().when(principal.getSubject()).thenReturn(TEST_USER_ID);
		org.mockito.Mockito.lenient()
			.when(securityContext.getAuthentication())
			.thenReturn(org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class));
		org.mockito.Mockito.lenient().when(securityContext.getAuthentication().getPrincipal()).thenReturn(principal);
		SecurityContextHolder.setContext(securityContext);

		log.info("finished setting up PacienteRestController test");
	}

	@Test
	public void testGetPageArray() {
		log.info("starting testGetPageArray");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente1, paciente2), pageable, 2);
		when(service.findListViewsByUserId(eq(TEST_USER_ID), any(Pageable.class)))
			.thenReturn(toListViewPage(springPage));
		when(service.countByUserId(eq(TEST_USER_ID))).thenReturn(2L);

		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		final PageArray result = controller.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(2);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(2);
		verify(service).findListViewsByUserId(eq(TEST_USER_ID), any(Pageable.class));
		verify(service, times(2)).countByUserId(eq(TEST_USER_ID));
		log.info("finished testGetPageArray");
	}

	@Test
	public void testGetPageArrayWithSearch() {
		log.info("starting testGetPageArrayWithSearch");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente1), pageable, 1);
		when(service.findListViewsByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"), any(Pageable.class)))
			.thenReturn(toListViewPage(springPage));
		when(service.countByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"))).thenReturn(1L);
		when(service.countByUserId(eq(TEST_USER_ID))).thenReturn(2L);

		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("juan", "false"));

		// Act
		final PageArray result = controller.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(1);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(1);
		verify(service).findListViewsByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"), any(Pageable.class));
		verify(service).countByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"));
		verify(service).countByUserId(eq(TEST_USER_ID));
		log.info("finished testGetPageArrayWithSearch");
	}

	@Test
	public void testGetPageArrayWithNullUserId() {
		log.info("starting testGetPageArrayWithNullUserId");
		// Arrange
		when(principal.getSubject()).thenReturn(null);

		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);

		// Act
		final PageArray result = controller.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(0);
		assertThat(result.getRecordsFiltered()).isEqualTo(0);
		assertThat(result.getData()).isEmpty();
		log.info("finished testGetPageArrayWithNullUserId");
	}

	@Test
	public void testGetPageArrayWithPagination() {
		log.info("starting testGetPageArrayWithPagination");
		// Arrange - Request second page with 1 item per page
		final Pageable pageable = PageRequest.of(1, 1);
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente2), pageable, 2);
		when(service.findListViewsByUserId(eq(TEST_USER_ID), any(Pageable.class)))
			.thenReturn(toListViewPage(springPage));
		when(service.countByUserId(eq(TEST_USER_ID))).thenReturn(2L);

		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(1); // Second page (start = page * length)
		pagingRequest.setLength(1); // 1 item per page
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		final PageArray result = controller.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(2);
		assertThat(result.getData()).hasSize(1);
		verify(service).findListViewsByUserId(eq(TEST_USER_ID), any(Pageable.class));
		log.info("finished testGetPageArrayWithPagination");
	}

	@Test
	public void testGetPageArrayWithSorting() {
		log.info("starting testGetPageArrayWithSorting");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10,
				org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name"));
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente1, paciente2), pageable, 2);
		when(service.findListViewsByUserId(eq(TEST_USER_ID), any(Pageable.class)))
			.thenReturn(toListViewPage(springPage));
		when(service.countByUserId(eq(TEST_USER_ID))).thenReturn(2L);

		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		// Sort by first column (nombre)
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		final PageArray result = controller.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		verify(service).findListViewsByUserId(eq(TEST_USER_ID), any(Pageable.class));
		log.info("finished testGetPageArrayWithSorting");
	}

	@Test
	public void testToStringList() {
		log.info("starting testToStringList");
		// Act
		final List<String> result = controller.toStringList(toListView(paciente1));

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(8);
		assertThat(result.get(0)).contains("Juan Perez");
		assertThat(result.get(0)).contains("/admin/pacientes/1");
		assertThat(result.get(1)).isNotEmpty(); // Date formatted
		assertThat(result.get(2)).isEqualTo("juan@example.com");
		assertThat(result.get(3)).isEqualTo("1234567890");
		assertThat(result.get(4)).isEqualTo("M");
		assertThat(result.get(5)).isEqualTo("Maria Perez");
		assertThat(result.get(6)).contains("badge");
		assertThat(result.get(7)).contains("paciente-export-btn");
		assertThat(result.get(7)).contains("paciente-delete-btn");
		log.info("finished testToStringList");
	}

	@Test
	public void testToStringListWithNullValues() {
		log.info("starting testToStringListWithNullValues");
		// Arrange
		final Paciente paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Test");
		paciente.setDob(null);
		paciente.setEmail(null);
		paciente.setPhone(null);
		paciente.setGender(null);
		paciente.setResponsibleName(null);

		// Act
		final List<String> result = controller.toStringList(toListView(paciente));

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(8);
		assertThat(result.get(1)).isEmpty(); // Null date
		assertThat(result.get(2)).isNull(); // Null email
		assertThat(result.get(3)).isNull(); // Null phone
		assertThat(result.get(4)).isNull(); // Null gender
		assertThat(result.get(5)).isNull(); // Null responsibleName
		assertThat(result.get(7)).contains("paciente-delete-btn");
		log.info("finished testToStringListWithNullValues");
	}

	@Test
	public void testGetColumns() {
		log.info("starting testGetColumns");
		// Act
		final List<Column> result = controller.getColumns();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(8);
		assertThat(result.get(0).getData()).isEqualTo("nombre");
		assertThat(result.get(1).getData()).isEqualTo("dob");
		assertThat(result.get(2).getData()).isEqualTo("email");
		assertThat(result.get(3).getData()).isEqualTo("phone");
		assertThat(result.get(4).getData()).isEqualTo("gender");
		assertThat(result.get(5).getData()).isEqualTo("responsible");
		assertThat(result.get(6).getData()).isEqualTo("mobileApp");
		assertThat(result.get(7).getData()).isEqualTo("acciones");
		log.info("finished testGetColumns");
	}

	@Test
	public void testGetRows() {
		log.info("starting testGetRows");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente1, paciente2), pageable, 2);
		when(service.findListViewsByUserId(eq(TEST_USER_ID), any(Pageable.class)))
			.thenReturn(toListViewPage(springPage));
		when(service.countByUserId(eq(TEST_USER_ID))).thenReturn(2L);

		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		pagingRequest.setColumns(controller.getColumns());

		// Act
		final com.nutriconsultas.dataTables.paging.Page<PacienteListView> result = controller.getRows(pagingRequest,
				TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(2);
		assertThat(result.getData()).hasSize(2);
		verify(service).findListViewsByUserId(eq(TEST_USER_ID), any(Pageable.class));
		log.info("finished testGetRows");
	}

	@Test
	public void testGetRowsWithSearch() {
		log.info("starting testGetRowsWithSearch");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente1), pageable, 1);
		when(service.findListViewsByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"), any(Pageable.class)))
			.thenReturn(toListViewPage(springPage));
		when(service.countByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"))).thenReturn(1L);
		when(service.countByUserId(eq(TEST_USER_ID))).thenReturn(2L);

		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("juan", "false"));
		pagingRequest.setColumns(controller.getColumns());

		// Act
		final com.nutriconsultas.dataTables.paging.Page<PacienteListView> result = controller.getRows(pagingRequest,
				TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(1);
		assertThat(result.getData()).hasSize(1);
		verify(service).findListViewsByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"), any(Pageable.class));
		verify(service).countByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"));
		log.info("finished testGetRowsWithSearch");
	}

	@Test
	@DisplayName("assignBmi with valid data returns 200 and updates patient imc")
	void assignBmi_validData_returns200AndUpdatesImc() {
		when(principal.getSubject()).thenReturn(TEST_USER_ID);
		when(service.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(paciente1);
		when(service.save(any(Paciente.class))).thenReturn(paciente1);

		final ResponseEntity<java.util.Map<String, Object>> response = controller.assignBmi(1L,
				new HashMap<>(java.util.Map.of("bmi", "22.5")), principal);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).containsEntry("success", true);
		assertThat(response.getBody()).containsKey("imc");
		verify(service).save(argThat(p -> p.getImc() != null && Double.compare(p.getImc(), 22.5) == 0));
	}

	@Test
	@DisplayName("assignBmi with patient not owned by user returns 404")
	void assignBmi_patientNotOwnedByUser_returns404() {
		when(principal.getSubject()).thenReturn(TEST_USER_ID);
		when(service.findByIdAndUserId(99L, TEST_USER_ID)).thenReturn(null);

		final ResponseEntity<java.util.Map<String, Object>> response = controller.assignBmi(99L,
				new HashMap<>(java.util.Map.of("bmi", "22.5")), principal);

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).containsEntry("success", false);
	}

	@Test
	@DisplayName("assignBmi unauthenticated returns 401")
	void assignBmi_unauthenticated_returns401() {
		final ResponseEntity<java.util.Map<String, Object>> response = controller.assignBmi(1L,
				new HashMap<>(java.util.Map.of("bmi", "22.5")), null);

		assertThat(response.getStatusCode().value()).isEqualTo(401);
	}

	@Test
	@DisplayName("assignBmr with valid data returns 200 and updates patient bmr")
	void assignBmr_validData_returns200AndUpdatesBmr() {
		when(principal.getSubject()).thenReturn(TEST_USER_ID);
		when(service.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(paciente1);
		when(service.save(any(Paciente.class))).thenReturn(paciente1);

		final ResponseEntity<java.util.Map<String, Object>> response = controller.assignBmr(1L,
				new HashMap<>(java.util.Map.of("bmr", "1650.0")), principal);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).containsEntry("success", true);
		assertThat(response.getBody()).containsKey("bmr");
		verify(service).save(argThat(p -> p.getBmr() != null && p.getBmr() > 0));
	}

	@Test
	@DisplayName("assignBmr with patient not owned by user returns 404")
	void assignBmr_patientNotOwnedByUser_returns404() {
		when(principal.getSubject()).thenReturn(TEST_USER_ID);
		when(service.findByIdAndUserId(99L, TEST_USER_ID)).thenReturn(null);

		final ResponseEntity<java.util.Map<String, Object>> response = controller.assignBmr(99L,
				new HashMap<>(java.util.Map.of("bmr", "1650.0")), principal);

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).containsEntry("success", false);
	}

	@Test
	@DisplayName("assignBmr unauthenticated returns 401")
	void assignBmr_unauthenticated_returns401() {
		final ResponseEntity<java.util.Map<String, Object>> response = controller.assignBmr(1L,
				new HashMap<>(java.util.Map.of("bmr", "1650.0")), null);

		assertThat(response.getStatusCode().value()).isEqualTo(401);
	}

	@Test
	@DisplayName("updateAvatar with valid data returns avatar metadata")
	void updateAvatar_validData_returnsAvatarMetadata() {
		when(principal.getSubject()).thenReturn(TEST_USER_ID);
		final PacienteAvatarUpdateRequest request = new PacienteAvatarUpdateRequest();
		request.setAvatarId("avatar_8");
		final Paciente updated = new Paciente();
		updated.setId(1L);
		updated.setAvatarId("avatar_8");
		updated.setGender("M");
		when(service.updateAvatar(1L, TEST_USER_ID, "avatar_8")).thenReturn(updated);

		final ResponseEntity<java.util.Map<String, Object>> response = controller.updateAvatar(1L, request, principal);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).containsEntry("success", true);
		assertThat(response.getBody()).containsEntry("avatarId", "avatar_8");
		assertThat(response.getBody()).containsEntry("avatarUrl", "/sbadmin/img/paciente-avatars/avatar_8.png");
	}

	@Test
	@DisplayName("updateAvatar unauthenticated returns 401")
	void updateAvatar_unauthenticated_returns401() {
		final PacienteAvatarUpdateRequest request = new PacienteAvatarUpdateRequest();
		request.setAvatarId("avatar_1");

		final ResponseEntity<java.util.Map<String, Object>> response = controller.updateAvatar(1L, request, null);

		assertThat(response.getStatusCode().value()).isEqualTo(401);
	}

	private static PacienteListView toListView(final Paciente paciente) {
		return new PacienteListView() {
			@Override
			public Long getId() {
				return paciente.getId();
			}

			@Override
			public String getName() {
				return paciente.getName();
			}

			@Override
			public String getEmail() {
				return paciente.getEmail();
			}

			@Override
			public String getPhone() {
				return paciente.getPhone();
			}

			@Override
			public Date getDob() {
				return paciente.getDob();
			}

			@Override
			public String getGender() {
				return paciente.getGender();
			}

			@Override
			public String getResponsibleName() {
				return paciente.getResponsibleName();
			}

			@Override
			public PacienteStatus getStatus() {
				return paciente.getStatus();
			}

			@Override
			public String getPatientAuthSub() {
				return paciente.getPatientAuthSub();
			}
		};
	}

	private static Page<PacienteListView> toListViewPage(final Page<Paciente> springPage) {
		return springPage.map(PacienteRestControllerTest::toListView);
	}

}
