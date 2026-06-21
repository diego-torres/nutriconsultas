package com.nutriconsultas.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteService;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.subscription.lifecycle.SubscriptionAccessService;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PublicBookingServiceImpl implements PublicBookingService {

	private static final DateTimeFormatter SLOT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	private static final String DEFAULT_DISPLAY_NAME = "Consulta nutricional";

	private static final String ADVANCE_NOTICE = "Las citas requieren al menos 2 días de anticipación.";

	private final NutritionistProfileRepository profileRepository;

	private final SubscriptionAccessService subscriptionAccessService;

	private final NutritionistAvailabilityService availabilityService;

	private final BookingAvailabilitySlotService bookingAvailabilitySlotService;

	private final PacienteRepository pacienteRepository;

	private final PacienteService pacienteService;

	private final CalendarEventService calendarEventService;

	public PublicBookingServiceImpl(final NutritionistProfileRepository profileRepository,
			final SubscriptionAccessService subscriptionAccessService,
			final NutritionistAvailabilityService availabilityService,
			final BookingAvailabilitySlotService bookingAvailabilitySlotService,
			final PacienteRepository pacienteRepository, final PacienteService pacienteService,
			final CalendarEventService calendarEventService) {
		this.profileRepository = profileRepository;
		this.subscriptionAccessService = subscriptionAccessService;
		this.availabilityService = availabilityService;
		this.bookingAvailabilitySlotService = bookingAvailabilitySlotService;
		this.pacienteRepository = pacienteRepository;
		this.pacienteService = pacienteService;
		this.calendarEventService = calendarEventService;
	}

	@Override
	@Transactional(readOnly = true)
	public PublicBookingNutritionistContext resolveContext(@NonNull final String publicBookingId) {
		final NutritionistProfile profile = resolveActiveProfile(publicBookingId);
		final AvailabilityScheduleDto schedule = availabilityService.getSchedule(profile.getUserId());
		return new PublicBookingNutritionistContext(profile.getPublicBookingId(), resolveDisplayName(profile),
				schedule.getTimezone(), BookingAvailabilityConstants.MIN_BOOKING_ADVANCE_DAYS);
	}

	@Override
	@Transactional(readOnly = true)
	public PublicBookingSlotsResponse getPublicSlots(@NonNull final String publicBookingId,
			@NonNull final String date) {
		final NutritionistProfile profile = resolveActiveProfile(publicBookingId);
		final AvailabilityScheduleDto schedule = availabilityService.getSchedule(profile.getUserId());
		final ZoneId zoneId = ZoneId.of(schedule.getTimezone());
		final LocalDate parsedDate = LocalDate.parse(date);
		final LocalDate minBookableDate = PublicBookingAdvanceRules.earliestBookableDate(zoneId);
		if (!PublicBookingAdvanceRules.isDateBookable(parsedDate, zoneId)) {
			return new PublicBookingSlotsResponse(parsedDate.toString(), minBookableDate,
					BookingAvailabilityConstants.MIN_BOOKING_ADVANCE_DAYS, List.of(), ADVANCE_NOTICE);
		}
		final List<String> slots = bookingAvailabilitySlotService
			.getAvailableSlotStarts(profile.getUserId(), parsedDate)
			.stream()
			.map(SLOT_TIME_FORMAT::format)
			.collect(Collectors.toList());
		return new PublicBookingSlotsResponse(parsedDate.toString(), minBookableDate,
				BookingAvailabilityConstants.MIN_BOOKING_ADVANCE_DAYS, slots, null);
	}

	@Override
	@Transactional
	public PublicBookingConfirmation book(@NonNull final String publicBookingId,
			@NonNull final PublicBookingRequestDto request) {
		final NutritionistProfile profile = resolveActiveProfile(publicBookingId);
		final String userId = profile.getUserId();
		final AvailabilityScheduleDto schedule = availabilityService.getSchedule(userId);
		final ZoneId zoneId = ZoneId.of(schedule.getTimezone());
		final LocalDate date = LocalDate.parse(request.getDate());
		final LocalTime time = LocalTime.parse(request.getTime(), SLOT_TIME_FORMAT);
		final LocalDateTime slotStart = date.atTime(time);
		if (!PublicBookingAdvanceRules.isSlotBookable(slotStart, zoneId)) {
			throw new IllegalArgumentException(ADVANCE_NOTICE);
		}
		final List<LocalTime> available = bookingAvailabilitySlotService.getAvailableSlotStarts(userId, date);
		if (!available.contains(time)) {
			throw new IllegalArgumentException("El horario seleccionado ya no está disponible");
		}
		final Paciente paciente = findOrCreatePatient(userId, request);
		final CalendarEvent event = new CalendarEvent();
		event.setPaciente(paciente);
		event.setTitle("Consulta");
		event.setDescription("Reserva en línea");
		event.setStatus(EventStatus.SCHEDULED);
		event.setDurationMinutes(schedule.getSlotDurationMinutes());
		event.setEventDateTime(Date.from(slotStart.atZone(zoneId).toInstant()));
		final CalendarEvent saved = calendarEventService.save(event);
		log.info("Public booking created event id={} for nutritionist userId={}", saved.getId(),
				LogRedaction.redactUserId(userId));
		return new PublicBookingConfirmation(saved.getId(), date.toString(), SLOT_TIME_FORMAT.format(time));
	}

	private NutritionistProfile resolveActiveProfile(final String publicBookingId) {
		if (!StringUtils.hasText(publicBookingId)) {
			throw new PublicBookingNotFoundException();
		}
		final NutritionistProfile profile = profileRepository.findByPublicBookingId(publicBookingId.trim())
			.orElseThrow(PublicBookingNotFoundException::new);
		if (subscriptionAccessService.findGrantingSubscriptionForUser(profile.getUserId()).isEmpty()) {
			throw new PublicBookingNotFoundException();
		}
		return profile;
	}

	private Paciente findOrCreatePatient(final String userId, final PublicBookingRequestDto request) {
		final String email = request.getPatientEmail().trim();
		final Optional<Paciente> existing = pacienteRepository.findFirstByUserIdAndEmailIgnoreCase(userId, email);
		if (existing.isPresent()) {
			final Paciente paciente = existing.get();
			if (StringUtils.hasText(request.getPatientPhone()) && !StringUtils.hasText(paciente.getPhone())) {
				paciente.setPhone(request.getPatientPhone().trim());
				return pacienteRepository.save(paciente);
			}
			return paciente;
		}
		return pacienteService.save(PublicBookingPatientFactory.buildProspect(userId, request));
	}

	private static String resolveDisplayName(final NutritionistProfile profile) {
		if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
			return profile.getDisplayName().trim();
		}
		return DEFAULT_DISPLAY_NAME;
	}

}
