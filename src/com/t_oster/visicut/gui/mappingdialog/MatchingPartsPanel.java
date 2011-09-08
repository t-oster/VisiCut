package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
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

  protected boolean previewMode = false;
  public static final String PROP_PREVIEWMODE = "previewMode";

  /**
   * Get the value of previewMode
   *
   * @return the value of previewMode
   */
  public boolean isPreviewMode()
  {
    return previewMode;
  }

  /**
   * Set the value of previewMode
   *
   * @param previewMode new value of previewMode
   */
  public void setPreviewMode(boolean previewMode)
  {
    boolean oldPreviewMode = this.previewMode;
    this.previewMode = previewMode;
    firePropertyChange(PROP_PREVIEWMODE, oldPreviewMode, previewMode);
    this.repaint();
  }
  protected MappingSet mappings = new MappingSet();

  /**
   * Get the value of mappings
   *
   * @return the value of mappings
   */
  public MappingSet getMappings()
  {
    return mappings;
  }

  /**
   * Set the value of mappings
   *
   * @param mappings new value of mappings
   */
  public void setMappings(MappingSet mappings)
  {
    this.mappings = mappings;
  }
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
    if (selectedFilterSet != null)
    {
      selectedMapping = null;
    }
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
    if (selectedMapping != null)
    {
      selectedFilterSet = null;
    }
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
    //this.setBackground(this.material == null ? Color.white : this.material.getColor());
    this.repaint();
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    this.setBackground(previewMode ? material.getColor() : null);
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D) g;
    if (this.graphicElements != null)
    {
      if (this.getSelectedMapping() != null)
      {
        GraphicSet set = this.getSelectedMapping().getFilterSet().getMatchingObjects(this.graphicElements);
        set.setTransform(null);
        LaserProfile p = this.material.getLaserProfile(this.getSelectedMapping().getProfileName());
        if (this.previewMode)
        {
          if (p == null)
          {
            AffineTransform trans = gg.getTransform();
            gg.setTransform(new AffineTransform());
            gg.drawString("Profile not available for current Material", 10, this.getHeight() / 2);
            gg.setTransform(trans);
          }
          else
          {
            p.renderPreview(gg, set);
          }
        }
        else
        {
          if (p instanceof VectorProfile)
          {
            for (GraphicObject e : set)
            {
              if (e instanceof ShapeObject)
              {
                Shape s = ((ShapeObject) e).getShape();
                gg.setColor(Color.red);
                gg.draw(s);
              }
            }
          }
          else
          {
            for (GraphicObject e : set)
            {
              e.render(gg);
            }
          }
        }
      }
      else
      {
        if (this.getSelectedFilterSet() != null)
        {
          for (GraphicObject e : this.getSelectedFilterSet().getMatchingObjects(graphicElements))
          {
            e.render(gg);
          }
        }
        else
        {
          for (GraphicObject e : graphicElements)
          {
            e.render(gg);
          }
        }
      }

    }
  }
}
