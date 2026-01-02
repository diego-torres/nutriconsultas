package com.nutriconsultas.search;

import org.springframework.lang.NonNull;

@FunctionalInterface
public interface SearchService {

	SearchResponse search(@NonNull String query, @NonNull String userId, @NonNull String category, int page);

}
