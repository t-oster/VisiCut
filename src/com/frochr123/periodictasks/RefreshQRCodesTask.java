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
package com.frochr123.periodictasks;

import com.frochr123.helper.CachedFileDownloader;
import com.frochr123.helper.QRCodeInfo;
import com.frochr123.qrcodescan.QRCodeScanner;
import com.frochr123.qrcodescan.QRCodeScannerResult;
import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.managers.MappingManager;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.model.PlfFile;
import com.t_oster.visicut.model.PlfPart;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

/**
 * RefreshQRCodesTask.java: Thread to refresh the display of QR codes
 * @author Christian
 */
public class RefreshQRCodesTask implements Observer
{
  // Constant values
  public static final int DEFAULT_QRCODE_TIME = 50;
  public static final int DEFAULT_QRCODE_GRAPHICS_CACHE_ITEMS = 25;
  public static final int DEFAULT_QRCODE_DUPLICATE_DETECTION_PIXEL_DIFFERENCE = 15;

  // Variables
  // Store: Key: String filename, Value: Rectangle2D maximum original bounding box for all plf parts
  // LinkedHashMap stores items in their insertion order, which is important for removing old entries later on
  private LinkedHashMap<String, Rectangle2D> cacheMap = new LinkedHashMap<String, Rectangle2D>();
  private List<QRCodeScannerResult> qrResults = null;
  private QRCodeScanner qrCodeScanner = null;
  private boolean storePositions = false;
  
  // Constructor
  public RefreshQRCodesTask()
  {
    super();
    qrResults = null;
    storePositions = false;
    qrCodeScanner = new QRCodeScanner(null, true, true, getUpdateTimerMs());
    qrCodeScanner.addObserver(this);
  }
  
  public void startOrContinueScan()
  {
    qrCodeScanner.startOrContinueScan();
  }
  
  public void pauseScan()
  {
    qrCodeScanner.pauseScan();
  }
  
  public void interruptScan()
  {
    qrCodeScanner.interruptScan();
  }

  // Compute update timer, ensure valid data
  public static int getUpdateTimerMs()
  {
    if (VisicutModel.getInstance().getSelectedLaserDevice() != null)
    {
      if (VisicutModel.getInstance().getSelectedLaserDevice().getCameraTiming() > 0)
      {
        return VisicutModel.getInstance().getSelectedLaserDevice().getCameraTiming();
      }
    }

    return DEFAULT_QRCODE_TIME;
  }

  // Functions for store positions flag
  public synchronized boolean isStorePositions()
  {
    return storePositions;
  }

  public synchronized void setStorePositions(boolean storePositions)
  {
    this.storePositions = storePositions;
  }
  
  // Function to update QR codes
  public synchronized void updateQRCodes()
  {
    try
    {
      if (MainView.getInstance() != null && MappingManager.getInstance() != null && PreferencesManager.getInstance() != null && VisicutModel.getInstance() != null
          && PreferencesManager.getInstance().getPreferences() != null && VisicutModel.getInstance().getSelectedLaserDevice() != null
          && VisicutModel.getInstance().getSelectedLaserDevice().getLaserCutter() != null
          && VisicutModel.getInstance().getPlfFile() != null && VisicutModel.getInstance().getPropertyChangeSupport() != null
          && MainView.getInstance().isCameraActive() && MainView.getInstance().isPreviewPanelShowBackgroundImage()
          && !MainView.getInstance().isFabqrUploadDialogOpened() && !MainView.getInstance().isLaserJobInProgress())
      {
        // Prepare variables
        ArrayList<PlfPart> removePlfParts = new ArrayList<PlfPart>();
        AffineTransform qrCodePixelPosition2mm = null;
        double laserbedWidthMm = 0.0;
        double laserbedHeightMm = 0.0;
        int addedPartsCount = 0;
        boolean storePos = isStorePositions();

        // Reset store position variable
        if (storePos)
        {
          setStorePositions(false);
        }
        
        // Get calibration values for correct computation of position, is actually allowed to be null
        // Use copy constructor
        if (VisicutModel.getInstance().getSelectedLaserDevice().getCameraCalibration() != null)
        {
          qrCodePixelPosition2mm = new AffineTransform(VisicutModel.getInstance().getSelectedLaserDevice().getCameraCalibration());
        }

        // Find laser bed width and height
        laserbedWidthMm = VisicutModel.getInstance().getSelectedLaserDevice().getLaserCutter().getBedWidth();
        laserbedHeightMm = VisicutModel.getInstance().getSelectedLaserDevice().getLaserCutter().getBedHeight();
        
        // Iterate over all parts, detect parts which were added with QR codes, remove them later on
        for (PlfPart part : VisicutModel.getInstance().getPlfFile().getPartsCopy())
        {
          QRCodeInfo qrCodePartInfo = part.getQRCodeInfo();
          
          if (qrCodePartInfo != null)
          {
            // Check if this part was loaded by preview QR code scanning and is not position stored
            if (qrCodePartInfo.isPreviewQRCodeSource() && !qrCodePartInfo.isPreviewPositionQRStored())
            {
              // If positions from last iteration should be stored, store them and redraw them
              if (storePos)
              {
                qrCodePartInfo.setPreviewPositionQRStored(true);
                part.setIsMappingEnabled(true);
                VisicutModel.getInstance().firePartUpdated(part);
              }
              // Otherwise add old preview parts to remove list
              else
              {
                removePlfParts.add(part);
              }
            }
          }
        }

        // Iterate over list of QR codes, download files, load files, add them on correct position with correct angle from QR code
        for (QRCodeScannerResult qrCode : qrResults)
        {
          try
          {
            // Check valid pointer data
            if (qrCode == null || qrCode.getText() == null || qrCode.getText().isEmpty())
            {
              continue;
            }
            
            // Variables
            double centerPixelX = qrCode.getCenterX();
            double centerPixelY = qrCode.getCenterY();
            String qrCodeText = qrCode.getText();
            double centerMmX = 0.0;
            double centerMmY = 0.0;
            double rotationFinalRad = 0.0;
            File file = null;

            // Check if this QR code should be processed at all, might be a copy of a QR code which was just stored at same position
            // Iterate over all parts, detect parts which have same URL and similar position
            boolean qrCodeDuplicateAtSamePosition = false;

            for (PlfPart part : VisicutModel.getInstance().getPlfFile().getPartsCopy())
            {
              QRCodeInfo qrCodePartInfo = part.getQRCodeInfo();

              if (qrCodePartInfo != null)
              {
                // Check for duplicate, correct properties and same URL
                if (qrCodePartInfo.isPreviewQRCodeSource() && qrCodePartInfo.isPreviewPositionQRStored() && qrCodeText.equals(qrCodePartInfo.getQRCodeSourceURL()))
                {
                  // Check for similar positions, use distance of 2 cm for x and y center pixel coordinates
                  if (Math.abs(centerPixelX - qrCodePartInfo.getPreviewOriginalQRCodePositionPixelX()) < DEFAULT_QRCODE_DUPLICATE_DETECTION_PIXEL_DIFFERENCE && Math.abs(centerPixelY - qrCodePartInfo.getPreviewOriginalQRCodePositionPixelY()) < DEFAULT_QRCODE_DUPLICATE_DETECTION_PIXEL_DIFFERENCE)
                  {
                    // Duplicate detected, ignore everything
                    qrCodeDuplicateAtSamePosition = true;
                    break;
                  }
                }
              }
            }
            
            // If duplicate detected, silently continue with next QR code
            if (qrCodeDuplicateAtSamePosition)
            {
              continue;
            }

            // Compute position of qr code in pixel and millimeters
            // Use default coordinate 0.0 for millimeters if no information for correct computation is available (= uncalibrated camera)
            if (qrCodePixelPosition2mm != null)
            {
              Point2D sourcePoint = new Point2D.Double(centerPixelX, centerPixelY);
              Point2D destPoint = qrCodePixelPosition2mm.transform(sourcePoint, null);
              centerMmX = destPoint.getX();
              centerMmY = destPoint.getY();
            }
            
            // Check valid position, it might happen that the QR codes are detected
            // outside of the transformed and calibrated camera image, these QR codes
            // which do not have their center in the bounds of the preview should be ignored
            if (laserbedWidthMm != 0.0 && laserbedHeightMm != 0.0)
            {
              if (centerMmX <= 0.0 || centerMmX > laserbedWidthMm || centerMmY <= 0.0 || centerMmY > laserbedHeightMm)
              {
                continue;
              }
            }

            // Snap rotation to nearest multiple of 15
            int rotationSnapped = (int)(qrCode.getAngleDeg());
            int rotationMod = rotationSnapped % 15;
            
            if (rotationMod < 8)
            {
              rotationSnapped = rotationSnapped - rotationMod;
            }
            else
            {
              rotationSnapped = rotationSnapped + (15 - rotationMod);
            }

            // Ensure valid values
            if (rotationSnapped < 0 || rotationSnapped >= 360)
            {
              rotationSnapped = 0;
            }

            // Convert to radian
            rotationFinalRad = Math.toRadians((double)(rotationSnapped));

            // QR code seems to be fine, begin downloading
            try
            {
              SimpleEntry<String, SimpleEntry<String, File>> fileDownload = CachedFileDownloader.downloadFile(qrCodeText, CachedFileDownloader.CACHE_DOWNLOADER_DEFAULT_FILETYPES);
              
              if (fileDownload != null && fileDownload.getValue() != null && fileDownload.getValue().getValue() != null)
              {
                file = (File)(fileDownload.getValue().getValue());
              }
            }
            catch (Exception e)
            {
              // On exception continue with next QR code silently
              continue;
            }

            // If no file found, continue with next QR code silently
            if (file == null)
            {
              continue;
            }

            // File download done, begin loading of file
            try
            {
              // Prepare variables
              Rectangle2D maxOriginalBoundingBox = null;
              LinkedList<PlfPart> addPlfParts = new LinkedList<PlfPart>();
              
              // Check if filename is already stored in cache map, if yes return cache hit for max bound box
              if (cacheMap.containsKey(file.getName()))
              {
                if (cacheMap.get(file.getName()) != null)
                {
                  maxOriginalBoundingBox = (Rectangle2D)(cacheMap.get(file.getName()));
                }
              }
              else
              {
                // Resize cache if needed, LinkedHashMap keeps insertion order, oldest entries are first entries
                // Temporary store keys in list and remove afterwards to avoid read / write issues
                // Get one free space for new filename
                LinkedList<String> deleteKeys = new LinkedList<String>();
                for (Entry<String, Rectangle2D> cacheEntry : cacheMap.entrySet())
                {
                  if ((cacheMap.size() - deleteKeys.size()) >= DEFAULT_QRCODE_GRAPHICS_CACHE_ITEMS)
                  {
                    deleteKeys.add(cacheEntry.getKey());
                  }
                  else
                  {
                    break;
                  }
                }

                // Remove keys
                if (!deleteKeys.isEmpty())
                {
                  for (String key : deleteKeys)
                  {
                    // Remove entry in cache map
                    cacheMap.remove(key);
                  }
                }
              }

              // Load PLF file
              if (VisicutModel.PLFFilter.accept(file))
              {
                // Not interested in warnings, but null pointers are not supported here
                LinkedList<String> warnings = new LinkedList<String>();
                PlfFile loadPlfFile = VisicutModel.getInstance().loadPlfFile(MappingManager.getInstance(), file, warnings);

                if (loadPlfFile != null)
                {
                  if (!loadPlfFile.isEmpty())
                  {
                    for (PlfPart p : loadPlfFile)
                    {
                      if (p != null)
                      {
                        addPlfParts.add(p);
                      }
                    }
                  }
                }
              }
              // Load other file type
              else
              {
                // Not interested in warnings, but null pointers are not supported here
                LinkedList<String> warnings = new LinkedList<String>();
                PlfPart p = VisicutModel.getInstance().loadGraphicFile(file, warnings);

                if (PreferencesManager.getInstance().getPreferences().getDefaultMapping() != null)
                {
                  p.setMapping(MappingManager.getInstance().getItemByName(PreferencesManager.getInstance().getPreferences().getDefaultMapping()));
                }
                
                addPlfParts.add(p);
              }

              // Iterate over all plf parts and compute maximum original bounding box
              // Needed for transformations of multiple plf parts as in plf files
              if (maxOriginalBoundingBox == null)
              {
                for (PlfPart plfPart : addPlfParts)
                {
                  if (plfPart != null)
                  {
                    Rectangle2D partBox = plfPart.getBoundingBox();

                    if (maxOriginalBoundingBox == null)
                    {
                      maxOriginalBoundingBox = (Rectangle2D)(partBox.clone());
                    }
                    else
                    {
                      Rectangle2D.union(maxOriginalBoundingBox, partBox, maxOriginalBoundingBox);
                    }
                  }
                }

                // Store in cache
                if (maxOriginalBoundingBox != null)
                {
                  cacheMap.put(file.getName(), maxOriginalBoundingBox);
                }
              }

              // Add plf parts to current used plf file and preview and apply transformations
              if (addPlfParts != null && !addPlfParts.isEmpty() && maxOriginalBoundingBox != null)
              {
                double centerBoxX = maxOriginalBoundingBox.getCenterX();
                double centerBoxY = maxOriginalBoundingBox.getCenterY();
                
                for (PlfPart plfPart : addPlfParts)
                {
                  if (plfPart != null)
                  {
                    double deltaX = centerMmX - centerBoxX;
                    double deltaY = centerMmY - centerBoxY;

                    AffineTransform transformsQRAndOriginal = AffineTransform.getRotateInstance(((2 * Math.PI) - rotationFinalRad), centerMmX, centerMmY);
                    transformsQRAndOriginal.concatenate(AffineTransform.getTranslateInstance(deltaX, deltaY));

                    if (plfPart.getGraphicObjects() != null)
                    {
                      if (plfPart.getGraphicObjects().getTransform() != null)
                      {
                        transformsQRAndOriginal.concatenate(plfPart.getGraphicObjects().getTransform());
                        plfPart.getGraphicObjects().setTransform(transformsQRAndOriginal);
                      }
                      else if (plfPart.getGraphicObjects().getBasicTransform() != null)
                      {
                        transformsQRAndOriginal.concatenate(plfPart.getGraphicObjects().getBasicTransform());
                        plfPart.getGraphicObjects().setTransform(transformsQRAndOriginal);
                      }
                      else
                      {
                        plfPart.getGraphicObjects().setTransform(transformsQRAndOriginal);
                      }

                      // Try to manually fit object into laser bed (no check for scale)
                      Rectangle2D newBoundingBox = plfPart.getBoundingBox();
                      deltaX = 0.0;
                      deltaY = 0.0;

                      if (newBoundingBox.getMaxX() > laserbedWidthMm)
                      {
                        deltaX = deltaX - (newBoundingBox.getMaxX() - laserbedWidthMm);
                      }
                      
                      if (newBoundingBox.getMinX() < 0.0)
                      {
                        deltaX = deltaX - newBoundingBox.getMinX();
                      }
                      
                      if (newBoundingBox.getMaxY() > laserbedHeightMm)
                      {
                        deltaY = deltaY - (newBoundingBox.getMaxY() - laserbedHeightMm);
                      }
                      
                      if (newBoundingBox.getMinY() < 0.0)
                      {
                        deltaY = deltaY - newBoundingBox.getMinY();
                      }

                      AffineTransform transformFitLaserBed = AffineTransform.getTranslateInstance(deltaX, deltaY);

                      if (plfPart.getGraphicObjects().getTransform() != null)
                      {
                        transformFitLaserBed.concatenate(plfPart.getGraphicObjects().getTransform());
                        plfPart.getGraphicObjects().setTransform(transformFitLaserBed);
                      }
                    }

                    // Increase counter
                    addedPartsCount++;

                    // Set QRCodeInfo to PlfPart
                    QRCodeInfo qrCodeInfo = new QRCodeInfo();
                    qrCodeInfo.setQRCodeSourceURL(qrCodeText);
                    qrCodeInfo.setPreviewQRCodeSource(true);
                    qrCodeInfo.setPreviewPositionQRStored(false);
                    qrCodeInfo.setPreviewOriginalQRCodePositionPixelX(centerPixelX);
                    qrCodeInfo.setPreviewOriginalQRCodePositionPixelY(centerPixelY);
                    plfPart.setQRCodeInfo(qrCodeInfo);

                    // Disable mapping, might cause too long loading times for preview, enable on store positions
                    plfPart.setIsMappingEnabled(false);

                    // Add part to current plf file
                    VisicutModel.getInstance().getPlfFile().add(plfPart);
                    VisicutModel.getInstance().getPropertyChangeSupport().firePropertyChange(VisicutModel.PROP_PLF_PART_ADDED, null, plfPart);
                  }
                }
              }
            }
            catch (Exception e)
            {
              // On exception continue with next QR code silently
              continue;
            }
          }
          catch (Exception e)
          {
            if (MainView.getInstance().getDialog() != null)
            {
              MainView.getInstance().getDialog().showWarningMessage("Exception in deep QR code detection task: " + e.getMessage());
            }
          }
        }
        
        // Iterate over all old parts which need to be deleted
        for (PlfPart part : removePlfParts)
        {
          VisicutModel.getInstance().removePlfPart(part);
        }

        // Check added counts to control some GUI behaviour
        // No new ones added, unlock GUI because QR code editing ended
        if (addedPartsCount == 0)
        {
          if (MainView.getInstance().isEditGuiForQRCodesDisabled())
          {
            MainView.getInstance().disableEditGuiForQRCodes(false);
          }
          
          // Refresh buttons
          MainView.getInstance().refreshButtonStates(VisicutModel.PROP_PLF_PART_REMOVED);
        }
        // QR code parts added
        // Lock GUI because QR code editing started
        else if (addedPartsCount > 0)
        {
          if (!MainView.getInstance().isEditGuiForQRCodesDisabled())
          {
            MainView.getInstance().disableEditGuiForQRCodes(true);
            
            // Clear selected part and edit rectangle
            VisicutModel.getInstance().setSelectedPart(null, false);
          }
          
          // Refresh buttons
          MainView.getInstance().refreshButtonStates(VisicutModel.PROP_PLF_PART_ADDED);
        }
      }
    }
    catch (Exception e)
    {
      if (MainView.getInstance() != null && MainView.getInstance().getDialog() != null)
      {
        MainView.getInstance().getDialog().showWarningMessage("Exception in QR code detection task: " + e.getMessage());
      }
    }
    
    qrCodeScanner.startOrContinueScan();
  }

  // Response from QR code scanner
  public synchronized void update(Observable obj, Object arg)
  {
    // Check for valid input from QR code scanner, never null, empty list is allowed to happen
    if (obj != null && arg != null && obj.equals(qrCodeScanner) && arg instanceof List)
    {
      qrResults = (List<QRCodeScannerResult>)(arg);
      updateQRCodes();
    }
  }
}
