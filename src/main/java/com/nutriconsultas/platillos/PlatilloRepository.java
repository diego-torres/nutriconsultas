package com.nutriconsultas.platillos;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface PlatilloRepository extends JpaRepository<Platillo, Long> {

	Optional<Platillo> findFirstByNameIgnoreCaseOrderByIdAsc(String name);

	Optional<Platillo> findByIdAndUserId(Long id, String userId);

	List<Platillo> findByUserId(String userId);

	List<Platillo> findByUserIdIn(Collection<String> userIds);

	@Modifying
	@Transactional
	@Query("delete from Ingrediente i where i.id = ?1")
	void deleteIngrediente(Long id);

	List<Platillo> findByNameContainingIgnoreCase(String name);

	@Query("SELECT p FROM Platillo p WHERE " + "(LOWER(p.name) LIKE LOWER(:searchTerm) OR "
			+ "(p.ingestasSugeridas IS NOT NULL AND LOWER(p.ingestasSugeridas) LIKE LOWER(:searchTerm)))")
	Page<Platillo> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

	@Query("SELECT COUNT(p) FROM Platillo p WHERE " + "(LOWER(p.name) LIKE LOWER(:searchTerm) OR "
			+ "(p.ingestasSugeridas IS NOT NULL AND LOWER(p.ingestasSugeridas) LIKE LOWER(:searchTerm)))")
	long countBySearchTerm(@Param("searchTerm") String searchTerm);

	@Query("SELECT p FROM Platillo p WHERE p.userId IN :userIds AND " + "(LOWER(p.name) LIKE LOWER(:searchTerm) OR "
			+ "(p.ingestasSugeridas IS NOT NULL AND LOWER(p.ingestasSugeridas) LIKE LOWER(:searchTerm))) "
			+ "AND (:ingestasFilter IS NULL OR (p.ingestasSugeridas IS NOT NULL "
			+ "AND LOWER(p.ingestasSugeridas) LIKE LOWER(:ingestasFilter)))")
	Page<Platillo> findForAuthorizedCatalogSearch(@Param("userIds") Collection<String> userIds,
			@Param("searchTerm") String searchTerm, @Param("ingestasFilter") String ingestasFilter, Pageable pageable);

}
