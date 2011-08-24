/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut;

import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.SVGUniverse;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains the state and business logic of the 
 * Application
 * 
 * @author thommy
 */
public class VisicutModel
{

  protected SVGRoot SVGRootElement = null;
  public static final String PROP_SVGROOTELEMENT = "SVGRootElement";

  /**
   * Get the value of SVGRootElement
   *
   * @return the value of SVGRootElement
   */
  public SVGRoot getSVGRootElement()
  {
    return SVGRootElement;
  }

  /**
   * Set the value of SVGRootElement
   *
   * @param SVGRootElement new value of SVGRootElement
   */
  public void setSVGRootElement(SVGRoot SVGRootElement)
  {
    SVGRoot oldSVGRootElement = this.SVGRootElement;
    this.SVGRootElement = SVGRootElement;
    propertyChangeSupport.firePropertyChange(PROP_SVGROOTELEMENT, oldSVGRootElement, SVGRootElement);
  }
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
  protected File sourceFile = null;
  public static final String PROP_SOURCEFILE = "sourceFile";

  /**
   * Get the value of sourceFile
   *
   * @return the value of sourceFile
   */
  public File getSourceFile()
  {
    return sourceFile;
  }

  /**
   * Set the value of sourceFile
   *
   * @param sourceFile new value of sourceFile
   */
  private void setSourceFile(File sourceFile)
  {
    File oldSourceFile = this.sourceFile;
    this.sourceFile = sourceFile;
    propertyChangeSupport.firePropertyChange(PROP_SOURCEFILE, oldSourceFile, sourceFile);
  }

  /**
   * Add PropertyChangeListener.
   *
   * @param listener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Remove PropertyChangeListener.
   *
   * @param listener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }

  public void loadSVG(File f)
  {
    if (f != null)
    {
      SVGUniverse u = new SVGUniverse();
      try
      {
        URI svg = u.loadSVG(f.toURI().toURL());
        this.setSVGRootElement(u.getDiagram(svg).getRoot());
        this.setSourceFile(f);
      }
      catch (MalformedURLException ex)
      {
        Logger.getLogger(VisicutModel.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
