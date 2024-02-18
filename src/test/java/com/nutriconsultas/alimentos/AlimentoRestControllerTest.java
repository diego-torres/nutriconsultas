package com.nutriconsultas.alimentos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

@ExtendWith(MockitoExtension.class)
public class AlimentoRestControllerTest {

    @InjectMocks
    private AlimentoRestController alimentoRestController;

    @Mock
    private AlimentoService alimentoService;


    @Test
    public void testArray() {
        // Arrange
        String[] columns = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        // Read CSV file from classpath and convert to list of Alimento
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/alimentos.csv"))) {
            List<Alimento> alimentos = new CsvToBeanBuilder<Alimento>(reader)
            .withType(Alimento.class).build().parse();
            when(alimentoService.findAll()).thenReturn(alimentos);
        } catch(IOException e) {
            e.printStackTrace();
        }

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

        // Act
        PageArray result = alimentoRestController.array(pagingRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecordsTotal()).isEqualTo(2355);
        assertThat(result.getRecordsFiltered()).isEqualTo(2355);
        assertThat(result.getDraw()).isEqualTo(1);
        assertThat(result.getData()).isNotEmpty();
    }
}
