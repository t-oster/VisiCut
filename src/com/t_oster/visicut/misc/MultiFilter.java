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
package com.t_oster.visicut.misc;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.swing.filechooser.FileFilter;

/**
 * This class implements a FileFilter which takes multiple
 * FileFilters into one.
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class MultiFilter extends FileFilter
{

  private List<FileFilter> filters = new LinkedList<FileFilter>();
  private String description = "";

  public MultiFilter()
  {
  }

  public MultiFilter(List<FileFilter> filters)
  {
    this.filters = filters;
  }

  public MultiFilter(FileFilter[] filters)
  {
    this.filters.addAll(Arrays.asList(filters));
  }
  
  public MultiFilter(List<FileFilter> filters, String description)
  {
    this(filters);
    this.description = description;
  }
  
  public MultiFilter(FileFilter[] filters, String desc)
  {
    this(filters);
    this.description = desc;
  }

  @Override
  public boolean accept(File file)
  {
    for (FileFilter f:this.filters)
    {
      if (f.accept(file))
      {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getDescription()
  {
    return description;
  }
}
