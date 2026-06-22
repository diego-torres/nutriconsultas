package com.nutriconsultas.subscription.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.PlanTier;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class SesInvitationEmailSenderTest {

	private static final String FROM = "invites@minutriporcion.com";

	private static final String INVITE_URL = "https://minutriporcion.com/invitation/nutritionist/redeem?token=abc";

	@Mock
	private InvitationEmailProperties invitationEmailProperties;

	@Mock
	private InvitationEmailTemplateRenderer templateRenderer;

	@Mock
	private SesV2Client sesV2Client;

	@Test
	void sendNutritionistInvitationUsesSesClient() {
		when(invitationEmailProperties.getFromAddress()).thenReturn(FROM);
		when(invitationEmailProperties.getSesRegion()).thenReturn("us-east-1");
		when(templateRenderer.subject()).thenReturn("Invitación a Minutriporción");
		when(templateRenderer.renderHtmlBody(PlanTier.PROFESIONAL, INVITE_URL)).thenReturn("<html>invite</html>");
		when(sesV2Client.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());
		final SesInvitationEmailSender sender = new SesInvitationEmailSender(invitationEmailProperties,
				templateRenderer, sesV2Client);

		sender.sendNutritionistInvitation("invitee@example.com", PlanTier.PROFESIONAL, INVITE_URL);

		final ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(sesV2Client).sendEmail(captor.capture());
		final SendEmailRequest request = captor.getValue();
		assertThat(request.fromEmailAddress()).isEqualTo(FROM);
		assertThat(request.destination().toAddresses()).containsExactly("invitee@example.com");
		assertThat(request.content().simple().subject().data()).isEqualTo("Invitación a Minutriporción");
		assertThat(request.content().simple().body().html().data()).contains("invite");
	}

	@Test
	void sendNutritionistInvitationRequiresFromAddress() {
		when(invitationEmailProperties.getFromAddress()).thenReturn("");
		final SesInvitationEmailSender sender = new SesInvitationEmailSender(invitationEmailProperties,
				templateRenderer, sesV2Client);

		assertThatThrownBy(
				() -> sender.sendNutritionistInvitation("invitee@example.com", PlanTier.PROFESIONAL, INVITE_URL))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("from-address");
	}

}
