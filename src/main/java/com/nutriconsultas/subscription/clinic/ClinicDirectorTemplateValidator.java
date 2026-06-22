package com.nutriconsultas.subscription.clinic;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.clinic.CreateClinicInvitationForm;
import com.nutriconsultas.subscription.ClinicMemberRole;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.validation.template.BaseTemplateValidator;

public class ClinicDirectorTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/clinic/**";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		variables.put("platformAdmin", false);
		variables.put("clinicDirector", true);
		variables.put("activeMenu", "clinic");
		variables.put("successMessage", null);
		variables.put("errorMessage", null);
		variables.put("inviteUrl", null);
		variables.put("inviteForm", createMockInviteForm());
		variables.put("roster", createMockRoster());
		variables.put("transferPage", createMockTransferPage());
		variables.put("transferForm", createMockTransferForm());
		variables.put("warningMessage", null);
		return variables;
	}

	private static CreateClinicInvitationForm createMockInviteForm() {
		final CreateClinicInvitationForm form = new CreateClinicInvitationForm();
		form.setEmail("");
		return form;
	}

	private static ClinicRosterOverview createMockRoster() {
		final ClinicMemberView director = new ClinicMemberView(1L, "auth0|director", "Dra. Demo",
				ClinicMemberRole.DIRECTOR, MembershipStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z"), true);
		final ClinicMemberView nutritionist = new ClinicMemberView(2L, "auth0|nutri-1", "nutriologo@example.com",
				ClinicMemberRole.NUTRITIONIST, MembershipStatus.ACTIVE, Instant.parse("2026-02-01T00:00:00Z"), false);
		final ClinicInvitationView pendingInvite = new ClinicInvitationView(10L, "pendiente@example.com",
				Instant.parse("2026-02-08T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"));
		return new ClinicRosterOverview(1L, "Consultorio Demo", PlanTier.CONSULTORIO, SubscriptionStatus.ACTIVE, 20, 2L,
				1L, List.of(director, nutritionist), List.of(pendingInvite));
	}

	private static ClinicPatientTransferPage createMockTransferPage() {
		final ClinicMemberView director = new ClinicMemberView(1L, "auth0|director", "Dra. Demo",
				ClinicMemberRole.DIRECTOR, MembershipStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z"), true);
		final ClinicMemberView nutritionist = new ClinicMemberView(2L, "auth0|nutri-1", "nutriologo@example.com",
				ClinicMemberRole.NUTRITIONIST, MembershipStatus.ACTIVE, Instant.parse("2026-02-01T00:00:00Z"), false);
		return new ClinicPatientTransferPage(List.of(director, nutritionist), 2L,
				List.of(new ClinicPatientSummary(100L, "Paciente Demo")));
	}

	private static com.nutriconsultas.clinic.ClinicPatientTransferForm createMockTransferForm() {
		final com.nutriconsultas.clinic.ClinicPatientTransferForm form = new com.nutriconsultas.clinic.ClinicPatientTransferForm();
		form.setSourceMemberId(2L);
		form.setTargetMemberId(1L);
		return form;
	}

}
