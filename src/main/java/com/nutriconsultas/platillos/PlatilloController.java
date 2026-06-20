package com.nutriconsultas.platillos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.nutriconsultas.alimentos.AlimentoService;
import com.nutriconsultas.controller.AbstractAuthorizedController;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Slf4j
public class PlatilloController extends AbstractAuthorizedController {

	@Autowired
	private PlatilloService service;

	@Autowired
	private AlimentoService alimentoService;

	@Autowired
	private PlatilloAuthorization platilloAuthorization;

	private String getUserId(@AuthenticationPrincipal final OidcUser principal) {
		if (principal == null) {
			log.warn("OAuth2 principal is null, cannot get user ID");
			return null;
		}
		return principal.getSubject();
	}

	private Platillo loadPlatilloForMutation(@NonNull final Long id,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final Platillo platillo = platilloAuthorization.resolveForMutation(id, userId, principal, service);
		if (platillo == null) {
			throw new IllegalArgumentException("Platillo no encontrado o no tiene permiso para modificarlo");
		}
		platilloAuthorization.verifyCanModify(platillo, userId, principal);
		return platillo;
	}

	private void addEditPermissions(final Platillo platillo, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = getUserId(principal);
		final boolean isOwner = platilloAuthorization.canModify(platillo, userId, principal);
		model.addAttribute("isOwner", isOwner);
		model.addAttribute("isSystemCatalog", PlatilloCatalogConstants.isSystemCatalog(platillo));
	}

	@GetMapping(path = "/admin/platillos/nuevo")
	public String nuevo(final Model model) {
		log.debug("Starting nuevo");
		model.addAttribute("activeMenu", "platillos");
		final Platillo platillo = new Platillo();
		platillo.setId(0L);
		model.addAttribute("platillo", platillo);
		model.addAttribute("isOwner", true);
		model.addAttribute("isSystemCatalog", false);
		log.debug("Finishing nuevo platillo con valores predeterminados: {}", platillo);
		return "sbadmin/platillos/formulario";
	}

	@GetMapping(path = "/admin/platillos")
	public String listado(final Model model) {
		log.debug("Starting listado");
		model.addAttribute("activeMenu", "platillos");
		log.debug("Finishing listado");
		return "sbadmin/platillos/listado";
	}

	@GetMapping(path = "/admin/platillos/{id}")
	public String editar(@PathVariable @NonNull final Long id, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Starting editar with id {}", id);
		model.addAttribute("activeMenu", "platillos");
		final Platillo platillo = service.findById(id);
		if (platillo == null) {
			throw new IllegalArgumentException("Platillo no encontrado");
		}
		model.addAttribute("platillo", platillo);
		addEditPermissions(platillo, model, principal);
		List<String> ingestas = new ArrayList<>();
		if (platillo.getIngestasSugeridas() != null && !platillo.getIngestasSugeridas().isEmpty()) {
			ingestas = Arrays.asList(platillo.getIngestasSugeridas().split(","));
		}
		model.addAttribute("ingestas", ingestas);
		log.debug("Finishing editar with platillo {}", platillo);
		model.addAttribute("alimentosList", alimentoService.findAll());
		return "sbadmin/platillos/formulario";
	}

	@PostMapping("/admin/platillos/save")
	public String save(@ModelAttribute @NonNull final Platillo platillo,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Starting save with platillo {}", platillo);

		final Long platilloId = platillo.getId();
		String result;
		if (platilloId == null) {
			log.error("Platillo ID is null, cannot save");
			result = "redirect:/admin/platillos";
		}
		else if (platilloId == 0L) {
			final String userId = getUserId(principal);
			if (userId == null) {
				throw new IllegalArgumentException("No se pudo identificar al usuario");
			}
			platillo.setUserId(userId);
			service.save(platillo);
			log.debug("Finishing save with new platillo {}", platillo);
			result = "redirect:/admin/platillos/" + platillo.getId();
		}
		else {
			final Platillo dbPlatillo = loadPlatilloForMutation(platilloId, principal);
			dbPlatillo.setName(platillo.getName());
			dbPlatillo.setDescription(platillo.getDescription());
			service.save(dbPlatillo);
			platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, dbPlatillo, "platillos.save");
			log.debug("Finishing save with platillo {}", platillo);
			result = "redirect:/admin/platillos/" + platillo.getId();
		}
		return result;
	}

	@PostMapping("/admin/platillos/{id}/picture")
	public String uploadPicture(@PathVariable @NonNull final Long id,
			@RequestParam("imgPlatillo") final MultipartFile file, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Starting uploadPicture with id {}", id);
		model.addAttribute("activeMenu", "platillos");

		String result;
		if (file.isEmpty()) {
			log.error("Failed to upload picture because the file is empty");
			final Platillo platillo = service.findById(id);
			model.addAttribute("platillo", platillo);
			addEditPermissions(platillo, model, principal);
			model.addAttribute("errorMessage", "The file is empty");
			if (platillo != null && platillo.getIngestasSugeridas() != null
					&& !platillo.getIngestasSugeridas().isEmpty()) {
				model.addAttribute("ingestas", Arrays.asList(platillo.getIngestasSugeridas().split(",")));
			}
			model.addAttribute("alimentosList", alimentoService.findAll());
			result = "sbadmin/platillos/formulario";
		}
		else {
			try {
				loadPlatilloForMutation(id, principal);
				final byte[] bytes = file.getBytes();
				final String fileName = file.getOriginalFilename();
				String fileExtension = "";
				if (fileName != null) {
					final int dotIndex = fileName.lastIndexOf('.');
					if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
						fileExtension = fileName.substring(dotIndex + 1);
					}
					log.debug("File extension is {}", fileExtension);
				}
				if (fileExtension == null) {
					fileExtension = "";
				}
				service.savePicture(id, bytes, fileExtension);
				final Platillo platillo = service.findById(id);
				platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, platillo, "platillos.picture");
				log.debug("Successfully uploaded picture for platillo with id {}", id);
				result = "redirect:/admin/platillos/" + id;
			}
			catch (final IOException e) {
				log.error("Failed to upload picture for platillo with id {}", id, e);
				final Platillo platillo = service.findById(id);
				model.addAttribute("platillo", platillo);
				addEditPermissions(platillo, model, principal);
				model.addAttribute("errorMessage", "Failed to upload picture");
				if (platillo != null && platillo.getIngestasSugeridas() != null
						&& !platillo.getIngestasSugeridas().isEmpty()) {
					model.addAttribute("ingestas", Arrays.asList(platillo.getIngestasSugeridas().split(",")));
				}
				model.addAttribute("alimentosList", alimentoService.findAll());
				result = "sbadmin/platillos/formulario";
			}
		}
		return result;
	}

	@GetMapping(value = "admin/platillos/platillo/{id}/{imageName}", produces = MediaType.IMAGE_JPEG_VALUE)
	public @ResponseBody byte[] getImage(@PathVariable @NonNull final Long id,
			@PathVariable @NonNull final String imageName, final Model model) throws IOException {
		log.debug("Starting getImage with id {} and imageName {}", id, imageName);
		return service.getPicture(id, imageName);
	}

	@PostMapping("/admin/platillos/{id}/pdf")
	public String uploadPdf(@PathVariable @NonNull final Long id, @RequestParam("pdfPlatillo") final MultipartFile file,
			final Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Starting uploadPdf with id {}", id);
		model.addAttribute("activeMenu", "platillos");

		String result;
		if (file.isEmpty()) {
			log.error("Failed to upload pdf because the file is empty");
			final Platillo platillo = service.findById(id);
			model.addAttribute("platillo", platillo);
			addEditPermissions(platillo, model, principal);
			model.addAttribute("errorMessage", "The file is empty");
			if (platillo != null && platillo.getIngestasSugeridas() != null
					&& !platillo.getIngestasSugeridas().isEmpty()) {
				model.addAttribute("ingestas", Arrays.asList(platillo.getIngestasSugeridas().split(",")));
			}
			model.addAttribute("alimentosList", alimentoService.findAll());
			result = "sbadmin/platillos/formulario";
		}
		else {
			try {
				loadPlatilloForMutation(id, principal);
				final byte[] bytes = file.getBytes();
				service.savePdf(id, bytes);
				final Platillo platillo = service.findById(id);
				platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, platillo, "platillos.pdf");
				log.debug("Successfully uploaded pdf for platillo with id {}", id);
				result = "redirect:/admin/platillos/" + id;
			}
			catch (final IOException e) {
				log.error("Failed to upload pdf for platillo with id {}", id, e);
				final Platillo platillo = service.findById(id);
				model.addAttribute("platillo", platillo);
				addEditPermissions(platillo, model, principal);
				model.addAttribute("errorMessage", "Failed to upload pdf");
				if (platillo != null && platillo.getIngestasSugeridas() != null
						&& !platillo.getIngestasSugeridas().isEmpty()) {
					model.addAttribute("ingestas", Arrays.asList(platillo.getIngestasSugeridas().split(",")));
				}
				model.addAttribute("alimentosList", alimentoService.findAll());
				result = "sbadmin/platillos/formulario";
			}
		}
		return result;
	}

	@GetMapping(value = "admin/platillos/platillo/{id}/instrucciones.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public @ResponseBody byte[] getPdf(@PathVariable @NonNull final Long id, final Model model) throws IOException {
		log.debug("Starting getPdf with id {}", id);
		return service.getPicture(id, "instrucciones.pdf");
	}

}
