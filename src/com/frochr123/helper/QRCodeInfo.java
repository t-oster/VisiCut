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

/**
 * QRCodeInfo.java: Class to store QR code information for a PLF part
 * @author Christian
 */
public class QRCodeInfo
{
  // Store QR code related data
  private boolean previewQRCodeSource = false;
  private boolean webcamQRCodeSource = false;
  private boolean previewPositionQRStored = false;
  private double previewOriginalQRCodePositionPixelX = 0.0f;
  private double previewOriginalQRCodePositionPixelY = 0.0f;
  private String qrCodeSourceURL = "";

  // Constructor
  public QRCodeInfo()
  {
    previewQRCodeSource = false;
    webcamQRCodeSource = false;
    previewPositionQRStored = false;
    previewOriginalQRCodePositionPixelX = 0.0f;
    previewOriginalQRCodePositionPixelY = 0.0f;
    qrCodeSourceURL = "";
  }
  
  // Getters and setters for all variables
  public boolean isPreviewPositionQRStored()
  {
    return previewPositionQRStored;
  }

  public void setPreviewPositionQRStored(boolean previewPositionQRStored)
  {
    this.previewPositionQRStored = previewPositionQRStored;
  }

  public double getPreviewOriginalQRCodePositionPixelX()
  {
    return previewOriginalQRCodePositionPixelX;
  }

  public void setPreviewOriginalQRCodePositionPixelX(double previewOriginalQRCodePositionPixelX)
  {
    this.previewOriginalQRCodePositionPixelX = previewOriginalQRCodePositionPixelX;
  }

  public double getPreviewOriginalQRCodePositionPixelY()
  {
    return previewOriginalQRCodePositionPixelY;
  }

  public void setPreviewOriginalQRCodePositionPixelY(double previewOriginalQRCodePositionPixelY)
  {
    this.previewOriginalQRCodePositionPixelY = previewOriginalQRCodePositionPixelY;
  }

  public boolean isPreviewQRCodeSource()
  {
    return previewQRCodeSource;
  }

  public void setPreviewQRCodeSource(boolean previewQRCodeSource)
  {
    this.previewQRCodeSource = previewQRCodeSource;
  }

  public String getQRCodeSourceURL()
  {
    return qrCodeSourceURL;
  }

  public void setQRCodeSourceURL(String qrCodeSourceURL)
  {
    this.qrCodeSourceURL = qrCodeSourceURL;
  }

  public boolean isWebcamQRCodeSource()
  {
    return webcamQRCodeSource;
  }

  public void setWebcamQRCodeSource(boolean webcamQRCodeSource)
  {
    this.webcamQRCodeSource = webcamQRCodeSource;
  }
}
