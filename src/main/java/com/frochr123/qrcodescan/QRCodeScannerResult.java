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
package com.frochr123.qrcodescan;

import java.awt.geom.Point2D;

/**
 * QRCodeScannerResult.java: Data structure to represent the results of QR code scanning
 * Contains detected coordinates x and y and decoded text
 * @author Christian
 */
public class QRCodeScannerResult
{
  // Variables
  private Point2D point1;
  private Point2D point2;
  private Point2D point3;
  private Point2D center;
  private double width;
  private double angleRad;
  private double angleDeg;
  private String text;

  // Constructor, take 2D coordinates and decoded text of QR code
  // Mandatory: 3 * Corner position squares 
  // Coordinate 1: Near square in bottom left corner
  // Coordinate 2: Near square in top left corner
  // Coordinate 3: Near square in top right corner
  public QRCodeScannerResult(float x1, float y1, float x2, float y2, float x3, float y3, String text)
  {
    super();    
    this.text = text;
    
    // Just use doubles here to avoid many conversions to float in other places
    // Coordinates are always a bit shifted, shift a bit to fit better
    this.point1 = new Point2D.Double((double)x1 - 1.5, (double)y1 - 1.0);
    this.point2 = new Point2D.Double((double)x2 - 1.5, (double)y2 - 1.0);
    this.point3 = new Point2D.Double((double)x3 - 1.5, (double)y3 - 1.0);
    
    // Compute values and store them in public variables for better access
    computeWidth();
    computeAngle();
    computeCenter();
  }

  // Compute width in pixel from values
  private void computeWidth()
  {
    // This value was found by multiple measurements
    // Detected coordinates are rather inside of the QR code, not at its border
    // Therefore, the actual size of the QR code is bigger by this factor
    double sizeFactor = 1.362f;

    // QR code might be rotated, compute width as length of 2D vectors
    // Vector length from coordinate one to two
    // should be nearly equal to
    // vector length from coordinate two to three
    double vectorLength21 = point2.distance(point1);
    double vectorLength23 = point2.distance(point3);

    // Compute result as average length of 2D vectors multiplied with size factor
    width = (vectorLength21 + vectorLength23) * 0.5 * sizeFactor;
  }

  // Compute angle from values
  private void computeAngle()
  {
    // Use atan2 function to compute the orientation, has more information than acos
    // Angle between vector from coordinate two to one and x-axis
    // should be nearly equal to
    // angle between vector from coordinate three to two and y-axis
    double vectorRad21 = Math.atan2(getY1() - getY2(), getX1() - getX2());
    double vectorRad32 = Math.atan2(getY2() - getY3(), getX2() - getX3()) - Math.PI * 0.5;

    // Normalization of values
    if (vectorRad21 < 0)
    {
      vectorRad21 = vectorRad21 + 2 * Math.PI;
    }

    if (vectorRad32 < 0)
    {
      vectorRad32 = vectorRad32 + 2 * Math.PI;
    }
    
    // Now this is in rad
    double resultAngle = vectorRad21;
    
    // Compute average for similar values
    // Avoid wrong results for one angle of 2*PI and other one of angle 0
    // 0.25 equals roughly 15 degree
    if (Math.abs(vectorRad21 - vectorRad32) <= 0.25)
    {
       resultAngle = resultAngle + vectorRad32;
       resultAngle = resultAngle * 0.5;
    }

    // Change result to match default angle of QR codes
    resultAngle = resultAngle - Math.PI * 0.5;
    
    // Normalization of values
    if (resultAngle < 0)
    {
      resultAngle = resultAngle + 2 * Math.PI;
    }
    
    // Now there is normalized clockwise rotation, compute counter clockwise rotation
    resultAngle = 2 * Math.PI - resultAngle;

    // Computed values for angles
    angleRad = resultAngle;
    angleDeg = resultAngle * 180.0 / Math.PI;
  }

  // Compute center point of QR code
  private void computeCenter()
  {
    // QR code might be rotated, compute as average of three combinations of vectors
    // 1) Point 2 + Half vector 2 -> 3 + Half vector 2 -> 1
    // 2) Point 1 + Half vector 1 -> 3
    Point2D tmpCenter1 = new Point2D.Double(getX2() + 0.5 * (getX3() - getX2()) + 0.5 * (getX1() - getX2()), getY2() + 0.5 * (getY3() - getY2()) + 0.5 * (getY1() - getY2()));
    Point2D tmpCenter2 = new Point2D.Double(getX1() + 0.5 * (getX3() - getX1()), getY1() + 0.5 * (getY3() - getY1()));

    center = new Point2D.Double(0.5 * (tmpCenter1.getX() + tmpCenter2.getX()), 0.5 * (tmpCenter1.getY() + tmpCenter2.getY()));
  }

  // Modified toString method
  @Override
  public String toString()
  {
    return "(X1: " + getX1() + ", Y1: " + getY1() + ") - (X2: " + getX2() + ", Y2: " + getY2() + ") - (X3: " + getX3() + ", Y3: " + getY3() + ") - (XCenter: " + getCenterX() + ", YCenter: " + getCenterY() + ") - Width: " + getWidth() + " - Rad: " + getAngleRad() + " - Degree: " + getAngleDeg() + " - Text: " + getText();
  }

  // Width of QR code measured in pixels
  public double getWidth()
  {
    return width;
  }

  // Values range from 0 to 2*pi, describe counter clockwise rotation compared to x-axis
  public double getAngleRad()
  {
    return angleRad;
  }

  // Values range from 0.0 to 360.0, describe counter clockwise rotation compared to x-axis
  public double getAngleDeg()
  {
    return angleDeg;
  }

  // Getter for text
  public String getText()
  {
    return text;
  }

  // Get x coordinate of center point
  public double getCenterX()
  {
    return center.getX();
  }
  
  // Get y coordinate of center point
  public double getCenterY()
  {
    return center.getY();
  }

  // Getters for single coordinates
  public double getX1()
  {
    return point1.getX();
  }

  public double getX2()
  {
    return point2.getX();
  }

  public double getX3()
  {
    return point3.getX();
  }

  public double getY1()
  {
    return point1.getY();
  }

  public double getY2()
  {
    return point2.getY();
  }

  public double getY3()
  {
    return point3.getY();
  }
}
