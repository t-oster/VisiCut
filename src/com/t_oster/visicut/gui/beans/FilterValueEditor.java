package com.t_oster.visicut.gui.beans;

import javax.swing.AbstractCellEditor;
import javax.swing.table.TableCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FilterValueEditor extends AbstractCellEditor
  implements TableCellEditor,
  ActionListener
{

  Object currentObject;
  JButton button;
  JColorChooser colorChooser;
  JDialog dialog;
  protected static final String EDIT = "edit";

  public FilterValueEditor()
  {
    button = new JButton();
    button.setActionCommand(EDIT);
    button.addActionListener(this);
    button.setBorderPainted(false);

    //Set up the dialog that the button brings up.
    colorChooser = new JColorChooser();
    dialog = JColorChooser.createDialog(button,
      "Pick a Color",
      true, //modal
      colorChooser,
      this, //OK button handler
      null); //no CANCEL button handler
  }

  /**
   * Handles events from the editor button and from
   * the dialog's OK button.
   */
  public void actionPerformed(ActionEvent e)
  {
    if (currentObject instanceof Color)
    {
      if (EDIT.equals(e.getActionCommand()))
      {

        button.setBackground((Color) currentObject);
        colorChooser.setColor((Color) currentObject);
        dialog.setVisible(true);
        //Make the renderer reappear.
        fireEditingStopped();
      }
      else
      { //User pressed dialog's "OK" button.
        currentObject = colorChooser.getColor();
      }
    }
  }

  //Implement the one CellEditor method that AbstractCellEditor doesn't.
  public Object getCellEditorValue()
  {
    return currentObject;
  }

  //Implement the one method defined by TableCellEditor.
  public Component getTableCellEditorComponent(JTable table,
    Object value,
    boolean isSelected,
    int row,
    int column)
  {
    currentObject = value;
    return button;
  }
}
