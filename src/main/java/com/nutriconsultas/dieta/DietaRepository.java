package com.nutriconsultas.dieta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DietaRepository extends JpaRepository<Dieta, Long> {

	java.util.Optional<Dieta> findByIdAndUserId(Long id, String userId);

	java.util.List<Dieta> findByUserId(String userId);

}
