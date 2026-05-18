package com.nutriconsultas.profile;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

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

	@Autowired
	private NutritionistProfileService profileService;

	/**
	 * Renders the profile form.
	 * @param principal the authenticated OIDC user
	 * @param model the MVC model
	 * @return the Thymeleaf template name
	 */
	@GetMapping("/admin/perfil")
	public String perfil(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		log.debug("Loading profile page");
		model.addAttribute("activeMenu", "perfil");
		final String userId = principal.getSubject();
		final NutritionistProfile profile = profileService.getOrCreateProfile(userId);
		log.info("Loaded profile: {}", LogRedaction.redactNutritionistProfile(profile.getId()));
		model.addAttribute("profile", profile);
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
	 * Handles logo upload.
	 * @param file the uploaded image file
	 * @param principal the authenticated OIDC user
	 * @param model the MVC model
	 * @return redirect to profile page, or form with error if upload fails
	 */
	@PostMapping("/admin/perfil/logo")
	public String uploadLogo(@RequestParam("logoFile") final MultipartFile file,
			@AuthenticationPrincipal final OidcUser principal, final Model model) {
		log.debug("Uploading logo");
		model.addAttribute("activeMenu", "perfil");
		final String userId = principal.getSubject();
		String result;
		if (file.isEmpty()) {
			log.error("Logo upload failed: file is empty");
			final NutritionistProfile profile = profileService.getOrCreateProfile(userId);
			model.addAttribute("profile", profile);
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
				final NutritionistProfile profile = profileService.getOrCreateProfile(userId);
				model.addAttribute("profile", profile);
				model.addAttribute("errorMessage", "Error al subir el logo");
				result = "sbadmin/profile/formulario";
			}
		}
		return result;
	}

}
