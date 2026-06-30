package com.nutriconsultas.dieta;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatilloIngestaRepository extends JpaRepository<PlatilloIngesta, Long> {

	@Query("SELECT pi FROM PlatilloIngesta pi JOIN pi.ingesta i JOIN PacienteDieta pd ON pd.dieta = i.dieta "
			+ "WHERE pi.id = :platilloIngestaId AND pd.id = :assignmentId AND pd.paciente.id = :pacienteId")
	Optional<PlatilloIngesta> findByIdForPatientAssignment(@Param("platilloIngestaId") Long platilloIngestaId,
			@Param("assignmentId") Long assignmentId, @Param("pacienteId") Long pacienteId);

	@Query("SELECT pi FROM PlatilloIngesta pi JOIN pi.ingesta i JOIN PacienteDieta pd ON pd.dieta = i.dieta "
			+ "WHERE pd.id = :assignmentId AND pd.paciente.id = :pacienteId ORDER BY pi.id ASC")
	List<PlatilloIngesta> findByPatientAssignment(@Param("assignmentId") Long assignmentId,
			@Param("pacienteId") Long pacienteId);

	@Query("SELECT COUNT(pi) FROM PlatilloIngesta pi WHERE pi.sourcePlatilloId = :platilloId")
	long countBySourcePlatilloId(@Param("platilloId") Long platilloId);

	@Query("SELECT COUNT(pi) FROM PlatilloIngesta pi JOIN pi.ingesta i JOIN i.dieta d "
			+ "WHERE pi.sourcePlatilloId = :platilloId AND d.userId = :userId")
	long countBySourcePlatilloIdAndDietaUserId(@Param("platilloId") Long platilloId, @Param("userId") String userId);

}
