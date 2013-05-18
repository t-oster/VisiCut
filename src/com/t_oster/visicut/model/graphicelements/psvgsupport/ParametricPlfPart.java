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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.ZipOutputStream;

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
    }
    return _xstream;
  }
  
  public static void serializeParameters(Map<String, Parameter> parameters, ZipOutputStream out)
  {
    XStream xstream = getXStream();
    xstream.toXML(parameters, out);
  }

  static Map<String, Parameter> unserializeParameters(FileInputStream in)
  {
    XStream xstream = getXStream();
    return (Map) xstream.fromXML(in);
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

  private PSVGImporter pi = new PSVGImporter();
  public void applyParameters() throws ImportException
  {
    AffineTransform t = getGraphicObjects().getTransform();
    setGraphicObjects(pi.importSetFromFile(getSourceFile(), new LinkedList<String>(), this.getParameters()));
    getGraphicObjects().setTransform(t);
  }
}
