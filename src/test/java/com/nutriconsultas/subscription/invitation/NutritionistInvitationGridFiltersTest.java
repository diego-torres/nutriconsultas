package com.nutriconsultas.subscription.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;

class NutritionistInvitationGridFiltersTest {

	@Test
	void fromPagingRequestReadsGlobalAndColumnFilters() {
		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setSearch(new Search("nutri@", null));
		pagingRequest.setColumns(List.of(column("email", "example.com"), column("status", "PENDING"),
				column("planTier", "BASICO"), column("paymentExempt", "true")));

		final NutritionistInvitationGridFilters filters = NutritionistInvitationGridFilters
			.fromPagingRequest(pagingRequest);

		assertThat(filters.globalSearch()).isEqualTo("nutri@");
		assertThat(filters.email()).isEqualTo("example.com");
		assertThat(filters.status()).isEqualTo("PENDING");
		assertThat(filters.planTier()).isEqualTo("BASICO");
		assertThat(filters.paymentExempt()).isEqualTo("true");
	}

	private static Column column(final String data, final String searchValue) {
		final Column column = new Column(data);
		column.setSearch(new Search(searchValue, null));
		return column;
	}

}
