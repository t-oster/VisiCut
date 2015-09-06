/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
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
package com.frochr123.helper;

import com.objectplanet.image.PngEncoder;
import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import com.t_oster.visicut.model.PlfPart;
import com.t_oster.visicut.VisicutModel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * PreviewImageExport.java: Generate the current preview as image and export / send it to other locations
 * @author Christian
 */
public class PreviewImageExport
{
  private static BufferedImage latestGeneratedImage = null;

  public static BufferedImage getLatestGeneratedImage()
  {
    return latestGeneratedImage;
  }

  private static void setLatestGeneratedImage(BufferedImage latestGeneratedImage)
  {
    PreviewImageExport.latestGeneratedImage = latestGeneratedImage;
  }

  public static BufferedImage generateImage(int imageWidth, int imageHeight, boolean filledImage)
  {
    if (MainView.getInstance() == null || MainView.getInstance().getDialog() == null
        || VisicutModel.getInstance() == null
        || VisicutModel.getInstance().getSelectedLaserDevice() == null
        || VisicutModel.getInstance().getSelectedLaserDevice().getLaserCutter() == null
        || imageWidth <= 0 || imageHeight <= 0)
    {
      return null;
    }

    // Color settings
    float strokeWidth = 1.5f;
    Color strokeColor = Color.RED;
    Color backgroundColor = Color.BLACK;

    // Measured in mm
    double bedWidth = VisicutModel.getInstance().getSelectedLaserDevice().getLaserCutter().getBedWidth();
    double bedHeight = VisicutModel.getInstance().getSelectedLaserDevice().getLaserCutter().getBedHeight();

    BufferedImage resultImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = resultImage.createGraphics();
    Stroke s = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
    g.setBackground(backgroundColor);
    g.clearRect(0, 0, resultImage.getWidth(), resultImage.getHeight());
    g.setColor(strokeColor);
    g.setStroke(s);

    // Return empty image if requested (e.g. turning projector from on to off)
    if (!filledImage)
    {
      setLatestGeneratedImage(resultImage);
      return resultImage;
    }

    double mm2pxScaleFactor = Math.min((double)(imageWidth)/bedWidth, (double)(imageHeight)/bedHeight);
    AffineTransform scaleTransform = AffineTransform.getScaleInstance(mm2pxScaleFactor, mm2pxScaleFactor);

    // TODO Add support for different mappings, currently only simple lines (as in cut, mark) supported, performance issue?
    for (PlfPart part : VisicutModel.getInstance().getPlfFile().getPartsCopy())
    {
      if (part == null)
      {
        continue;
      }

      GraphicSet graphicSet = part.getGraphicObjects();

      if (graphicSet == null)
      {
        continue;
      }

      for (GraphicObject graphicObject : graphicSet)
      {
        // Shape measured in mm
        Shape shape = (graphicObject instanceof ShapeObject) ? ((ShapeObject)graphicObject).getShape() : graphicObject.getBoundingBox();

        if (graphicSet.getTransform() != null)
        {
          shape = graphicSet.getTransform().createTransformedShape(shape);
        }

        // Transform shape from mm to pixel
        shape = scaleTransform.createTransformedShape(shape);

        if (shape != null)
        {
          PathIterator iter = shape.getPathIterator(null, 1);
          int startx = 0;
          int starty = 0;
          int lastx = 0;
          int lasty = 0;

          while (!iter.isDone())
          {
            double[] test = new double[8];
            int result = iter.currentSegment(test);
            //transform coordinates to preview-coordinates
            //laserPx2PreviewPx.transform(test, 0, test, 0, 1);
            if (result == PathIterator.SEG_MOVETO)
            {
              startx = (int) test[0];
              starty = (int) test[1];
              lastx = (int) test[0];
              lasty = (int) test[1];
            }
            else if (result == PathIterator.SEG_LINETO)
            {
              g.drawLine(lastx, lasty, (int)test[0], (int)test[1]);
              lastx = (int) test[0];
              lasty = (int) test[1];
            }
            else if (result == PathIterator.SEG_CLOSE)
            {
              g.drawLine(lastx, lasty, startx, starty);
            }
            iter.next();
          }
        }
      }
    }

    g.dispose();
    setLatestGeneratedImage(resultImage);
    return resultImage;
  }

  public static void writePngToOutputStream(OutputStream imageOutputStream, BufferedImage img) throws IOException
  {
    // PngEncoder by objectplanet, has better performance than default Java ImageIO converter
    PngEncoder pngencoder = new PngEncoder();
    pngencoder.encode(img, imageOutputStream);
  }

  public static void writePngToFile(String filePath, BufferedImage img) throws FileNotFoundException, IOException
  {
    File imageOutputFile = new File(filePath);
    FileOutputStream imageOutputStream = new FileOutputStream(imageOutputFile);
    writePngToOutputStream(imageOutputStream, img);
    imageOutputStream.close();
  }
}
