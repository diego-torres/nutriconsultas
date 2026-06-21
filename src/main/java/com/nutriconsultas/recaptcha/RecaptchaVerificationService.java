package com.nutriconsultas.recaptcha;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RecaptchaVerificationService {

	private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

	private final RestClient restClient;

	private final RecaptchaProperties properties;

	public RecaptchaVerificationService(final RecaptchaProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.create();
	}

	public boolean verifyToken(final String token) {
		if (!StringUtils.hasText(token)) {
			return false;
		}
		if (!properties.isVerificationEnabled()) {
			log.warn("reCAPTCHA verification skipped because recaptcha.verification-enabled=false");
			return true;
		}
		if (!properties.isConfigured()) {
			log.error("reCAPTCHA verification failed: site key or secret key is not configured");
			return false;
		}
		try {
			final RecaptchaVerifyResponse response = restClient.post()
				.uri(VERIFY_URL + "?secret={secret}&response={response}", properties.getSecretKey(), token)
				.retrieve()
				.body(RecaptchaVerifyResponse.class);

			if (response != null && Boolean.TRUE.equals(response.getSuccess())) {
				if (log.isDebugEnabled()) {
					log.debug("reCAPTCHA verification successful");
				}
				return true;
			}
			log.warn("reCAPTCHA verification rejected by Google");
			return false;
		}
		catch (Exception e) {
			log.error("Error verifying reCAPTCHA", e);
			return false;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static final class RecaptchaVerifyResponse {

		private Boolean success;

		public Boolean getSuccess() {
			return success;
		}

		@SuppressWarnings("unused")
		public void setSuccess(final Boolean success) {
			this.success = success;
		}

	}

}
