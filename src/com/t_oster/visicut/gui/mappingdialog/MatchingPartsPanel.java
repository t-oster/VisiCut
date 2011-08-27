package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import javax.swing.JPanel;

/**
 * This Class implements a JPanel which is able
 * to display Parts of an SVG file matching
 * certain criteria
 * 
 * @author thommy
 */
public class MatchingPartsPanel extends JPanel
{
  
  protected List<GraphicObject> graphicElements = null;

  /**
   * Get the value of graphicElements
   *
   * @return the value of graphicElements
   */
  public List<GraphicObject> getGraphicElements()
  {
    return graphicElements;
  }

  /**
   * Set the value of graphicElements
   *
   * @param graphicElements new value of graphicElements
   */
  public void setGraphicElements(List<GraphicObject> graphicElements)
  {
    this.graphicElements = graphicElements;
    this.repaint();
  }
  protected LaserProfile lineType = null;

  /**
   * Get the value of lineType
   *
   * @return the value of lineType
   */
  public LaserProfile getLineType()
  {
    return lineType;
  }

  /**
   * Set the value of lineType
   *
   * @param lineType new value of lineType
   */
  public void setLineType(LaserProfile lineType)
  {
    this.lineType = lineType;
    this.setBackground(this.lineType == null || this.material == null ? null : this.material.getColor());
    this.repaint();
  }
  protected MaterialProfile material = null;

  /**
   * Get the value of material
   *
   * @return the value of material
   */
  public MaterialProfile getMaterial()
  {
    return material;
  }

  /**
   * Set the value of material
   *
   * @param material new value of material
   */
  public void setMaterial(MaterialProfile material)
  {
    this.material = material;
    this.setBackground(this.lineType == null || this.material == null ? Color.white : this.material.getColor());
    this.repaint();
  }
  
  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D) g;
    if (this.graphicElements != null)
    {
      if (this.getLineType() == null)
      {
        for (GraphicObject e : this.graphicElements)
        {
          e.render(gg);
        }
      }
      else
      {
        this.getLineType().renderPreview(gg, graphicElements);
      }
    }
  }
}
