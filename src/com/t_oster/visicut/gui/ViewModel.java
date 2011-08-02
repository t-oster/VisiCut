package com.t_oster.visicut.gui;

import com.kitfox.svg.SVGElement;
import com.t_oster.visicut.model.AbstractModel;
import com.t_oster.liblasercut.platform.Util;
import java.awt.Point;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * This class represents the current state of the
 * View. It is a serializable Bean which allowes it
 * to be synchronzied with GUI Components through
 * PropertyChange Events and to be read and written
 * to Files for saving ViewState with Jobs
 * @author thommy
 */
public class ViewModel extends AbstractModel implements Serializable
{

  private boolean showGrid = true;
  private boolean showImage = true;
  private boolean showCut = true;
  private boolean showRaster = true;
  private boolean show3dRaster = true;
  private double zoomFactor = 1;
  private Point viewPoint = new Point(0, 0);
  private SVGElement selectedSVGElement = null;

  /**
   * @return the showGrid
   */
  public boolean isShowGrid()
  {
    return showGrid;
  }

  /**
   * @param showGrid the showGrid to set
   */
  public void setShowGrid(boolean showGrid)
  {
    if (showGrid != this.showGrid)
    {
      this.showGrid = showGrid;
      this.pcs.firePropertyChange("showGrid", !this.showGrid, this.showGrid);
    }
  }

  /**
   * @return the showImage
   */
  public boolean isShowImage()
  {
    return showImage;
  }

  /**
   * @param showImage the showImage to set
   */
  public void setShowImage(boolean showImage)
  {
    if (Util.differ(this.showImage, showImage))
    {
      this.showImage = showImage;
      this.pcs.firePropertyChange("showImage", !this.showImage, this.showImage);
    }
  }

  /**
   * @return the showCut
   */
  public boolean isShowCut()
  {
    return showCut;
  }

  /**
   * @param showCut the showCut to set
   */
  public void setShowCut(boolean showCut)
  {
    if (Util.differ(this.showCut, showCut))
    {
      this.showCut = showCut;
      this.pcs.firePropertyChange("showCut", !this.showCut, this.showCut);
    }

  }

  /**
   * @return the showRaster
   */
  public boolean isShowRaster()
  {
    return showRaster;
  }

  /**
   * @param showRaster the showRaster to set
   */
  public void setShowRaster(boolean showRaster)
  {
    if (Util.differ(this.showRaster, showRaster))
    {
      this.showRaster = showRaster;
      this.pcs.firePropertyChange("showRaster", !this.showRaster, this.showRaster);
    }
  }

  /**
   * @return the show3dRaster
   */
  public boolean isShow3dRaster()
  {
    return show3dRaster;
  }

  /**
   * @param show3dRaster the show3dRaster to set
   */
  public void setShow3dRaster(boolean show3dRaster)
  {
    if (Util.differ(this.show3dRaster, show3dRaster))
    {
      this.show3dRaster = show3dRaster;
      this.pcs.firePropertyChange("show3dRaster", !this.show3dRaster, this.show3dRaster);
    }
  }

  /**
   * @return the zoomFactor
   */
  public double getZoomFactor()
  {
    return zoomFactor;
  }

  /**
   * @param zoomFactor the zoomFactor to set
   */
  public void setZoomFactor(double zoomFactor)
  {
    if (Util.differ(this.zoomFactor, zoomFactor))
    {
      double old = this.zoomFactor;
      this.zoomFactor = zoomFactor;
      this.pcs.firePropertyChange("zoomFactor", old, this.zoomFactor);
    }
  }

  /**
   * @return the viewPoint
   */
  public Point getViewPoint()
  {
    return viewPoint;
  }

  /**
   * @param viewPoint the viewPoint to set
   */
  public void setViewPoint(Point viewPoint)
  {
    if (Util.differ(this.viewPoint, viewPoint))
    {
      Point old = this.viewPoint;
      this.viewPoint = viewPoint;
      this.pcs.firePropertyChange("viewPoint", old, this.viewPoint);
    }
  }

  /**
   * @return the selectedSVGElement
   */
  public SVGElement getSelectedSVGElement()
  {
    return selectedSVGElement;
  }

  /**
   * @param selectedSVGElement the selectedSVGElement to set
   */
  public void setSelectedSVGElement(SVGElement selectedSVGElement)
  {
    if (Util.differ(this.selectedSVGElement, selectedSVGElement))
    {
      SVGElement old = this.selectedSVGElement;
      this.selectedSVGElement = selectedSVGElement;
      this.pcs.firePropertyChange("selectedSVGElement", old, this.selectedSVGElement);
    }
  }
}
