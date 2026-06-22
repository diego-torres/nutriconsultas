package com.nutriconsultas.admin;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nutriconsultas.controller.AbstractPlatformAdminController;
import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.maintenance.MaintenanceRetentionProperties;
import com.nutriconsultas.subscription.maintenance.MaintenanceRetentionService;
import com.nutriconsultas.subscription.maintenance.MaintenanceRun;

@Controller
@RequestMapping("/admin/platform/maintenance")
public class MaintenanceAdminController extends AbstractPlatformAdminController {

	private static final int RUNS_PAGE_SIZE = 10;

	private final MaintenanceRetentionService maintenanceRetentionService;

	private final MaintenanceRetentionProperties maintenanceRetentionProperties;

	public MaintenanceAdminController(final PlatformAdminAuthorization platformAdminAuthorization,
			final MaintenanceRetentionService maintenanceRetentionService,
			final MaintenanceRetentionProperties maintenanceRetentionProperties) {
		super(platformAdminAuthorization);
		this.maintenanceRetentionService = maintenanceRetentionService;
		this.maintenanceRetentionProperties = maintenanceRetentionProperties;
	}

	@GetMapping
	public String list(@AuthenticationPrincipal final OidcUser principal,
			@RequestParam(defaultValue = "0") final int page, final Model model) {
		requirePlatformAdmin(principal, "maintenance.list");
		final Optional<MaintenanceRun> latestRun = maintenanceRetentionService.findLatestRun();
		final Page<MaintenanceRun> runs = maintenanceRetentionService.findRecentRuns(page, RUNS_PAGE_SIZE);
		model.addAttribute("activeMenu", "maintenance");
		model.addAttribute("latestRun", latestRun.orElse(null));
		model.addAttribute("runs", runs);
		model.addAttribute("retentionDays", maintenanceRetentionProperties.getRetentionDays());
		model.addAttribute("currentPage", page);
		return "sbadmin/platform/maintenance/list";
	}

	@PostMapping("/execute")
	public String execute(@AuthenticationPrincipal final OidcUser principal,
			final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "maintenance.execute");
		final String actorUserId = principal != null ? principal.getSubject() : null;
		try {
			final MaintenanceRun run = maintenanceRetentionService.executeCleanup(actorUserId);
			redirectAttributes.addFlashAttribute("successMessage", "Limpieza completada. Elegibles: "
					+ run.getEligibleCount() + ", purgados: " + run.getPurgedUserCount() + ".");
		}
		catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"La limpieza falló. No se eliminaron datos. Revise el historial de ejecuciones.");
		}
		return "redirect:/admin/platform/maintenance";
	}

	@GetMapping("/runs/{runId}/download")
	public String downloadBackup(@AuthenticationPrincipal final OidcUser principal, @PathVariable final String runId,
			final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "maintenance.download");
		final Optional<String> downloadUrl = maintenanceRetentionService.resolveBackupDownloadUrl(runId);
		if (downloadUrl.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "No se encontró respaldo para esta ejecución.");
			return "redirect:/admin/platform/maintenance";
		}
		return "redirect:" + downloadUrl.get();
	}

	@PostMapping("/runs/{runId}/delete-backup")
	public String deleteBackup(@AuthenticationPrincipal final OidcUser principal, @PathVariable final String runId,
			final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "maintenance.delete-backup");
		final String actorUserId = principal != null ? principal.getSubject() : null;
		maintenanceRetentionService.deleteBackup(runId, actorUserId);
		redirectAttributes.addFlashAttribute("successMessage", "Respaldo eliminado de S3.");
		return "redirect:/admin/platform/maintenance";
	}

}
