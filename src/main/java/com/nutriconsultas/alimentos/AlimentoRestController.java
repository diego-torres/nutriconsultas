package com.nutriconsultas.alimentos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.nutriconsultas.paciente.PacienteRestController;

@RestController
@RequestMapping("/rest")
public class AlimentoRestController {
  private static final Logger logger = LoggerFactory.getLogger(PacienteRestController.class);
  private static final Comparator<Alimento> EMPTY_COMPARATOR = (o1, o2) -> 0;

  @Autowired
  private AlimentosRepository repo;


  @PostMapping("alimentos")
  public PageArray array(@RequestBody PagingRequest pagingRequest) {
    pagingRequest.setColumns(
        Stream.of("alimento", "grupo", "cantidad", "unidad", "bruto", "neto", "kcal", "prot", "lip", "hc")
            .map(Column::new)
            .collect(Collectors.toList()));
    Page<Alimento> page = getRows(pagingRequest);
    PageArray pageArray = new PageArray();
    pageArray.setRecordsFiltered(page.getRecordsFiltered());
    pageArray.setRecordsTotal(page.getRecordsTotal());
    pageArray.setDraw(page.getDraw());
    pageArray.setData(page.getData()
        .stream()
        .map(this::toStringList)
        .collect(Collectors.toList()));

    return pageArray;
  }

  private List<String> toStringList(Alimento row) {
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
    return getPage(StreamSupport
        .stream(repo.findAll().spliterator(), false)
        .collect(Collectors.toList()), pagingRequest);
  }

  private Page<Alimento> getPage(List<Alimento> rows, PagingRequest pagingRequest) {
    List<Alimento> filtered = rows.stream()
        .sorted(sortRows(pagingRequest))
        .filter(filterRows(pagingRequest))
        .skip(pagingRequest.getStart())
        .limit(pagingRequest.getLength())
        .collect(Collectors.toList());

    long count = rows.stream()
        .filter(filterRows(pagingRequest))
        .count();

    Page<Alimento> page = new Page<>(filtered);
    page.setRecordsFiltered((int) count);
    page.setRecordsTotal((int) count);
    page.setDraw(pagingRequest.getDraw());

    return page;
  }

  private Predicate<Alimento> filterRows(PagingRequest pagingRequest) {
    if (pagingRequest.getSearch() == null || !StringUtils.hasLength(pagingRequest.getSearch().getValue())) {
      return row -> true;
    }

    String value = pagingRequest.getSearch().getValue().toLowerCase();

    return row -> row.getNombreAlimento().toLowerCase().contains(value)
        || row.getNombreAlimento().toLowerCase().startsWith(value)
        || row.getClasificacion().toLowerCase().contains(value)
        || row.getClasificacion().toLowerCase().startsWith(value);
  }

  private Comparator<Alimento> sortRows(PagingRequest pagingRequest) {
    if (pagingRequest.getOrder() == null) {
      return EMPTY_COMPARATOR;
    }

    try {
      Order order = pagingRequest.getOrder().get(0);

      int columnIndex = order.getColumn();
      Column column = pagingRequest.getColumns().get(columnIndex);

      Comparator<Alimento> comparator = AlimentoComparators.getComparator(column.getData(),
          order.getDir());
      if (comparator == null) {
        return EMPTY_COMPARATOR;
      }

      return comparator;

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }

    return EMPTY_COMPARATOR;
  }
}
