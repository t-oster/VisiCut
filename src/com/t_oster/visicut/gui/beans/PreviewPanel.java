package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.model.Mapping;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.util.List;

/**
 * This class implements the Panel which provides the Preview
 * of the current LaserJob
 * 
 * @author thommy
 */
public class PreviewPanel extends FilesDropPanel
{

  protected File droppedFile = null;
  public static final String PROP_DROPPEDFILE = "droppedFile";

  /**
   * Get the value of droppedFile
   *
   * @return the value of droppedFile
   */
  public File getDroppedFile()
  {
    return droppedFile;
  }

  /**
   * Set the value of droppedFiles
   *
   * @param droppedFiles new value of droppedFiles
   */
  public void setDroppedFile(File droppedFile)
  {
    File oldDroppedFile = this.droppedFile;
    this.droppedFile = droppedFile;
    this.firePropertyChange(PROP_DROPPEDFILE, oldDroppedFile, droppedFile);
  }
  protected List<GraphicObject> graphicObjects = null;

  /**
   * Get the value of graphicObjects
   *
   * @return the value of graphicObjects
   */
  public List<GraphicObject> getGraphicObjects()
  {
    return graphicObjects;
  }

  /**
   * Set the value of graphicObjects
   *
   * @param graphicObjects new value of graphicObjects
   */
  public void setGraphicObjects(List<GraphicObject> graphicObjects)
  {
    this.graphicObjects = graphicObjects;
    this.repaint();
  }

  @Override
  public void filesDropped(List files)
  {
    if (files != null && !files.isEmpty())
    {
      Object o = files.get(0);
      File f = null;
      if (o instanceof File)
      {
        f = (File) o;
      }
      else if (o instanceof String)
      {
        f = new File((String) o);
      }
      if (f != null && f.exists())
      {
        this.setDroppedFile(f);
      }
    }
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (g instanceof Graphics2D)
    {
      Graphics2D gg = (Graphics2D) g;
      if (this.getGraphicObjects() != null)
      {
        if (this.getMappings() != null)
        {
          for (Mapping m : this.getMappings())
          {
            m.getB().renderPreview(gg, m.getA().getMatchingObjects(this.getGraphicObjects()));
          }
        }
        else
        {
          for (GraphicObject o : this.getGraphicObjects())
          {
            o.render(gg);
          }
        }
      }

    }
  }
  protected List<Mapping> mappings = null;

  /**
   * Get the value of mappings
   *
   * @return the value of mappings
   */
  public List<Mapping> getMappings()
  {
    return mappings;
  }

  /**
   * Set the value of mappings
   *
   * @param mappings new value of mappings
   */
  public void setMappings(List<Mapping> mappings)
  {
    this.mappings = mappings;
  }
}
