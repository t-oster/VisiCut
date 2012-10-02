/**
 * This file is part of VisiCut. Copyright (C) 2012 Thomas Oster
 * <thomas.oster@rwth-aachen.de> RWTH Aachen University - 52062 Aachen, Germany
 *
 * VisiCut is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * VisiCut is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VisiCut. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.t_oster.visicut.managers;

import com.t_oster.visicut.misc.Helper;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.awt.Color;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ColorConverter implements Converter
{

  @Override
  public boolean canConvert(Class type)
  {
    return Color.class.equals(type);
  }

  public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc)
  {
    writer.setValue(Helper.toHtmlRGB((Color) o));
  }

  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc)
  {
    return Helper.fromHtmlRGB(reader.getValue());
  }
}
