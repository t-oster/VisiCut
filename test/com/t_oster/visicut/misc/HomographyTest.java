package com.t_oster.visicut.misc;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import static org.junit.Assert.*;
import org.junit.Test;

public class HomographyTest {
  @Test
  public void testAffine() throws IOException {
    final Point2D.Double[] referencePoints = new Point2D.Double[] {
      new Point2D.Double(81.2, 60.800000000000004),
      new Point2D.Double(284.2, 152.0)
    };
    final Point2D.Double[] viewPoints = new Point2D.Double[] {
      new Point2D.Double(1740.0, 1219.0),
      new Point2D.Double(1044.0, 916.0),
    };
    Homography h = new Homography(referencePoints, viewPoints);
    final double widthmm = 406.0;
    final double heightmm = 304.0;

    BufferedImage input = ImageIO.read(
      HomographyTest.class.getResourceAsStream("HomographyTest-input.png"));
    BufferedImage expectedOutput = ImageIO.read(
      HomographyTest.class.getResourceAsStream("HomographyTest-expected-affine-output.png"));
    BufferedImage output = h.correct(input, widthmm, heightmm, null);
    assertImagesAreEqual(expectedOutput, output);
  }

  @Test
  public void testHomography() throws IOException {
    final Point2D.Double[] referencePoints = new Point2D.Double[] {
      new Point2D.Double(81.2, 60.800000000000004),
      new Point2D.Double(324.8, 243.20000000000002),
      new Point2D.Double(324.8, 60.800000000000004),
      new Point2D.Double(81.2, 243.20000000000002),
      new Point2D.Double(203.0, 60.800000000000004),
      new Point2D.Double(203.0, 243.20000000000002),
      new Point2D.Double(121.8, 152.0),
      new Point2D.Double(284.2, 152.0)
    };
    final Point2D.Double[] viewPoints = new Point2D.Double[] {
      new Point2D.Double(1740.0, 1219.0),
      new Point2D.Double(881.0, 589.0),
      new Point2D.Double(904.0, 1231.0),
      new Point2D.Double(1747.0, 589.0),
      new Point2D.Double(1320.0, 1222.0),
      new Point2D.Double(1317.0, 595.0),
      new Point2D.Double(1600.0, 910.0),
      new Point2D.Double(1044.0, 916.0),
    };
    Homography h = new Homography(referencePoints, viewPoints);
    final double widthmm = 406.0;
    final double heightmm = 304.0;

    BufferedImage input = ImageIO.read(
      HomographyTest.class.getResourceAsStream("HomographyTest-input.png"));
    BufferedImage expectedOutput = ImageIO.read(
      HomographyTest.class.getResourceAsStream("HomographyTest-expected-output.png"));
    BufferedImage output = h.correct(input, widthmm, heightmm, null);
    assertImagesAreEqual(expectedOutput, output);

    final double NUM_ITERATIONS = 10;
    long startMillis = System.currentTimeMillis();
    for (int i = 0; i < NUM_ITERATIONS; ++i) {
      h.correct(input, heightmm, heightmm, output);
    }
    long elapsedMillis = System.currentTimeMillis() - startMillis;
    System.out.println(NUM_ITERATIONS + " iterations averaged " + (elapsedMillis/NUM_ITERATIONS) + "ms");
  }

  private void assertImagesAreEqual(BufferedImage expected, BufferedImage actual) throws IOException {
    // Validate "correctness".  Note that since floating-point math and
    // colorspace conversions are involved, a "wrong" output due to code changes
    // might be perfectly acceptable.  Verify visually and replace the expected
    // output if this is the case.
    try {
      assertEquals(expected.getWidth(), actual.getWidth());
      assertEquals(expected.getHeight(), actual.getHeight());
      for (int x = 0; x < actual.getWidth(); ++x) {
        for (int y = 0; y < actual.getHeight(); ++y) {
          assertEquals("Unexpected pixel diff at (" + x + ", " + y + ")",
            expected.getRGB(x, y), actual.getRGB(x, y));
        }
      }
    } catch (AssertionError e) {
      // To debug unexpected outputs, uncomment the next line.
      // assertTrue(ImageIO.write(actual, "PNG", new File("/tmp/test-output.png")));
      throw e;
    }
  }
}
