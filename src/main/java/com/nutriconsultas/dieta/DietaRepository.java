package com.nutriconsultas.dieta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DietaRepository extends JpaRepository<Dieta, Long> {

	java.util.Optional<Dieta> findByIdAndUserId(Long id, String userId);

	java.util.List<Dieta> findByUserId(String userId);

	java.util.List<Dieta> findByPacienteId(Long pacienteId);

	@org.springframework.data.jpa.repository.Query("SELECT d FROM Dieta d WHERE d.pacienteId IS NULL")
	java.util.List<Dieta> findAllCatalogDiets();

	@org.springframework.data.jpa.repository.Query("SELECT d FROM Dieta d WHERE d.userId = :userId AND d.pacienteId IS NULL")
	java.util.List<Dieta> findByUserIdAndPacienteIdIsNull(
			@org.springframework.data.repository.query.Param("userId") String userId);

	void deleteByPacienteId(Long pacienteId);

}
