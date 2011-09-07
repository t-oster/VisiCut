package com.t_oster.visicut.gui.beans;

/**
 * A class needs to implement this interface
 * to provide Data for an EditableTablePanel
 * 
 * @author thommy
 */
public interface EditableTableProvider
{
  public Object getNewInstance();
  /**
   * Edits the Object and returns the new Object
   * if editing shall be saved, null otherwise
   * @param o
   * @return 
   */
  public Object editObject(Object o);
}
