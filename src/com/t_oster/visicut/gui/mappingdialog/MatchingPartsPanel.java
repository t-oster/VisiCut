package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.model.Mapping;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.FilterSet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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

  protected GraphicSet graphicElements = null;

  /**
   * Get the value of graphicElements
   *
   * @return the value of graphicElements
   */
  public GraphicSet getGraphicElements()
  {
    return graphicElements;
  }

  /**
   * Set the value of graphicElements
   *
   * @param graphicElements new value of graphicElements
   */
  public void setGraphicElements(GraphicSet graphicElements)
  {
    this.graphicElements = graphicElements;
    this.repaint();
  }
  protected FilterSet selectedFilterSet = null;

  /**
   * Get the value of selectedFilterSet
   *
   * @return the value of selectedFilterSet
   */
  public FilterSet getSelectedFilterSet()
  {
    return selectedFilterSet;
  }

  /**
   * Set the value of selectedFilterSet
   *
   * @param selectedFilterSet new value of selectedFilterSet
   */
  public void setSelectedFilterSet(FilterSet selectedFilterSet)
  {
    this.selectedFilterSet = selectedFilterSet;
    this.repaint();
  }
  protected Mapping selectedMapping = null;

  /**
   * Get the value of selectedMapping
   *
   * @return the value of selectedMapping
   */
  public Mapping getSelectedMapping()
  {
    return selectedMapping;
  }

  /**
   * Set the value of selectedMapping
   *
   * @param selectedMapping new value of selectedMapping
   */
  public void setSelectedMapping(Mapping selectedMapping)
  {
    this.selectedMapping = selectedMapping;
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
    this.setBackground(this.material == null ? Color.white : this.material.getColor());
    this.repaint();
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D) g;
    if (this.graphicElements != null)
    {
      if (this.getSelectedMapping() != null)
      {
        this.getSelectedMapping().getB().renderPreview(
          gg, this.getSelectedMapping().getA().getMatchingObjects(this.getGraphicElements()));
      }
      else if (this.getSelectedFilterSet() != null)
      {
        for (GraphicObject e : this.getSelectedFilterSet().getMatchingObjects(graphicElements))
        {
          e.render(gg);
        }
      }
      else
      {
        for (GraphicObject e : this.graphicElements)
        {
          e.render(gg);
        }
      }

    }
  }
}
