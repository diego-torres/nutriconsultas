package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
		when(service.findAllByUserId(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(springPage);
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
		verify(service).findAllByUserId(eq(TEST_USER_ID), any(Pageable.class));
		verify(service, times(2)).countByUserId(eq(TEST_USER_ID));
		log.info("finished testGetPageArray");
	}

	@Test
	public void testGetPageArrayWithSearch() {
		log.info("starting testGetPageArrayWithSearch");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente1), pageable, 1);
		when(service.findAllByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"), any(Pageable.class)))
			.thenReturn(springPage);
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
		verify(service).findAllByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"), any(Pageable.class));
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
		when(service.findAllByUserId(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(springPage);
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
		verify(service).findAllByUserId(eq(TEST_USER_ID), any(Pageable.class));
		log.info("finished testGetPageArrayWithPagination");
	}

	@Test
	public void testGetPageArrayWithSorting() {
		log.info("starting testGetPageArrayWithSorting");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10,
				org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name"));
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente1, paciente2), pageable, 2);
		when(service.findAllByUserId(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(springPage);
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
		verify(service).findAllByUserId(eq(TEST_USER_ID), any(Pageable.class));
		log.info("finished testGetPageArrayWithSorting");
	}

	@Test
	public void testToStringList() {
		log.info("starting testToStringList");
		// Act
		final List<String> result = controller.toStringList(paciente1);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(6);
		assertThat(result.get(0)).contains("Juan Perez");
		assertThat(result.get(0)).contains("/admin/pacientes/1");
		assertThat(result.get(1)).isNotEmpty(); // Date formatted
		assertThat(result.get(2)).isEqualTo("juan@example.com");
		assertThat(result.get(3)).isEqualTo("1234567890");
		assertThat(result.get(4)).isEqualTo("M");
		assertThat(result.get(5)).isEqualTo("Maria Perez");
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
		final List<String> result = controller.toStringList(paciente);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(6);
		assertThat(result.get(1)).isEmpty(); // Null date
		assertThat(result.get(2)).isNull(); // Null email
		assertThat(result.get(3)).isNull(); // Null phone
		assertThat(result.get(4)).isNull(); // Null gender
		assertThat(result.get(5)).isNull(); // Null responsibleName
		log.info("finished testToStringListWithNullValues");
	}

	@Test
	public void testGetColumns() {
		log.info("starting testGetColumns");
		// Act
		final List<Column> result = controller.getColumns();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(6);
		assertThat(result.get(0).getData()).isEqualTo("nombre");
		assertThat(result.get(1).getData()).isEqualTo("dob");
		assertThat(result.get(2).getData()).isEqualTo("email");
		assertThat(result.get(3).getData()).isEqualTo("phone");
		assertThat(result.get(4).getData()).isEqualTo("gender");
		assertThat(result.get(5).getData()).isEqualTo("responsible");
		log.info("finished testGetColumns");
	}

	@Test
	public void testGetRows() {
		log.info("starting testGetRows");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente1, paciente2), pageable, 2);
		when(service.findAllByUserId(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(springPage);
		when(service.countByUserId(eq(TEST_USER_ID))).thenReturn(2L);

		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		pagingRequest.setColumns(controller.getColumns());

		// Act
		final com.nutriconsultas.dataTables.paging.Page<Paciente> result = controller.getRows(pagingRequest,
				TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(2);
		assertThat(result.getData()).hasSize(2);
		verify(service).findAllByUserId(eq(TEST_USER_ID), any(Pageable.class));
		log.info("finished testGetRows");
	}

	@Test
	public void testGetRowsWithSearch() {
		log.info("starting testGetRowsWithSearch");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> springPage = new PageImpl<>(Arrays.asList(paciente1), pageable, 1);
		when(service.findAllByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"), any(Pageable.class)))
			.thenReturn(springPage);
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
		final com.nutriconsultas.dataTables.paging.Page<Paciente> result = controller.getRows(pagingRequest,
				TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(1);
		assertThat(result.getData()).hasSize(1);
		verify(service).findAllByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"), any(Pageable.class));
		verify(service).countByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("juan"));
		log.info("finished testGetRowsWithSearch");
	}

}
