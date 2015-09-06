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

import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.VisicutModel;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.t_oster.visicut.managers.PreferencesManager;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.AbstractMap.SimpleEntry;
import javax.swing.Icon;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * QRCodeScannerThread.java: Thread for QRCodeScanner
 * @author Christian
 */
public class QRCodeScannerThread extends Thread
{
  // Variables
  private QRCodeScanner scanner;
  private boolean active;

  // Constructor, needs QRCodeScanner
  public QRCodeScannerThread(QRCodeScanner scanner)
  {
    super();
    this.scanner = scanner;
    active = false;
  }

  // Check if thread is active
  protected boolean isActive()
  {
    boolean scanningQRCodesEnabled = true;

    if (PreferencesManager.getInstance() != null && PreferencesManager.getInstance().getPreferences() != null)
    {
      scanningQRCodesEnabled = PreferencesManager.getInstance().getPreferences().isEnableQRCodes();
    }

    return active && scanningQRCodesEnabled;
  }
  
  // Setter for active variable
  protected void setActive(boolean active)
  {
    this.active = active;
  }

  // Run method
  @Override
  public void run()
  {
    while (true)
    {
      // Create a list as result
      List<QRCodeScannerResult> resList = new ArrayList<QRCodeScannerResult>();

      try
      {
        // Scanner must be set, otherwise results can not be returned
        // Exit thread
        if (scanner == null)
        {
          return;
        }

        // Sleep to give other threads computation time
        Thread.currentThread().sleep(scanner.getUpdateTimer());

        // Prepare variables
        boolean cameraActive = true;
        boolean previewActive = true;
        boolean guiLockedForQRCodeEdit = false;

        // Check if GUI needs to be unlocked
        if (MainView.getInstance() != null)
        {
          cameraActive = MainView.getInstance().isCameraActive();
          previewActive = MainView.getInstance().isPreviewPanelShowBackgroundImage();
          guiLockedForQRCodeEdit = MainView.getInstance().isEditGuiForQRCodesDisabled();
          
          if ((VisicutModel.getInstance() == null || !cameraActive || !previewActive) && guiLockedForQRCodeEdit)
          {
            MainView.getInstance().disableEditGuiForQRCodes(false);
          }
        }
        
        // Check if thread is active and QR codes enabled
        if (!isActive())
        {
          continue;
        }

        // Variable for current image to check
        BufferedImage img = null;

        // Preview panel mode
        if (scanner.isUsePreviewPanel())
        {
          if (VisicutModel.getInstance() != null && cameraActive && previewActive)
          {
            img = VisicutModel.getInstance().getBackgroundImage();
          }
        }
        // Label mode
        else if (scanner.getLabelPhoto() != null)
        {
          Icon icon = scanner.getLabelPhoto().getIcon();
                    
          // Convert icon to BufferedImage
          if (icon != null)
          {
            img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = (Graphics2D)(img.getGraphics());
            icon.paintIcon(null, graphics, 0, 0);
            graphics.dispose();
          }
        }
        else
        {
          // No image will be found with this settings, quit thread
          return;
        }

        // No image found, might be intentional, skip this run and try again
        if (img == null)
        {
          continue;
        }

        // Prepare empty list to store unfiltered result data
        LinkedList<Result> detectedQRCodes = new LinkedList<Result>();
        
        // Use ZXing library to get correct format of data
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(img);
        HybridBinarizer binarizer = new HybridBinarizer(source);
        BinaryBitmap fullBitmap = new BinaryBitmap(binarizer);
        
        // Set decode flags for QRCodeMultiReader
        Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        // Although ZXing supports reading multiple QR codes in an image natively,
        // this feature will not be used here! Results are often not good enough.
        // Use single QR code detection in parts of the image instead
        
        // Map to store binary bitmap parts, store their top left position in full image
        // to recompute to full pixel coordinates later
        LinkedList<SimpleEntry<BinaryBitmap, SimpleEntry<Integer, Integer>>> bitmapParts = new LinkedList<SimpleEntry<BinaryBitmap, SimpleEntry<Integer, Integer>>>();

        // Store data which is often used in variables, do not always compute them again
        int fullWidth = fullBitmap.getWidth();
        int fullHeight = fullBitmap.getHeight();

        // Temporary variables to store often used values
        int width = 0;
        int height = 0;
        int tempValue1 = 0;
        int tempValue2 = 0;

        // Use traditional section mode with different sizes
        // Full image
        width = fullWidth;
        height = fullHeight;
        tempValue1 = 0;
        tempValue2 = 0;

        bitmapParts.add(new SimpleEntry(fullBitmap, new SimpleEntry(0, 0)));
        
        // 5 parts, Quadrants + Center quadrant
        width = fullWidth/2;
        height = fullHeight/2;
        tempValue1 = 0;
        tempValue2 = 0;

        bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, 0, width, height), new SimpleEntry(0, 0)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, height, width, height), new SimpleEntry(0, height)));

        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width, 0, width, height), new SimpleEntry(width, 0)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width, height, width, height), new SimpleEntry(width, height)));
        
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width/2, height/2, width, height), new SimpleEntry(width/2, height/2)));
        
        // 9 parts
        width = fullWidth/3;
        height = fullHeight/3;
        tempValue1 = 0;
        tempValue2 = 0;

        bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, 0, width, height), new SimpleEntry(0, 0)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, height, width, height), new SimpleEntry(0, height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, 2*height, width, height), new SimpleEntry(0, 2*height)));

        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width, 0, width, height), new SimpleEntry(width, 0)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width, height, width, height), new SimpleEntry(width, height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width, 2*height, width, height), new SimpleEntry(width, 2*height)));
        
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(2*width, 0, width, height), new SimpleEntry(2*width, 0)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(2*width, height, width, height), new SimpleEntry(2*width, height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(2*width, 2*height, width, height), new SimpleEntry(2*width, 2*height)));
        
        // 16 parts
        width = fullWidth/4;
        height = fullHeight/4;
        tempValue1 = 0;
        tempValue2 = 0;

        bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, 0, width, height), new SimpleEntry(0, 0)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, height, width, height), new SimpleEntry(0, height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, 2*height, width, height), new SimpleEntry(0, 2*height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, 3*height, width, height), new SimpleEntry(0, 3*height)));

        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width, 0, width, height), new SimpleEntry(width, 0)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width, height, width, height), new SimpleEntry(width, height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width, 2*height, width, height), new SimpleEntry(width, 2*height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(width, 3*height, width, height), new SimpleEntry(width, 3*height)));
        
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(2*width, 0, width, height), new SimpleEntry(2*width, 0)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(2*width, height, width, height), new SimpleEntry(2*width, height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(2*width, 2*height, width, height), new SimpleEntry(2*width, 2*height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(2*width, 3*height, width, height), new SimpleEntry(2*width, 3*height)));
        
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(3*width, 0, width, height), new SimpleEntry(3*width, 0)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(3*width, height, width, height), new SimpleEntry(3*width, height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(3*width, 2*height, width, height), new SimpleEntry(3*width, 2*height)));
        bitmapParts.add(new SimpleEntry(fullBitmap.crop(3*width, 3*height, width, height), new SimpleEntry(3*width, 3*height)));
        
        // Slicing mode, cover areas with overlap, e.g. two slices [] with center slice () to cover
        // cut area in the center: [   (   ][   )   ]

        // Slice 2 horizontal, use for loop to change start coordinates of the slice
        // Only increase by half of the size of the slice
        width = fullWidth;
        height = fullHeight/2;
        tempValue1 = fullHeight/4;
        tempValue2 = 0;

        for (int i = 0; i < 3; ++i)
        {
          bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, tempValue2, width, height), new SimpleEntry(0, tempValue2)));
          tempValue2 = tempValue2 + tempValue1;
        }

        // Slice 3 horizontal, use for loop to change start coordinates of the slice
        // Only increase by half of the size of the slice
        width = fullWidth;
        height = fullHeight/3;
        tempValue1 = fullHeight/6;
        tempValue2 = 0;

        for (int i = 0; i < 5; ++i)
        {
          bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, tempValue2, width, height), new SimpleEntry(0, tempValue2)));
          tempValue2 = tempValue2 + tempValue1;
        }
        
        // Slice 4 horizontal, use for loop to change start coordinates of the slice
        // Only increase by half of the size of the slice
        width = fullWidth;
        height = fullHeight/4;
        tempValue1 = fullHeight/8;
        tempValue2 = 0;

        for (int i = 0; i < 7; ++i)
        {
          bitmapParts.add(new SimpleEntry(fullBitmap.crop(0, tempValue2, width, height), new SimpleEntry(0, tempValue2)));
          tempValue2 = tempValue2 + tempValue1;
        }
        
        // Slice 2 vertical, use for loop to change start coordinates of the slice
        // Only increase by half of the size of the slice
        width = fullWidth/2;
        height = fullHeight;
        tempValue1 = fullWidth/4;
        tempValue2 = 0;

        for (int i = 0; i < 3; ++i)
        {
          bitmapParts.add(new SimpleEntry(fullBitmap.crop(tempValue2, 0, width, height), new SimpleEntry(tempValue2, 0)));
          tempValue2 = tempValue2 + tempValue1;
        }

        // Slice 5 vertical, use for loop to change start coordinates of the slice
        // Only increase by half of the size of the slice
        width = fullWidth/5;
        height = fullHeight;
        tempValue1 = fullWidth/10;
        tempValue2 = 0;

        for (int i = 0; i < 9; i++)
        {
          bitmapParts.add(new SimpleEntry(fullBitmap.crop(tempValue2, 0, width, height), new SimpleEntry(tempValue2, 0)));
          tempValue2 = tempValue2 + tempValue1;
        }
        
        // Slice 8 vertical, use for loop to change start coordinates of the slice
        // Only increase by half of the size of the slice
        width = fullWidth/8;
        height = fullHeight;
        tempValue1 = fullWidth/16;
        tempValue2 = 0;

        for (int i = 0; i < 15; i++)
        {
          bitmapParts.add(new SimpleEntry(fullBitmap.crop(tempValue2, 0, width, height), new SimpleEntry(tempValue2, 0)));
          tempValue2 = tempValue2 + tempValue1;
        }
        
        // Use ZXing library to detect and decode all QR codes in binary bitmap parts
        // Start at index 1 because index 0 is dummy image for later
        for (int i = 0; i < bitmapParts.size(); ++i)
        {
          // Normal orientation
          try
          {
            if (bitmapParts.get(i) != null && bitmapParts.get(i).getKey() != null && bitmapParts.get(i).getValue() != null
                && bitmapParts.get(i).getValue().getKey() != null && bitmapParts.get(i).getValue().getValue() != null)
            {
              BinaryBitmap bitmap = (BinaryBitmap)(bitmapParts.get(i).getKey());
              Result temporaryResult = new QRCodeReader().decode(bitmap, hints);
              ResultPoint[] resPoints = temporaryResult.getResultPoints();
              float xGlobal = (float)(bitmapParts.get(i).getValue().getKey());
              float yGlobal = (float)(bitmapParts.get(i).getValue().getValue());
              
              // Only read valid positions, sometimes library reads
              // QR code positions with 0.0 coordinates for all values
              if (resPoints != null && resPoints.length >= 3 && resPoints[0] != null && resPoints[1] != null && resPoints[2] != null &&
                 (resPoints[0].getX() != 0.0f || resPoints[0].getY() != 0.0f || resPoints[1].getX() != 0.0f || resPoints[1].getY() != 0.0f
                 || resPoints[2].getX() != 0.0f || resPoints[2].getY() != 0.0f))
              {
                // Map local coordinates back to global coordinates, store them as last 3 points in array
                ResultPoint[] globalPoints = new ResultPoint[3];
                globalPoints[0] = new ResultPoint(resPoints[0].getX() + xGlobal, resPoints[0].getY() + yGlobal);
                globalPoints[1] = new ResultPoint(resPoints[1].getX() + xGlobal, resPoints[1].getY() + yGlobal);
                globalPoints[2] = new ResultPoint(resPoints[2].getX() + xGlobal, resPoints[2].getY() + yGlobal);
                temporaryResult.addResultPoints(globalPoints);

                // Add to detected QR codes list
                detectedQRCodes.add(temporaryResult);
              }
            }
          }
          catch (Exception ex)
          {
            // Could not successfully find any QR code in this piece
          }
        }

        // Rotation mode
        // Use ZXing library to detect and decode all QR codes in rotated binary bitmap parts
        // Manually compute rotated pieces of the image, slices 2 vertical
        // Default function rotateCounterClockwise45 does not solve that very well
        // Can be enabled and disabled by preference option
        if (PreferencesManager.getInstance() != null && PreferencesManager.getInstance().getPreferences() != null && !PreferencesManager.getInstance().getPreferences().isFastQRCodes())
        {
          // Get a bitmap which fits the size of rotated quadrants best
          // Cropping rotated coordinates may not exceed full coordinates, otherwise matrix is set to null, use minimum of both!
          int diagonalSizeSlice2Vertical = (int)(Math.sqrt(((fullWidth/2) * (fullWidth/2)) + (fullHeight * fullHeight)));
          int rotatedWidth = Math.min(diagonalSizeSlice2Vertical, fullWidth);
          int rotatedHeight = Math.min(diagonalSizeSlice2Vertical, fullHeight);
          BinaryBitmap rotationBitmap = fullBitmap.crop(0, 0, rotatedWidth, rotatedHeight);

          // Prepare transformations
          AffineTransform rotate45Clockwise = AffineTransform.getRotateInstance(Math.toRadians(360.0 - 45.0), (rotatedWidth/2), (rotatedHeight/2));
          AffineTransform rotate45CounterClockwise = AffineTransform.getRotateInstance(Math.toRadians(45.0), (rotatedWidth/2), (rotatedHeight/2));
          AffineTransform imageTransformMoveCenterRotate45Counter = AffineTransform.getTranslateInstance((rotatedWidth/2) - (fullWidth/4), 0);
          imageTransformMoveCenterRotate45Counter.preConcatenate(rotate45Clockwise);

          // Prepare colors
          int whiteRGB = 16777215;
          int grayRGBNegative = -8388607;
          int blackRGB = 0;

          // Entries for slice 2 vertical
          for (int i = 46; i <= 48; ++i)
          {
            // Rotated 45 degree counter clockwise orientation
            try
            {
              if (bitmapParts.get(i) != null && bitmapParts.get(i).getKey() != null && bitmapParts.get(i).getValue() != null
                  && bitmapParts.get(i).getValue().getKey() != null && bitmapParts.get(i).getValue().getValue() != null
                  && bitmapParts.get(i).getKey().getBlackMatrix() != null && rotationBitmap.getBlackMatrix() != null)
              {
                // Clear rotation bitmap, set to full white
                for (int x = 0; x < rotationBitmap.getBlackMatrix().getWidth(); ++x)
                {
                  for (int y = 0; y < rotationBitmap.getBlackMatrix().getHeight(); ++y)
                  {
                    rotationBitmap.getBlackMatrix().unset(x, y);
                  }
                }

                // Create buffered image from black matrix of current bitmap part
                BinaryBitmap bitmap = (BinaryBitmap)(bitmapParts.get(i).getKey());
                BufferedImage originalImg = new BufferedImage(bitmap.getWidth(), bitmap.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

                // Copy bitmap to buffered image
                for (int x = 0; x < originalImg.getWidth(); ++x)
                {
                  for (int y = 0; y < originalImg.getHeight(); ++y)
                  {
                    if (bitmap.getBlackMatrix().get(x, y))
                    {
                      originalImg.setRGB(x, y, blackRGB);
                    }
                    else
                    {
                      originalImg.setRGB(x, y, whiteRGB);
                    }
                  }
                }

                // Create Graphics2D from buffered image and apply transformations
                BufferedImage finalImage = new BufferedImage(rotationBitmap.getWidth(), rotationBitmap.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D graphics = (Graphics2D)(finalImage.getGraphics());
                graphics.setBackground(Color.WHITE);
                graphics.clearRect(0, 0, rotationBitmap.getWidth(), rotationBitmap.getHeight());
                graphics.drawImage(originalImg, imageTransformMoveCenterRotate45Counter, null);
                graphics.dispose();

                // Store rotated buffered image in binary bitmap
                for (int x = 0; x < finalImage.getWidth(); ++x)
                {
                  for (int y = 0; y < finalImage.getHeight(); ++y)
                  {
                    int colorRGB = finalImage.getRGB(x, y);

                    // Draw only black pixels, binary image is completely white anyways
                    if (colorRGB < grayRGBNegative)
                    {
                      rotationBitmap.getBlackMatrix().set(x, y);
                    }
                  }
                }

                // Try to decode
                Result temporaryResult = new QRCodeReader().decode(rotationBitmap, hints);
                ResultPoint[] resPoints = temporaryResult.getResultPoints();
                float xGlobal = (float)(bitmapParts.get(i).getValue().getKey());
                float yGlobal = (float)(bitmapParts.get(i).getValue().getValue());

                // Translate resulting coordinates after back rotation to match coordinate system of original image
                float xDelta = (float)(-(rotatedWidth - originalImg.getWidth()) / 2);
                float yDelta = (float)(-(rotatedHeight - originalImg.getHeight()) / 2);

                // Only read valid positions, sometimes library reads
                // QR code positions with 0.0 coordinates for all values
                if (resPoints != null && resPoints.length >= 3 && resPoints[0] != null && resPoints[1] != null && resPoints[2] != null &&
                   (resPoints[0].getX() != 0.0f || resPoints[0].getY() != 0.0f || resPoints[1].getX() != 0.0f || resPoints[1].getY() != 0.0f
                   || resPoints[2].getX() != 0.0f || resPoints[2].getY() != 0.0f))
                {
                  // Rotate coordinates back to 0 rotation
                  Point2D point0 = new Point2D.Double(resPoints[0].getX(), resPoints[0].getY());
                  Point2D point1 = new Point2D.Double(resPoints[1].getX(), resPoints[1].getY());
                  Point2D point2 = new Point2D.Double(resPoints[2].getX(), resPoints[2].getY());
                  rotate45CounterClockwise.transform(point0, point0);
                  rotate45CounterClockwise.transform(point1, point1);
                  rotate45CounterClockwise.transform(point2, point2);

                  // Map local coordinates back to global coordinates, store them as last 3 points in array
                  ResultPoint[] globalPoints = new ResultPoint[3];
                  globalPoints[0] = new ResultPoint((float)(point0.getX()) + xGlobal + xDelta, (float)(point0.getY()) + yGlobal + yDelta);
                  globalPoints[1] = new ResultPoint((float)(point1.getX()) + xGlobal + xDelta, (float)(point1.getY()) + yGlobal + yDelta);
                  globalPoints[2] = new ResultPoint((float)(point2.getX()) + xGlobal + xDelta, (float)(point2.getY()) + yGlobal + yDelta);
                  temporaryResult.addResultPoints(globalPoints);

                  // Add to detected QR codes list
                  detectedQRCodes.add(temporaryResult);
                }
              }
            }
            catch (Exception ex)
            {
              // Could not successfully find any QR code in this piece
            }
          }
        }

        // Add items to list
        for (Result qrCode : detectedQRCodes)
        {
          // Array of 6 or more points for each QR code
          // Coordinate 0: Local coordinate near square in bottom left corner
          // Coordinate 1: Local coordinate near square in top left corner
          // Coordinate 2: Local coordinate near square in top right corner
          // Optional amount: Alignment squares
          // Coordinate n-3: Global coordinate near square in bottom left corner
          // Coordinate n-2: Global coordinate near square in top left corner
          // Coordinate n-1:   Global coordinate near square in top right corner
          ResultPoint[] resPoints = qrCode.getResultPoints();
          float x1 = 0.0f;
          float y1 = 0.0f;
          float x2 = 0.0f;
          float y2 = 0.0f;
          float x3 = 0.0f;
          float y3 = 0.0f;

          if (resPoints.length >= 6)
          {
            // Mandatory coordinates
            x1 = resPoints[resPoints.length - 3].getX();
            y1 = resPoints[resPoints.length - 3].getY();
            x2 = resPoints[resPoints.length - 2].getX();
            y2 = resPoints[resPoints.length - 2].getY();
            x3 = resPoints[resPoints.length - 1].getX();
            y3 = resPoints[resPoints.length - 1].getY();
            
            // Check for duplicate detection, if code was already added, do not add it again
            // Use tolerance value of 15 pixel
            int tolerance = 15;
            boolean alreadyInList = false;
            
            for (QRCodeScannerResult tempQrRes : resList)
            {
              if (Math.abs(tempQrRes.getX1() - x1) <= tolerance && Math.abs(tempQrRes.getY1() - y1) <= tolerance)
              {
                alreadyInList = true;
                break;
              }
            }

            // QR code not in list yet, add it
            if (!alreadyInList)
            {
              QRCodeScannerResult qrRes = new QRCodeScannerResult(x1, y1, x2, y2, x3, y3, qrCode.getText());
              resList.add(qrRes);
            }
          }
        }

        // Pause thread after that
        // Return result list only if valid results were found OR scanner is set to return empty results
        if (!resList.isEmpty() || scanner.isRespondNoQRCode())
        {
          // Deactivate thread, wait for response of scanner
          setActive(false);

          // Give results back to scanner
          scanner.processResult(resList);
        }
      }
      catch (Exception e)
      {
        // Silent error message, should not happen
        e.printStackTrace();
      }
    }
  }
}
