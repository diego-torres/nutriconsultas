package com.nutriconsultas.reports;

import java.util.List;

/**
 * Renders simple SVG line charts for PDF reports (Flying Saucer compatible).
 */
public final class ReportLineChartRenderer {

	private static final int WIDTH = 480;

	private static final int HEIGHT = 150;

	private static final int PADDING_LEFT = 45;

	private static final int PADDING_RIGHT = 15;

	private static final int PADDING_TOP = 15;

	private static final int PADDING_BOTTOM = 30;

	private ReportLineChartRenderer() {
	}

	public static String render(final List<String> labels, final List<Double> values, final String lineColor,
			final String yUnit) {
		if (values == null || values.isEmpty()) {
			return "";
		}

		final double minValue = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
		final double maxValue = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
		final double range = maxValue - minValue;
		final double paddedMin = range > 0 ? minValue - range * 0.1 : minValue - 1;
		final double paddedMax = range > 0 ? maxValue + range * 0.1 : maxValue + 1;
		final double valueRange = paddedMax - paddedMin;
		final double safeRange = valueRange > 0 ? valueRange : 1;

		final int plotWidth = WIDTH - PADDING_LEFT - PADDING_RIGHT;
		final int plotHeight = HEIGHT - PADDING_TOP - PADDING_BOTTOM;

		final StringBuilder svg = new StringBuilder();
		svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
			.append(WIDTH)
			.append("\" height=\"")
			.append(HEIGHT)
			.append("\" viewBox=\"0 0 ")
			.append(WIDTH)
			.append(' ')
			.append(HEIGHT)
			.append("\">");

		// Plot area background
		svg.append("<rect x=\"")
			.append(PADDING_LEFT)
			.append("\" y=\"")
			.append(PADDING_TOP)
			.append("\" width=\"")
			.append(plotWidth)
			.append("\" height=\"")
			.append(plotHeight)
			.append("\" fill=\"#f8f9fc\" stroke=\"#e3e6f0\" stroke-width=\"1\"/>");

		// Y-axis labels (min and max)
		appendYAxisLabel(svg, paddedMin, yUnit, plotHeight);
		appendYAxisLabel(svg, paddedMax, yUnit, 0);

		// Grid lines
		for (int gridLine = 0; gridLine <= 4; gridLine++) {
			final int gridY = PADDING_TOP + (plotHeight * gridLine / 4);
			svg.append("<line x1=\"")
				.append(PADDING_LEFT)
				.append("\" y1=\"")
				.append(gridY)
				.append("\" x2=\"")
				.append(PADDING_LEFT + plotWidth)
				.append("\" y2=\"")
				.append(gridY)
				.append("\" stroke=\"#e3e6f0\" stroke-width=\"1\"/>");
		}

		// Build polyline and data points
		final StringBuilder polylinePoints = new StringBuilder();
		for (int index = 0; index < values.size(); index++) {
			final double value = values.get(index);
			final int x = computeX(index, values.size(), plotWidth);
			final int y = computeY(value, paddedMin, safeRange, plotHeight);
			if (index > 0) {
				polylinePoints.append(' ');
			}
			polylinePoints.append(x).append(',').append(y);
			svg.append("<circle cx=\"")
				.append(x)
				.append("\" cy=\"")
				.append(y)
				.append("\" r=\"3\" fill=\"")
				.append(lineColor)
				.append("\"/>");
		}

		svg.append("<polyline fill=\"none\" stroke=\"")
			.append(lineColor)
			.append("\" stroke-width=\"2\" points=\"")
			.append(polylinePoints)
			.append("\"/>");

		// X-axis date labels (first, middle if enough points, last)
		appendXAxisLabels(svg, labels, plotWidth);

		svg.append("</svg>");
		return svg.toString();
	}

	private static void appendYAxisLabel(final StringBuilder svg, final double value, final String yUnit,
			final int plotYOffset) {
		final int y = PADDING_TOP + plotYOffset;
		final String formattedValue = formatValue(value);
		final String label = yUnit != null && !yUnit.isEmpty() ? formattedValue + " " + yUnit : formattedValue;
		svg.append("<text x=\"")
			.append(PADDING_LEFT - 5)
			.append("\" y=\"")
			.append(y + 4)
			.append("\" font-size=\"8\" fill=\"#858796\" text-anchor=\"end\">")
			.append(escapeXml(label))
			.append("</text>");
	}

	private static void appendXAxisLabels(final StringBuilder svg, final List<String> labels, final int plotWidth) {
		if (labels == null || labels.isEmpty()) {
			return;
		}
		final int lastIndex = labels.size() - 1;
		appendXAxisLabel(svg, labels.get(0), 0, lastIndex, plotWidth);
		if (lastIndex >= 2) {
			final int midIndex = lastIndex / 2;
			appendXAxisLabel(svg, labels.get(midIndex), midIndex, lastIndex, plotWidth);
		}
		if (lastIndex > 0) {
			appendXAxisLabel(svg, labels.get(lastIndex), lastIndex, lastIndex, plotWidth);
		}
	}

	private static void appendXAxisLabel(final StringBuilder svg, final String label, final int index,
			final int lastIndex, final int plotWidth) {
		final int x = computeX(index, lastIndex + 1, plotWidth);
		svg.append("<text x=\"")
			.append(x)
			.append("\" y=\"")
			.append(HEIGHT - 8)
			.append("\" font-size=\"8\" fill=\"#858796\" text-anchor=\"middle\">")
			.append(escapeXml(label))
			.append("</text>");
	}

	private static int computeX(final int index, final int count, final int plotWidth) {
		if (count <= 1) {
			return PADDING_LEFT + plotWidth / 2;
		}
		return PADDING_LEFT + (plotWidth * index / (count - 1));
	}

	private static int computeY(final double value, final double paddedMin, final double safeRange,
			final int plotHeight) {
		final double normalized = (value - paddedMin) / safeRange;
		return PADDING_TOP + (int) Math.round(plotHeight * (1 - normalized));
	}

	private static String formatValue(final double value) {
		if (Math.abs(value) >= 100) {
			return String.format("%.0f", value);
		}
		if (Math.abs(value) >= 10) {
			return String.format("%.1f", value);
		}
		return String.format("%.2f", value);
	}

	private static String escapeXml(final String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

}
