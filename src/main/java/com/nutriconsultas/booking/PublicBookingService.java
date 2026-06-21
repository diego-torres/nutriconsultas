package com.nutriconsultas.booking;

import org.springframework.lang.NonNull;

public interface PublicBookingService {

	PublicBookingNutritionistContext resolveContext(@NonNull String publicBookingId);

	PublicBookingSlotsResponse getPublicSlots(@NonNull String publicBookingId, @NonNull String date);

	PublicBookingConfirmation book(@NonNull String publicBookingId, @NonNull PublicBookingRequestDto request);

}
