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

import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.VectorPart;
import java.awt.geom.AffineTransform;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VectorPartScriptInterface implements ScriptInterface
{
  
  private VectorPart vp;
  private AffineTransform objectTrans;
  
  public VectorPartScriptInterface(VectorPart vp, AffineTransform objectTrans)
  {
    this.vp = vp;
    this.objectTrans = objectTrans;
  }
  
  public void move(double x, double y)
  {
    double[] p = new double[2];
    objectTrans.transform(new double[]{x,y}, 0, p, 0, 1);
    vp.moveto((int) p[0],(int) p[1]);
  }
  
  public void line(double x, double y)
  {
    double[] p = new double[2];
    objectTrans.transform(new double[]{x,y}, 0, p, 0, 1);
    vp.lineto((int) p[0],(int) p[1]);
  }
  
  public void set(String property, Object value)
  {
    LaserProperty cp = vp.getCurrentCuttingProperty().clone();
    Object current = cp.getProperty(property);
    if (current instanceof Float)
    {
      cp.setProperty(property, (Float) ((Double) value).floatValue());
    }
    else if (current instanceof Integer)
    {
      cp.setProperty(property, (Integer) ((Double) value).intValue());
    }
    else if (current instanceof String)
    {
      cp.setProperty(property, current.toString());
    }
    else
    {
      cp.setProperty(property, value);
    }
    vp.setProperty(cp);
  }
  
  public Object get(String property)
  {
    return vp.getCurrentCuttingProperty().getProperty(property);
  }
  
}
