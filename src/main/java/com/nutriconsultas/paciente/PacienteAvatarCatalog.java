package com.nutriconsultas.paciente;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Curated patient avatar keys and static asset paths (#241).
 */
public final class PacienteAvatarCatalog {

	private static final String AVATAR_BASE_PATH = "/sbadmin/img/paciente-avatars/";

	public static final String DEFAULT_MALE_ID = "avatar_1";

	public static final String DEFAULT_FEMALE_ID = "avatar_6";

	private static final List<PacienteAvatarOption> OPTIONS = buildOptions();

	private static final Map<String, PacienteAvatarOption> BY_ID = OPTIONS.stream()
		.collect(Collectors.toUnmodifiableMap(PacienteAvatarOption::id, Function.identity()));

	private static final Map<String, String> LEGACY_ID_ALIASES = buildLegacyAliases();

	private PacienteAvatarCatalog() {
	}

	public static List<PacienteAvatarOption> allOptions() {
		return OPTIONS;
	}

	public static List<PacienteAvatarOption> adultOptions() {
		return OPTIONS.stream().filter(PacienteAvatarCatalog::isAdultOption).toList();
	}

	public static List<PacienteAvatarOption> kidOptions() {
		return OPTIONS.stream().filter(option -> !isAdultOption(option)).toList();
	}

	public static boolean isValid(final String avatarId) {
		return avatarId != null && BY_ID.containsKey(avatarId);
	}

	public static String defaultIdForGender(final String gender) {
		return "M".equalsIgnoreCase(gender) ? DEFAULT_MALE_ID : DEFAULT_FEMALE_ID;
	}

	public static String defaultImagePath() {
		return imagePathForId(DEFAULT_MALE_ID);
	}

	public static String resolveImagePath(final Paciente paciente) {
		if (paciente == null) {
			return defaultImagePath();
		}
		return imagePathForId(resolveSelectedId(paciente));
	}

	public static String resolveSelectedId(final Paciente paciente) {
		if (paciente == null) {
			return DEFAULT_MALE_ID;
		}
		final String storedId = paciente.getAvatarId();
		if (storedId != null) {
			final String normalizedId = normalizeId(storedId);
			if (isValid(normalizedId)) {
				return normalizedId;
			}
		}
		return defaultIdForGender(paciente.getGender());
	}

	public static String imagePathForId(final String avatarId) {
		final String normalizedId = normalizeId(avatarId);
		return Optional.ofNullable(BY_ID.get(normalizedId))
			.map(PacienteAvatarOption::imagePath)
			.orElseGet(() -> Objects.requireNonNull(BY_ID.get(DEFAULT_MALE_ID)).imagePath());
	}

	static String normalizeId(final String avatarId) {
		if (avatarId == null) {
			return null;
		}
		if (BY_ID.containsKey(avatarId)) {
			return avatarId;
		}
		return LEGACY_ID_ALIASES.get(avatarId);
	}

	private static List<PacienteAvatarOption> buildOptions() {
		final List<PacienteAvatarOption> options = new ArrayList<>();
		IntStream.rangeClosed(1, 10)
			.forEach(index -> options
				.add(new PacienteAvatarOption(adultId(index), adultImagePath(index), "Avatar " + index)));
		IntStream.rangeClosed(1, 10)
			.forEach(index -> options
				.add(new PacienteAvatarOption(kidId(index), kidImagePath(index), "Avatar niño " + index)));
		return List.copyOf(options);
	}

	private static Map<String, String> buildLegacyAliases() {
		final Map<String, String> aliases = new HashMap<>();
		aliases.put("ava3", DEFAULT_MALE_ID);
		aliases.put("ava5", DEFAULT_FEMALE_ID);
		aliases.put("profile1", "avatar_3");
		aliases.put("profile2", "avatar_8");
		aliases.put("profile3", "avatar_10");
		IntStream.rangeClosed(1, 10).forEach(index -> {
			aliases.put(String.format("avatar%02d", index), adultId(index));
			aliases.put(String.format("avatar%02d", 10 + index), kidId(index));
		});
		return Map.copyOf(aliases);
	}

	private static boolean isAdultOption(final PacienteAvatarOption option) {
		return option.id().startsWith("avatar_");
	}

	private static String adultId(final int index) {
		return "avatar_" + index;
	}

	private static String kidId(final int index) {
		return "kid_avatar_" + index;
	}

	private static String adultImagePath(final int index) {
		return AVATAR_BASE_PATH + adultId(index) + ".png";
	}

	private static String kidImagePath(final int index) {
		return AVATAR_BASE_PATH + kidId(index) + ".png";
	}

}
