package com.nutriconsultas.admin;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.nutriconsultas.subscription.SubscriptionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateSubscriptionForm {

	private boolean paymentExempt;

	private String periodEndInput;

	private int gracePeriodDays = 7;

	private SubscriptionStatus status;

	@NotBlank
	@Size(max = 50)
	private String reasonCode;

	@Size(max = 500)
	private String details;

	public boolean isPaymentExempt() {
		return paymentExempt;
	}

	public void setPaymentExempt(final boolean paymentExempt) {
		this.paymentExempt = paymentExempt;
	}

	public String getPeriodEndInput() {
		return periodEndInput;
	}

	public void setPeriodEndInput(final String periodEndInput) {
		this.periodEndInput = periodEndInput;
	}

	public Instant getPeriodEnd() {
		if (periodEndInput == null || periodEndInput.isBlank()) {
			return null;
		}
		final LocalDateTime local = LocalDateTime.parse(periodEndInput,
				DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
		return local.atZone(ZoneId.of("America/Mexico_City")).toInstant();
	}

	public int getGracePeriodDays() {
		return gracePeriodDays;
	}

	public void setGracePeriodDays(final int gracePeriodDays) {
		this.gracePeriodDays = gracePeriodDays;
	}

	public SubscriptionStatus getStatus() {
		return status;
	}

	public void setStatus(final SubscriptionStatus status) {
		this.status = status;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(final String reasonCode) {
		this.reasonCode = reasonCode;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(final String details) {
		this.details = details;
	}

}
