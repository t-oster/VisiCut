/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.visicut.model.graphicelements.lssupport;

import com.t_oster.liblasercut.laserscript.ScriptInterface;
import com.t_oster.uicomponents.LogFrame;
import javax.swing.JFrame;

/**
 *
 *  @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ScriptInterfaceLogUi implements ScriptInterface
{
  
  private static LogFrame win;
  private ScriptInterface decoratee;
  
  public ScriptInterfaceLogUi(ScriptInterface decoratee)
  {
    this.decoratee = decoratee;
  }
  
  public void echo(String text)
  {
    if (win == null || !win.isVisible())
    {
      win = new LogFrame("LaserScript output");
      win.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      win.setVisible(true);
    }
    win.addLine(text);
  }

  public void move(double x, double y)
  {
    decoratee.move(x, y);
  }

  public void line(double x, double y)
  {
    decoratee.line(x, y);
  }

  public void set(String property, Object value)
  {
    decoratee.set(property, value);
  }

  public Object get(String property)
  {
    return decoratee.get(property);
  }
  
}
