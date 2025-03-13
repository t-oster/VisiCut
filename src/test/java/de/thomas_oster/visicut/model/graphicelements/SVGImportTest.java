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
package de.thomas_oster.visicut.model.graphicelements;

import org.junit.Test;
import static org.junit.Assert.*;
import de.thomas_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import de.thomas_oster.visicut.model.graphicelements.svgsupport.SVGShape;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SVGImportTest
{
  
  @Test
  public void PathWithLocalTransform() throws ImportException, IOException
  {
    // Regression test: Bounding box calculated wrong when path has transform attribute.
    // https://github.com/t-oster/VisiCut/issues/720
    final String exampleSVG = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
      "<svg width=\"40mm\" height=\"30mm\" viewBox=\"0 0 40 30\">\n" +
      "  <path\n" +
      "     d=\"M 0,0 H 20000 L 5000,20000 h 10000\"\n" +
      "     style=\"fill:none;stroke:#0000ff;stroke-width:100\"\n" +
      "     transform=\"scale(0.001,0.001)\"\n" +
      "     id=\"path4\" />\n" +
      "</svg>";
    File tempFile = File.createTempFile("example", ".svg");
    tempFile.deleteOnExit();
    try (FileWriter s = new FileWriter(tempFile)) {
      s.write(exampleSVG);
    }
    SVGImporter imp = new SVGImporter();
    GraphicSet result = imp.importSetFromFile(tempFile.getAbsoluteFile(), new ArrayList<>());
    assertEquals(result.size(), 1);
    // stroke width = 100 * 0.001 local transform * 1 mm width per 1 unit viewbox = 0.1
    assertEquals(0.1, ((SVGShape) result.get(0)).getEffectiveStrokeWidthMm(), 0);
    // "visual bounding box" in SVG pixels including stroke width
    // (expected values were determined in Inkscape)
    // Note: here, SVG pixels are the same as millimeters
    Rectangle2D bb = result.get(0).getBoundingBox();
    // left X = 0.0 mm according to Inkscape, but -0.05mm due to simplified approximation in VisiCut
    assertEquals(-0.05, bb.getMinX(), 1e-9);
    // other values are identical (partly because approximation errors cancel out)
    assertEquals(-0.05, bb.getMinY(), 1e-9);
    assertEquals(20.1, bb.getHeight(), 1e-9);
    assertEquals(20.1, bb.getWidth(), 1e-9);
  }
}