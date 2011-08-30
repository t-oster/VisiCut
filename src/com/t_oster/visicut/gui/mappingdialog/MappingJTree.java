package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
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

  protected FilterSet selectedFilterSet = null;
  private static final String MAPPINGS = "Mappings";
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
    if (Util.differ(oldSelectedFilterSet, this.selectedFilterSet))
    {
      if (this.selectedFilterSet == null)
      {
        this.getSelectionModel().clearSelection();
      }
      else
      {
        //Generate Tree Path from selected FilterSet
        TreePath p = new TreePath(new Object[]
          {
            this.getRoot()
          });
        for (MappingFilter f : this.selectedFilterSet)
        {
          AttributeNode a = new AttributeNode((FilterSet) p.getLastPathComponent(), f.getAttribute());
          p = p.pathByAddingChild(a);
          FilterSet fs = new FilterSet();
          fs.addAll(a.getA());
          fs.add(f);
          p = p.pathByAddingChild(fs);
        }
        this.getSelectionModel().clearSelection();
        this.getSelectionModel().setSelectionPath(p);
      }
    }
  }
  protected Mapping selectedMapping = null;
  public static final String PROP_SELECTEDMAPPING = "selectedMapping";

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
    Mapping oldSelectedMapping = this.selectedMapping;
    this.selectedMapping = selectedMapping;
    firePropertyChange(PROP_SELECTEDMAPPING, oldSelectedMapping, selectedMapping);
    if (Util.differ(oldSelectedMapping, this.selectedMapping))
    {
      if (this.selectedMapping == null)
      {
        this.getSelectionModel().clearSelection();
      }
      else
      {
        this.valueForPathChanged(new TreePath(new Object[]
          {
            this.getRoot()
          }), this.getRoot());
        TreePath p = new TreePath(new Object[]
          {
            this.getRoot(), MAPPINGS, this.selectedMapping
          });
        this.getSelectionModel().clearSelection();
        this.getSelectionModel().setSelectionPath(p);
      }
    }
    this.repaint();
  }

  public void valueChanged(TreeSelectionEvent evt)
  {
    if (evt.getNewLeadSelectionPath() != null && evt.getNewLeadSelectionPath().getPathCount() >= 1)
    {
      Object selected = evt.getNewLeadSelectionPath().getLastPathComponent();
      if (selected != null)
      {
        if (selected instanceof FilterSet)
        {
          this.setSelectedMapping(null);
          this.setSelectedFilterSet((FilterSet) selected);
        }
        else if (selected instanceof Mapping)
        {
          this.setSelectedFilterSet(null);
          this.setSelectedMapping((Mapping) selected);
        }
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
      GraphicSet gos = this.getA().getMatchingObjects(MappingJTree.this.getUnmappedObjects());
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

  public GraphicSet getUnmappedObjects()
  {
    GraphicSet result = new GraphicSet();
    result.setTransform(this.graphicObjects.getTransform());
    if (this.graphicObjects != null)
    {
      result.addAll(this.graphicObjects);
      if (this.mappings != null)
      {
        for (Mapping m : this.getMappings())
        {
          for (GraphicObject o : m.getA().getMatchingObjects(result))
          {
            result.remove(o);
          }
        }
      }
    }
    return result;
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
    return this.graphicObjects == null ? "root" : root;
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
      List<GraphicObject> gos = fs.getMatchingObjects(MappingJTree.this.getUnmappedObjects());
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
      if (fs.size() == 0)//RootElement
      {
        result.add(MAPPINGS);
      }
      return result;
    }
    else if (o instanceof AttributeNode)
    {
      return ((AttributeNode) o).getChildren();
    }
    else if (MAPPINGS.equals(o))
    {
      return this.getMappings() == null ? new LinkedList() : this.getMappings();
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
