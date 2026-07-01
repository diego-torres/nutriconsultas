package com.nutriconsultas.alimentos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlimentosRepository extends JpaRepository<Alimento, Long> {

	List<Alimento> findByNombreAlimentoContainingIgnoreCase(String nombreAlimento);

	Optional<Alimento> findFirstByNombreAlimentoIgnoreCaseOrderByIdAsc(String nombreAlimento);

	@Query("SELECT a FROM Alimento a WHERE "
			+ "(LOWER(a.nombreAlimento) LIKE LOWER(:searchTerm) OR LOWER(a.clasificacion) LIKE LOWER(:searchTerm))")
	Page<Alimento> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

	@Query("SELECT COUNT(a) FROM Alimento a WHERE "
			+ "(LOWER(a.nombreAlimento) LIKE LOWER(:searchTerm) OR LOWER(a.clasificacion) LIKE LOWER(:searchTerm))")
	long countBySearchTerm(@Param("searchTerm") String searchTerm);

	@Query("SELECT a FROM Alimento a WHERE "
			+ "(LOWER(a.nombreAlimento) LIKE LOWER(:searchTerm) OR LOWER(a.clasificacion) LIKE LOWER(:searchTerm)) "
			+ "AND (:clasificacionFilter IS NULL OR LOWER(a.clasificacion) LIKE LOWER(:clasificacionFilter))")
	Page<Alimento> findForCatalogSearch(@Param("searchTerm") String searchTerm,
			@Param("clasificacionFilter") String clasificacionFilter, Pageable pageable);

	@Query("SELECT COUNT(a) FROM Alimento a WHERE "
			+ "(LOWER(a.nombreAlimento) LIKE LOWER(:searchTerm) OR LOWER(a.clasificacion) LIKE LOWER(:searchTerm)) "
			+ "AND (:clasificacionFilter IS NULL OR LOWER(a.clasificacion) LIKE LOWER(:clasificacionFilter))")
	long countForCatalogSearch(@Param("searchTerm") String searchTerm,
			@Param("clasificacionFilter") String clasificacionFilter);

}
