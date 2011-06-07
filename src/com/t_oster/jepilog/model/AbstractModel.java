/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.model;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

/**
 * This class is the superclass of all Models for easy PropertyChangeSuppoer
 * @author oster
 */
public abstract class AbstractModel
{

  protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    this.pcs.addPropertyChangeListener(listener);
  }

  public void addPropertyChangeListener(String property, PropertyChangeListener listener)
  {
    this.pcs.addPropertyChangeListener(property, listener);
  }

  public void removePropertyChangeListener(String property, PropertyChangeListener listener)
  {
    this.pcs.removePropertyChangeListener(property, listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    this.pcs.removePropertyChangeListener(listener);
  }
}
