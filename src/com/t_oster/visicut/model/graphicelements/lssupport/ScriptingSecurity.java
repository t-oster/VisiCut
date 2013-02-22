/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.lssupport;

import org.mozilla.javascript.ClassShutter;

/**
 *
 * @author thommy
 */
public class ScriptingSecurity implements ClassShutter
{
  private static ScriptingSecurity instance;
  public static ScriptingSecurity getInstance()
  {
    if (instance == null)
    {
      instance = new ScriptingSecurity();
    }
    return instance;
  }
  
    private boolean locked = false;

  public boolean isLocked()
  {
    return locked;
  }

  public void setLocked(boolean locked)
  {
    this.locked = locked;
  }
  
  private ScriptingSecurity()
  {
  }

  public boolean visibleToScripts(String className)
  {
    if(className.startsWith("adapter"))
    {
			return true;
    }
		return !locked;
  }
}
