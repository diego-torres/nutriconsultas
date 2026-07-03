package com.nutriconsultas.ai;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.controller.AbstractAuthorizedController;

/**
 * Nutritionist web AI chat page (#388, #389).
 */
@Controller
@RequestMapping("/admin/ai")
public class AiChatController extends AbstractAuthorizedController {

	private final AiProperties aiProperties;

	public AiChatController(final AiProperties aiProperties) {
		this.aiProperties = aiProperties;
	}

	@GetMapping
	public String chatHome(@RequestParam(name = "threadId", required = false) final Long threadId, final Model model) {
		if (!aiProperties.isEnabled()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		model.addAttribute("activeMenu", "ai");
		model.addAttribute("initialThreadId", threadId);
		return "sbadmin/ai/chat";
	}

}
