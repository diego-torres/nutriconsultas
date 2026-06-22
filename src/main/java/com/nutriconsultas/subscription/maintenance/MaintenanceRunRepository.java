package com.nutriconsultas.subscription.maintenance;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRunRepository extends JpaRepository<MaintenanceRun, String> {

	Optional<MaintenanceRun> findFirstByOrderByStartedAtDesc();

	Page<MaintenanceRun> findAllByOrderByStartedAtDesc(Pageable pageable);

}
