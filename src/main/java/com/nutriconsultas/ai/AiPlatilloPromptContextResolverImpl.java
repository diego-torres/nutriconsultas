package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloCatalogConstants;
import com.nutriconsultas.platillos.PlatilloService;

@Service
public class AiPlatilloPromptContextResolverImpl implements AiPlatilloPromptContextResolver {

	private static final int MAX_DESCRIPTION_CHARS = 400;

	private static final int MAX_INGREDIENT_NAMES = 15;

	private final PlatilloService platilloService;

	public AiPlatilloPromptContextResolverImpl(final PlatilloService platilloService) {
		this.platilloService = platilloService;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AiPlatilloPromptContext> resolve(@Nullable final Long platilloId, final String nutritionistId) {
		if (platilloId == null || !StringUtils.hasText(nutritionistId)) {
			return Optional.empty();
		}
		final Platillo platillo = resolveAccessiblePlatillo(platilloId, nutritionistId);
		if (platillo == null) {
			throw new AiChatException(org.springframework.http.HttpStatus.NOT_FOUND, AiToolErrorCode.NOT_FOUND,
					"No se encontró el platillo.");
		}
		return Optional.of(toPromptContext(platillo));
	}

	private Platillo resolveAccessiblePlatillo(final Long platilloId, final String nutritionistId) {
		final Platillo owned = platilloService.findByIdAndUserId(platilloId, nutritionistId);
		if (owned != null) {
			return owned;
		}
		final Platillo platillo = platilloService.findById(platilloId);
		if (platillo != null && PlatilloCatalogConstants.isSystemCatalog(platillo)) {
			return platillo;
		}
		return null;
	}

	private AiPlatilloPromptContext toPromptContext(final Platillo platillo) {
		final List<String> ingredientNames = new ArrayList<>();
		if (platillo.getIngredientes() != null) {
			platillo.getIngredientes()
				.stream()
				.sorted(Comparator.comparing(Ingrediente::getId, Comparator.nullsLast(Comparator.naturalOrder())))
				.limit(MAX_INGREDIENT_NAMES)
				.map(ingrediente -> ingredientLabel(ingrediente))
				.filter(StringUtils::hasText)
				.forEach(ingredientNames::add);
		}
		return new AiPlatilloPromptContext(platillo.getId(), platillo.getName(),
				summarizeDescription(platillo.getDescription()), platillo.getEnergia(),
				platillo.getIngredientes() != null ? platillo.getIngredientes().size() : 0,
				List.copyOf(ingredientNames), platillo.getIngestasSugeridas());
	}

	private static String ingredientLabel(final Ingrediente ingrediente) {
		if (ingrediente.getAlimento() == null || !StringUtils.hasText(ingrediente.getAlimento().getNombreAlimento())) {
			return null;
		}
		final String nombre = ingrediente.getAlimento().getNombreAlimento().trim();
		if (ingrediente.getCantSugerida() != null && ingrediente.getCantSugerida() > 0) {
			final String unidad = StringUtils.hasText(ingrediente.getUnidad()) ? ingrediente.getUnidad().trim() : "";
			return unidad.isEmpty() ? nombre + " (" + ingrediente.getCantSugerida() + ")"
					: nombre + " (" + ingrediente.getDisplayCantSugerida(unidad) + " " + unidad + ")";
		}
		return nombre;
	}

	private static String summarizeDescription(@Nullable final String description) {
		if (!StringUtils.hasText(description)) {
			return null;
		}
		final String trimmed = description.replaceAll("\\p{Cntrl}", " ").trim();
		if (trimmed.length() <= MAX_DESCRIPTION_CHARS) {
			return trimmed;
		}
		return trimmed.substring(0, MAX_DESCRIPTION_CHARS);
	}

}
