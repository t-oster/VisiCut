/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.liblasercut.platform.Util;
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
import org.apache.fop.fonts.Font;

/**
 *
 * @author thommy
 */
public class MappingJTree extends JTree implements TreeModel, TreeSelectionListener
{

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
   *
   * @param selectedFilterSet new value of selectedFilterSet
   */
  public void setSelectedFilterSet(FilterSet selectedFilterSet)
  {
    FilterSet oldSelectedFilterSet = this.selectedFilterSet;
    this.selectedFilterSet = selectedFilterSet;
    firePropertyChange(PROP_SELECTEDFILTERSET, oldSelectedFilterSet, selectedFilterSet);
    if (Util.differ(oldSelectedFilterSet, selectedFilterSet))
    {
      if (selectedFilterSet == null && this.getSelectionModel().getSelectionPath() != null)
      {
        this.getSelectionModel().clearSelection();
      }
      else if (selectedFilterSet != null && (this.getSelectionModel().getSelectionPath() == null || this.getSelectionModel().getSelectionPath().getLastPathComponent() != selectedFilterSet))
      {
        //Generate Tree Path from selected FilterSet
        TreePath p = new TreePath(new Object[]
          {
            this.getRoot()
          });
        for (MappingFilter f : selectedFilterSet)
        {
          AttributeNode a = new AttributeNode((FilterSet) p.getLastPathComponent(), f.getAttribute());
          p = p.pathByAddingChild(a);
          FilterSet fs = new FilterSet();
          fs.addAll(a.getA());
          fs.add(f);
          p = p.pathByAddingChild(fs);
        }
        this.getSelectionModel().setSelectionPath(p);
      }
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

  private class AttributeNode extends Tuple<FilterSet, String>
  {

    public AttributeNode(FilterSet path, String attribute)
    {
      super(path, attribute);
    }

    public List<FilterSet> getChildren()
    {
      List<FilterSet> result = new LinkedList<FilterSet>();
      GraphicSet gos = this.getA().getMatchingObjects(MappingJTree.this.getGraphicObjects());
      List<Object> visitedValues = new LinkedList<Object>();
      for (GraphicObject g : gos)
      {
        for (Object value : g.getAttributeValues(this.getB()))
        {
          if (!visitedValues.contains(value))
          {
            visitedValues.add(value);
            //GraphicSet restObjects = this.getA().getMatchingObjects(gos);
            MappingFilter f = new MappingFilter(this.getB(), value);
            int newrest = f.getMatchingElements(gos).size();
            //Check if filter makes a difference
            if (newrest != 0 && newrest != gos.size())
            {
              FilterSet node = new FilterSet();
              node.addAll(this.getA());
              node.add(f);
              if (!result.contains(node))
              {
                result.add(node);
                //Add inverted filter
                f = new MappingFilter(this.getB(), value);
                f.setInverted(true);
                node = new FilterSet();
                node.addAll(this.getA());
                node.add(f);
                result.add(node);
              }
            }
          }
        }
      }
      return result;
    }

    @Override
    public String toString()
    {
      return (this.getA().isEmpty() ? "WHERE " : "AND ") + this.getB();
    }
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
        if (o instanceof FilterSet)
        {
          GraphicSet unmapped = ((FilterSet) o).getMatchingObjects(graphicObjects);
          if (MappingJTree.this.mappings != null)
          {
            for (Mapping m : MappingJTree.this.mappings)
            {
              unmapped.removeAll(m.getFilterSet().getMatchingObjects(unmapped));
              if (unmapped.size() == 0)
              {
                break;
              }
            }
          }
          if (c instanceof JLabel)
          {
            JLabel l = (JLabel) c;
            if (unmapped.size() == 0)
            {
              l.setText("<html><table bgcolor=#fdfbc5><tr><td>" + l.getText() + "</td></tr></table></html>");
            }
            MappingFilter f = ((FilterSet) o).peekLast();
            if (f != null)
            {
              if (f.getValue() instanceof Color)
              {

                l.setText("<html><table" + (unmapped.size() == 0 ? " bgcolor=#fdfbc5" : "") + "><tr><td>" + (f.isInverted() ? "IS NOT" : "IS") + "</td><td border=1 bgcolor=" + Helper.toHtmlRGB((Color) f.getValue()) + ">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table></html>");
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
    this.valueForPathChanged(new TreePath(new Object[]
      {
        this.getRoot()
      }), this.getRoot());
  }
  private FilterSet root = new FilterSet();

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
    if (o instanceof FilterSet)
    {

      FilterSet fs = (FilterSet) o;
      List<Object> result = new LinkedList<Object>();
      if (this.getGraphicObjects() != null)
      {
        List<GraphicObject> gos = fs.getMatchingObjects(MappingJTree.this.getGraphicObjects());
        List<String> visitedAttributes = new LinkedList<String>();
        for (GraphicObject g : gos)
        {
          for (String attribute : g.getAttributes())
          {
            if (!visitedAttributes.contains(attribute))
            {
              visitedAttributes.add(attribute);
              AttributeNode node = new AttributeNode(fs, attribute);
              if (!result.contains(node) && node.getChildren().size() > 1)
              {
                result.add(node);
              }
            }
          }
        }
      }
      return result;
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
