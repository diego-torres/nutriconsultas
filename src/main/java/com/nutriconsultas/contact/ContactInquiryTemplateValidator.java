package com.nutriconsultas.contact;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

public class ContactInquiryTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/contact-inquiries/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		final ContactInquiry unread = new ContactInquiry();
		unread.setId(1L);
		unread.setName("Ana López");
		unread.setEmail("ana@example.com");
		unread.setSubject("Consulta");
		unread.setMessage("Mensaje de prueba");
		unread.setPlanRoleSlug("nutriologo-profesional");
		unread.setReadByAdmin(false);
		unread.setCreatedAt(Instant.parse("2026-01-15T10:30:00Z"));

		final ContactInquiry read = new ContactInquiry();
		read.setId(2L);
		read.setName("Carlos Ruiz");
		read.setEmail("carlos@example.com");
		read.setSubject("Acceso");
		read.setMessage("Ya atendido");
		read.setReadByAdmin(true);
		read.setCreatedAt(Instant.parse("2026-01-10T09:00:00Z"));

		variables.put("inquiries", List.of(unread, read));
		variables.put("platformAdmin", true);
		return variables;
	}

}
