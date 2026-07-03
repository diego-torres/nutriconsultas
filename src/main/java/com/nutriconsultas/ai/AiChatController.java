package com.nutriconsultas.ai;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.controller.AbstractAuthorizedController;

/**
 * Nutritionist web entry point for the AI assistant (#388). Full chat UI follows in #389.
 */
@Controller
@RequestMapping("/admin/ai")
public class AiChatController extends AbstractAuthorizedController {

	private final AiProperties aiProperties;

	public AiChatController(final AiProperties aiProperties) {
		this.aiProperties = aiProperties;
	}

	@GetMapping
	public String chatHome(final Model model) {
		if (!aiProperties.isEnabled()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		model.addAttribute("activeMenu", "ai");
		return "sbadmin/ai/chat";
	}

}
