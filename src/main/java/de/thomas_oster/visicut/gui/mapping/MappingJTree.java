/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package de.thomas_oster.visicut.gui.mapping;

import de.thomas_oster.visicut.misc.Helper;
import de.thomas_oster.visicut.model.graphicelements.GraphicObject;
import de.thomas_oster.visicut.model.graphicelements.GraphicSet;
import de.thomas_oster.visicut.model.mapping.FilterSet;
import de.thomas_oster.visicut.model.mapping.Mapping;
import de.thomas_oster.visicut.model.mapping.MappingFilter;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class MappingJTree extends JTree implements TreeModel, TreeSelectionListener
{

  private Color mappedBackgroundColor = new Color(253, 248, 183);
  private String bgHtml = Helper.toHtmlRGB(mappedBackgroundColor);
  protected FilterSet selectedFilterSet = null;
  public static final String PROP_SELECTEDFILTERSET = "selectedFilterSet";

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
   * and fires a property change event
   * this does NOT alter the real selected value
   * of the JTree, except if the value is null,
   * then the selection is cleared
   *
   * @param selectedFilterSet new value of selectedFilterSet
   */
  public void setSelectedFilterSet(FilterSet selectedFilterSet)
  {
    FilterSet oldSelectedFilterSet = this.selectedFilterSet;
    this.selectedFilterSet = selectedFilterSet;
    firePropertyChange(PROP_SELECTEDFILTERSET, oldSelectedFilterSet, selectedFilterSet);
  }

  /**
   * Selects the path which leads to the given filter set,
   * or clears the selection if the path is not in the tree
   */
  public void representFilterSet(FilterSet fs)
  {
    if (fs == null)
    {
      this.setSelectionPath(new TreePath(new Object[]{dummyRoot, EVERYTHING_ELSE}));
    }
    else if (fs.size() == 0)
    {
      this.setSelectionPath(new TreePath(new Object[]{dummyRoot, root}));
    }
    else
    {
      List<Object> path = new LinkedList<Object>();
      path.add(dummyRoot);
      path.add(root);
      Object current = root;
      for (MappingFilter f : fs)
      {
        boolean attributeFound = false;
        for (Object c : getChildren(current))
        {
          if (c instanceof AttributeNode && ((AttributeNode) c).getAttribute().equals(f.getAttribute()))
          {
            current = c;
            path.add(c);
            attributeFound = true;
            break;
          }
        }
        if (attributeFound)
        {
          boolean valueFound = false;
          for(Object c : getChildren(current))
          {
            if (c instanceof FilterSetNode && ((FilterSetNode) c).getLast().equals(f))
            {
              //TODO: need to check inverted and compare???
              valueFound = true;
              current = c;
              path.add(c);
              break;
            }
          }
          if (!valueFound)
          {
            //tree does not contain selected value
            path = null;
            break;
          }
        }
        else
        {
          //tree doesnt contain attribute
          path = null;
          break;
        }
      }
      if (path != null)
      {
        this.setSelectionPath(new TreePath(path.toArray()));
      }
      else
      {
        this.clearSelection();
      }
    }
    // To make the UI more discoverable, ensure that all relevant selection options are expanded.
    // Expand "Everything" node
    this.expandPath(new TreePath(new Object[]{dummyRoot, root}));
    // Expand current selection
    this.expandPath(this.getSelectionPath());
  }
  
  public void valueChanged(TreeSelectionEvent evt)
  {
    // Expand current selection to make the UI more discoverable
    this.expandPath(this.getSelectionPath());

    // Update filter set from UI selection
    if (evt.getNewLeadSelectionPath() != null && evt.getNewLeadSelectionPath().getPathCount() >= 1)
    {
      Object selected = evt.getNewLeadSelectionPath().getLastPathComponent();
      if (selected == null || selected == EVERYTHING_ELSE)
      {
        this.setSelectedFilterSet(null);
      }
      else if (selected instanceof FilterSet)
      {
        this.setSelectedFilterSet(((FilterSet) selected).clone());
      }
    }
  }

  private class FilterSetNode extends FilterSet
  {

    private List<AttributeNode> children;

    public List<AttributeNode> getChildren()
    {
      if (children == null)
      {
        children = new LinkedList<AttributeNode>();
        if (MappingJTree.this.getGraphicObjects() != null)
        {
          List<GraphicObject> gos = this.getMatchingObjects(MappingJTree.this.getGraphicObjects());
          List<String> visitedAttributes = new LinkedList<String>();
          for (GraphicObject g : gos)
          {
            for (String attribute : g.getAttributes())
            {
              if (!visitedAttributes.contains(attribute))
              {
                visitedAttributes.add(attribute);
                AttributeNode node = new AttributeNode();
                node.addAll(this);
                node.setAttribute(attribute);
                if (!children.contains(node) && node.getChildren().size() > 1)
                {
                  children.add(node);
                }
              }
            }
          }
        }
      }
      return children;
    }
  }

  private class AttributeNode extends FilterSet
  {

    private String attribute;
    private List<FilterSetNode> children;

    public void setAttribute(String attribute)
    {
      this.attribute = attribute;
    }
    
    String getAttribute()
    {
      return attribute;
    }

    public List<FilterSet> getChildren()
    {
      if (children == null)
      {
        children = new LinkedList<FilterSetNode>();
        GraphicSet gos = this.getMatchingObjects(MappingJTree.this.getGraphicObjects());
        List<Object> visitedValues = new LinkedList<Object>();
        for (Object value : gos.getAttributeValues(attribute))
        {
          if (!visitedValues.contains(value))
          {
            visitedValues.add(value);
            List<MappingFilter> possibleFilters = new ArrayList<MappingFilter>(4);
            //create all 4 possible filters
            MappingFilter pf = new MappingFilter(attribute, value);
            possibleFilters.add(pf);
            pf = new MappingFilter(attribute, value);
            pf.setInverted(true);
            possibleFilters.add(pf);
            if (value instanceof Number)
            {
              pf = new MappingFilter(attribute, value);
              pf.setCompare(true);
              possibleFilters.add(pf);
              pf = new MappingFilter(attribute, value);
              pf.setInverted(true);
              pf.setCompare(true);
              possibleFilters.add(pf);
            }
            for (MappingFilter f : possibleFilters)
            {
              int newrest = f.getMatchingElements(gos).size();
              //Check if filter makes a difference
              if (newrest != 0 && newrest != gos.size())
              {
                FilterSetNode node = new FilterSetNode();
                node.addAll(this);
                node.add(f);
                if (!children.contains(node))
                {
                  children.add(node);
                }
              }
            }
          }
        }
      }
      return (List) children;
    }

    @Override
    public String toString()
    {
      return (this.isEmpty() ? GraphicSet.translateAttVal("WHERE") : GraphicSet.translateAttVal("AND")) + " " + GraphicSet.translateAttVal(attribute);
    }

    @Override
    public boolean equals(Object o)
    {
      if (o instanceof AttributeNode)
      {
        return ((AttributeNode) o).attribute.equals(attribute) && super.equals(o);
      }
      return super.equals(o);
    }

    @Override
    public int hashCode()
    {
      int hash = 7;
      hash = 89 * hash + (this.attribute != null ? this.attribute.hashCode() : 0);
      hash = 89 * hash + (this.children != null ? this.children.hashCode() : 0);
      return hash;
    }
  }

  public MappingJTree()
  {
    this.setModel(this);
    this.setRootVisible(false);
    this.setCellRenderer(new DefaultTreeCellRenderer()
    {

      @Override
      public Component getTreeCellRendererComponent(JTree jtree, Object o, boolean bln, boolean bln1, boolean bln2, int i, boolean bln3)
      {
        Component c = super.getTreeCellRendererComponent(jtree, o, bln, bln1, bln2, i, bln3);
        if (c instanceof JLabel)
        {
          JLabel l = (JLabel) c;
          if (o == EVERYTHING_ELSE)
          {
            l.setText(GraphicSet.translateAttVal("EVERYTHING_ELSE"));
          }
          else if (o instanceof FilterSet)
          {
            FilterSet fs = (FilterSet) o;
            if (fs instanceof FilterSetNode)
            {
              MappingFilter f = fs.peekLast();
              if (f != null)
              {
                if (f.getValue() instanceof Color)
                {
                  l.setText("<html><table><tr><td>" + (f.isInverted() ? GraphicSet.translateAttVal("IS NOT") : GraphicSet.translateAttVal("IS")) + "</td><td border=1 bgcolor=" + Helper.toHtmlRGB((Color) f.getValue()) + ">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table></html>");
                }
                else
                {
                  l.setText(f.getValueString());
                }
              }
            }
          }
        }
        return c;
      }
    });
    this.getSelectionModel().addTreeSelectionListener(this);
  }
  protected GraphicSet graphicObjects = null;

  /**
   * Get the value of graphicObjects
   *
   * @return the value of graphicObjects
   */
  public GraphicSet getGraphicObjects()
  {
    return graphicObjects;
  }

  /**
   * Set the value of graphicObjects
   *
   * @param graphicObjects new value of graphicObjects
   */
  public void setGraphicObjects(GraphicSet graphicObjects)
  {
    this.graphicObjects = graphicObjects;
    this.root = new FilterSetNode();
    this.roots[0] = root;
    this.valueForPathChanged(new TreePath(new Object[]
      {
        this.getRoot()
      }), this.getRoot());
  }
  
  private String dummyRoot = "DUMMY";
  private FilterSet root = new FilterSetNode();
  
  public Object getRoot()
  {
    return dummyRoot;
  }

  public Object getChild(Object o, int i)
  {
    return getChildren(o).get(i);
  }

  private String EVERYTHING_ELSE = "dummy";
  
  private Object[] roots = new Object[]{root, EVERYTHING_ELSE};
  
  private List getChildren(Object o)
  {
    if (o == dummyRoot)
    {
      return Arrays.asList(roots);
    }
    else if (o instanceof FilterSetNode)
    {
      return ((FilterSetNode) o).getChildren();
    }
    else if (o instanceof AttributeNode)
    {
      return ((AttributeNode) o).getChildren();
    }
    else
    {
      return new LinkedList();
    }
  }

  public int getChildCount(Object o)
  {
    return getChildren(o).size();

  }

  public boolean isLeaf(Object o)
  {
    return this.getChildCount(o) == 0;
  }

  public void valueForPathChanged(TreePath tp, Object o)
  {
    for (TreeModelListener l : listeners)
    {
      l.treeStructureChanged(new TreeModelEvent(o, tp));
    }
    //throw new UnsupportedOperationException("Not supported yet.");
  }

  public int getIndexOfChild(Object parent, Object child)
  {
    if (parent == null || child == null)
    {
      return -1;
    }
    int i = 0;
    for (Object o : this.getChildren(parent))
    {
      if (o.equals(child))
      {
        return i;
      }
      i++;
    }
    return -1;
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
  protected List<GraphicObject> matchingElements = null;
  public static final String PROP_MATCHINGELEMENTS = "matchingElements";
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
