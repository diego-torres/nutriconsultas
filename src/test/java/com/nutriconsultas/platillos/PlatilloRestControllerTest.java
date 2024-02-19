package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;
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

  @BeforeEach
  public void setUp() {
    log.info("Starting setUp");
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
  }

  @Test
  public void testArray() {
    log.info("Starting testArray");
    // Arrange
    PagingRequest pagingRequest = new PagingRequest();

    pagingRequest.setStart(0);
    pagingRequest.setLength(10);
    pagingRequest.setDraw(1);
    pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
    pagingRequest.setSearch(new Search("", "false"));
    log.debug("Paging request: {}", pagingRequest);

    // Act
    PageArray result = platilloRestController.array(pagingRequest);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getRecordsTotal()).isEqualTo(10);
    assertThat(result.getRecordsFiltered()).isEqualTo(10);
    assertThat(result.getDraw()).isEqualTo(1);
    assertThat(result.getData()).isNotEmpty();
    assertThat(result.getData().size()).isEqualTo(10);
    log.info("finished testArray with records {}", result.getRecordsTotal());
  }

}
