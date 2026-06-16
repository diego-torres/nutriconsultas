package com.nutriconsultas.subscription.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.subscription.PlanTier;

class PaymentPropertiesTest {

	@Test
	void monthlyPriceMxnMatchesMarketingPricing() {
		assertThat(PaymentProperties.monthlyPriceMxn(PlanTier.BASICO)).isEqualByComparingTo(new BigDecimal("5"));
		assertThat(PaymentProperties.monthlyPriceMxn(PlanTier.PROFESIONAL)).isEqualByComparingTo(new BigDecimal("10"));
		assertThat(PaymentProperties.monthlyPriceMxn(PlanTier.PLUS)).isEqualByComparingTo(new BigDecimal("30"));
		assertThat(PaymentProperties.monthlyPriceMxn(PlanTier.CONSULTORIO)).isEqualByComparingTo(new BigDecimal("45"));
	}

	@Test
	void isMercadoPagoConfiguredRequiresAccessToken() {
		final PaymentProperties properties = new PaymentProperties();
		properties.setProvider(PaymentProperties.PROVIDER_MERCADOPAGO);
		properties.setMercadopagoAccessToken("");

		assertThat(properties.isMercadoPagoConfigured()).isFalse();

		properties.setMercadopagoAccessToken("APP_USR-test");
		assertThat(properties.isMercadoPagoConfigured()).isTrue();
	}

}
