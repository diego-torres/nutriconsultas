package com.nutriconsultas.clinical.exam.anthropometric;

import java.util.Map;
import java.util.Optional;

/**
 * Static images that illustrate how to take anthropometric measurements (pliegues,
 * perímetros, diámetros, etc.).
 */
public final class AnthropometricVisualAidCatalog {

	private static final String ANTHROPOMETRY_BASE_PATH = "/sbadmin/img/anthropometry/";

	private record VisualAidAsset(String folder, String fileName) {
	}

	private static final Map<String, VisualAidAsset> ASSETS = Map.ofEntries(
			Map.entry("skinfolds.subscapularSkinfold", new VisualAidAsset("pliegues", "subescapular.png")),
			Map.entry("skinfolds.tricepsSkinfold", new VisualAidAsset("pliegues", "triceps.png")),
			Map.entry("skinfolds.bicepsSkinfold", new VisualAidAsset("pliegues", "biceps.png")),
			Map.entry("skinfolds.iliacCrestSkinfold", new VisualAidAsset("pliegues", "cresta-iliaca.png")),
			Map.entry("skinfolds.supraespinalSkinfold", new VisualAidAsset("pliegues", "supraespinal.png")),
			Map.entry("skinfolds.abdominalSkinfold", new VisualAidAsset("pliegues", "abdominal.png")),
			Map.entry("skinfolds.frontalThighSkinfold", new VisualAidAsset("pliegues", "muslo-frontal.png")),
			Map.entry("skinfolds.medialCalfSkinfold", new VisualAidAsset("pliegues", "pantorrilla-medial.png")),
			Map.entry("skinfolds.medialAxillarySkinfold", new VisualAidAsset("pliegues", "axilar-medial.png")),
			Map.entry("skinfolds.pectoralSkinfold", new VisualAidAsset("pliegues", "pectoral.png")),
			Map.entry("circumferences.cephalicCircumference", new VisualAidAsset("perimetros", "cefalico.png")),
			Map.entry("circumferences.neckCircumference", new VisualAidAsset("perimetros", "cuello.png")),
			Map.entry("circumferences.midUpperArmCircumferenceRelaxed",
					new VisualAidAsset("perimetros", "brazo-relajado.png")),
			Map.entry("circumferences.midUpperArmCircumferenceContracted",
					new VisualAidAsset("perimetros", "brazo-contraido.png")),
			Map.entry("circumferences.forearmCircumference", new VisualAidAsset("perimetros", "antebrazo.png")),
			Map.entry("circumferences.wristCircumference", new VisualAidAsset("perimetros", "muneca.png")),
			Map.entry("circumferences.mesosternalCircumference", new VisualAidAsset("perimetros", "mesoesternal.png")),
			Map.entry("circumferences.umbilicalCircumference", new VisualAidAsset("perimetros", "umbilical.png")),
			Map.entry("circumferences.waistCircumference", new VisualAidAsset("perimetros", "cintura.png")),
			Map.entry("circumferences.hipCircumference", new VisualAidAsset("perimetros", "cadera.png")),
			Map.entry("circumferences.thighCircumference", new VisualAidAsset("perimetros", "muslo.png")),
			Map.entry("circumferences.midThighCircumference", new VisualAidAsset("perimetros", "muslo-medio.png")),
			Map.entry("circumferences.calfCircumference", new VisualAidAsset("perimetros", "pantorrilla.png")),
			Map.entry("circumferences.ankleCircumference", new VisualAidAsset("perimetros", "tobillo.png")),
			Map.entry("diameters.biacromialDiameter", new VisualAidAsset("diametros", "biacromial.png")),
			Map.entry("diameters.biiliocrestalDiameter", new VisualAidAsset("diametros", "biiliocrestal.png")),
			Map.entry("diameters.footLength", new VisualAidAsset("diametros", "longitud-pie.png")),
			Map.entry("diameters.transverseThoraxDiameter", new VisualAidAsset("diametros", "transverso-torax.png")),
			Map.entry("diameters.anteroposteriorThoraxDiameter",
					new VisualAidAsset("diametros", "anteroposterior-torax.png")),
			Map.entry("diameters.humerusDiameter", new VisualAidAsset("diametros", "humero.png")),
			Map.entry("diameters.biestiloidWristDiameter", new VisualAidAsset("diametros", "biestiloideo-muneca.png")),
			Map.entry("diameters.femurDiameter", new VisualAidAsset("diametros", "femur.png")),
			Map.entry("diameters.bimaleolarDiameter", new VisualAidAsset("diametros", "bimaleolar.png")),
			Map.entry("diameters.transverseFootDiameter", new VisualAidAsset("diametros", "transverso-pie.png")),
			Map.entry("diameters.handLength", new VisualAidAsset("diametros", "longitud-mano.png")),
			Map.entry("diameters.transverseHandDiameter", new VisualAidAsset("diametros", "transverso-mano.png")));

	private AnthropometricVisualAidCatalog() {
	}

	public static boolean isSkinfoldField(final String fieldKey) {
		return fieldKey != null && fieldKey.startsWith("skinfolds.");
	}

	public static boolean isCircumferenceField(final String fieldKey) {
		return fieldKey != null && fieldKey.startsWith("circumferences.");
	}

	public static boolean isDiameterField(final String fieldKey) {
		return fieldKey != null && fieldKey.startsWith("diameters.");
	}

	public static boolean isVisualAidField(final String fieldKey) {
		return isSkinfoldField(fieldKey) || isCircumferenceField(fieldKey) || isDiameterField(fieldKey);
	}

	public static Optional<String> imageFolder(final String fieldKey) {
		return asset(fieldKey).map(VisualAidAsset::folder);
	}

	public static Optional<String> imageFileName(final String fieldKey) {
		return asset(fieldKey).map(VisualAidAsset::fileName);
	}

	public static Optional<String> imagePath(final String fieldKey) {
		return asset(fieldKey).map(asset -> ANTHROPOMETRY_BASE_PATH + asset.folder() + "/" + asset.fileName());
	}

	private static Optional<VisualAidAsset> asset(final String fieldKey) {
		if (fieldKey == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(ASSETS.get(fieldKey));
	}

}
