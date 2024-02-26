package com.nutriconsultas.platillos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("rest")
@Slf4j
public class PlatilloRestController {
  private static final Comparator<Platillo> EMPTY_COMPARATOR = (o1, o2) -> 0;

  @Autowired
  private PlatilloService service;

  @Autowired
  private AlimentoService alimentoService;

  @PostMapping("platillos/{id}/ingredientes")
  public ResponseEntity<ApiResponse<Ingrediente>> addIngrediente(@PathVariable Long id,
      @RequestBody @NonNull IngredienteFormModel ingrediente) {
    log.info("starting addIngrediente with id {} and ingrediente {}.", id, ingrediente);
    Platillo platillo = service.findById(id);
    Long alimentoId = Objects.requireNonNull(ingrediente.getAlimentoId(), "Alimento ID must not be null");
    Alimento alimento = alimentoService.findById(alimentoId);
    // Calculate ingrediente values
    Ingrediente _ingrediente = new Ingrediente();
    _ingrediente.setAlimento(alimento);
    // if cantidad is different than cantSugerida, then calculate the new values
    Boolean calculatedFromCantidad = false;
    if (ingrediente.getCantidad() != null
        && ingrediente.getCantidad() != _ingrediente.getAlimento().getFractionalCantSugerida()) {
      _ingrediente = calculateIngredienteFromCantiadChange(ingrediente.getCantidad().toString(),
          alimento.getFractionalCantSugerida(), _ingrediente, alimento);
      calculatedFromCantidad = true;
    } else {
      _ingrediente = convertAlimentoToIngrediente(_ingrediente, alimento);
    }

    // if peso is different than pesoNeto, then calculate the new values
    if (ingrediente.getPeso() != null && ingrediente.getPeso() != _ingrediente.getAlimento().getPesoNeto()
        && !calculatedFromCantidad) {
      _ingrediente = calculateIngredienteFromPesoChange(ingrediente.getPeso(), alimento.getPesoNeto(), _ingrediente,
          alimento);
    } else {
      _ingrediente = convertAlimentoToIngrediente(_ingrediente, alimento);
    }

    _ingrediente.setPlatillo(platillo);

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
    Boolean hasInteger = given.contains(" ");
    Boolean hasFraction = given.contains("//");
    Integer iGivenIntPart = hasInteger ? Integer.parseInt(given.split(" ")[0]) : 0;
    Integer iGivenNumeratorPart = hasFraction ? Integer.parseInt(given.split(" ")[1].split("//")[0]) : 0;
    Integer iGivenDenominatorPart = hasFraction ? Integer.parseInt(given.split(" ")[1].split("//")[1]) : 0;
    Double dGiven = iGivenIntPart.doubleValue() + (hasFraction ? (iGivenNumeratorPart / iGivenDenominatorPart) : 0d);
    Double dFactor = dGiven / alimento.getCantSugerida();

    ingrediente.setCantSugerida(dGiven);
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

  // calculate ingrediente from peso change
  private Ingrediente calculateIngredienteFromPesoChange(Integer given, Integer suggested, Ingrediente ingrediente,
      Alimento alimento) {
    Double dFactor = (double) given / (double) suggested;

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

  @PostMapping("platillos/add")
  public Platillo add(@RequestBody @NonNull Platillo platillo) {
    log.info("starting add with platillo {}.", platillo);
    Platillo saved = service.save(platillo);
    log.info("finish add with platillo {}.", saved);
    return saved;
  }

  @PostMapping("platillos")
  public PageArray array(@RequestBody PagingRequest pagingRequest) {
    log.info("starting array with paging request {}.", pagingRequest);
    String[] columns = { "platillo", "ingestas", "kcal", "prot", "lip", "hc" };
    log.debug("setting columns at page request {}", (Object) columns);
    pagingRequest.setColumns(
        Stream.of(columns)
            .map(Column::new)
            .collect(Collectors.toList()));
    Page<Platillo> page = getRows(pagingRequest);
    log.debug("Platillos page with records {}", page.getRecordsTotal());
    PageArray pageArray = new PageArray();
    pageArray.setRecordsFiltered(page.getRecordsFiltered());
    pageArray.setRecordsTotal(page.getRecordsTotal());
    pageArray.setDraw(page.getDraw());
    pageArray.setData(page.getData()
        .stream()
        .map(this::toStringList)
        .collect(Collectors.toList()));

    log.info("finish Platillos page array with records {}", pageArray.getRecordsTotal());
    return pageArray;
  }

  private List<String> toStringList(Platillo row) {
    log.debug("converting Platillo row {} to string list.", row);
    return Arrays.asList(
        "<a href='/admin/platillos/" + row.getId() + "'>" + row.getName() + "</a>",
        row.getIngestasSugeridas() == null ? "" : row.getIngestasSugeridas(),
        row.getEnergia() == null ? "" : row.getEnergia().toString(),
        String.format("%.1f", row.getProteina()), //
        String.format("%.1f", row.getLipidos()), //
        String.format("%.1f", row.getHidratosDeCarbono()));
  }

  private Page<Platillo> getRows(PagingRequest pagingRequest) {
    log.debug("starting getRows with paging request {}.", pagingRequest);
    return getPage(StreamSupport.stream(service.findAll().spliterator(), false).toList(), pagingRequest);
  }

  private Page<Platillo> getPage(List<Platillo> platillos, PagingRequest pagingRequest) {
    log.debug("converting {} platillos to page with paging request {}", platillos.size(), pagingRequest);
    List<Platillo> filtered = platillos.stream()
        .filter(filterRows(pagingRequest))
        .sorted(sortRows(pagingRequest))
        .skip(pagingRequest.getStart())
        .limit(pagingRequest.getLength())
        .toList();
    log.debug("filtered records {}", filtered.size());

    long count = platillos.stream()
        .filter(filterRows(pagingRequest))
        .count();

    log.debug("total records {}", count);

    Page<Platillo> page = new Page<>(filtered);
    page.setRecordsFiltered((int) count);
    page.setRecordsTotal((int) count);
    page.setDraw(pagingRequest.getDraw());

    log.debug("returning Platillos page with records {}", page.getRecordsTotal());
    return page;
  }

  private Predicate<Platillo> filterRows(PagingRequest pagingRequest) {
    log.debug("filtering Platillo rows with paging request {}", pagingRequest);
    String searchValue = pagingRequest.getSearch().getValue();
    if (searchValue == null || searchValue.isEmpty()) {
      return platillo -> true;
    }
    return platillo -> platillo.getName().toLowerCase().contains(searchValue.toLowerCase())
        || platillo.getIngestasSugeridas().toLowerCase().contains(searchValue.toLowerCase());
  }

  private Comparator<Platillo> sortRows(PagingRequest pagingRequest) {
    log.debug("sorting Platillo rows with paging request {}", pagingRequest);
    Comparator<Platillo> comparator = EMPTY_COMPARATOR;
    if (pagingRequest.getOrder() != null) {
      Order order = pagingRequest.getOrder().get(0);
      String column = pagingRequest.getColumns().get(order.getColumn()).getData();
      comparator = PlatilloComparators.getComparator(column, order.getDir());
    }
    log.debug("sorting rows with comparator {}", comparator);
    return comparator;
  }

}
