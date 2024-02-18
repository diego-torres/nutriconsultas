package com.nutriconsultas.alimentos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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
@RequestMapping("/rest")
@Slf4j
public class AlimentoRestController {
  private static final Comparator<Alimento> EMPTY_COMPARATOR = (o1, o2) -> 0;

  @Autowired
  private AlimentoService service;

  @PostMapping("alimentos")
  public PageArray array(@RequestBody PagingRequest pagingRequest) {
    log.info("starting array with paging request {}.", pagingRequest);
    String [] columns = {"alimento", "grupo", "cantidad", "unidad", "bruto", "neto", "kcal", "prot", "lip", "hc"};
    log.debug("setting columns at page request {}", (Object) columns);
    pagingRequest.setColumns(
        Stream.of(columns)
            .map(Column::new)
            .collect(Collectors.toList()));
    Page<Alimento> page = getRows(pagingRequest);
    log.debug("Alimentos page with records {}", page.getRecordsTotal());
    PageArray pageArray = new PageArray();
    pageArray.setRecordsFiltered(page.getRecordsFiltered());
    pageArray.setRecordsTotal(page.getRecordsTotal());
    pageArray.setDraw(page.getDraw());
    pageArray.setData(page.getData()
        .stream()
        .map(this::toStringList)
        .collect(Collectors.toList()));

    log.info("finish Alimentos page array with records {}", pageArray.getRecordsTotal());
    return pageArray;
  }

  private List<String> toStringList(Alimento row) {
    log.debug("converting Alimento row {} to string list.", row);
    return Arrays.asList(
        "<a href='/admin/alimentos/" + row.getId() + "'>" + row.getNombreAlimento() + "</a>",
        row.getClasificacion(), //
        row.getCantSugerida(), //
        row.getUnidad(), //
        row.getPesoBrutoRedondeado().toString(), //
        row.getPesoNeto().toString(), //
        row.getEnergia().toString(), //
        String.format("%.1f", row.getProteina()), //
        String.format("%.1f", row.getLipidos()), //
        String.format("%.1f", row.getHidratosDeCarbono()));
  }

  private Page<Alimento> getRows(PagingRequest pagingRequest) {
    log.debug("Requesting Alimentos page with paging request {}.", pagingRequest);
    return getPage(StreamSupport
        .stream(service.findAll().spliterator(), false)
        .collect(Collectors.toList()), pagingRequest);
  }

  private Page<Alimento> getPage(List<Alimento> rows, PagingRequest pagingRequest) {
    log.debug("converting {} Alimento records to page with request {}", rows.size(), pagingRequest);
    List<Alimento> filtered = rows.stream()
        .sorted(sortRows(pagingRequest))
        .filter(filterRows(pagingRequest))
        .skip(pagingRequest.getStart())
        .limit(pagingRequest.getLength())
        .collect(Collectors.toList());

    log.debug("filtered records {}", filtered.size());
    long count = rows.stream()
        .filter(filterRows(pagingRequest))
        .count();
    log.debug("total records count {}", count);

    Page<Alimento> page = new Page<>(filtered);
    page.setRecordsFiltered((int) count);
    page.setRecordsTotal((int) count);
    page.setDraw(pagingRequest.getDraw());

    log.debug("returning Alimentos page with records {}", page.getRecordsTotal());
    return page;
  }

  private Predicate<Alimento> filterRows(PagingRequest pagingRequest) {
    log.debug("filtering Alimento with paging request {}.", pagingRequest);
    if (pagingRequest.getSearch() == null || !StringUtils.hasLength(pagingRequest.getSearch().getValue())) {
      log.debug("page request search is null or empty. Returning true.");
      return row -> true;
    }

    String value = pagingRequest.getSearch().getValue().toLowerCase();
    log.debug("page request search value {}.", value);

    return row -> row.getNombreAlimento().toLowerCase().contains(value)
        || row.getNombreAlimento().toLowerCase().startsWith(value)
        || row.getClasificacion().toLowerCase().contains(value)
        || row.getClasificacion().toLowerCase().startsWith(value);
  }

  private Comparator<Alimento> sortRows(PagingRequest pagingRequest) {
    log.debug("sorting Alimento with paging request {}.", pagingRequest);
    if (pagingRequest.getOrder() == null) {
      log.debug("page request order is null. Returning empty comparator.");
      return EMPTY_COMPARATOR;
    }

    try {
      Order order = pagingRequest.getOrder().get(0);
      log.debug("page request order column {} and direction {}.", order.getColumn(), order.getDir());

      int columnIndex = order.getColumn();
      log.debug("page request order column index {}.", columnIndex);
      Column column = pagingRequest.getColumns().get(columnIndex);
      log.debug("page request order column data {}.", column.getData());

      Comparator<Alimento> comparator = AlimentoComparators.getComparator(column.getData(),
          order.getDir());
      log.debug("returning Alimento comparator {}.", comparator);
      if (comparator == null) {
        return EMPTY_COMPARATOR;
      }

      return comparator;

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return EMPTY_COMPARATOR;
  }
}
