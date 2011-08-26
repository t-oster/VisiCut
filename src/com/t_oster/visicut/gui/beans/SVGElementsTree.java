package com.t_oster.visicut.gui.beans;

import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGElementException;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.xml.StyleAttribute;
import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.visicut.gui.beans.SVGFilter.FilterType;
import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author thommy
 */
public class SVGElementsTree extends JTree implements TreeModel, TreeSelectionListener
{

  public SVGElementsTree()
  {
    this.setModel(this);
    this.getSelectionModel().addTreeSelectionListener(this);
  }
  protected SVGRoot SVGRootElement = null;

  /**
   * Get the value of SVGRootElement
   *
   * @return the value of SVGRootElement
   */
  public SVGRoot getSVGRootElement()
  {
    return SVGRootElement;
  }
  
  private SVGFilter rootFilter = null;

  /**
   * Set the value of SVGRootElement
   *
   * @param SVGRootElement new value of SVGRootElement
   */
  public void setSVGRootElement(SVGRoot SVGRootElement)
  {
    if (SVGRootElement != null)
    {
      try
      {
        this.SVGRootElement = SVGRootElement;
        List<SVGElement> rootList = new LinkedList<SVGElement>();
        rootList.add(this.SVGRootElement);
        this.rootFilter = new SVGFilter(null, null, SVGFilter.expandChildren(rootList));
        this.valueForPathChanged(new TreePath(new Object[]
          {
            this.rootFilter
          }), this.rootFilter);
      }
      catch (SVGElementException ex)
      {
        Logger.getLogger(SVGElementsTree.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  public Object getRoot()
  {
    return this.rootFilter == null ? "root" : this.rootFilter;
  }

  public Object getChild(Object o, int i)
  {
    try
    {
      return getChildren(o).get(i);
    }
    catch (SVGElementException ex)
    {
      Logger.getLogger(SVGElementsTree.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }

  private List getChildren(Object o) throws SVGElementException
  {
    if (o != null && o == this.SVGRootElement)
    {
      List<SVGElement> rootlist = new LinkedList<SVGElement>();
      rootlist.add(this.SVGRootElement);
      try
      {
        List<Tuple<FilterType, List<SVGElement>>> result = new LinkedList<Tuple<FilterType, List<SVGElement>>>();
        for (FilterType t : SVGFilter.getOccuringFilterTypes(rootlist))
        {
          result.add(new Tuple<FilterType, List<SVGElement>>(t, rootlist));
        }
        return result;
      }
      catch (SVGElementException ex)
      {
        Logger.getLogger(SVGElementsTree.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    else if (o instanceof Tuple)
    {
      List<SVGElement> elements = (List<SVGElement>) ((Tuple) o).getB();
      FilterType t = (FilterType) ((Tuple) o).getA();
      try
      {
        List<SVGFilter> result = new LinkedList<SVGFilter>();
        for (Object attribute : SVGFilter.getOccuringFilterAttributes(t, elements))
        {
          result.add(new SVGFilter(t, attribute, elements));
        }
        return result;
      }
      catch (Exception ex)
      {
        Logger.getLogger(SVGElementsTree.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    else if (o instanceof SVGFilter)
    {
      SVGFilter f = (SVGFilter) o;
      List result = new LinkedList();
      result.addAll(f.getMatchingElements());
      for (FilterType t : SVGFilter.getOccuringFilterTypes(f.getMatchingElements()))
      {
        result.add(new Tuple<FilterType, List<SVGElement>>(t, f.getMatchingElements()));
      }
      return result;
    }
    return new LinkedList();
  }

  public int getChildCount(Object o)
  {
    try
    {
      return getChildren(o).size();
    }
    catch (SVGElementException ex)
    {
      Logger.getLogger(SVGElementsTree.class.getName()).log(Level.SEVERE, null, ex);
      return 0;
    }
  }

  public boolean isLeaf(Object o)
  {
    if (o instanceof SVGElement)
    {
      return !this.SVGRootElement.equals(o);
    }
    return false;
  }

  public void valueForPathChanged(TreePath tp, Object o)
  {
    for (TreeModelListener l : listeners)
    {
      l.treeStructureChanged(new TreeModelEvent(o, tp));
    }
    //throw new UnsupportedOperationException("Not supported yet.");
  }

  public int getIndexOfChild(Object o, Object o1)
  {
    return 3;//throw new UnsupportedOperationException("Not supported yet.");
  }
  private List<TreeModelListener> listeners = new LinkedList<TreeModelListener>();

  public void addTreeModelListener(TreeModelListener tl)
  {
    listeners.add(tl);
  }

  public void removeTreeModelListener(TreeModelListener tl)
  {
    listeners.remove(tl);
  }
  protected List<SVGElement> matchingSVGelements = null;
  public static final String PROP_MATCHINGSVGELEMENTS = "matchingSVGelements";

  /**
   * Get the value of matchingSVGelements
   *
   * @return the value of matchingSVGelements
   */
  public List<SVGElement> getMatchingSVGelements()
  {
    return matchingSVGelements;
  }

  /**
   * Set the value of matchingSVGelements
   *
   * @param matchingSVGelements new value of matchingSVGelements
   */
  public void setMatchingSVGelements(List<SVGElement> matchingSVGelements)
  {
    List<SVGElement> oldMatchingSVGelements = this.matchingSVGelements;
    this.matchingSVGelements = matchingSVGelements;
    firePropertyChange(PROP_MATCHINGSVGELEMENTS, oldMatchingSVGelements, matchingSVGelements);
  }

  public void valueChanged(TreeSelectionEvent tse)
  {
    if (tse.getNewLeadSelectionPath() != null && tse.getNewLeadSelectionPath().getPathCount() >= 1)
    {
      Object selected = tse.getNewLeadSelectionPath().getLastPathComponent();
      if (selected != null)
      {
        if (selected instanceof SVGElement)
        {
          List<SVGElement> matching = new LinkedList<SVGElement>();
          matching.add((SVGElement) selected);
          this.setMatchingSVGelements(matching);
        }
        else if (selected instanceof Tuple)
        {
          List<SVGElement> elements = (List<SVGElement>) ((Tuple) selected).getB();
          this.setMatchingSVGelements(elements);
        }
        else if (selected instanceof SVGFilter)
        {
          this.setMatchingSVGelements(((SVGFilter) selected).getMatchingElements());
        }
      }
    }
  }
}
