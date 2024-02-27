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
    Platillo platillo = service.findById(id);
    Long alimentoId = Objects.requireNonNull(ingrediente.getAlimentoId(), "Alimento ID must not be null");
    Alimento alimento = alimentoService.findById(alimentoId);
    // Calculate ingrediente values
    Ingrediente _ingrediente = new Ingrediente();
    _ingrediente.setAlimento(alimento);
    _ingrediente.setUnidad(alimento.getUnidad());
    // if cantidad is different than cantSugerida, then calculate the new values
    Boolean calculatedFromCantidad = false;
    if (ingrediente.getCantidad() != null
        && ingrediente.getCantidad() != _ingrediente.getAlimento().getFractionalCantSugerida()) {
      log.debug("calculating ingrediente from cantidad change.");
      _ingrediente = calculateIngredienteFromCantiadChange(ingrediente.getCantidad().toString(),
          alimento.getFractionalCantSugerida(), _ingrediente, alimento);
      calculatedFromCantidad = true;
    } else {
      log.debug("cantidad did not changed, setting ingrediente default values.");
      _ingrediente = convertAlimentoToIngrediente(_ingrediente, alimento);
    }

    // if peso is different than pesoNeto, then calculate the new values
    if (ingrediente.getPeso() != null && ingrediente.getPeso() != _ingrediente.getAlimento().getPesoNeto()
        && !calculatedFromCantidad) {
      log.debug("calculating ingrediente from peso change.");
      _ingrediente = calculateIngredienteFromPesoChange(ingrediente.getPeso(), alimento.getPesoNeto(), _ingrediente,
          alimento);
    } 
    _ingrediente.setPlatillo(platillo);

    log.debug("setting ingrediente in platillo: {}.", _ingrediente);
    platillo.getIngredientes().add(_ingrediente);
    service.save(platillo);
    log.info("finish addIngrediente with id {} and ingrediente {}.", id, ingrediente);
    return ResponseEntity.ok(new ApiResponse<Ingrediente>(_ingrediente));
  }

  // convert alimento to ingrediente
  private Ingrediente convertAlimentoToIngrediente(Ingrediente ingrediente, Alimento alimento) {
    ingrediente.setCantSugerida(alimento.getCantSugerida());
    ingrediente.setAcidoAscorbico(alimento.getAcidoAscorbico());
    ingrediente.setAcidoFolico(alimento.getAcidoAscorbico());
    ingrediente.setAgMonoinsaturados(alimento.getAgMonoinsaturados());
    ingrediente.setAgPoliinsaturados(alimento.getAgPoliinsaturados());
    ingrediente.setAgSaturados(alimento.getAgSaturados());
    ingrediente.setAzucarPorEquivalente(alimento.getAzucarPorEquivalente());
    ingrediente.setCalcio(alimento.getCalcio());
    ingrediente.setCargaGlicemica(alimento.getCargaGlicemica());
    ingrediente.setColesterol(alimento.getColesterol());
    ingrediente.setEnergia(alimento.getEnergia());
    ingrediente.setFibra(alimento.getFibra());
    ingrediente.setFosforo(alimento.getFosforo());
    ingrediente.setHierro(alimento.getHierro());
    ingrediente.setHierroNoHem(alimento.getHierroNoHem());
    ingrediente.setIndiceGlicemico(alimento.getIndiceGlicemico());
    ingrediente.setEtanol(alimento.getEtanol());
    ingrediente.setHidratosDeCarbono(alimento.getHidratosDeCarbono());
    ingrediente.setLipidos(alimento.getLipidos());
    ingrediente.setPotasio(alimento.getPotasio());
    ingrediente.setProteina(alimento.getProteina());
    ingrediente.setSelenio(alimento.getSelenio());
    ingrediente.setSodio(alimento.getSodio());
    ingrediente.setVitA(alimento.getVitA());
    ingrediente.setPesoBrutoRedondeado(alimento.getPesoBrutoRedondeado());
    ingrediente.setPesoNeto(alimento.getPesoNeto());

    return ingrediente;
  }

  // calculate ingrediente from cantidad change
  private Ingrediente calculateIngredienteFromCantiadChange(String given, String suggested, Ingrediente ingrediente,
      Alimento alimento) {
    Boolean hasInteger = given.contains(" ") || !given.contains("//");
    Boolean hasFraction = given.contains("//");
    Integer iGivenIntPart = hasInteger ? Integer.parseInt(given.split(" ")[0]) : 0;
    Integer iGivenNumeratorPart = hasFraction ? Integer.parseInt(given.split(" ")[1].split("//")[0]) : 0;
    Integer iGivenDenominatorPart = hasFraction ? Integer.parseInt(given.split(" ")[1].split("//")[1]) : 0;
    Double dGiven = iGivenIntPart.doubleValue() + (hasFraction ? (iGivenNumeratorPart / iGivenDenominatorPart) : 0d);
    Double dFactor = dGiven / alimento.getCantSugerida();
    
    log.debug("setting cantSugerida to {}.", dGiven);
    ingrediente.setCantSugerida(dGiven);
    if (alimento.getAcidoAscorbico() != null)
      ingrediente.setAcidoAscorbico(alimento.getAcidoAscorbico() * dFactor);

    if (alimento.getAcidoFolico() != null)
      ingrediente.setAcidoFolico(alimento.getAcidoFolico() * dFactor);

    if (alimento.getAgMonoinsaturados() != null)
      ingrediente.setAgMonoinsaturados(alimento.getAgMonoinsaturados() * dFactor);

    if (alimento.getAgPoliinsaturados() != null)
      ingrediente.setAgPoliinsaturados(alimento.getAgPoliinsaturados() * dFactor);

    if (alimento.getAgSaturados() != null)
      ingrediente.setAgSaturados(alimento.getAgSaturados() * dFactor);

    if (alimento.getAzucarPorEquivalente() != null)
      ingrediente.setAzucarPorEquivalente(alimento.getAzucarPorEquivalente() * dFactor);

    if (alimento.getCalcio() != null)
      ingrediente.setCalcio(alimento.getCalcio() * dFactor);

    if (alimento.getCargaGlicemica() != null)
      ingrediente.setCargaGlicemica(alimento.getCargaGlicemica() * dFactor);

    if (alimento.getColesterol() != null)
      ingrediente.setColesterol(alimento.getColesterol() * dFactor);

    if (alimento.getEnergia() != null)
      ingrediente.setEnergia(alimento.getEnergia() * dFactor.intValue());

    if (alimento.getFibra() != null)
      ingrediente.setFibra(alimento.getFibra() * dFactor);

    if (alimento.getFosforo() != null)
      ingrediente.setFosforo(alimento.getFosforo() * dFactor);

    if (alimento.getHierro() != null)
      ingrediente.setHierro(alimento.getHierro() * dFactor);

    if (alimento.getHierroNoHem() != null)
      ingrediente.setHierroNoHem(alimento.getHierroNoHem() * dFactor);

    if (alimento.getIndiceGlicemico() != null)
      ingrediente.setIndiceGlicemico(alimento.getIndiceGlicemico() * dFactor);

    if (alimento.getEtanol() != null)
      ingrediente.setEtanol(alimento.getEtanol() * dFactor);

    if (alimento.getHidratosDeCarbono() != null)
      ingrediente.setHidratosDeCarbono(alimento.getHidratosDeCarbono() * dFactor);

    if (alimento.getLipidos() != null)
      ingrediente.setLipidos(alimento.getLipidos() * dFactor);

    if (alimento.getPotasio() != null)
      ingrediente.setPotasio(alimento.getPotasio() * dFactor);

    if (alimento.getProteina() != null)
      ingrediente.setProteina(alimento.getProteina() * dFactor);

    if (alimento.getSelenio() != null)
      ingrediente.setSelenio(alimento.getSelenio() * dFactor);

    if (alimento.getSodio() != null)
      ingrediente.setSodio(alimento.getSodio() * dFactor);

    if (alimento.getVitA() != null)
      ingrediente.setVitA(alimento.getVitA() * dFactor);

    if (alimento.getPesoBrutoRedondeado() != null)
      ingrediente.setPesoBrutoRedondeado(alimento.getPesoBrutoRedondeado() * dFactor.intValue());

    if (alimento.getPesoNeto() != null)
      ingrediente.setPesoNeto(alimento.getPesoNeto() * dFactor.intValue());

    return ingrediente;
  }

  // calculate ingrediente from peso change
  private Ingrediente calculateIngredienteFromPesoChange(Integer given, Integer suggested, Ingrediente ingrediente,
      Alimento alimento) {
    Double dFactor = (double) given / (double) suggested;

    ingrediente.setCantSugerida(dFactor * alimento.getCantSugerida());

    if (alimento.getAcidoAscorbico() != null)
      ingrediente.setAcidoAscorbico(alimento.getAcidoAscorbico() * dFactor);

    if (alimento.getAcidoFolico() != null)
      ingrediente.setAcidoFolico(alimento.getAcidoAscorbico() * dFactor);

    if (alimento.getAgMonoinsaturados() != null)
      ingrediente.setAgMonoinsaturados(alimento.getAgMonoinsaturados() * dFactor);

    if (alimento.getAgPoliinsaturados() != null)
      ingrediente.setAgPoliinsaturados(alimento.getAgPoliinsaturados() * dFactor);

    if (alimento.getAgSaturados() != null)
      ingrediente.setAgSaturados(alimento.getAgSaturados() * dFactor);

    if (alimento.getAzucarPorEquivalente() != null)
      ingrediente.setAzucarPorEquivalente(alimento.getAzucarPorEquivalente() * dFactor);

    if (alimento.getCalcio() != null)
      ingrediente.setCalcio(alimento.getCalcio() * dFactor);

    if (alimento.getCargaGlicemica() != null)
      ingrediente.setCargaGlicemica(alimento.getCargaGlicemica() * dFactor);

    if (alimento.getColesterol() != null)
      ingrediente.setColesterol(alimento.getColesterol() * dFactor);

    if (alimento.getEnergia() != null)
      ingrediente.setEnergia(alimento.getEnergia() * dFactor.intValue());

    if (alimento.getFibra() != null)
      ingrediente.setFibra(alimento.getFibra() * dFactor);

    if (alimento.getFosforo() != null)
      ingrediente.setFosforo(alimento.getFosforo() * dFactor);

    if (alimento.getHierro() != null)
      ingrediente.setHierro(alimento.getHierro() * dFactor);

    if (alimento.getHierroNoHem() != null)
      ingrediente.setHierroNoHem(alimento.getHierroNoHem() * dFactor);

    if (alimento.getIndiceGlicemico() != null)
      ingrediente.setIndiceGlicemico(alimento.getIndiceGlicemico() * dFactor);

    if (alimento.getEtanol() != null)
      ingrediente.setEtanol(alimento.getEtanol() * dFactor);

    if (alimento.getHidratosDeCarbono() != null)
      ingrediente.setHidratosDeCarbono(alimento.getHidratosDeCarbono() * dFactor);

    if (alimento.getLipidos() != null)
      ingrediente.setLipidos(alimento.getLipidos() * dFactor);

    if (alimento.getPotasio() != null)
      ingrediente.setPotasio(alimento.getPotasio() * dFactor);

    if (alimento.getProteina() != null)
      ingrediente.setProteina(alimento.getProteina() * dFactor);

    if (alimento.getSelenio() != null)
      ingrediente.setSelenio(alimento.getSelenio() * dFactor);

    if (alimento.getSodio() != null)
      ingrediente.setSodio(alimento.getSodio() * dFactor);

    if (alimento.getVitA() != null)
      ingrediente.setVitA(alimento.getVitA() * dFactor);

    if (alimento.getPesoBrutoRedondeado() != null)
      ingrediente.setPesoBrutoRedondeado(alimento.getPesoBrutoRedondeado() * dFactor.intValue());

    if (alimento.getPesoNeto() != null)
      ingrediente.setPesoNeto(alimento.getPesoNeto() * dFactor.intValue());

    return ingrediente;
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
