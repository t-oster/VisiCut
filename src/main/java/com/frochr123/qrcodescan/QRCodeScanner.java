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

import java.util.List;
import java.util.Observable;
import javax.swing.JLabel;

/**
 * QRCodeScanner.java: Frequently scan QR codes from given label element
 * Callback result to calling thread with Observer pattern
 * @author Christian
 */
public class QRCodeScanner extends Observable
{
  // Variables
  private boolean usePreviewPanel = false;
  private boolean respondNoQRCode = true;
  private boolean threadStarted = false;
  private int updateTimer = 50;
  private QRCodeScannerThread scanThread = null;
  private JLabel labelPhoto = null;

  // Constructor, needs label element to scan and thread update timer
  public QRCodeScanner(JLabel labelPhoto, boolean usePreviewPanel, boolean respondNoQRCode, int updateTimer)
  {
    super();
    this.usePreviewPanel = usePreviewPanel;
    this.respondNoQRCode = respondNoQRCode;
    this.threadStarted = false;
    this.updateTimer = updateTimer;
    this.scanThread = new QRCodeScannerThread(this);

    this.labelPhoto = null;
    
    if (!usePreviewPanel)
    {
      this.labelPhoto = labelPhoto;
    }
  }

  public JLabel getLabelPhoto()
  {
    return labelPhoto;
  }

  public boolean isRespondNoQRCode()
  {
    return respondNoQRCode;
  }

  public int getUpdateTimer()
  {
    return updateTimer;
  }

  public boolean isUsePreviewPanel()
  {
    return usePreviewPanel;
  }

  // Start infinitely scanning
  public synchronized void startOrContinueScan()
  {
    if (scanThread != null)
    {
      scanThread.setActive(true);

      if (!threadStarted)
      {
        threadStarted = true;
        scanThread.start();
      }
    }
  }
  
  // Pause scanning
  public synchronized void pauseScan()
  {
    if (scanThread != null)
    {
      scanThread.setActive(false);
    }
  }

  // Interrupt thread for scanning, not reactivatble again after that
  // Should only be called before cleanup and QR code scanning no longer needed
  public synchronized void interruptScan()
  {
    if (scanThread != null)
    {
      scanThread.interrupt();
      scanThread = null;
    }
  }
  
  // Process result from scanner thread
  protected void processResult(List<QRCodeScannerResult> list)
  {
    // Notify observers
    // Observers need to set continue scan
    setChanged();
    notifyObservers(list);
  }
}
