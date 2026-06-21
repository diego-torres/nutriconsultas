package com.nutriconsultas.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

/**
 * Unit tests for {@link NutritionistProfileServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class NutritionistProfileServiceTest {

	private static final String TEST_USER_ID = "auth0|test123";

	@InjectMocks
	private NutritionistProfileServiceImpl service;

	@Mock
	private NutritionistProfileRepository repository;

	private NutritionistProfile existingProfile;

	@BeforeEach
	public void setup() {
		existingProfile = new NutritionistProfile();
		existingProfile.setId(1L);
		existingProfile.setUserId(TEST_USER_ID);
		existingProfile.setCedulaProfesional("12345678");
		existingProfile.setDisplayName("Dra. Test");
		existingProfile.setPublicBookingId(UUID.randomUUID().toString());
	}

	@Test
	public void testGetOrCreateProfile_WhenProfileExistsWithoutPublicId_AssignsAndSaves() {
		existingProfile.setPublicBookingId(null);
		when(repository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existingProfile));
		when(repository.save(any(NutritionistProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final NutritionistProfile result = service.getOrCreateProfile(TEST_USER_ID);

		assertThat(result.getPublicBookingId()).isNotBlank();
		verify(repository).save(existingProfile);
	}

	@Test
	public void testGetOrCreateProfile_WhenProfileExists_ReturnExisting() {
		when(repository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existingProfile));

		final NutritionistProfile result = service.getOrCreateProfile(TEST_USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
		assertThat(result.getCedulaProfesional()).isEqualTo("12345678");
		verify(repository, never()).save(any(NutritionistProfile.class));
	}

	@Test
	public void testGetOrCreateProfile_WhenProfileNotFound_CreateEmpty() {
		when(repository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
		final NutritionistProfile saved = new NutritionistProfile();
		saved.setId(2L);
		saved.setUserId(TEST_USER_ID);
		when(repository.save(any(NutritionistProfile.class))).thenReturn(saved);

		final NutritionistProfile result = service.getOrCreateProfile(TEST_USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
		verify(repository).save(any(NutritionistProfile.class));
	}

	@Test
	public void testSaveProfile_UpdatesFieldsOfExistingProfile() {
		when(repository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existingProfile));
		final NutritionistProfile updated = new NutritionistProfile();
		updated.setId(1L);
		updated.setUserId(TEST_USER_ID);
		updated.setCedulaProfesional("99887766");
		updated.setDisplayName("Dr. Updated");
		when(repository.save(any(NutritionistProfile.class))).thenReturn(updated);

		final NutritionistProfile incomingProfile = new NutritionistProfile();
		incomingProfile.setCedulaProfesional("99887766");
		incomingProfile.setDisplayName("Dr. Updated");

		final NutritionistProfile result = service.saveProfile(incomingProfile, TEST_USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getCedulaProfesional()).isEqualTo("99887766");
		assertThat(result.getDisplayName()).isEqualTo("Dr. Updated");
	}

	@Test
	public void testGetLogo_WhenNoProfileExists_ReturnsNull() {
		when(repository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

		final byte[] result = service.getLogo(TEST_USER_ID);

		assertThat(result).isNull();
	}

	@Test
	public void testGetLogo_WhenProfileHasNoLogoExtension_ReturnsNull() {
		existingProfile.setLogoExtension(null);
		when(repository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existingProfile));

		final byte[] result = service.getLogo(TEST_USER_ID);

		assertThat(result).isNull();
	}

	@Test
	public void testGetLogoAsBase64DataUri_WhenNoLogo_ReturnsNull() {
		when(repository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

		final String result = service.getLogoAsBase64DataUri(TEST_USER_ID);

		assertThat(result).isNull();
	}

	@Test
	public void testGetLogoAsBase64DataUri_WhenLogoExtensionNull_ReturnsNull() {
		existingProfile.setLogoExtension(null);
		when(repository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existingProfile));

		final String result = service.getLogoAsBase64DataUri(TEST_USER_ID);

		assertThat(result).isNull();
	}

}
