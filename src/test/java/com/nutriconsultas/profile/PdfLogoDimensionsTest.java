package com.nutriconsultas.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PdfLogoDimensions}.
 */
public class PdfLogoDimensionsTest {

	@Test
	public void computeFromPixelSizeScalesSquareImageToMaxBox() {
		final PdfLogoDimensions.DisplaySize size = PdfLogoDimensions.computeFromPixelSize(1500, 1500);

		assertThat(size.widthPt()).isEqualTo(108.0);
		assertThat(size.heightPt()).isEqualTo(108.0);
	}

	@Test
	public void computeFromPixelSizePreservesAspectRatioForWideImage() {
		final PdfLogoDimensions.DisplaySize size = PdfLogoDimensions.computeFromPixelSize(2000, 1000);

		assertThat(size.widthPt()).isEqualTo(108.0);
		assertThat(size.heightPt()).isEqualTo(54.0);
	}

	@Test
	public void computeFromPixelSizePreservesAspectRatioForTallImage() {
		final PdfLogoDimensions.DisplaySize size = PdfLogoDimensions.computeFromPixelSize(800, 1600);

		assertThat(size.widthPt()).isEqualTo(54.0);
		assertThat(size.heightPt()).isEqualTo(108.0);
	}

	@Test
	public void toInlineStyleUsesExplicitPointDimensions() {
		final String style = PdfLogoDimensions.toInlineStyle(new PdfLogoDimensions.DisplaySize(108.0, 54.0));

		assertThat(style).isEqualTo("width: 108.0pt; height: 54.0pt;");
	}

	@Test
	public void computeFromDataUriParsesPngAndFitsBox() throws Exception {
		final String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(createPngBytes(1200, 600));

		final PdfLogoDimensions.DisplaySize size = PdfLogoDimensions.computeFromDataUri(dataUri);

		assertThat(size).isNotNull();
		assertThat(size.widthPt()).isEqualTo(108.0);
		assertThat(size.heightPt()).isEqualTo(54.0);
	}

	@Test
	public void computeFromDataUriReturnsNullForMissingUri() {
		assertThat(PdfLogoDimensions.computeFromDataUri(null)).isNull();
		assertThat(PdfLogoDimensions.computeFromDataUri("")).isNull();
	}

	private byte[] createPngBytes(final int width, final int height) throws Exception {
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.RED);
		graphics.fillRect(0, 0, width, height);
		graphics.dispose();
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", outputStream);
		return outputStream.toByteArray();
	}

}
