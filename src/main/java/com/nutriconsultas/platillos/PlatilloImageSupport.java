package com.nutriconsultas.platillos;

/**
 * Helpers for platillo picture URLs and presence checks.
 */
public final class PlatilloImageSupport {

	private static final String PLACEHOLDER_IMAGE_PATH = "/sbadmin/img/plato-vacio.jpg";

	private PlatilloImageSupport() {
	}

	public static boolean hasAssignedPicture(final Platillo platillo) {
		return platillo != null && platillo.getImageUrl() != null && !platillo.getImageUrl().isBlank();
	}

	public static String resolveGridImageUrl(final Platillo platillo) {
		if (!hasAssignedPicture(platillo)) {
			return PLACEHOLDER_IMAGE_PATH;
		}
		final String fileName = extractPictureFileName(platillo.getImageUrl());
		return "/admin/platillos/platillo/" + platillo.getId() + "/" + fileName;
	}

	public static String buildGridImageColumn(final Platillo platillo) {
		if (!hasAssignedPicture(platillo)) {
			return "<div class='text-center'>" + "<img src='" + PLACEHOLDER_IMAGE_PATH
					+ "' alt='Sin imagen' class='rounded platillo-grid-thumb platillo-grid-thumb-missing' "
					+ "title='Sin imagen asignada'>"
					+ "<span class='badge badge-warning d-block mt-1'>Sin imagen</span>" + "</div>";
		}
		final String imageUrl = resolveGridImageUrl(platillo);
		return "<div class='text-center'>" + "<img src='" + imageUrl + "' alt='Imagen del platillo' "
				+ "class='rounded platillo-grid-thumb' title='Imagen asignada'>" + "</div>";
	}

	private static String extractPictureFileName(final String imageUrl) {
		final int slashIndex = imageUrl.lastIndexOf('/');
		if (slashIndex >= 0 && slashIndex < imageUrl.length() - 1) {
			return imageUrl.substring(slashIndex + 1);
		}
		return imageUrl;
	}

}
