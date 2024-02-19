package com.nutriconsultas.platillos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("rest")
@Slf4j
public class PlatilloRestController {
  private static final Comparator<Platillo> EMPTY_COMPARATOR = (o1, o2) -> 0;

  @Autowired
  private PlatilloService service;

  @PostMapping("platillos")
  public PageArray array(@RequestBody PagingRequest pagingRequest) {
    log.info("starting array with paging request {}.", pagingRequest);
    String[] columns = {"platillo", "ingestas", "kcal", "prot", "lip", "hc"};
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
        row.getIngestasSugeridas()==null?"":row.getIngestasSugeridas(),
        row.getEnergia()==null?"": row.getEnergia().toString(),
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
