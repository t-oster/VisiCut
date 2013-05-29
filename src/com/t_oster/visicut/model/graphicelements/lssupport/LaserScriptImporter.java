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
import com.t_oster.liblasercut.laserscript.ScriptInterpreter;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.model.graphicelements.AbstractImporter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.script.ScriptException;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author thommy
 */
public class LaserScriptImporter extends AbstractImporter
{

  public FileFilter getFileFilter()
  {
    return new FileFilter()
    {

      @Override
      public boolean accept(File file)
      {
        String name = file.getAbsolutePath().toLowerCase();
        return file.isDirectory() || name.endsWith(".ls");
      }

      @Override
      public String getDescription()
      {
        return "LaserScript (ls)";
      }

    };
  }

  public GraphicSet importSetFromFile(File inputFile, List<String> warnings) throws ImportException
  {
    GraphicSet result = new GraphicSet();
    result.setBasicTransform(new AffineTransform());
    final GeneralPath resultingShape = new GeneralPath();
    final List<String> messages = new LinkedList<String>();
    ScriptInterpreter ip = new ScriptInterpreter();
    try
    {
      ip.execute(new FileReader(inputFile), new ScriptInterfaceLogUi(new ScriptInterface(){

        private Map<String,Object> settings = new LinkedHashMap<String,Object>();
        
        //first element in a shape has to be a move
        private boolean firstMove = true;
        
        public void move(double x, double y)
        {
          if (x == Double.NaN || y == Double.NaN)
          {
            throw new IllegalArgumentException("Move called with ("+x+","+y+")");
          }
          resultingShape.moveTo(x, y);
          firstMove = false;
        }

        public void line(double x, double y)
        {
          if (x == Double.NaN || y == Double.NaN)
          {
            throw new IllegalArgumentException("Line called with ("+x+","+y+")");
          }
          if (firstMove)
          {
            move(0, 0);
          }
          resultingShape.lineTo(x, y);
        }

        public void set(String property, Object value)
        {
          settings.put(property, value);
        }

        public Object get(String property)
        {
          return settings.get(property);
        }

        public void echo(String text)
        {
          //intercepted by decorator anyway
        }

      }), !PreferencesManager.getInstance().getPreferences().isDisableSandbox());
    }
    catch (IOException e)
    {
      throw new ImportException(e);
    }
    catch (ScriptException ex)
    {
      warnings.add("Error in line: "+ex.getLineNumber()+": "+ex.getMessage());
    }
    warnings.addAll(messages);
    result.add(new LaserScriptShape(resultingShape, inputFile));
    return result;
  }
  
}
