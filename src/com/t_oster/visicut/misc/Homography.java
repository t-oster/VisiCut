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

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import com.t_oster.visicut.model.LaserDevice;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

/**
 * This class solves and applies a homography, which warps one planar space to
 * another.  This is used for perspective correction.
 */
public class Homography {


  private final Point2D.Double[] referencePoints;  //marker points in the cutter, in mm
  private final Point2D.Double[] viewPoints; //points seen in the camera, in pixels

  /** this matrix will hold the homography to convert from camera space to view space */
  private Matrix homography;

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
      // Account for historical calls with only 2 points.
      AffineTransform affine = Helper.getTransform(
          new Rectangle2D.Double(referencePoints[0].x, referencePoints[0].y, referencePoints[1].x - referencePoints[0].x, referencePoints[1].y - referencePoints[0].y),
          new Rectangle2D.Double(viewPoints[0].x, viewPoints[0].y, viewPoints[1].x - viewPoints[0].x, viewPoints[1].y - viewPoints[0].y)
      );
      double[] flat = new double[6];
      affine.getMatrix(flat);  // Stores as {m00 m10 m01 m11 m02 m12}.
      homography = new Matrix(new double[][]{
        {flat[0], flat[2], flat[4]},
        {flat[1], flat[3], flat[5]},
        {0, 0, 1}});
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

  public Point2D.Double transform(Point2D.Double input) {
    Point2D.Double output = new Point2D.Double();
    Matrix m = new Matrix(new double[]{input.getX(),input.getY(),1},3);
    Matrix r = homography.times(m);
    output.x = r.get(0,0)/r.get(2,0);
    output.y = r.get(1,0)/r.get(2,0);
    return output;
  }

  public BufferedImage correct(BufferedImage input, double widthmm, double heightmm, BufferedImage output) {
    if (input.getType() != BufferedImage.TYPE_INT_ARGB) {
      // NOTE: Since we get & set pixels by RGB value inside
      // the per-pixel loop below, avoid pixel-wise colorspace
      // conversion by forcing the output's type to an RGB
      // colorspace.  Using _ARGB is costlier in space than _RGB,
      // but access is faster.  Using _ARGB over _RGB for output
      // cuts down running time of this method by 7%, and using
      // it for the input cuts down running time by a further
      // 25%.
      BufferedImage transformed = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
      new ColorConvertOp(null).filter(input, transformed);
      input = transformed;
    }

    // The purpose of this function is to do a matrix multiply by the homography
    // and retrieve the normalized results (like transform() above does).
    // However doing this matrix multiplication via method calls and object
    // construction+allocations in the inner per-pixel loop is expensive.
    // Instead we pull out the math into local variables, which speeds up this
    // function by over 2x.
    final double h00 = homography.get(0, 0);
    final double h01 = homography.get(0, 1);
    final double h02 = homography.get(0, 2);
    final double h10 = homography.get(1, 0);
    final double h11 = homography.get(1, 1);
    final double h12 = homography.get(1, 2);
    final double h20 = homography.get(2, 0);
    final double h21 = homography.get(2, 1);
    final double h22 = homography.get(2, 2);
    //  The transformation this is used for is: (x,y) => (x,y,1) =>
    // (h00*x+h01*y+h02, h10*x+h11*y+h12, h20*x+h21*y+h22) =>
    // ((h00*x+h01*y+h02)/(h20*x+h21*y+h22), (h10*x+h11*y+h12)/(h20*x+h21*y+h22))

    // determine output size by mapping 0,0 and width,height to the input image
    Point2D.Double top = new Point2D.Double(h02/h22, h12/h22);
    Point2D.Double bottom = new Point2D.Double(
      (h00*widthmm+h01*heightmm+h02)/(h20*widthmm+h21*heightmm+h22),
      (h10*widthmm+h11*heightmm+h12)/(h20*widthmm+h21*heightmm+h22));
    double xscalefactor = Math.abs(top.x - bottom.x) / widthmm;
    double yscalefactor = Math.abs(top.y - bottom.y) / heightmm;
    // Choose the larger scale factor because it will lose fewer input pixels,
    // and then do a uniform scale to match the bed size.
    double scalefactor = xscalefactor > yscalefactor ? xscalefactor : yscalefactor;
    int width = (int)(widthmm * scalefactor);
    int height = (int)(heightmm * scalefactor);
    // choose the one that will lose the least pixels, make a proportional image based on that scale factor
    if (output == null || output.getWidth() != width || output.getHeight() != height) {
      // See NOTE above about _ARGB.
      output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
    // for every point in output image, find sample from camera image
    final int inputWidth = input.getWidth();
    final int inputHeight = input.getHeight();
    final int outputWidth = output.getWidth();
    final int outputHeight = output.getHeight();
    final double invScaleFactor = 1/scalefactor;
    double sy = 0;
    for (int y =0; y < outputHeight; y++) {
      double sx = 0;
      for (int x = 0; x < outputWidth; x++) {
        final double div = h20*sx+h21*sy+h22;
        final int px = (int)((h00*sx+h01*sy+h02)/div);
        final int py = (int)((h10*sx+h11*sy+h12)/div);
        if (px >=0 && px < inputWidth && py>=0 && py < inputHeight) {
          output.setRGB(x,y,input.getRGB(px,py));
        } else {
          output.setRGB(x,y,0xFFFFFF);
        }
        sx += invScaleFactor;
      }
      sy += invScaleFactor;
    }
    return output;
  }
}
