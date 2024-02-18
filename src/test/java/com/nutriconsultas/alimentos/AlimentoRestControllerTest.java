package com.nutriconsultas.alimentos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class AlimentoRestControllerTest {

    @InjectMocks
    private AlimentoRestController alimentoRestController;

    @Mock
    private AlimentoService alimentoService;

    // setup the alimento service
    @BeforeEach
    public void setup() {
        log.info("setting up alimento service");
        // Read CSV file from classpath and convert to list of Alimento
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/alimentos.csv"))) {
            log.debug("reading alimentos from csv file");
            List<Alimento> alimentos = new CsvToBeanBuilder<Alimento>(reader)
            .withType(Alimento.class).build().parse();
            log.debug("setting up alimento service with {} alimentos", alimentos.size());
            when(alimentoService.findAll()).thenReturn(alimentos);
        } catch(IOException e) {
            log.error("error while reading alimentos from csv file", e);
        }
        log.info("finished setting up alimento service");
    }

    @Test
    public void testArray() {
        log.info("starting testArray");
        // Arrange
        PagingRequest pagingRequest = new PagingRequest();

        List<Column> columnList = new ArrayList<>();
        columnList.add(new Column("0", "", true, true, new Search("", "false")));
        columnList.add(new Column("1", "", true, true, new Search("", "false")));
        columnList.add(new Column("2", "", true, true, new Search("", "false")));
        columnList.add(new Column("3", "", true, true, new Search("", "false")));
        columnList.add(new Column("4", "", true, true, new Search("", "false")));
        columnList.add(new Column("5", "", true, true, new Search("", "false")));
        columnList.add(new Column("6", "", true, true, new Search("", "false")));
        columnList.add(new Column("7", "", true, true, new Search("", "false")));
        columnList.add(new Column("8", "", true, true, new Search("", "false")));
        columnList.add(new Column("9", "", true, true, new Search("", "false")));


        pagingRequest.setColumns(columnList);
        pagingRequest.setStart(0);
        pagingRequest.setLength(10);
        pagingRequest.setDraw(1);
        pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
        pagingRequest.setSearch(new Search("", "false"));
        log.debug("arrange paging request {}.", pagingRequest);

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(2355);
        assertThat(result.getRecordsFiltered()).isEqualTo(2355);
        assertThat(result.getDraw()).isEqualTo(1);
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().size()).isEqualTo(10);
        log.info("finished testArray with records {}", result.getRecordsTotal());
    }

    // Test no order given
    @Test
    public void testArrayNoOrder() {
        log.info("starting testArrayNoOrder");
        // Arrange
        PagingRequest pagingRequest = new PagingRequest();
        List<Column> columnList = new ArrayList<>();
        columnList.add(new Column("0", "", true, true, new Search("", "false")));
        columnList.add(new Column("1", "", true, true, new Search("", "false")));
        columnList.add(new Column("2", "", true, true, new Search("", "false")));
        columnList.add(new Column("3", "", true, true, new Search("", "false")));
        columnList.add(new Column("4", "", true, true, new Search("", "false")));
        columnList.add(new Column("5", "", true, true, new Search("", "false")));
        columnList.add(new Column("6", "", true, true, new Search("", "false")));
        columnList.add(new Column("7", "", true, true, new Search("", "false")));
        columnList.add(new Column("8", "", true, true, new Search("", "false")));
        columnList.add(new Column("9", "", true, true, new Search("", "false")));
        pagingRequest.setColumns(columnList);
        pagingRequest.setStart(0);
        pagingRequest.setLength(10);
        pagingRequest.setDraw(1);
        pagingRequest.setSearch(new Search("", "false"));

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(2355);
        assertThat(result.getRecordsFiltered()).isEqualTo(2355);
        assertThat(result.getDraw()).isEqualTo(1);
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().size()).isEqualTo(10);
        log.info("finished testArrayNoOrder with records {}", result.getRecordsTotal());
    }

    // Test no search given
    @Test
    public void testArrayNoSearch() {
        log.info("starting testArrayNoSearch");
        // Arrange
        PagingRequest pagingRequest = new PagingRequest();
        List<Column> columnList = new ArrayList<>();
        columnList.add(new Column("0", "", true, true, new Search("", "false")));
        columnList.add(new Column("1", "", true, true, new Search("", "false")));
        columnList.add(new Column("2", "", true, true, new Search("", "false")));
        columnList.add(new Column("3", "", true, true, new Search("", "false")));
        columnList.add(new Column("4", "", true, true, new Search("", "false")));
        columnList.add(new Column("5", "", true, true, new Search("", "false")));
        columnList.add(new Column("6", "", true, true, new Search("", "false")));
        columnList.add(new Column("7", "", true, true, new Search("", "false")));
        columnList.add(new Column("8", "", true, true, new Search("", "false")));
        columnList.add(new Column("9", "", true, true, new Search("", "false")));
        pagingRequest.setColumns(columnList);
        pagingRequest.setStart(0);
        pagingRequest.setLength(10);
        pagingRequest.setDraw(1);
        pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
        log.debug("arrange paging request {}.", pagingRequest);

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(2355);
        assertThat(result.getRecordsFiltered()).isEqualTo(2355);
        assertThat(result.getDraw()).isEqualTo(1);
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().size()).isEqualTo(10);
        log.info("finished testArrayNoSearch with records {}", result.getRecordsTotal());
    }

    // Test no paging given
    @Test
    public void testArrayNoPaging() {
        log.info("starting testArrayNoPaging");
        // Arrange
        PagingRequest pagingRequest = new PagingRequest();
        List<Column> columnList = new ArrayList<>();
        columnList.add(new Column("0", "", true, true, new Search("", "false")));
        columnList.add(new Column("1", "", true, true, new Search("", "false")));
        columnList.add(new Column("2", "", true, true, new Search("", "false")));
        columnList.add(new Column("3", "", true, true, new Search("", "false")));
        columnList.add(new Column("4", "", true, true, new Search("", "false")));
        columnList.add(new Column("5", "", true, true, new Search("", "false")));
        columnList.add(new Column("6", "", true, true, new Search("", "false")));
        columnList.add(new Column("7", "", true, true, new Search("", "false")));
        columnList.add(new Column("8", "", true, true, new Search("", "false")));
        columnList.add(new Column("9", "", true, true, new Search("", "false")));
        pagingRequest.setColumns(columnList);
        pagingRequest.setDraw(1);
        pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
        pagingRequest.setSearch(new Search("", "false"));
        log.debug("arrange paging request {}.", pagingRequest);

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(2355);
        assertThat(result.getRecordsFiltered()).isEqualTo(2355);
        assertThat(result.getDraw()).isEqualTo(1);
        assertThat(result.getData()).isEmpty();
        log.info("finished testArrayNoPaging with records {}", result.getRecordsTotal());
    }

    // Test no columns given
    @Test
    public void testArrayNoColumns() {
        log.info("starting testArrayNoColumns");
        // Arrange
        PagingRequest pagingRequest = new PagingRequest();
        pagingRequest.setStart(0);
        pagingRequest.setLength(10);
        pagingRequest.setDraw(1);
        pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
        pagingRequest.setSearch(new Search("", "false"));
        log.debug("arrange paging request {}.", pagingRequest);

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(2355);
        assertThat(result.getRecordsFiltered()).isEqualTo(2355);
        assertThat(result.getDraw()).isEqualTo(1);
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().size()).isEqualTo(10);
        log.info("finished testArrayNoColumns with records {}", result.getRecordsTotal());
    }

    // Test no data given
    @Test
    public void testArrayNoData() {
        log.info("starting testArrayNoData");
        // Arrange
        when(alimentoService.findAll()).thenReturn(new ArrayList<>());

        PagingRequest pagingRequest = new PagingRequest();
        List<Column> columnList = new ArrayList<>();
        columnList.add(new Column("0", "", true, true, new Search("", "false")));
        columnList.add(new Column("1", "", true, true, new Search("", "false")));
        columnList.add(new Column("2", "", true, true, new Search("", "false")));
        columnList.add(new Column("3", "", true, true, new Search("", "false")));
        columnList.add(new Column("4", "", true, true, new Search("", "false")));
        columnList.add(new Column("5", "", true, true, new Search("", "false")));
        columnList.add(new Column("6", "", true, true, new Search("", "false")));
        columnList.add(new Column("7", "", true, true, new Search("", "false")));
        columnList.add(new Column("8", "", true, true, new Search("", "false")));
        columnList.add(new Column("9", "", true, true, new Search("", "false")));
        pagingRequest.setColumns(columnList);
        pagingRequest.setStart(0);
        pagingRequest.setLength(10);
        pagingRequest.setDraw(1);
        pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
        pagingRequest.setSearch(new Search("", "false"));
        log.debug("arrange paging request {}.", pagingRequest);

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(0);
        assertThat(result.getRecordsFiltered()).isEqualTo(0);
        assertThat(result.getDraw()).isEqualTo(1);
        assertThat(result.getData()).isEmpty();
        log.info("finished testArrayNoData with records {}", result.getRecordsTotal());
    }

    // Test no draw given
    @Test
    public void testArrayNoDraw() {
        log.info("starting testArrayNoDraw");
        // Arrange
        PagingRequest pagingRequest = new PagingRequest();
        List<Column> columnList = new ArrayList<>();
        columnList.add(new Column("0", "", true, true, new Search("", "false")));
        columnList.add(new Column("1", "", true, true, new Search("", "false")));
        columnList.add(new Column("2", "", true, true, new Search("", "false")));
        columnList.add(new Column("3", "", true, true, new Search("", "false")));
        columnList.add(new Column("4", "", true, true, new Search("", "false")));
        columnList.add(new Column("5", "", true, true, new Search("", "false")));
        columnList.add(new Column("6", "", true, true, new Search("", "false")));
        columnList.add(new Column("7", "", true, true, new Search("", "false")));
        columnList.add(new Column("8", "", true, true, new Search("", "false")));
        columnList.add(new Column("9", "", true, true, new Search("", "false")));
        pagingRequest.setColumns(columnList);
        pagingRequest.setStart(0);
        pagingRequest.setLength(10);
        pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
        pagingRequest.setSearch(new Search("", "false"));
        log.debug("arrange paging request {}.", pagingRequest);

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(2355);
        assertThat(result.getRecordsFiltered()).isEqualTo(2355);
        assertThat(result.getDraw()).isEqualTo(0);
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().size()).isEqualTo(10);
        log.info("finished testArrayNoDraw with records {}", result.getRecordsTotal());
    }

    // Test no records filtered given
    @Test
    public void testArrayNoRecordsFiltered() {
        log.info("starting testArrayNoRecordsFiltered");
        // Arrange
        PagingRequest pagingRequest = new PagingRequest();
        List<Column> columnList = new ArrayList<>();
        columnList.add(new Column("0", "", true, true, new Search("", "false")));
        columnList.add(new Column("1", "", true, true, new Search("", "false")));
        columnList.add(new Column("2", "", true, true, new Search("", "false")));
        columnList.add(new Column("3", "", true, true, new Search("", "false")));
        columnList.add(new Column("4", "", true, true, new Search("", "false")));
        columnList.add(new Column("5", "", true, true, new Search("", "false")));
        columnList.add(new Column("6", "", true, true, new Search("", "false")));
        columnList.add(new Column("7", "", true, true, new Search("", "false")));
        columnList.add(new Column("8", "", true, true, new Search("", "false")));
        columnList.add(new Column("9", "", true, true, new Search("", "false")));
        pagingRequest.setColumns(columnList);
        pagingRequest.setStart(0);
        pagingRequest.setLength(10);
        pagingRequest.setDraw(1);
        pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
        pagingRequest.setSearch(new Search("not a valid record", "false"));
        log.debug("arrange paging request {}.", pagingRequest);

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(0);
        assertThat(result.getRecordsFiltered()).isEqualTo(0);
        assertThat(result.getDraw()).isEqualTo(1);
        assertThat(result.getData()).isEmpty();
        log.info("finished testArrayNoRecordsFiltered with records {}", result.getRecordsTotal());
    }

    // Test filtering records
    @Test
    public void testArrayFiltering() {
        log.info("starting testArrayFiltering");
        // Arrange
        PagingRequest pagingRequest = new PagingRequest();
        List<Column> columnList = new ArrayList<>();
        columnList.add(new Column("0", "", true, true, new Search("leche", "false")));
        columnList.add(new Column("1", "", true, true, new Search("", "false")));
        columnList.add(new Column("2", "", true, true, new Search("", "false")));
        columnList.add(new Column("3", "", true, true, new Search("", "false")));
        columnList.add(new Column("4", "", true, true, new Search("", "false")));
        columnList.add(new Column("5", "", true, true, new Search("", "false")));
        columnList.add(new Column("6", "", true, true, new Search("", "false")));
        columnList.add(new Column("7", "", true, true, new Search("", "false")));
        columnList.add(new Column("8", "", true, true, new Search("", "false")));
        columnList.add(new Column("9", "", true, true, new Search("", "false")));
        pagingRequest.setColumns(columnList);
        pagingRequest.setStart(0);
        pagingRequest.setLength(10);
        pagingRequest.setDraw(1);
        pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
        pagingRequest.setSearch(new Search("pera", "false"));
        log.debug("arrange paging request {}.", pagingRequest);

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(18);
        assertThat(result.getRecordsFiltered()).isEqualTo(18);
        assertThat(result.getDraw()).isEqualTo(1);
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().size()).isEqualTo(10);
        log.info("finished testArrayFiltering with records {}", result.getRecordsTotal());
    }
}
