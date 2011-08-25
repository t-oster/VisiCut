/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.beans;

import com.kitfox.svg.Group;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGElementException;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.xml.StyleAttribute;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class SVGFilter
{

  public enum FilterType
  {

    strokecolor,
    fillcolor,
    groupid,
    id,
  }

  /**
   * Returns the path from root to the given Element
   * @param e
   * @return 
   */
  private static List<SVGElement> getPath(SVGElement e)
  {
    List<SVGElement> result = new LinkedList<SVGElement>();
    while (e != null)
    {
      result.add(0, e);
      e = e.getParent();
    }
    return result;
  }

  private static List<SVGElement> getChildren(SVGElement e)
  {
    List<SVGElement> children = new LinkedList<SVGElement>();
    for (int i = 0; i < e.getNumChildren(); i++)
    {
      children.add(e.getChild(i));
    }
    return children;
  }

  /**
   * Returns a List of all Elements and Children of the elements
   * of the given Elements.
   * @param elements
   * @return 
   */
  private static List<SVGElement> expandChildren(List<SVGElement> elements)
  {
    List<SVGElement> result = new LinkedList<SVGElement>();
    for (SVGElement e : elements)
    {
      if (!result.contains(e))
      {
        result.add(e);
      }
      for (SVGElement child : expandChildren(getChildren(e)))
      {
        if (!result.contains(child))
        {
          result.add(child);
        }
      }

    }
    return result;
  }

  private static Object getOccuringFilterAttribute(FilterType type, SVGElement e) throws SVGElementException
  {
    switch (type)
    {
      case id:
      {
        return e.getId();
      }
      case groupid:
      {
        if (e instanceof Group)
        {
          return e.getId();
        }
        break;
      }
      case strokecolor:
      {
        if (e.hasAttribute("stroke", AnimationElement.AT_CSS))
        {
          StyleAttribute sa = e.getStyleAbsolute("stroke");
          return sa.getColorValue();
        }
        break;
      }
      case fillcolor:
      {
        if (e.hasAttribute("fill", AnimationElement.AT_CSS))
        {
          StyleAttribute sa = e.getStyleAbsolute("fill");
          return sa.getColorValue();
        }
        break;
      }
    }
    return null;
  }

  /**
   * Returns a List of valid Attributes for Filters of the 
   * given types where every Attribute matches at least one
   * Element in the given List
   * @param type
   * @param elements
   * @return 
   */
  public static List<Object> getOccuringFilterAttributes(FilterType type, List<SVGElement> elements) throws SVGElementException
  {
    List<Object> result = new LinkedList<Object>();
    for (SVGElement e : expandChildren(elements))
    {
      Object match = getOccuringFilterAttribute(type, e);
      if (match != null && !result.contains(match))
      {
        result.add(match);
      }
    }
    return result;
  }

  public static List<FilterType> getOccuringFilterTypes(List<SVGElement> elements) throws SVGElementException
  {
    List<FilterType> result = new LinkedList<FilterType>();
    for (FilterType t : FilterType.values())
    {
      if (SVGFilter.getOccuringFilterAttributes(t, elements).size() > 0)
      {
        result.add(t);
      }
    }
    return result;
  }

  public final boolean matches(SVGElement e) throws SVGElementException
  {
    switch (type)
    {
      case id:
      {
        return e.getId().equals(attribute);
      }
      case fillcolor:
      {
        if (e.hasAttribute("fill", AnimationElement.AT_CSS))
        {
          StyleAttribute sa = e.getStyleAbsolute("fill");
          return sa.getColorValue().equals(attribute);
        }
        return false;
      }
      case strokecolor:
      {
        if (e.hasAttribute("stroke", AnimationElement.AT_CSS))
        {
          StyleAttribute sa = e.getStyleAbsolute("stroke");
          return sa.getColorValue().equals(attribute);
        }
        return false;
      }
      case groupid:
      {
        for (SVGElement node : getPath(e))
        {
          if (node instanceof Group && node.getId().equals(attribute))
          {
            return true;
          }
        }
        return false;
      }
    }
    return false;
  }
  private FilterType type;
  private Object attribute;
  private List<SVGElement> matchedElements;

  public SVGFilter(FilterType type, Object attribute, List<SVGElement> elements) throws SVGElementException
  {
    this.type = type;
    this.attribute = attribute;
    this.matchedElements = new LinkedList<SVGElement>();
    for (SVGElement e : expandChildren(elements))
    {
      if (this.matches(e))
      {
        this.matchedElements.add(e);
      }
    }
  }

  public List<SVGElement> getMatchingElements()
  {
    return matchedElements;
  }

  @Override
  public String toString()
  {
    return "" + attribute + "(" + matchedElements.size() + ")";
  }
}
