package com.nutriconsultas.paciente;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.consulta.Consulta;
import com.nutriconsultas.consulta.ConsultaComparators;
import com.nutriconsultas.consulta.ConsultaRepository;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

@RestController
@RequestMapping("/rest")
public class PacienteConsultaRestController {
  private static final Logger logger = LoggerFactory.getLogger(PacienteConsultaRestController.class);
  private static final Comparator<Consulta> EMPTY_COMPARATOR = (o1, o2) -> 0;

  @Autowired
  private ConsultaRepository repo;

  @PostMapping("pacientes/{id}/consultas")
  public PageArray Array(@PathVariable("id") Long id, @RequestBody PagingRequest pagingRequest) {
    pagingRequest.setColumns(
        Stream.of("id", "fecha", "peso", "estatura", "imc", "presion", "indGluc")
            .map(Column::new)
            .collect(Collectors.toList()));
    Page<Consulta> page = getRows(id, pagingRequest);
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

  private List<String> toStringList(Consulta row) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return Arrays.asList(
        "<a href='/admin/consultas/" + row.getId() + "'>" + row.getId() + "</a>",
        row.getFechaConsulta() != null ? dateFormat.format(row.getFechaConsulta()) : "", //
        row.getPeso().toString(), //
        row.getEstatura().toString(), //
        row.getImc() != null ? String.format("%.2f", row.getImc()) : "",
        row.getSistolica().toString() + "/" + row.getDiastolica().toString(), //
        row.getIndiceGlucemico().toString());
  }

  private Page<Consulta> getRows(Long pacienteId, PagingRequest pagingRequest) {
    return getPage(StreamSupport
        .stream(repo.findByPacienteId(pacienteId).spliterator(), false)
        .collect(Collectors.toList()), pagingRequest);
  }

  private Page<Consulta> getPage(List<Consulta> rows, PagingRequest pagingRequest) {
    List<Consulta> filtered = rows.stream()
        .sorted(sortRows(pagingRequest))
        // .filter(filterRows(pagingRequest))
        .skip(pagingRequest.getStart())
        .limit(pagingRequest.getLength())
        .collect(Collectors.toList());

    long count = rows.stream()
        // .filter(filterRows(pagingRequest))
        .count();

    Page<Consulta> page = new Page<>(filtered);
    page.setRecordsFiltered((int) count);
    page.setRecordsTotal((int) count);
    page.setDraw(pagingRequest.getDraw());

    return page;
  }
  // filter not required for the list of consultas for a paciente.

  private Comparator<Consulta> sortRows(PagingRequest pagingRequest) {
    if (pagingRequest.getOrder() == null) {
      return EMPTY_COMPARATOR;
    }

    try {
      Order order = pagingRequest.getOrder().get(0);

      int columnIndex = order.getColumn();
      Column column = pagingRequest.getColumns().get(columnIndex);

      Comparator<Consulta> comparator = ConsultaComparators.getComparator(column.getData(),
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
