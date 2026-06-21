package com.nutriconsultas.profile;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.subscription.Entitlement;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.lifecycle.SubscriptionAccessService;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller for managing the nutritionist's professional profile.
 *
 * <p>
 * Provides read and update access to the {@link NutritionistProfile} for the currently
 * authenticated user. Tenant isolation is enforced via the OAuth2 principal {@code sub}
 * claim.
 */
@Controller
@Slf4j
public class NutritionistProfileController extends AbstractAuthorizedController {

	private static final DateTimeFormatter VIGENCIA_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
		.withZone(ZoneId.of("America/Mexico_City"));

	private final NutritionistProfileService profileService;

	private final SubscriptionAccessService subscriptionAccessService;

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public NutritionistProfileController(final NutritionistProfileService profileService,
			final SubscriptionAccessService subscriptionAccessService,
			final SubscriptionEntitlementService subscriptionEntitlementService) {
		this.profileService = profileService;
		this.subscriptionAccessService = subscriptionAccessService;
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	/**
	 * Renders the profile form.
	 * @param principal the authenticated OIDC user
	 * @param model the MVC model
	 * @return the Thymeleaf template name
	 */
	@GetMapping("/admin/perfil")
	public String perfil(@AuthenticationPrincipal final OidcUser principal, final Model model,
			final HttpServletRequest request) {
		log.debug("Loading profile page");
		model.addAttribute("activeMenu", "perfil");
		populateProfileModel(model, principal.getSubject(), request);
		return "sbadmin/profile/formulario";
	}

	/**
	 * Saves text fields (display name, cédula profesional).
	 * @param profile the form-bound profile data
	 * @param principal the authenticated OIDC user
	 * @return redirect to profile page
	 */
	@PostMapping("/admin/perfil/save")
	public String save(@ModelAttribute final NutritionistProfile profile,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Saving profile text fields");
		final String userId = principal.getSubject();
		final NutritionistProfile saved = profileService.saveProfile(profile, userId);
		log.info("Saved profile: {}", LogRedaction.redactNutritionistProfile(saved.getId()));
		return "redirect:/admin/perfil";
	}

	/**
	 * Serves the authenticated user's clinic logo from S3 for inline preview on the
	 * profile page.
	 * @param principal the authenticated OIDC user
	 * @return logo bytes with appropriate content type, or 404/403 when unavailable
	 */
	@GetMapping("/admin/perfil/logo")
	@ResponseBody
	public ResponseEntity<byte[]> getLogo(@AuthenticationPrincipal final OidcUser principal) {
		final String userId = principal.getSubject();
		if (!subscriptionEntitlementService.hasEntitlement(userId, Entitlement.REPORTS_BRANDED)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		final NutritionistProfile profile = profileService.getOrCreateProfile(userId);
		if (profile.getLogoExtension() == null || profile.getLogoExtension().isBlank()) {
			return ResponseEntity.notFound().build();
		}
		final byte[] logoBytes = profileService.getLogo(userId);
		if (logoBytes == null) {
			return ResponseEntity.notFound().build();
		}
		final MediaType mediaType = MediaType.parseMediaType(resolveLogoMediaType(profile.getLogoExtension()));
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(mediaType).body(logoBytes);
	}

	/**
	 * Handles logo upload.
	 * @param file the uploaded image file
	 * @param principal the authenticated OIDC user
	 * @param model the MVC model
	 * @return redirect to profile page, or form with error if upload fails
	 */
	@PostMapping("/admin/perfil/logo")
	public String uploadLogo(@RequestParam("logoFile") final MultipartFile file,
			@AuthenticationPrincipal final OidcUser principal, final Model model, final HttpServletRequest request) {
		log.debug("Uploading logo");
		model.addAttribute("activeMenu", "perfil");
		final String userId = principal.getSubject();
		String result;
		if (!subscriptionEntitlementService.hasEntitlement(userId, Entitlement.REPORTS_BRANDED)) {
			populateProfileModel(model, userId, request);
			model.addAttribute("errorMessage",
					"La personalización con logo está disponible en los planes Profesional y superiores.");
			result = "sbadmin/profile/formulario";
		}
		else if (file.isEmpty()) {
			log.error("Logo upload failed: file is empty");
			populateProfileModel(model, userId, request);
			model.addAttribute("errorMessage", "El archivo está vacío");
			result = "sbadmin/profile/formulario";
		}
		else {
			try {
				final byte[] bytes = file.getBytes();
				final String originalName = file.getOriginalFilename();
				String extension = "png";
				if (originalName != null) {
					final int dotIndex = originalName.lastIndexOf('.');
					if (dotIndex > 0 && dotIndex < originalName.length() - 1) {
						extension = originalName.substring(dotIndex + 1).toLowerCase();
					}
				}
				profileService.saveLogo(userId, bytes, extension);
				log.info("Logo uploaded for user: {}", LogRedaction.redactUserId(userId));
				result = "redirect:/admin/perfil";
			}
			catch (final IOException e) {
				log.error("Failed to upload logo for user: {}", LogRedaction.redactUserId(userId), e);
				populateProfileModel(model, userId, request);
				model.addAttribute("errorMessage", "Error al subir el logo");
				result = "sbadmin/profile/formulario";
			}
		}
		return result;
	}

	private void populateProfileModel(final Model model, final String userId, final HttpServletRequest request) {
		final NutritionistProfile profile = profileService.getOrCreateProfile(userId);
		log.info("Loaded profile: {}", LogRedaction.redactNutritionistProfile(profile.getId()));
		model.addAttribute("profile", profile);
		final boolean brandedReportsEnabled = subscriptionEntitlementService.hasEntitlement(userId,
				Entitlement.REPORTS_BRANDED);
		model.addAttribute("brandedReportsEnabled", brandedReportsEnabled);
		if (brandedReportsEnabled && profile.getLogoExtension() != null && !profile.getLogoExtension().isBlank()) {
			model.addAttribute("logoUrl", "/admin/perfil/logo");
		}
		final Optional<Subscription> grantingSubscription = subscriptionAccessService
			.findGrantingSubscriptionForUser(userId);
		populatePublicBookingLink(model, profile, grantingSubscription, request);
		grantingSubscription.ifPresent(subscription -> {
			model.addAttribute("subscriptionPlanLabel", subscription.getPlanTier().getDisplayName());
			model.addAttribute("subscriptionStatus", subscription.getStatus());
			model.addAttribute("subscriptionPeriodStartLabel", formatVigencia(subscription.getPeriodStart()));
			model.addAttribute("subscriptionPeriodEndLabel", formatVigencia(subscription.getPeriodEnd()));
		});
	}

	private void populatePublicBookingLink(final Model model, final NutritionistProfile profile,
			final Optional<Subscription> grantingSubscription, final HttpServletRequest request) {
		if (grantingSubscription.isEmpty()) {
			model.addAttribute("publicBookingLinkEnabled", false);
			model.addAttribute("publicBookingLinkDisabledMessage",
					"Necesitas una suscripción activa para compartir tu enlace para agendar citas en línea.");
			return;
		}
		if (!StringUtils.hasText(profile.getPublicBookingId())) {
			model.addAttribute("publicBookingLinkEnabled", false);
			model.addAttribute("publicBookingLinkDisabledMessage",
					"Tu enlace para agendar citas en línea aún no está disponible. Guarda tu perfil e inténtalo de nuevo.");
			return;
		}
		model.addAttribute("publicBookingLinkEnabled", true);
		model.addAttribute("publicBookingUrl", buildPublicBookingUrl(request, profile.getPublicBookingId()));
	}

	static String buildPublicBookingUrl(final HttpServletRequest request, final String publicBookingId) {
		return ServletUriComponentsBuilder.fromRequest(request)
			.replacePath("/consultas/{publicBookingId}/agendar-cita")
			.replaceQuery(null)
			.buildAndExpand(publicBookingId)
			.toUriString();
	}

	private static String resolveLogoMediaType(final String extension) {
		if ("jpg".equalsIgnoreCase(extension) || "jpeg".equalsIgnoreCase(extension)) {
			return "image/jpeg";
		}
		if ("gif".equalsIgnoreCase(extension)) {
			return "image/gif";
		}
		return "image/png";
	}

	private static String formatVigencia(final Instant instant) {
		if (instant == null) {
			return null;
		}
		return VIGENCIA_FORMAT.format(instant);
	}

}
