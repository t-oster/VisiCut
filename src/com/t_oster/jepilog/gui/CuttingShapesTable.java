/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.gui;


import com.kitfox.svg.SVGElement;
import com.kitfox.svg.ShapeElement;
import com.t_oster.jepilog.model.CuttingShape;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.JTable;
import java.util.Scanner;
import java.awt.Shape;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import com.t_oster.jepilog.model.JepilogModel;
/**
 *
 * @author oster
 */
public class CuttingShapesTable extends JTable{
    private static final long serialVersionUID = 1L;
    
    private JepilogModel jpModel;
    
    public CuttingShape getSelectedShape(){
        int idx = this.getSelectedRow();
        if (idx==-1){
            return null;
        }
        else{
            return jpModel.getCuttingShape(idx);
        }
    }
    
    public void setSelectedSVGElement(SVGElement e){
        if (e instanceof ShapeElement){
            for (CuttingShape c:jpModel.getCuttingShapes()){
                if (c.getShapeElement().equals((ShapeElement) e)){
                    this.setSelectedShape(c);
                    return;
                }
            }
        }
        this.setSelectedShape(null);
    }
    
    public SVGElement getSelectedSVGElement(){
        CuttingShape cs = this.getSelectedShape();
        if (cs != null){
            return cs.getShapeElement();
        }
        else{
            return null;
        }
    }
    
    public void setSelectedShape(CuttingShape selectedShape){
        CuttingShape old = this.getSelectedShape();
        if (selectedShape == null){
            this.getSelectionModel().clearSelection();
        }
        else{
            CuttingShape[] shapes = jpModel.getCuttingShapes();
            for(int idx=0;idx<shapes.length;idx++){
                if (shapes[idx].equals(selectedShape)){
                    this.getSelectionModel().setSelectionInterval(idx, idx);
                    break;
                }
            }
        }
        firePropertyChange("selectedShape", old, selectedShape);
        firePropertyChange("selectedSVGElement", old, selectedShape);
    }
    
    public void setJpModel(JepilogModel m){
        this.jpModel = m;
        this.setModel(new CuttingShapesTableModel());
    }
    public JepilogModel getJpModel(){
        return this.jpModel;
    }
    
    public CuttingShapesTable(){
        this.jpModel = new JepilogModel();
        this.setModel(new CuttingShapesTableModel());
        this.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent lse) {
                CuttingShapesTable.this.firePropertyChange("selectedShape", null, CuttingShapesTable.this.getSelectedShape());
                CuttingShapesTable.this.firePropertyChange("selectedSVGElement", null, CuttingShapesTable.this.getSelectedSVGElement());
            }
            
        });
    }
   
    class CuttingShapesTableModel extends DefaultTableModel implements PropertyChangeListener {
        private static final long serialVersionUID = 1L;
        private Class[] classes = new Class[]{String.class, String.class};
        private String[] title = new String[]{"Shape Name", "Cutting Depth"};
        
        public CuttingShapesTableModel(){
            jpModel.addPropertyChangeListener(JepilogModel.PROPERTY_CUTTINGSHAPES, this);
            jpModel.addPropertyChangeListener(JepilogModel.PROPERTY_MATERIAL, this);
        }
        
        @Override
        public Class getColumnClass(int column){
            return classes[column];
        }
        
        @Override
        public boolean isCellEditable(int row, int column){
            return column==1 && getValueAt(row, column).toString().endsWith("mm");
        }
        
        @Override
        public void setValueAt(Object value, int row, int column){
            if (column == 1){
                double val = new Scanner(value.toString().replace(".",",")).nextDouble();
                jpModel.getCuttingShape(row).setProperty(jpModel.getMaterial().getCuttingProperty(val));
            }
            else {
                throw new IllegalArgumentException("Wrong column or datatype");
            }
        }
        
        @Override
        public Object getValueAt(int row, int column){
            if (column==0){
                String name = jpModel.getCuttingShape(row).getShapeElement().getId();
                return name == null || name.equals("") ? jpModel.getCuttingShape(row).getShapeElement().toString() : name;
            }
            else if (column == 1){
                if (jpModel.getMaterial()==null){//Unable to calculate Depth
                    return "no Material selected";
                }
                else if (jpModel.getCuttingShape(row).getProperty()==null){// Default depth is Materialdepth
                    return jpModel.getMaterial().getHeight()+ " mm";
                }
                return jpModel.getMaterial().getCuttingPropertyDepth(jpModel.getCuttingShapes()[row].getProperty())+ " mm";
            }
            else{
                throw new IllegalArgumentException("Column doesn't exist");
            }
        }
        
        @Override
        public int getColumnCount(){
            return title.length;
        }
        
        @Override
        public String getColumnName(int column){
            return title[column];
        }
        
        @Override
        public int getRowCount(){
            return jpModel.getCuttingShapes().length;
        }

        public void propertyChange(PropertyChangeEvent pce) {
            this.fireTableDataChanged();
        }
    }
}