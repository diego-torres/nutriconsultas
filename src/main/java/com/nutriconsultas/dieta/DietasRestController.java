package com.nutriconsultas.dieta;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/dietas")
@Slf4j
public class DietasRestController extends AbstractGridController<Dieta> {

  @Autowired
  private DietaService dietaService;

  @Override
  protected List<String> toStringList(Dieta row) {
    log.debug("converting Dieta row {} to string list.", row);
    return Arrays.asList(
        "<a href='/admin/dietas/" + row.getId() + "'>" + row.getNombre() + "</a>");
  }

  @Override
  protected List<Dieta> getData() {
    log.debug("getting all Dieta records.");
    return dietaService.getDietas();
  }

  @Override
  protected Predicate<Dieta> getPredicate(String value) {
    return row -> row.getNombre().toLowerCase().contains(value)
        || row.getNombre().toLowerCase().startsWith(value);
  }

  @Override
  protected Comparator<Dieta> getComparator(String column, Direction dir) {
    log.debug("getting Dieta comparator with column {} and direction {}.", column, dir);
    return DietaComparators.getComparator(column, dir);
  }

  @Override
  protected List<Column> getColumns() {
    return Stream.of("nombre")
        .map(Column::new)
        .collect(Collectors.toList());
  }
}
