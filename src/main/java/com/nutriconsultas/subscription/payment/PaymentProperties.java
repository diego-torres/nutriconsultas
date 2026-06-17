package com.nutriconsultas.subscription.payment;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Payment provider configuration. Secrets are supplied via environment variables only.
 */
@ConfigurationProperties(prefix = "nutriconsultas.subscription.payment")
public class PaymentProperties {

	public static final String PROVIDER_MERCADOPAGO = "mercadopago";

	private String provider = PROVIDER_MERCADOPAGO;

	private String webhookSecret = "";

	private String mercadopagoAccessToken = "";

	private String mercadopagoBackUrl = "https://minutriporcion.com/admin";

	private String currency = "MXN";

	public String getProvider() {
		return provider;
	}

	public void setProvider(final String provider) {
		this.provider = provider;
	}

	public String getWebhookSecret() {
		return webhookSecret;
	}

	public void setWebhookSecret(final String webhookSecret) {
		this.webhookSecret = webhookSecret;
	}

	public String getMercadopagoAccessToken() {
		return mercadopagoAccessToken;
	}

	public void setMercadopagoAccessToken(final String mercadopagoAccessToken) {
		this.mercadopagoAccessToken = mercadopagoAccessToken;
	}

	public String getMercadopagoBackUrl() {
		return mercadopagoBackUrl;
	}

	public void setMercadopagoBackUrl(final String mercadopagoBackUrl) {
		this.mercadopagoBackUrl = mercadopagoBackUrl;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(final String currency) {
		this.currency = currency;
	}

	public boolean isMercadoPagoConfigured() {
		return PROVIDER_MERCADOPAGO.equalsIgnoreCase(provider) && mercadopagoAccessToken != null
				&& !mercadopagoAccessToken.isBlank();
	}

	public static BigDecimal monthlyPriceMxn(final PlanTier planTier) {
		return switch (planTier) {
			case BASICO -> new BigDecimal("100");
			case PROFESIONAL -> new BigDecimal("200");
			case PLUS -> new BigDecimal("600");
			case CONSULTORIO -> new BigDecimal("900");
		};
	}

}
