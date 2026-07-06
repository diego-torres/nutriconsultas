package com.nutriconsultas.config;

import java.util.Collections;
import java.util.Set;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import com.nutriconsultas.util.AnthropometricVisualAidUtils;

/**
 * Thymeleaf dialect exposing {@link AnthropometricVisualAidUtils} as
 * {@code #anthropometricVisualAid} in templates.
 */
public final class AnthropometricVisualAidDialect extends AbstractDialect implements IExpressionObjectDialect {

	private static final String EXPRESSION_OBJECT_NAME = "anthropometricVisualAid";

	public AnthropometricVisualAidDialect() {
		super(EXPRESSION_OBJECT_NAME);
	}

	@Override
	public IExpressionObjectFactory getExpressionObjectFactory() {
		return new IExpressionObjectFactory() {

			@Override
			public Set<String> getAllExpressionObjectNames() {
				return Collections.singleton(EXPRESSION_OBJECT_NAME);
			}

			@Override
			public Object buildObject(final IExpressionContext context, final String expressionObjectName) {
				return AnthropometricVisualAidUtils.class;
			}

			@Override
			public boolean isCacheable(final String expressionObjectName) {
				return true;
			}

		};
	}

}
