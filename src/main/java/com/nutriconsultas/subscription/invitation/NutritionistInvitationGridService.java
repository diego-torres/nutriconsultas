package com.nutriconsultas.subscription.invitation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nutriconsultas.subscription.NutritionistInvitation;

public interface NutritionistInvitationGridService {

	Page<NutritionistInvitation> findPage(NutritionistInvitationGridFilters filters, Pageable pageable);

	long countFiltered(NutritionistInvitationGridFilters filters);

	long countAll();

}
