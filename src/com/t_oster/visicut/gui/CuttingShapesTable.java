/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui;

import com.kitfox.svg.SVGElement;
import com.kitfox.svg.ShapeElement;
import com.t_oster.visicut.model.CuttingShape;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import com.t_oster.visicut.model.JepilogModel;
import com.t_oster.liblasercut.platform.Util;
import java.text.DecimalFormat;

/**
 *
 * @author oster
 */
public class CuttingShapesTable extends JTable
{

  private static final long serialVersionUID = 1L;
  private JepilogModel jpModel;

  public void setSelectedSVGElement(SVGElement e)
  {
    SVGElement old = this.getSelectedSVGElement();
    if (Util.differ(old, e))
    {
      if (e == null)
      {
        this.getSelectionModel().clearSelection();
        firePropertyChange("selectedSVGElement", old, null);
      }
      else
      {
        if (e instanceof ShapeElement)
        {
          int idx = 0;
          for (CuttingShape c : jpModel.getCuttingShapes())
          {
            if (c.getShapeElement().equals((ShapeElement) e))
            {
              this.getSelectionModel().setSelectionInterval(idx, idx);
              firePropertyChange("selectedSVGElement", old, e);
              return;
            }
            idx++;
          }
          this.getSelectionModel().clearSelection();
        }
      }

    }
  }

  public SVGElement getSelectedSVGElement()
  {
    int idx = this.getSelectionModel().getMinSelectionIndex();
    if (idx >= 0)
    {
      return this.jpModel.getCuttingShape(idx).getShapeElement();
    }
    else
    {
      return null;
    }
  }

  public void setJpModel(JepilogModel m)
  {
    this.jpModel = m;
    this.setModel(new CuttingShapesTableModel());
  }

  public JepilogModel getJpModel()
  {
    return this.jpModel;
  }

  public CuttingShapesTable()
  {
    this.jpModel = new JepilogModel();
    this.setModel(new CuttingShapesTableModel());
    this.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.getSelectionModel().addListSelectionListener(new ListSelectionListener()
    {

      public void valueChanged(ListSelectionEvent lse)
      {
        CuttingShapesTable.this.firePropertyChange("selectedSVGElement", null, CuttingShapesTable.this.getSelectedSVGElement());
      }
    });
  }

  class CuttingShapesTableModel extends DefaultTableModel implements PropertyChangeListener
  {

    private static final long serialVersionUID = 1L;
    private Class[] classes = new Class[]
    {
      String.class, String.class, String.class
    };
    private String[] title = new String[]
    {
      "Shape Name", "Cutting Depth", "Z-Position"
    };

    public CuttingShapesTableModel()
    {
      jpModel.addPropertyChangeListener(JepilogModel.PROPERTY_CUTTINGSHAPES, this);
      jpModel.addPropertyChangeListener(JepilogModel.PROPERTY_MATERIAL, this);
    }

    @Override
    public Class getColumnClass(int column)
    {
      return classes[column];
    }

    @Override
    public boolean isCellEditable(int row, int column)
    {
      return (column == 1 && getValueAt(row, column).toString().endsWith("mm")) || column == 2;
    }

    @Override
    public void setValueAt(Object value, int row, int column)
    {
      if (column == 1)
      {
        double val = Double.parseDouble(value.toString().replace(",", ".").split(" ")[0]);
        int oldfoc = jpModel.getCuttingShape(row).getProperty() == null
          ? 0
          : jpModel.getCuttingShape(row).getProperty().getFocus();
        jpModel.getCuttingShape(row).setProperty(jpModel.getMaterial().getCuttingProperty(val));
        jpModel.getCuttingShape(row).getProperty().setFocus(oldfoc);
      }
      else
      {
        if (column == 2)
        {
          int val = Integer.parseInt(value.toString());
          if (jpModel.getCuttingShape(row).getProperty() == null)
          {
            jpModel.getCuttingShape(row).setProperty(jpModel.getMaterial().getCuttingProperty().clone());
          }
          jpModel.getCuttingShape(row).getProperty().setFocus(val);
        }
        else
        {
          throw new IllegalArgumentException("Wrong column or datatype");
        }
      }
    }

    @Override
    public Object getValueAt(int row, int column)
    {
      if (column == 0)
      {
        String name = jpModel.getCuttingShape(row).getShapeElement().getId();
        return name == null || name.equals("") ? jpModel.getCuttingShape(row).getShapeElement().toString() : name;
      }
      else
      {
        if (column == 1)
        {
          DecimalFormat format = new DecimalFormat("#.### mm");
          if (jpModel.getMaterial() == null)
          {//Unable to calculate Depth
            return "no Material selected";
          }
          else
          {
            if (jpModel.getCuttingShape(row).getProperty() == null)
            {// Default depth is Materialdepth

              return format.format(jpModel.getMaterial().getHeight());
            }
          }
          return format.format(jpModel.getMaterial().getCuttingPropertyDepth(jpModel.getCuttingShapes()[row].getProperty()));
        }
        else
        {
          if (column == 2)
          {
            return jpModel.getCuttingShape(row).getProperty() == null
              ? 0
              : jpModel.getCuttingShape(row).getProperty().getFocus();
          }
          else
          {
            throw new IllegalArgumentException("Column doesn't exist");
          }
        }
      }
    }

    @Override
    public int getColumnCount()
    {
      return title.length;
    }

    @Override
    public String getColumnName(int column)
    {
      return title[column];
    }

    @Override
    public int getRowCount()
    {
      return jpModel.getCuttingShapes().length;
    }

    public void propertyChange(PropertyChangeEvent pce)
    {
      this.fireTableDataChanged();
    }
  }
}