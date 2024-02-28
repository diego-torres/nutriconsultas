package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;
import com.nutriconsultas.model.ApiResponse;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class PlatilloRestControllerTest {
  @InjectMocks
  private PlatilloRestController platilloRestController;

  @Mock
  private PlatilloService platilloService;

  @SuppressWarnings("null")
  @Test
  public void testAdd() {
    log.info("Starting testAdd");
    // Arrange
    Platillo platillo = new Platillo();
    platillo.setName("Test platillo");
    log.debug("Platillo to add: {}", platillo);

    // Mock the save method
    when(platilloService.save(any(Platillo.class))).thenReturn(platillo);

    // Act
    Platillo result = platilloRestController.add(platillo);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(platillo.getName());
  }

  @Test
  public void testArray() {
    log.info("Starting testArray");
    // Read CSV file from classpath and convert to list of Platillo
    try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/platillos.csv"))) {
      log.debug("Reading platillos.csv");
      List<Platillo> platillos = new CsvToBeanBuilder<Platillo>(reader)
          .withType(Platillo.class)
          .withIgnoreLeadingWhiteSpace(true)
          .build()
          .parse();
      log.debug("Platillos read from CSV: {}", platillos);
      when(platilloService.findAll()).thenReturn(platillos);
    } catch (IOException e) {
      log.error("Error reading platillos.csv", e);
    }
    // Arrange
    PagingRequest pagingRequest = new PagingRequest();

    pagingRequest.setStart(0);
    pagingRequest.setLength(10);
    pagingRequest.setDraw(1);
    pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
    pagingRequest.setSearch(new Search("", "false"));
    log.debug("Paging request: {}", pagingRequest);

    // Act
    PageArray result = platilloRestController.getPageArray(pagingRequest);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getRecordsTotal()).isEqualTo(20);
    assertThat(result.getRecordsFiltered()).isEqualTo(20);
    assertThat(result.getDraw()).isEqualTo(1);
    assertThat(result.getData()).isNotEmpty();
    assertThat(result.getData().size()).isEqualTo(10);
    log.info("finished testArray with records {}", result.getRecordsTotal());
  }

  // Test addIngrediente
  @SuppressWarnings("null")
  @Test
  public void testAddIngrediente() {
    log.info("Starting testAddIngrediente");
    // Arrange
    Platillo platillo = new Platillo();
    platillo.setId(1L);
    platillo.setName("Test platillo");
    log.debug("Platillo to add ingrediente: {}", platillo);

    Alimento alimento = new Alimento();
    alimento.setId(1L);
    alimento.setNombreAlimento("Test alimento");
    alimento.setCantSugerida(1d);
    alimento.setUnidad("pieza");
    alimento.setPesoNeto(100);
    alimento.setPesoBrutoRedondeado(100);

    IngredienteFormModel ingrediente = new IngredienteFormModel();
    ingrediente.setAlimentoId(1L);
    ingrediente.setCantidad("1");
    ingrediente.setPeso(100);

    Ingrediente _Ingrediente = new Ingrediente();
    _Ingrediente.setId(1L);
    _Ingrediente.setAlimento(alimento);
    _Ingrediente.setCantSugerida(1d);
    _Ingrediente.setPesoNeto(100);

    log.debug("Ingrediente to add: {}", ingrediente);

    // Mock the save method
    when(platilloService.addIngrediente(1L, 1L, "1", 100)).thenReturn(_Ingrediente);

    // Act
    ResponseEntity<ApiResponse<Ingrediente>> result = platilloRestController.addIngrediente(1L, ingrediente);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(result.getBody().getData()).isEqualTo(_Ingrediente);
  }

}
