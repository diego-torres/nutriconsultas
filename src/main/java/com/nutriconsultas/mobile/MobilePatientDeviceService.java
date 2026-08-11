package com.nutriconsultas.mobile;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.device.PatientDevice;
import com.nutriconsultas.device.PatientDevicePlatform;
import com.nutriconsultas.device.PatientDeviceRepository;
import com.nutriconsultas.mobile.dto.PatientDeviceDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilePatientDeviceService {

	private final PatientDeviceRepository patientDeviceRepository;

	private final PacienteRepository pacienteRepository;

	public MobilePatientDeviceService(final PatientDeviceRepository patientDeviceRepository,
			final PacienteRepository pacienteRepository) {
		this.patientDeviceRepository = patientDeviceRepository;
		this.pacienteRepository = pacienteRepository;
	}

	@Transactional
	public PatientDeviceDto upsertDevice(final Long pacienteId, final PatientDevicePlatform platform,
			final String token, final String appVersion) {
		final Instant now = Instant.now();
		final PatientDevice device = patientDeviceRepository.findByToken(token).orElseGet(PatientDevice::new);
		final boolean isNew = device.getId() == null;
		final Paciente pacienteRef = pacienteRepository.getReferenceById(pacienteId);
		device.setPaciente(pacienteRef);
		device.setPlatform(platform);
		device.setToken(token);
		device.setAppVersion(StringUtils.hasText(appVersion) ? appVersion.trim() : null);
		device.setLastSeenAt(now);
		device.setUpdatedAt(now);
		final PatientDevice saved = patientDeviceRepository.save(device);
		if (log.isInfoEnabled()) {
			log.info("Patient device {} for patient {} platform={} token={}", isNew ? "registered" : "refreshed",
					LogRedaction.redactPaciente(pacienteId), platform, LogRedaction.redactDeviceToken(token));
		}
		return PatientDeviceDto.fromEntity(saved);
	}

	@Transactional
	public void deregisterDevice(final Long pacienteId, final String token) {
		final long removed = patientDeviceRepository.deleteByPacienteIdAndToken(pacienteId, token);
		if (log.isInfoEnabled()) {
			log.info("Patient device deregister removed={} for patient {} token={}", removed,
					LogRedaction.redactPaciente(pacienteId), LogRedaction.redactDeviceToken(token));
		}
	}

}
