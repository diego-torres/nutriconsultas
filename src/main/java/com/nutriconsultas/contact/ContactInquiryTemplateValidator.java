package com.nutriconsultas.contact;

import java.util.ArrayList;
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
		variables.put("inquiries", new ArrayList<>());
		variables.put("platformAdmin", true);
		return variables;
	}

}
