package com.nutriconsultas.mobile.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.spi.FilterReply;

class PhiLogTurboFilterTest {

	private final PhiLogTurboFilter filter = new PhiLogTurboFilter();

	private final Logger mobileLogger = logger("com.nutriconsultas.mobile.MobilePatientMessageService");

	private final Logger otherLogger = logger("com.nutriconsultas.paciente.PacienteService");

	private static Logger logger(final String name) {
		final LoggerContext context = new LoggerContext();
		return context.getLogger(name);
	}

	@Test
	void deniesInfoLogsWithEmailInMobilePackage() {
		filter.start();

		final FilterReply reply = filter.decide(null, mobileLogger, Level.INFO, "Patient email is {}",
				new Object[] { "patient@example.com" }, null);

		assertThat(reply).isEqualTo(FilterReply.DENY);
	}

	@Test
	void deniesInfoLogsWithRawAuth0SubInMobilePackage() {
		filter.start();

		final FilterReply reply = filter.decide(null, mobileLogger, Level.INFO, "Linked sub={}",
				new Object[] { "auth0|abcdefghijklmnopqrstuvwxyz" }, null);

		assertThat(reply).isEqualTo(FilterReply.DENY);
	}

	@Test
	void allowsRedactedAuth0SubAtInfo() {
		filter.start();

		final FilterReply reply = filter.decide(null, mobileLogger, Level.INFO, "Linked sub={}",
				new Object[] { "userId[auth0|...REDACTED]" }, null);

		assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
	}

	@Test
	void allowsDebugLogsEvenWithSensitivePatterns() {
		filter.start();

		final FilterReply reply = filter.decide(null, mobileLogger, Level.DEBUG, "Patient email is {}",
				new Object[] { "patient@example.com" }, null);

		assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
	}

	@Test
	void neutralForNonMobileLoggers() {
		filter.start();

		final FilterReply reply = filter.decide(null, otherLogger, Level.INFO, "Patient email is {}",
				new Object[] { "patient@example.com" }, null);

		assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
	}

}
