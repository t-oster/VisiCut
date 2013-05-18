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

package com.t_oster.visicut.model.graphicelements.psvgsupport;

import com.t_oster.uicomponents.parameter.Parameter;
import com.t_oster.visicut.model.PlfPart;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.thoughtworks.xstream.XStream;
import java.awt.geom.AffineTransform;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ParametricPlfPart extends PlfPart
{

  private static XStream _xstream;
  private static XStream getXStream()
  {
    if (_xstream == null)
    {
      _xstream = new XStream();
      _xstream.alias("parameters", LinkedHashMap.class);
      _xstream.alias("parameter", Entry.class);
    }
    return _xstream;
  }
  
  /**
   * Saves the curent values of the given parameters to XML
   * @param parameters
   * @param out 
   */
  public static void serializeParameterValues(Map<String, Parameter> parameters, OutputStream out)
  {
    XStream xstream = getXStream();
    Map<String, Object> values = new LinkedHashMap<String, Object>();
    for (Entry<String, Parameter> e : parameters.entrySet())
    {
      values.put(e.getKey(), e.getValue().value);
    }
    xstream.toXML(values, out);
  }

  /*
   * Updates the current values of the given parameters from XML
   */
  static void unserializeParameterValues(Map<String, Parameter> parameters, FileInputStream in)
  {
    XStream xstream = getXStream();
    Map<String, Object> values = (Map<String, Object>) xstream.fromXML(in);
    for (Entry<String, Object> e : values.entrySet())
    {
      if (parameters.containsKey(e.getKey()))
      {
        parameters.get(e.getKey()).value = e.getValue();
      }
    }
  }
  private Map<String, Parameter> map;
  
  public Map<String, Parameter> getParameters()
  {
    if (map == null)
    {
      map = new LinkedHashMap<String, Parameter>();
    }
    return map;
  }
  
  public void setParameters(Map<String, Parameter> params)
  {
    this.map = params;
  }

  private ParametricSVGImporter pi = new ParametricSVGImporter();
  public void applyParameters() throws ImportException
  {
    AffineTransform t = getGraphicObjects().getTransform();
    setGraphicObjects(pi.importSetFromFile(getSourceFile(), new LinkedList<String>(), this.getParameters()));
    getGraphicObjects().setTransform(t);
  }
}
