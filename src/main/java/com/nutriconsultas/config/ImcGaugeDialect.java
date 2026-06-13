package com.nutriconsultas.config;

import java.util.Collections;
import java.util.Set;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import com.nutriconsultas.util.ImcGaugeUtils;

/**
 * Thymeleaf dialect exposing {@link ImcGaugeUtils} as {@code #imcGauge} in templates.
 */
public final class ImcGaugeDialect extends AbstractDialect implements IExpressionObjectDialect {

	private static final String EXPRESSION_OBJECT_NAME = "imcGauge";

	public ImcGaugeDialect() {
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
				return ImcGaugeUtils.class;
			}

			@Override
			public boolean isCacheable(final String expressionObjectName) {
				return true;
			}

		};
	}

}
