/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2024 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package de.thomas_oster.visicut.model.graphicelements.gcodesupport;

import org.junit.Test;
import static org.junit.Assert.*;
import de.thomas_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class GCodeImporterTest
{
  /**
   * Imports the G-Code and returns the drawn contours (flattened polylines).
   * Moves that are not followed by a drawing command are dropped.
   */
  private List<List<double[]>> contours(String gcode, List<String> warnings) throws Exception
  {
    File file = File.createTempFile("visicut-gcode-test", ".nc");
    file.deleteOnExit();
    try (FileWriter w = new FileWriter(file))
    {
      w.write(gcode);
    }
    GraphicSet set = new GCodeImporter().importSetFromFile(file, warnings);
    GCodeShape shape = (GCodeShape) set.get(0);
    List<List<double[]>> result = new ArrayList<>();
    List<double[]> current = null;
    double[] c = new double[6];
    for (PathIterator it = shape.getShape().getPathIterator(null, 0.001); !it.isDone(); it.next())
    {
      int type = it.currentSegment(c);
      if (type == PathIterator.SEG_MOVETO)
      {
        current = new ArrayList<>();
        result.add(current);
      }
      if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
      {
        current.add(new double[]{c[0], c[1]});
      }
    }
    result.removeIf(contour -> contour.size() < 2);
    return result;
  }

  private List<List<double[]>> contours(String gcode) throws Exception
  {
    return contours(gcode, new ArrayList<>());
  }

  private void assertPoints(List<double[]> contour, double[]... expected)
  {
    for (double[] p : expected)
    {
      boolean found = contour.stream().anyMatch(q -> Math.abs(q[0] - p[0]) < 0.01 && Math.abs(q[1] - p[1]) < 0.01);
      assertTrue("missing point " + p[0] + "," + p[1], found);
    }
  }

  private void assertOnCircle(List<double[]> contour, double cx, double cy, double radius)
  {
    for (double[] p : contour)
    {
      assertEquals(radius, Math.hypot(p[0] - cx, p[1] - cy), 0.01);
    }
  }

  private void assertExtent(List<double[]> contour, double minX, double minY, double maxX, double maxY)
  {
    double[] min = {Double.MAX_VALUE, Double.MAX_VALUE};
    double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE};
    for (double[] p : contour)
    {
      min[0] = Math.min(min[0], p[0]);
      min[1] = Math.min(min[1], p[1]);
      max[0] = Math.max(max[0], p[0]);
      max[1] = Math.max(max[1], p[1]);
    }
    assertEquals(minX, min[0], 0.02);
    assertEquals(minY, min[1], 0.02);
    assertEquals(maxX, max[0], 0.02);
    assertEquals(maxY, max[1], 0.02);
  }

  @Test
  public void testAbsoluteLines() throws Exception
  {
    List<List<double[]>> result = contours("G21 G90\nG0 X10 Y10\nG1 X30 Y10\nX30 Y20\nX10 Y20\nX10 Y10\nG0 X0 Y0\n");
    assertEquals(1, result.size());
    assertPoints(result.get(0), new double[]{10, 10}, new double[]{30, 10}, new double[]{30, 20}, new double[]{10, 20});
  }

  @Test
  public void testSingleDigitCommands() throws Exception
  {
    List<List<double[]>> result = contours("G1 X10 Y0\nG0 X20 Y0\nG1 X30 Y0\n");
    assertEquals(2, result.size());
    assertPoints(result.get(0), new double[]{0, 0}, new double[]{10, 0});
    assertPoints(result.get(1), new double[]{20, 0}, new double[]{30, 0});
  }

  @Test
  public void testRelativeCoordinates() throws Exception
  {
    List<List<double[]>> result = contours("G91\nG0 X5 Y5\nG1 X10\nY10\nX-10\nY-10\nG90\nG0 X0 Y0\n");
    assertEquals(1, result.size());
    assertPoints(result.get(0), new double[]{5, 5}, new double[]{15, 5}, new double[]{15, 15}, new double[]{5, 15});
    assertExtent(result.get(0), 5, 5, 15, 15);
  }

  @Test
  public void testArcsWithRelativeCenter() throws Exception
  {
    // counter-clockwise quarter from 0 to 90 degrees around the origin
    List<List<double[]>> ccw = contours("G0 X10 Y0\nG3 X0 Y10 I-10 J0\n");
    assertEquals(1, ccw.size());
    assertOnCircle(ccw.get(0), 0, 0, 10);
    assertExtent(ccw.get(0), 0, 0, 10, 10);

    // clockwise the same end points take the 270 degree way round
    List<List<double[]>> cw = contours("G0 X10 Y0\nG2 X0 Y10 I-10 J0\n");
    assertOnCircle(cw.get(0), 0, 0, 10);
    assertExtent(cw.get(0), -10, -10, 10, 10);

    // full circle when start and end coincide
    List<List<double[]>> full = contours("G0 X20 Y10\nG3 X20 Y10 I-5\n");
    assertOnCircle(full.get(0), 15, 10, 5);
    assertExtent(full.get(0), 10, 5, 20, 15);

    // relative coordinates
    List<List<double[]>> relative = contours("G91\nG0 X10 Y0\nG3 X-10 Y10 I-10\n");
    assertOnCircle(relative.get(0), 0, 0, 10);
    assertExtent(relative.get(0), 0, 0, 10, 10);
  }

  @Test
  public void testArcsWithAbsoluteCenter() throws Exception
  {
    List<List<double[]>> ccw = contours("G90.1\nG0 X20 Y10\nG3 X10 Y20 I10 J10\n");
    assertOnCircle(ccw.get(0), 10, 10, 10);
    assertExtent(ccw.get(0), 10, 10, 20, 20);
  }

  @Test
  public void testArcsWithRadius() throws Exception
  {
    // R > 0: short arc
    List<List<double[]>> shortArc = contours("G2 X10 Y0 R10\n");
    assertOnCircle(shortArc.get(0), 5, -8.6603, 10);
    assertExtent(shortArc.get(0), 0, 0, 10, 1.3397);

    // R < 0: long arc
    List<List<double[]>> longArc = contours("G0 X10 Y0\nG3 X20 Y0 R-10\n");
    assertOnCircle(longArc.get(0), 15, -8.6603, 10);
    assertExtent(longArc.get(0), 5, -18.6603, 25, 0);

    // full circle with R is not defined
    List<String> warnings = new ArrayList<>();
    assertTrue(contours("G0 X10 Y0\nG2 X10 Y0 R5\n", warnings).isEmpty());
    assertEquals(1, warnings.size());
  }

  @Test
  public void testInchUnits() throws Exception
  {
    List<List<double[]>> result = contours("G20\nG0 X1 Y1\nG1 X2 Y1\nX2 Y2\n");
    assertPoints(result.get(0), new double[]{25.4, 25.4}, new double[]{50.8, 25.4}, new double[]{50.8, 50.8});
  }
}
