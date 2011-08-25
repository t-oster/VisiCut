package com.t_oster.visicut.gui.beans;

import com.kitfox.svg.Path;
import com.kitfox.svg.RenderableElement;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (this.svgElements != null)
    {
      for (SVGElement e : this.svgElements)
      {
        if (e instanceof RenderableElement)
        {
          try
          {
            ((RenderableElement) e).render((Graphics2D) g);
          }
          catch (SVGException ex)
          {
            Logger.getLogger(MatchingPartsPanel.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    }
    g.setColor(Color.yellow);
    g.drawLine(0, 0, 100, 100);
  }
}
