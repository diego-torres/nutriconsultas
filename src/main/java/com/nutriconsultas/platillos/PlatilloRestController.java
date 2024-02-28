package com.nutriconsultas.platillos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentoService;
import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("rest/platillos")
@Slf4j
public class PlatilloRestController extends AbstractGridController<Platillo> {
  @Autowired
  private PlatilloService service;

  @Autowired
  private AlimentoService alimentoService;

  @PostMapping("{id}/ingredientes/add")
  public ResponseEntity<ApiResponse<Ingrediente>> addIngrediente(@PathVariable @NonNull Long id,
      @RequestBody @NonNull IngredienteFormModel ingrediente) {
    log.info("starting addIngrediente with id {} and ingrediente {}.", id, ingrediente);
    @SuppressWarnings("null")
    Ingrediente _ingrediente = service.addIngrediente(id, ingrediente.getAlimentoId(), ingrediente.getCantidad(), ingrediente.getPeso());

    log.info("finish addIngrediente with id {} and ingrediente {}.", id, ingrediente);
    return ResponseEntity.ok(new ApiResponse<Ingrediente>(_ingrediente));
  }

  @PostMapping("add")
  public Platillo add(@RequestBody @NonNull Platillo platillo) {
    log.info("starting add with platillo {}.", platillo);
    Platillo saved = service.save(platillo);
    log.info("finish add with platillo {}.", saved);
    return saved;
  }

  @Override
  protected List<Column> getColumns() {
    log.debug("getting Platillo columns.");
    return Arrays.asList(
        new Column("platillo"), //
        new Column("ingestas"), //
        new Column("kcal"), //
        new Column("prot"), //
        new Column("lip"), //
        new Column("hc"));
  }

  @Override
  protected List<String> toStringList(Platillo row) {
    log.debug("converting Platillo row {} to string list.", row);
    return Arrays.asList(
        "<a href='/admin/platillos/" + row.getId() + "'>" + row.getName() + "</a>",
        row.getIngestasSugeridas() == null ? "" : row.getIngestasSugeridas(),
        row.getEnergia() == null ? "" : row.getEnergia().toString(),
        String.format("%.1f", row.getProteina()), //
        String.format("%.1f", row.getLipidos()), //
        String.format("%.1f", row.getHidratosDeCarbono()));
  }

  @Override
  protected List<Platillo> getData() {
    log.debug("getting all Platillos.");
    return service.findAll();
  }

  @Override
  protected Predicate<Platillo> getPredicate(String value) {
    log.debug("getting Platillo predicate with value {}.", value);
    return platillo -> platillo.getName().toLowerCase().contains(value.toLowerCase())
        || platillo.getIngestasSugeridas().toLowerCase().contains(value.toLowerCase());
  }

  @Override
  protected Comparator<Platillo> getComparator(String column, com.nutriconsultas.dataTables.paging.Direction dir) {
    log.debug("getting Platillo comparator with column {} and direction {}.", column, dir);
    return PlatilloComparators.getComparator(column, dir);
  }

}
