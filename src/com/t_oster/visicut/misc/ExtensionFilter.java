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
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ExtensionFilter extends FileFilter
  {

    private String[] extensions = new String[]{".xml"};
    private String description = "XML Document";
    
    public ExtensionFilter(String extension, String description)
    {
      this(new String[]{extension},description);
    }

    public ExtensionFilter(String[] extensions, String description)
    {
      this.extensions = extensions;
      this.description = description;
    }

    @Override
    public boolean accept(File file)
    {
      if (file.isDirectory())
      {
        return true;
      }
      else
      {
        for (String extension : this.extensions)
        {
          if (file.getName().toLowerCase().endsWith(extension))
          {
            return true;
          }
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
