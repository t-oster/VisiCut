/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.MappingFilter;
import java.awt.Color;
import java.awt.Component;
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
 * @author thommy
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
    if (selectedFilterSet == null)
    {
      this.clearSelection();
    }
  }

  public void valueChanged(TreeSelectionEvent evt)
  {
    if (evt.getNewLeadSelectionPath() != null && evt.getNewLeadSelectionPath().getPathCount() >= 1)
    {
      Object selected = evt.getNewLeadSelectionPath().getLastPathComponent();
      if (selected != null && selected instanceof FilterSet)
      {
        this.setSelectedFilterSet((FilterSet) selected);
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

    public List<FilterSet> getChildren()
    {
      if (children == null)
      {
        children = new LinkedList<FilterSetNode>();
        GraphicSet gos = this.getMatchingObjects(MappingJTree.this.getGraphicObjects());
        List<Object> visitedValues = new LinkedList<Object>();
        for (GraphicObject g : gos)
        {
          for (Object value : g.getAttributeValues(attribute))
          {
            if (!visitedValues.contains(value))
            {
              visitedValues.add(value);
              //GraphicSet restObjects = this.getA().getMatchingObjects(gos);
              MappingFilter f = new MappingFilter(attribute, value);
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
                  //Add inverted filter
                  f = new MappingFilter(attribute, value);
                  f.setInverted(true);
                  node = new FilterSetNode();
                  node.addAll(this);
                  node.add(f);
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
      return (this.isEmpty() ? "WHERE " : "AND ") + attribute;
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

  private boolean hasUnmappedChildren(FilterSet fs)
  {
    //Calculate if there are matching elements which are
    //not already mapped
    GraphicSet unmapped = fs.getMatchingObjects(graphicObjects);
    if (MappingJTree.this.mappings != null)
    {
      for (Mapping m : MappingJTree.this.mappings)
      {
        unmapped.removeAll(m.getFilterSet().getMatchingObjects(unmapped));
        if (unmapped.size() == 0)
        {
          return false;
        }
      }
    }
    return true;
  }

  public MappingJTree()
  {
    this.setModel(this);
    this.setCellRenderer(new DefaultTreeCellRenderer()
    {

      @Override
      public Component getTreeCellRendererComponent(JTree jtree, Object o, boolean bln, boolean bln1, boolean bln2, int i, boolean bln3)
      {
        Component c = super.getTreeCellRendererComponent(jtree, o, bln, bln1, bln2, i, bln3);
        if (c instanceof JLabel && o instanceof FilterSet)
        {
          JLabel l = (JLabel) c;
          FilterSet fs = (FilterSet) o;
          boolean hasUnmappedChildren = MappingJTree.this.hasUnmappedChildren(fs);
          if (!hasUnmappedChildren)
          {//If all chilren are already mapped, change bgcolor
            l.setText("<html><table bgcolor=" + bgHtml + "><tr><td>" + l.getText() + "</td></tr></table></html>");
          }
          if (fs instanceof FilterSetNode)
          {
            MappingFilter f = fs.peekLast();
            if (f != null && f.getValue() instanceof Color)
            {
              l.setText("<html><table" + (!hasUnmappedChildren ? " bgcolor=" + bgHtml : "") + "><tr><td>" + (f.isInverted() ? "IS NOT" : "IS") + "</td><td border=1 bgcolor=" + Helper.toHtmlRGB((Color) f.getValue()) + ">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table></html>");
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
    this.valueForPathChanged(new TreePath(new Object[]
      {
        this.getRoot()
      }), this.getRoot());
  }
  private FilterSet root = new FilterSetNode();

  public Object getRoot()
  {
    return root;
  }

  public Object getChild(Object o, int i)
  {
    return getChildren(o).get(i);
  }

  private List getChildren(Object o)
  {
    if (o instanceof FilterSetNode)
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
