package com.nutriconsultas.alimentos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlimentosRepository extends JpaRepository<Alimento, Long> {

	List<Alimento> findByNombreAlimentoContainingIgnoreCase(String nombreAlimento);

}
