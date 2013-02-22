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
package com.t_oster.visicut.laserscript;

import com.t_oster.visicut.model.graphicelements.lssupport.ScriptInterpreter;
import com.t_oster.liblasercut.PowerSpeedFocusProperty;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.visicut.model.graphicelements.lssupport.ScriptInterface;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ScriptInterpreterTest
{
  
  public ScriptInterpreterTest()
  {
  }
  

  /**
   * Test of execute method, of class ScriptInterpreter.
   */
  @Test
  public void testExecute() throws Exception
  {
    System.out.println("execute");
    String script = "function rectangle(x, y, width, height) {";
    script += "move(x, y);";
    script += "line(x+width, y);";
    script += "line(x+width, y+height);";
    script += "line(x, y+height);";
    script += "line(x, y);";
    script += "}";
    script += "rectangle(0, 0, 20, 30);";
    ScriptInterpreter instance = new ScriptInterpreter();
    instance.execute(script, new ScriptInterface(){

      public void move(double x, double y)
      {
        
      }

      public void line(double x, double y)
      {
        
      }

      public void set(String property, Object value)
      {
        
      }

      public Object get(String property)
      {
        return null;
      }
    });
  }
}
