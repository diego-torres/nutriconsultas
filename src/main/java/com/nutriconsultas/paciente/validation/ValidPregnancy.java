package com.nutriconsultas.paciente.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Custom validation annotation to ensure pregnancy can only be set for female patients
 * aged 12-50.
 */
@Documented
@Constraint(validatedBy = PregnancyValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPregnancy {

	String message() default "El estado de embarazo solo puede ser asignado a pacientes femeninas entre 12 y 50 años";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
