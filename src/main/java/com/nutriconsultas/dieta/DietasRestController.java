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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/rest/dietas")
@Slf4j
public class DietasRestController extends AbstractGridController<Dieta> {

  @Autowired
  private DietaService dietaService;

  @PostMapping("add")
  public Dieta addDieta(@RequestBody Dieta dieta) {
    log.info("starting addDieta with dieta {}.", dieta);
    List<Ingesta> ingestas = Stream.of("Desayuno", "Comida", "Cena")
                                    .map(Ingesta::new)
                                    .collect(Collectors.toList());
    ingestas.forEach(i -> i.setDieta(dieta)); 
    dieta.setIngestas(ingestas);
    Dieta _dieta = dietaService.saveDieta(dieta);
    log.info("finish addDieta with dieta {}.", dieta);
    return _dieta;
  }
  

  @Override
  protected List<Column> getColumns() {
    return Stream.of("dieta", "ingestas", "dist", "kcal", "prot", "lip", "hc")
        .map(Column::new)
        .collect(Collectors.toList());
  }

  @Override
  protected List<String> toStringList(Dieta row) {
    log.debug("converting Dieta row {} to string list.", row);
    return Arrays.asList(
        "<a href='/admin/dietas/" + row.getId() + "'>" + row.getNombre() + "</a>",
        getIngestas(row),
        getDist(row),
        String.format("%.1f", getKCal(row)),
        String.format("%.1f", getTotalProteina(row)),
        String.format("%.1f", getTotalLipidos(row)),
        String.format("%.1f", getTotalHidratosDeCarbono(row)
        ));
  }

  private String getIngestas(Dieta row) {
    return row.getIngestas().stream()
        .map(Ingesta::getNombre)
        .collect(Collectors.joining(", "));
  }

  private String getDist(Dieta row) {
    // use protein, lipid, and carbohydrate values to calculate distribution
    Double distProteina, distLipido, distHidratoCarbono, kCal;
    kCal = getKCal(row);

    distProteina = getTotalProteina(row) * 4 / kCal;
    distLipido = getTotalLipidos(row) * 9 / kCal;
    distHidratoCarbono = getTotalHidratosDeCarbono(row) * 4 / kCal;

    return  String.format("%.1f",distProteina) + " / " +
            String.format("%.1f",distLipido) + " / " +
            String.format("%.1f",distHidratoCarbono);
  }

  private Double getKCal(Dieta row) {
    return getTotalProteina(row) * 4 + getTotalLipidos(row) * 9 + getTotalHidratosDeCarbono(row) * 4;
  } 

  private Double getTotalProteina(Dieta row) {
    return row.getIngestas()
      .stream()
      .mapToDouble(
        i -> i.getPlatillos()
              .stream()
              .mapToDouble(p -> p.getProteina())
              .sum())
      .sum();
  }

  private Double getTotalLipidos(Dieta row) {
    return row.getIngestas()
      .stream()
      .mapToDouble(
        i -> i.getPlatillos()
              .stream()
              .mapToDouble(p -> p.getLipidos())
              .sum())
      .sum();
  }

  private Double getTotalHidratosDeCarbono(Dieta row) {
    return row.getIngestas()
      .stream()
      .mapToDouble(
        i -> i.getPlatillos()
              .stream()
              .mapToDouble(p -> p.getHidratosDeCarbono())
              .sum())
      .sum();
  }

  @Override
  protected List<Dieta> getData() {
    log.debug("getting all Dieta records.");
    return dietaService.getDietas();
  }

  @Override
  protected Predicate<Dieta> getPredicate(String value) {
    return row -> row.getNombre().toLowerCase().contains(value)
        || row.getNombre().toLowerCase().startsWith(value)
        || row.getIngestas().stream().anyMatch(i -> i.getNombre().toLowerCase().contains(value))
        || row.getIngestas().stream().anyMatch(i -> i.getNombre().toLowerCase().startsWith(value));
  }

  @Override
  protected Comparator<Dieta> getComparator(String column, Direction dir) {
    log.debug("getting Dieta comparator with column {} and direction {}.", column, dir);
    return DietaComparators.getComparator(column, dir);
  }
}
