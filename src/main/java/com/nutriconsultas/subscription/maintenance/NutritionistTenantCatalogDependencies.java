package com.nutriconsultas.subscription.maintenance;

import org.springframework.stereotype.Component;

import com.nutriconsultas.dieta.DietaRepository;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.paciente.PacienteDeletionService;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platillos.PlatilloRepository;
import com.nutriconsultas.platillos.PlatilloService;

@Component
public final class NutritionistTenantCatalogDependencies {

	private final PacienteRepository pacientes;

	private final PacienteDeletionService patientDeletion;

	private final DietaRepository dietas;

	private final DietaService dietaService;

	private final PlatilloRepository platillos;

	private final PlatilloService platilloService;

	public NutritionistTenantCatalogDependencies(final PacienteRepository pacientes,
			final PacienteDeletionService patientDeletion, final DietaRepository dietas,
			final DietaService dietaService, final PlatilloRepository platillos,
			final PlatilloService platilloService) {
		this.pacientes = pacientes;
		this.patientDeletion = patientDeletion;
		this.dietas = dietas;
		this.dietaService = dietaService;
		this.platillos = platillos;
		this.platilloService = platilloService;
	}

	public PacienteRepository getPacientes() {
		return pacientes;
	}

	public PacienteDeletionService getPatientDeletion() {
		return patientDeletion;
	}

	public DietaRepository getDietas() {
		return dietas;
	}

	public DietaService getDietaService() {
		return dietaService;
	}

	public PlatilloRepository getPlatillos() {
		return platillos;
	}

	public PlatilloService getPlatilloService() {
		return platilloService;
	}

}
