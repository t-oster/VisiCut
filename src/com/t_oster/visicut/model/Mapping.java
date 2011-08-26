/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model;

import com.t_oster.visicut.model.mapping.MappingFilter;
import java.util.LinkedList;
import java.util.List;

/**
 * A Mapping represents a Set of Filters to Match
 * Elements of the InputImage and a Target which is
 * a CuttingProfile, specifying how the matched 
 * Elements should be rendered and cut on the LaserCutter
 * @author thommy
 */
public class Mapping
{
  private List<MappingFilter> filters = new LinkedList<MappingFilter>();
  private CuttingProfile target = new VectorProfile();
}
