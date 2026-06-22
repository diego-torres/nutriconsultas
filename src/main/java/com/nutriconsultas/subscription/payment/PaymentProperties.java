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

	public static final String PROVIDER_STRIPE = "stripe";

	private String provider = PROVIDER_STRIPE;

	private String webhookSecret = "";

	private String mercadopagoAccessToken = "";

	private String mercadopagoBackUrl = "https://minutriporcion.com/admin";

	private String stripeSecretKey = "";

	private String stripeWebhookSecret = "";

	private String stripeSuccessUrl = "https://minutriporcion.com/admin";

	private String stripeCancelUrl = "https://minutriporcion.com/admin";

	private String stripePriceIdBasico = "";

	private String stripePriceIdProfesional = "";

	private String stripePriceIdPlus = "";

	private String stripePriceIdConsultorio = "";

	private String currency = "MXN";

	private boolean stubSimulateCheckout = true;

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

	public String getStripeSecretKey() {
		return stripeSecretKey;
	}

	public void setStripeSecretKey(final String stripeSecretKey) {
		this.stripeSecretKey = stripeSecretKey;
	}

	public String getStripeWebhookSecret() {
		return stripeWebhookSecret;
	}

	public void setStripeWebhookSecret(final String stripeWebhookSecret) {
		this.stripeWebhookSecret = stripeWebhookSecret;
	}

	public String getStripeSuccessUrl() {
		return stripeSuccessUrl;
	}

	public void setStripeSuccessUrl(final String stripeSuccessUrl) {
		this.stripeSuccessUrl = stripeSuccessUrl;
	}

	public String getStripeCancelUrl() {
		return stripeCancelUrl;
	}

	public void setStripeCancelUrl(final String stripeCancelUrl) {
		this.stripeCancelUrl = stripeCancelUrl;
	}

	public String getStripePriceIdBasico() {
		return stripePriceIdBasico;
	}

	public void setStripePriceIdBasico(final String stripePriceIdBasico) {
		this.stripePriceIdBasico = stripePriceIdBasico;
	}

	public String getStripePriceIdProfesional() {
		return stripePriceIdProfesional;
	}

	public void setStripePriceIdProfesional(final String stripePriceIdProfesional) {
		this.stripePriceIdProfesional = stripePriceIdProfesional;
	}

	public String getStripePriceIdPlus() {
		return stripePriceIdPlus;
	}

	public void setStripePriceIdPlus(final String stripePriceIdPlus) {
		this.stripePriceIdPlus = stripePriceIdPlus;
	}

	public String getStripePriceIdConsultorio() {
		return stripePriceIdConsultorio;
	}

	public void setStripePriceIdConsultorio(final String stripePriceIdConsultorio) {
		this.stripePriceIdConsultorio = stripePriceIdConsultorio;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(final String currency) {
		this.currency = currency;
	}

	public boolean isStubSimulateCheckout() {
		return stubSimulateCheckout;
	}

	public void setStubSimulateCheckout(final boolean stubSimulateCheckout) {
		this.stubSimulateCheckout = stubSimulateCheckout;
	}

	public boolean isMercadoPagoConfigured() {
		return PROVIDER_MERCADOPAGO.equalsIgnoreCase(provider) && mercadopagoAccessToken != null
				&& !mercadopagoAccessToken.isBlank();
	}

	public boolean isStripeConfigured() {
		return PROVIDER_STRIPE.equalsIgnoreCase(provider) && stripeSecretKey != null && !stripeSecretKey.isBlank();
	}

	public boolean isLivePaymentProviderConfigured() {
		return isStripeConfigured() || isMercadoPagoConfigured();
	}

	public String resolveStripePriceId(final PlanTier planTier) {
		return switch (planTier) {
			case BASICO -> stripePriceIdBasico;
			case PROFESIONAL -> stripePriceIdProfesional;
			case PLUS -> stripePriceIdPlus;
			case CONSULTORIO -> stripePriceIdConsultorio;
		};
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
