package com.t_oster.visicut.model;

import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.MappingFilter;
import java.awt.Graphics2D;

/**
 * A Mapping represents a Set of Filters to Match
 * Elements of the InputImage and a Target which is
 * a CuttingProfile, specifying how the matched 
 * Elements should be rendered and cut on the LaserCutter
 * @author thommy
 */
public class Mapping extends Tuple<FilterSet,LaserProfile>
{
  public Mapping(FilterSet fs, LaserProfile pf)
  {
    super(fs,pf);
  }
  
  /**
   * Matches the Graphic Set against the filter
   * and renders the result with the Laser Profile.
   * This method is for Buffering
   * 
   * @param g
   * @param set 
   */
  public void renderPreview(Graphics2D g, GraphicSet set)
  {
    set = this.getA().getMatchingObjects(set);
    this.getB().renderPreview(g, set);
    //TODO: Render the Objects to a Buffered Image (transparent)
    //and render this image to the Graphics2D
  }
  
  @Override
  public String toString()
  {
    String fs = "";
    for (MappingFilter f:this.getA())
    {
      fs+="/"+f.toString();
    }
    return fs+"=>"+this.getB();
  }
}
