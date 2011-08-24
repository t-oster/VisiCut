package com.t_oster.visicut.gui.beans;

import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGRoot;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author thommy
 */
public class SVGElementsTree extends JTree implements TreeModel
{

  public SVGElementsTree()
  {
    this.setModel(this);
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

  /**
   * Set the value of SVGRootElement
   *
   * @param SVGRootElement new value of SVGRootElement
   */
  public void setSVGRootElement(SVGRoot SVGRootElement)
  {
    if (SVGRootElement != null)
    {
      this.SVGRootElement = SVGRootElement;
      this.valueForPathChanged(new TreePath(new Object[]
        {
          SVGRootElement
        }), SVGRootElement);
    }
  }

  public Object getRoot()
  {
    return this.SVGRootElement == null ? "root" : this.SVGRootElement;
  }

  public Object getChild(Object o, int i)
  {
    if (o instanceof SVGElement)
    {
      return ((SVGElement) o).getChild(i);
    }
    else
    {
      return (o + "->Child " + i);
    }
  }

  public int getChildCount(Object o)
  {
    if (o instanceof SVGElement)
    {
      return ((SVGElement) o).getNumChildren();
    }
    else
    {
      return 5;
    }
  }

  public boolean isLeaf(Object o)
  {
    if (o instanceof SVGElement)
    {
      return ((SVGElement) o).getNumChildren() == 0;
    }
    else
    {
      return o.toString().length() > 100;
    }
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
}
