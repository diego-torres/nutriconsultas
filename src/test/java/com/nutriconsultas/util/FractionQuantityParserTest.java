package com.nutriconsultas.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FractionQuantityParserTest {

	@Test
	void parseFractionalQuantity_parsesWholeNumber() {
		assertThat(FractionQuantityParser.parseFractionalQuantity("2")).isEqualTo(2d);
	}

	@Test
	void parseFractionalQuantity_parsesSimpleFraction() {
		assertThat(FractionQuantityParser.parseFractionalQuantity("1/2")).isEqualTo(0.5d);
	}

	@Test
	void parseFractionalQuantity_parsesMixedNumber() {
		assertThat(FractionQuantityParser.parseFractionalQuantity("1 1/2")).isEqualTo(1.5d);
	}

	@Test
	void parseFractionalQuantity_returnsNullForBlankInput() {
		assertThat(FractionQuantityParser.parseFractionalQuantity("")).isNull();
		assertThat(FractionQuantityParser.parseFractionalQuantity(null)).isNull();
	}

}
