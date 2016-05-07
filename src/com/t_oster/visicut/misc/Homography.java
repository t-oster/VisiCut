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
/*
 * Copyright 2016 Google Inc.
 */
package com.t_oster.visicut.misc;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import Jama.Matrix;
import Jama.EigenvalueDecomposition;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import com.t_oster.visicut.model.LaserDevice;

/**
 * This class solves and applies a homography, which warps one planar space to
 * another.  This is used for perspective correction.
 */
public class Homography {


  private Point2D.Double[] referencePoints;  //marker points in the cutter, in mm
  private Point2D.Double[] viewPoints; //points seen in the camera, in pixels

  /** this matrix will hold the homography to convert from camera space to view space */
  Matrix homography;
  /** for historical reasons, support a 2-point correspondance affine transform */
  AffineTransform affine;

  public Homography(Point2D.Double[] referencePoints, Point2D.Double[] viewPoints) {
    this.referencePoints = referencePoints;
    this.viewPoints = viewPoints;
    solveHomography();
  }

  public static Homography fromAffineTransform(AffineTransform f, LaserDevice ld) {
    // The old format for camera calibration just used an affine transform.
    // Here we can convert that to a 2-point correspondence.
    Point2D.Double[] referencePoints = {
      new Point2D.Double(0.2d*ld.getLaserCutter().getBedWidth(), 0.2d*ld.getLaserCutter().getBedHeight()),
      new Point2D.Double(0.8d*ld.getLaserCutter().getBedWidth(), 0.8d*ld.getLaserCutter().getBedHeight())};
    Point2D.Double[] imagePoints = new Point2D.Double[2];
    try {
      AffineTransform mm2img = f.createInverse();
      mm2img.transform(referencePoints, 0, imagePoints, 0, 2);
    } catch (java.awt.geom.NoninvertibleTransformException e) {
      // just assign a default
      imagePoints = referencePoints;
    }
    return new Homography(referencePoints, imagePoints);
  }

  public Point2D.Double[] getReferencePoints() {
    return referencePoints;
  }

  public Point2D.Double[] getViewPoints() {
    return viewPoints;
  }

  /** compute the homography between two image spaces given some correspondences.
   * http://www.cs.washington.edu/education/courses/cse576/03sp/lectures/projective_files/frame.htm
   * It takes 4 points to solve a homography, but having more correspondence points
   * can smooth out the result.  For historical reasons, also produce an affine
   * transformation if only two points are given.
   */
  private void solveHomography() {
    if (referencePoints.length == 2) {
      affine = Helper.getTransform(
          new Rectangle2D.Double(referencePoints[0].x, referencePoints[0].y, referencePoints[1].x - referencePoints[0].x, referencePoints[1].y - referencePoints[0].y),
      new Rectangle2D.Double(viewPoints[0].x, viewPoints[0].y, viewPoints[1].x - viewPoints[0].x, viewPoints[1].y - viewPoints[0].y)
      );
      return;
    }
    Matrix A = new Matrix(2*viewPoints.length,9);
    //set the matrix A based on the provided correspondences
    for (int i=0; i < viewPoints.length; i++) {
      A.set(i*2,0,referencePoints[i].x);
      A.set(i*2,1,referencePoints[i].y);
      A.set(i*2,2,1);
      A.set(i*2+1,3,referencePoints[i].x);
      A.set(i*2+1,4,referencePoints[i].y);
      A.set(i*2+1,5,1);
      A.set(i*2,6,-viewPoints[i].x*referencePoints[i].x);
      A.set(i*2,7,-viewPoints[i].x*referencePoints[i].y);
      A.set(i*2,8,-viewPoints[i].x);
      A.set(i*2+1,6,-viewPoints[i].y*referencePoints[i].x);
      A.set(i*2+1,7,-viewPoints[i].y*referencePoints[i].y);
      A.set(i*2+1,8,-viewPoints[i].y);
    }
    //the eigenvector corresponding to the smallest eigenvalue of ATA
    // is the homography to be used
    Matrix ATA = A.transpose().times(A);
    EigenvalueDecomposition egd = new EigenvalueDecomposition(ATA);
    int s=0;
    double min = Double.MAX_VALUE;
    double[] eigval = egd.getRealEigenvalues();
    for (int i=0; i < eigval.length; i++) {
      if (eigval[i]< min) {
        min = eigval[i];
        s = i;
      }
    }
    Matrix v = egd.getV();
    //take that eigenvector (column) and make a 3x3 matrix out of it
    homography = new Matrix(new double[][]{{v.get(0,s),v.get(1,s),v.get(2,s)},
                                           {v.get(3,s),v.get(4,s),v.get(5,s)},
                                           {v.get(6,s),v.get(7,s),v.get(8,s)}
        });
  }

  public Point2D transform(Point2D input, Point2D output) {
    if (affine != null) {
      return affine.transform(input, output);
    }
    Point2D.Double outputPt;
    if (output == null || !(output instanceof Point2D.Double)) {
      outputPt = new Point2D.Double();
    } else {
      outputPt = (Point2D.Double)output;
    }
    Matrix m = new Matrix(new double[]{input.getX(),input.getY(),1},3);
    Matrix r = homography.times(m);
    outputPt.x = r.get(0,0)/r.get(2,0);
    outputPt.y = r.get(1,0)/r.get(2,0);
    return outputPt;
  }

  public BufferedImage correct(BufferedImage input, double widthmm, double heightmm, BufferedImage output) {
    // determine output size by mapping 0,0 and width,height to the input image
    Point2D.Double top = (Point2D.Double)transform(new Point2D.Double(0, 0), null);
    Point2D.Double bottom = (Point2D.Double)transform(new Point2D.Double(widthmm, heightmm), null);
    double xscalefactor = Math.abs(top.x - bottom.x) / widthmm;
    double yscalefactor = Math.abs(top.y - bottom.y) / heightmm;
    // Choose the larger scale factor because it will lose less input pixels,
    // and then do a uniform scale to match the bed size.
    double scalefactor = xscalefactor > yscalefactor ? xscalefactor : yscalefactor;
    int width = (int)(widthmm * scalefactor);
    int height = (int)(heightmm * scalefactor);
    // choose the one that will lose the least pixels, make a proportional image based on that scale factor
    if (output == null || output.getWidth() != width || output.getHeight() != height) {
      output = new BufferedImage(width, height, input.getType());
    }
    // for every point in output image, find sample from camera image
    int tc =0;
    Point2D.Double p = null;
    for (int y =0; y < output.getHeight(); y++) {
      for (int x =0; x < output.getWidth(); x++) {
        p = (Point2D.Double)transform(new Point2D.Double(x/scalefactor,y/scalefactor), p);
        if (p.x >=0 && p.x < input.getWidth() && p.y>=0 && p.y < input.getHeight()) {
          output.setRGB(x,y,input.getRGB((int)p.x,(int)p.y));
        } else {
          output.setRGB(x,y,0xFFFFFF);
        }
      }
    }
    return output;
  }

}
