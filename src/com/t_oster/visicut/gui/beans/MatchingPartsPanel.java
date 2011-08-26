package com.t_oster.visicut.gui.beans;

import com.kitfox.svg.Path;
import com.kitfox.svg.RenderableElement;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.ShapeElement;
import com.t_oster.visicut.model.LineProfile;
import com.t_oster.visicut.model.MaterialProfile;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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

  protected List<SVGElement> svgElements = null;

  /**
   * Get the value of svgElements
   *
   * @return the value of svgElements
   */
  public List<SVGElement> getSvgElements()
  {
    return svgElements;
  }

  /**
   * Set the value of svgElements
   *
   * @param svgElements new value of svgElements
   */
  public void setSvgElements(List<SVGElement> svgElements)
  {
    this.svgElements = svgElements;
    this.repaint();
  }
  protected LineProfile lineType = null;

  /**
   * Get the value of lineType
   *
   * @return the value of lineType
   */
  public LineProfile getLineType()
  {
    return lineType;
  }

  /**
   * Set the value of lineType
   *
   * @param lineType new value of lineType
   */
  public void setLineType(LineProfile lineType)
  {
    this.lineType = lineType;
    this.setBackground(this.lineType == null || this.material == null ? Color.white : this.material.getColor());
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
    if (this.svgElements != null)
    {
      for (SVGElement e : this.svgElements)
      {
        if (e instanceof RenderableElement)
        {
          try
          {
            if (this.getLineType() == null)
            {
              ((RenderableElement) e).render(gg);
            }
            else
            {
              if (e instanceof ShapeElement)
              {
                gg.setColor(this.getLineType().getColor());
                Stroke s = new BasicStroke(this.getLineType().getWidth());
                gg.setStroke(s);
                Shape sh = ((ShapeElement) e).getShape();
                gg.draw(sh);
              }
            }
          }
          catch (SVGException ex)
          {
            Logger.getLogger(MatchingPartsPanel.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    }
  }
}
