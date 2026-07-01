package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SearchFoodCatalogToolServiceImpl implements SearchFoodCatalogToolService {

	static final int DEFAULT_LIMIT = 10;

	static final int MAX_LIMIT = 25;

	static final int MIN_QUERY_LENGTH = 2;

	static final int MAX_QUERY_LENGTH = 120;

	static final int MAX_CLASIFICACION_LENGTH = 80;

	private final AlimentosRepository alimentosRepository;

	public SearchFoodCatalogToolServiceImpl(final AlimentosRepository alimentosRepository) {
		this.alimentosRepository = alimentosRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AiToolResult<FoodCatalogSearchData> search(@NonNull final String nutritionistId, @NonNull final String query,
			@Nullable final String clasificacion, @Nullable final Integer limit) {
		if (!StringUtils.hasText(nutritionistId)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Sesión de nutriólogo no válida.");
		}
		final String trimmedQuery = query.trim();
		if (trimmedQuery.length() < MIN_QUERY_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"La búsqueda debe tener al menos " + MIN_QUERY_LENGTH + " caracteres.");
		}
		if (trimmedQuery.length() > MAX_QUERY_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"La búsqueda no puede superar " + MAX_QUERY_LENGTH + " caracteres.");
		}
		if (clasificacion != null && clasificacion.length() > MAX_CLASIFICACION_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"La clasificación no puede superar " + MAX_CLASIFICACION_LENGTH + " caracteres.");
		}
		final int effectiveLimit = resolveLimit(limit);
		if (effectiveLimit < 1 || effectiveLimit > MAX_LIMIT) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El límite debe estar entre 1 y " + MAX_LIMIT + ".");
		}

		final String searchPattern = "%" + trimmedQuery + "%";
		final String clasificacionFilter = buildClasificacionFilter(clasificacion);
		final long totalMatches = alimentosRepository.countForCatalogSearch(searchPattern, clasificacionFilter);
		final List<Alimento> alimentos = alimentosRepository
			.findForCatalogSearch(searchPattern, clasificacionFilter, PageRequest.of(0, effectiveLimit))
			.getContent();
		final List<FoodCatalogSearchItem> items = alimentos.stream().map(this::toItem).toList();
		final boolean truncated = totalMatches > items.size();
		final FoodCatalogSearchData data = new FoodCatalogSearchData(items, items.size(), truncated);
		if (log.isInfoEnabled()) {
			log.info("AI tool search_food_catalog resultCount={} truncated={}", data.totalReturned(), truncated);
		}
		return AiToolResult.success(data);
	}

	private static int resolveLimit(@Nullable final Integer limit) {
		if (limit == null) {
			return DEFAULT_LIMIT;
		}
		return limit;
	}

	@Nullable
	private static String buildClasificacionFilter(@Nullable final String clasificacion) {
		if (!StringUtils.hasText(clasificacion)) {
			return null;
		}
		return "%" + clasificacion.trim() + "%";
	}

	private FoodCatalogSearchItem toItem(final Alimento alimento) {
		return new FoodCatalogSearchItem(alimento.getId(), alimento.getNombreAlimento(), alimento.getClasificacion(),
				alimento.getUnidad(), alimento.getCantSugerida(), alimento.getEnergia());
	}

}
