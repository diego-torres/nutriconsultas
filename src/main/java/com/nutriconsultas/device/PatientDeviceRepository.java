package com.nutriconsultas.device;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientDeviceRepository extends JpaRepository<PatientDevice, Long> {

	Optional<PatientDevice> findByToken(String token);

	List<PatientDevice> findByPacienteId(Long pacienteId);

	long deleteByPacienteIdAndToken(Long pacienteId, String token);

	@Modifying(flushAutomatically = true)
	@Query("DELETE FROM PatientDevice d WHERE d.paciente.id = :pacienteId")
	void deleteByPacienteId(@Param("pacienteId") Long pacienteId);

}
