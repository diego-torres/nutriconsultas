package com.nutriconsultas.platillos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface PlatilloRepository extends JpaRepository<Platillo, Long> {

	@Modifying
	@Transactional
	@Query("delete from Ingrediente i where i.id = ?1")
	void deleteIngrediente(Long id);

	List<Platillo> findByNameContainingIgnoreCase(String name);

}
