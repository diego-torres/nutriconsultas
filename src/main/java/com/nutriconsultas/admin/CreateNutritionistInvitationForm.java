package com.nutriconsultas.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.nutriconsultas.subscription.PlanTier;

public class CreateNutritionistInvitationForm {

	@NotBlank
	@Email
	private String email;

	@NotNull
	private PlanTier planTier;

	private boolean paymentExempt;

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public PlanTier getPlanTier() {
		return planTier;
	}

	public void setPlanTier(final PlanTier planTier) {
		this.planTier = planTier;
	}

	public boolean isPaymentExempt() {
		return paymentExempt;
	}

	public void setPaymentExempt(final boolean paymentExempt) {
		this.paymentExempt = paymentExempt;
	}

}
