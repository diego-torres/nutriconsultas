package com.nutriconsultas.ai;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloCatalogConstants;
import com.nutriconsultas.platillos.PlatilloRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SearchDishCatalogToolServiceImpl implements SearchDishCatalogToolService {

	static final int DEFAULT_LIMIT = 10;

	static final int MAX_LIMIT = 25;

	static final int MIN_QUERY_LENGTH = 2;

	static final int MAX_QUERY_LENGTH = 120;

	static final int MAX_INGESTAS_LENGTH = 80;

	private final PlatilloRepository platilloRepository;

	public SearchDishCatalogToolServiceImpl(final PlatilloRepository platilloRepository) {
		this.platilloRepository = platilloRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AiToolResult<DishCatalogSearchData> search(@NonNull final String nutritionistId, @NonNull final String query,
			@Nullable final String ingestasSugeridas, @Nullable final Integer limit) {
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
		if (ingestasSugeridas != null && ingestasSugeridas.length() > MAX_INGESTAS_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El filtro de ingesta no puede superar " + MAX_INGESTAS_LENGTH + " caracteres.");
		}
		final int effectiveLimit = resolveLimit(limit);
		if (effectiveLimit < 1 || effectiveLimit > MAX_LIMIT) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El límite debe estar entre 1 y " + MAX_LIMIT + ".");
		}

		final List<String> authorizedUserIds = List.of(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID,
				nutritionistId.trim());
		final String searchPattern = "%" + trimmedQuery + "%";
		final String ingestasFilter = buildIngestasFilter(ingestasSugeridas);
		final List<Platillo> platillos = platilloRepository
			.findForAuthorizedCatalogSearch(authorizedUserIds, searchPattern, ingestasFilter,
					PageRequest.of(0, effectiveLimit))
			.getContent();
		final List<DishCatalogSearchItem> items = platillos.stream()
			.map(platillo -> toItem(platillo, nutritionistId.trim()))
			.toList();
		final DishCatalogSearchData data = new DishCatalogSearchData(items, items.size());
		if (log.isInfoEnabled()) {
			log.info("AI tool search_dish_catalog resultCount={}", data.totalReturned());
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
	private static String buildIngestasFilter(@Nullable final String ingestasSugeridas) {
		if (!StringUtils.hasText(ingestasSugeridas)) {
			return null;
		}
		return "%" + ingestasSugeridas.trim() + "%";
	}

	private static DishCatalogSearchItem toItem(final Platillo platillo, final String nutritionistId) {
		final boolean systemCatalog = PlatilloCatalogConstants.isSystemCatalog(platillo);
		return new DishCatalogSearchItem(platillo.getId(), platillo.getName(), platillo.getIngestasSugeridas(),
				platillo.getEnergia(), platillo.getProteina(), Objects.equals(nutritionistId, platillo.getUserId()),
				systemCatalog);
	}

}
