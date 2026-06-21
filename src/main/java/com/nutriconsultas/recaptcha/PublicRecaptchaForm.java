package com.nutriconsultas.recaptcha;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link org.springframework.stereotype.Controller} that renders public forms
 * protected by reCAPTCHA v2 (checkbox). Adds {@code recaptchaSiteKey} to the model for
 * Thymeleaf widgets.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicRecaptchaForm {

}
