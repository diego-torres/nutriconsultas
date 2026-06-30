package com.nutriconsultas.alimentos;

import java.beans.PropertyEditorSupport;

import com.nutriconsultas.util.FractionQuantityParser;

/**
 * Binds fractional portion strings (e.g. {@code 1/2}, {@code 1 1/4}) to {@link Double}
 * for {@link Alimento#cantSugerida}.
 */
public final class FractionalQuantityPropertyEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(final String text) {
		if (text == null || text.isBlank()) {
			setValue(null);
			return;
		}
		try {
			final Double parsed = FractionQuantityParser.parseFractionalQuantity(text);
			if (parsed == null) {
				throw new IllegalArgumentException("Cantidad sugerida requerida.");
			}
			setValue(parsed);
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Use un número o fracción válida (ej. 1/2, 1 1/4, 0.5).", ex);
		}
	}

	@Override
	public String getAsText() {
		final Object value = getValue();
		if (value instanceof Double doubleValue) {
			return doubleValue.toString();
		}
		return "";
	}

}
