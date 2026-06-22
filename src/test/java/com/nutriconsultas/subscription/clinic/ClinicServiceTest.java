package com.nutriconsultas.subscription.clinic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicInvitationRepository;
import com.nutriconsultas.subscription.ClinicMember;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicMemberRole;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.Entitlement;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.subscription.lifecycle.SubscriptionAccessService;

@ExtendWith(MockitoExtension.class)
class ClinicServiceTest {

	private static final String DIRECTOR_ID = "auth0|director-1";

	private static final String OTHER_DIRECTOR_ID = "auth0|director-2";

	private static final String NUTRITIONIST_ID = "auth0|nutri-1";

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Mock
	private ClinicRepository clinicRepository;

	@Mock
	private ClinicMemberRepository clinicMemberRepository;

	@Mock
	private ClinicInvitationRepository clinicInvitationRepository;

	@Mock
	private ClinicMemberLabelResolver clinicMemberLabelResolver;

	@Mock
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@Mock
	private SubscriptionAccessService subscriptionAccessService;

	@Mock
	private SubscriptionAuditEventRepository subscriptionAuditEventRepository;

	@Mock
	private Auth0RoleSyncClient auth0RoleSyncClient;

	@Mock
	private PacienteRepository pacienteRepository;

	private Clinic clinic;

	private Subscription subscription;

	@BeforeEach
	void setUp() {
		subscription = new Subscription();
		subscription.setId(10L);
		subscription.setPlanTier(PlanTier.CONSULTORIO);
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		clinic = new Clinic();
		clinic.setId(1L);
		clinic.setName("Consultorio Norte");
		clinic.setDirectorUserId(DIRECTOR_ID);
		clinic.setSubscription(subscription);
	}

	@Test
	void getDirectorRoster_returnsSeatUsageAndMembers() {
		stubDirectorAccess();
		final ClinicMember director = member(1L, DIRECTOR_ID, ClinicMemberRole.DIRECTOR, MembershipStatus.ACTIVE);
		final ClinicMember nutritionist = member(2L, NUTRITIONIST_ID, ClinicMemberRole.NUTRITIONIST,
				MembershipStatus.ACTIVE);
		when(clinicMemberRepository.findByClinicIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(director, nutritionist));
		when(clinicMemberRepository.countByClinicIdAndMembershipStatus(1L, MembershipStatus.ACTIVE)).thenReturn(2L);
		when(clinicInvitationRepository.countByClinicIdAndStatus(1L, InvitationStatus.PENDING)).thenReturn(1L);
		when(clinicInvitationRepository.findByClinicIdAndStatusOrderByCreatedAtDesc(1L, InvitationStatus.PENDING))
			.thenReturn(List.of());
		when(clinicMemberLabelResolver.resolveLabel(NUTRITIONIST_ID)).thenReturn("Lic. Ana");
		when(clinicMemberLabelResolver.resolveLabel(DIRECTOR_ID)).thenReturn("director@example.com");

		final ClinicRosterOverview roster = clinicService.getDirectorRoster(DIRECTOR_ID);

		assertThat(roster.clinicId()).isEqualTo(1L);
		assertThat(roster.maxNutritionists()).isEqualTo(20);
		assertThat(roster.activeSeatCount()).isEqualTo(2L);
		assertThat(roster.pendingInviteCount()).isEqualTo(1L);
		assertThat(roster.members()).hasSize(2);
		assertThat(roster.members().get(1).displayLabel()).isEqualTo("Lic. Ana");
	}

	@Test
	void getDirectorRoster_whenNotDirector_throwsForbidden() {
		when(subscriptionEntitlementService.hasEntitlement(OTHER_DIRECTOR_ID, Entitlement.USER_ADMINISTRATION))
			.thenReturn(false);

		assertThatThrownBy(() -> clinicService.getDirectorRoster(OTHER_DIRECTOR_ID))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void suspendMember_updatesStatusAndRecordsAudit() {
		stubDirectorAccess();
		final ClinicMember nutritionist = member(2L, NUTRITIONIST_ID, ClinicMemberRole.NUTRITIONIST,
				MembershipStatus.ACTIVE);
		when(clinicMemberRepository.findById(2L)).thenReturn(Optional.of(nutritionist));
		when(auth0RoleSyncClient.isConfigured()).thenReturn(false);

		clinicService.suspendMember(DIRECTOR_ID, 2L);

		assertThat(nutritionist.getMembershipStatus()).isEqualTo(MembershipStatus.SUSPENDED);
		verify(clinicMemberRepository).save(nutritionist);
		final ArgumentCaptor<SubscriptionAuditEvent> auditCaptor = ArgumentCaptor
			.forClass(SubscriptionAuditEvent.class);
		verify(subscriptionAuditEventRepository).save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getEventType()).isEqualTo(SubscriptionAuditEventType.CLINIC_DIRECTOR_ACTION);
		assertThat(auditCaptor.getValue().getDetails()).contains("clinic.member.suspend");
	}

	@Test
	void suspendMember_whenMemberBelongsToOtherClinic_throwsForbidden() {
		stubDirectorAccess();
		final Clinic otherClinic = new Clinic();
		otherClinic.setId(99L);
		final ClinicMember foreignMember = member(3L, "auth0|foreign", ClinicMemberRole.NUTRITIONIST,
				MembershipStatus.ACTIVE);
		foreignMember.setClinic(otherClinic);
		when(clinicMemberRepository.findById(3L)).thenReturn(Optional.of(foreignMember));

		assertThatThrownBy(() -> clinicService.suspendMember(DIRECTOR_ID, 3L))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(HttpStatus.FORBIDDEN.value());
		verify(clinicMemberRepository, never()).save(any());
	}

	@Test
	void suspendMember_whenTargetIsDirector_throwsConflict() {
		stubDirectorAccess();
		final ClinicMember directorMember = member(1L, "auth0|other-director", ClinicMemberRole.DIRECTOR,
				MembershipStatus.ACTIVE);
		when(clinicMemberRepository.findById(1L)).thenReturn(Optional.of(directorMember));

		assertThatThrownBy(() -> clinicService.suspendMember(DIRECTOR_ID, 1L))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(HttpStatus.CONFLICT.value());
	}

	@Test
	void reactivateMember_whenSeatAvailable_updatesStatus() {
		stubDirectorAccess();
		final ClinicMember nutritionist = member(2L, NUTRITIONIST_ID, ClinicMemberRole.NUTRITIONIST,
				MembershipStatus.SUSPENDED);
		when(clinicMemberRepository.findById(2L)).thenReturn(Optional.of(nutritionist));

		clinicService.reactivateMember(DIRECTOR_ID, 2L);

		assertThat(nutritionist.getMembershipStatus()).isEqualTo(MembershipStatus.ACTIVE);
		verify(subscriptionEntitlementService).assertCanInviteNutritionist(DIRECTOR_ID);
		verify(clinicMemberRepository).save(nutritionist);
	}

	@Test
	void reactivateMember_whenNoSeatsAvailable_throwsConflict() {
		stubDirectorAccess();
		final ClinicMember nutritionist = member(2L, NUTRITIONIST_ID, ClinicMemberRole.NUTRITIONIST,
				MembershipStatus.SUSPENDED);
		when(clinicMemberRepository.findById(2L)).thenReturn(Optional.of(nutritionist));
		doThrow(new SubscriptionLimitExceededException("error.subscription.nutritionist_limit", 20))
			.when(subscriptionEntitlementService)
			.assertCanInviteNutritionist(DIRECTOR_ID);

		assertThatThrownBy(() -> clinicService.reactivateMember(DIRECTOR_ID, 2L))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(HttpStatus.CONFLICT.value());
		verify(clinicMemberRepository, never()).save(any());
	}

	@Test
	void transferPatients_updatesUserIdAndRecordsAudit() {
		stubDirectorAccess();
		final ClinicMember source = member(2L, NUTRITIONIST_ID, ClinicMemberRole.NUTRITIONIST, MembershipStatus.ACTIVE);
		final ClinicMember target = member(3L, "auth0|nutri-2", ClinicMemberRole.NUTRITIONIST, MembershipStatus.ACTIVE);
		when(clinicMemberRepository.findById(2L)).thenReturn(Optional.of(source));
		when(clinicMemberRepository.findById(3L)).thenReturn(Optional.of(target));
		final Paciente paciente = new Paciente();
		paciente.setId(50L);
		paciente.setUserId(NUTRITIONIST_ID);
		paciente.setName("Paciente A");
		when(pacienteRepository.findByIdInAndUserId(List.of(50L), NUTRITIONIST_ID)).thenReturn(List.of(paciente));

		final ClinicPatientTransferResult result = clinicService.transferPatients(DIRECTOR_ID, 2L, 3L, List.of(50L),
				false);

		assertThat(result.transferredCount()).isEqualTo(1);
		assertThat(paciente.getUserId()).isEqualTo("auth0|nutri-2");
		verify(pacienteRepository).saveAll(List.of(paciente));
		final ArgumentCaptor<SubscriptionAuditEvent> auditCaptor = ArgumentCaptor
			.forClass(SubscriptionAuditEvent.class);
		verify(subscriptionAuditEventRepository).save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getDetails()).contains("clinic.patient.transfer");
		assertThat(auditCaptor.getValue().getDetails()).contains("patientIds=50");
	}

	@Test
	void transferPatients_whenTargetSuspended_throwsConflict() {
		stubDirectorAccess();
		final ClinicMember source = member(2L, NUTRITIONIST_ID, ClinicMemberRole.NUTRITIONIST, MembershipStatus.ACTIVE);
		final ClinicMember suspended = member(3L, "auth0|nutri-2", ClinicMemberRole.NUTRITIONIST,
				MembershipStatus.SUSPENDED);
		when(clinicMemberRepository.findById(2L)).thenReturn(Optional.of(source));
		when(clinicMemberRepository.findById(3L)).thenReturn(Optional.of(suspended));

		assertThatThrownBy(() -> clinicService.transferPatients(DIRECTOR_ID, 2L, 3L, List.of(50L), false))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(HttpStatus.CONFLICT.value());
		verify(pacienteRepository, never()).saveAll(any());
	}

	@Test
	void transferPatients_whenMemberOutsideClinic_throwsForbidden() {
		stubDirectorAccess();
		final Clinic otherClinic = new Clinic();
		otherClinic.setId(99L);
		final ClinicMember foreignMember = member(5L, "auth0|foreign", ClinicMemberRole.NUTRITIONIST,
				MembershipStatus.ACTIVE);
		foreignMember.setClinic(otherClinic);
		when(clinicMemberRepository.findById(5L)).thenReturn(Optional.of(foreignMember));

		assertThatThrownBy(() -> clinicService.transferPatients(DIRECTOR_ID, 5L, 3L, List.of(50L), false))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void getPatientTransferPage_listsActiveMembersAndSourcePatients() {
		stubDirectorAccess();
		final ClinicMember director = member(1L, DIRECTOR_ID, ClinicMemberRole.DIRECTOR, MembershipStatus.ACTIVE);
		final ClinicMember nutritionist = member(2L, NUTRITIONIST_ID, ClinicMemberRole.NUTRITIONIST,
				MembershipStatus.ACTIVE);
		when(clinicMemberRepository.findByClinicIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(director, nutritionist));
		when(clinicMemberRepository.findById(2L)).thenReturn(Optional.of(nutritionist));
		final Paciente paciente = new Paciente();
		paciente.setId(10L);
		paciente.setName("Ana");
		when(pacienteRepository.findByUserIdOrderByNameAsc(NUTRITIONIST_ID)).thenReturn(List.of(paciente));
		when(clinicMemberLabelResolver.resolveLabel(NUTRITIONIST_ID)).thenReturn("Lic. Ana");
		when(clinicMemberLabelResolver.resolveLabel(DIRECTOR_ID)).thenReturn("Director");

		final ClinicPatientTransferPage page = clinicService.getPatientTransferPage(DIRECTOR_ID, 2L);

		assertThat(page.activeMembers()).hasSize(2);
		assertThat(page.sourcePatients()).hasSize(1);
		assertThat(page.sourcePatients().get(0).patientId()).isEqualTo(10L);
	}

	private void stubDirectorAccess() {
		when(subscriptionEntitlementService.hasEntitlement(DIRECTOR_ID, Entitlement.USER_ADMINISTRATION))
			.thenReturn(true);
		when(clinicRepository.findByDirectorUserIdWithSubscription(DIRECTOR_ID)).thenReturn(Optional.of(clinic));
		lenient().when(subscriptionAccessService.findGrantingSubscriptionForUser(DIRECTOR_ID))
			.thenReturn(Optional.of(subscription));
		lenient()
			.when(clinicInvitationRepository.findByClinicIdAndStatusOrderByCreatedAtDesc(1L, InvitationStatus.PENDING))
			.thenReturn(List.of());
	}

	private ClinicMember member(final Long id, final String userId, final ClinicMemberRole role,
			final MembershipStatus status) {
		final ClinicMember member = new ClinicMember();
		member.setId(id);
		member.setClinic(clinic);
		member.setUserId(userId);
		member.setRole(role);
		member.setMembershipStatus(status);
		member.setCreatedAt(Instant.parse("2026-01-15T12:00:00Z"));
		return member;
	}

}
