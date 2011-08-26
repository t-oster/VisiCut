package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.MappingFilter;
import java.util.LinkedList;
import java.util.List;
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
public class MappingJTree extends JTree implements TreeModel, TreeSelectionListener
{

  private class AttributeNode extends Tuple<FilterSet, String>
  {

    public AttributeNode(FilterSet path, String attribute)
    {
      super(path, attribute);
    }

    public List<FilterSet> getChildren()
    {
      List<FilterSet> result = new LinkedList<FilterSet>();
      List<GraphicObject> gos = this.getA().getMatchingObjects(MappingJTree.this.getGraphicObjects());
      for (GraphicObject g : gos)
      {
        for (Object value : g.getAttributeValues(this.getB()))
        {
          MappingFilter f = new MappingFilter(this.getB(), value);
          //Check if filter already present
          if (!this.getA().contains(f))
          {
            FilterSet node = new FilterSet();
            node.addAll(this.getA());
            node.add(f);
            if (!result.contains(node))
            {
              result.add(node);
            }
          }
        }
      }
      return result;
    }

    @Override
    public String toString()
    {
      return this.getB();
    }
  }

  public MappingJTree()
  {
    this.setModel(this);
    this.getSelectionModel().addTreeSelectionListener(this);
  }
  protected List<GraphicObject> graphicObjects = null;

  /**
   * Get the value of graphicObjects
   *
   * @return the value of graphicObjects
   */
  public List<GraphicObject> getGraphicObjects()
  {
    return graphicObjects;
  }

  /**
   * Set the value of graphicObjects
   *
   * @param graphicObjects new value of graphicObjects
   */
  public void setGraphicObjects(List<GraphicObject> graphicObjects)
  {
    this.graphicObjects = graphicObjects;
    this.valueForPathChanged(new TreePath(new Object[]
      {
        this.getRoot()
      }), this.getRoot());
  }

  public Object getRoot()
  {
    return this.graphicObjects == null ? "root" : new FilterSet();
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
      List<AttributeNode> result = new LinkedList<AttributeNode>();
      List<GraphicObject> gos = fs.getMatchingObjects(MappingJTree.this.getGraphicObjects());
      for (GraphicObject g : gos)
      {
        for (String attribute : g.getAttributes())
        {
          AttributeNode node = new AttributeNode(fs, attribute);
          if (!result.contains(node) && node.getChildren().size() > 0)
          {
            result.add(node);
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
  protected List<GraphicObject> matchingElements = null;
  public static final String PROP_MATCHINGELEMENTS = "matchingElements";

  /**
   * Get the value of matchingSVGelements
   *
   * @return the value of matchingSVGelements
   */
  public List<GraphicObject> getMatchingElements()
  {
    return matchingElements;
  }

  /**
   * Set the value of matchingSVGelements
   *
   * @param matchingSVGelements new value of matchingSVGelements
   */
  public void setMatchingElements(List<GraphicObject> matchingElements)
  {
    List<GraphicObject> oldMatchingSVGelements = this.matchingElements;
    this.matchingElements = matchingElements;
    firePropertyChange(PROP_MATCHINGELEMENTS, oldMatchingSVGelements, matchingElements);
  }

  public void valueChanged(TreeSelectionEvent tse)
  {
    if (tse.getNewLeadSelectionPath() != null && tse.getNewLeadSelectionPath().getPathCount() >= 1)
    {
      Object selected = tse.getNewLeadSelectionPath().getLastPathComponent();
      if (selected != null)
      {
        if (selected instanceof FilterSet)
        {
          this.setMatchingElements(((FilterSet) selected).getMatchingObjects(this.graphicObjects));
        }
        else if (selected instanceof AttributeNode)
        {
          FilterSet fs = ((AttributeNode) selected).getA();
          this.setMatchingElements(fs.getMatchingObjects(this.graphicObjects));
        }
      }
    }
  }
}
