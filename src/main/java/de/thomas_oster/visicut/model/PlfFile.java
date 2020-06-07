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
package de.thomas_oster.visicut.model;

import de.thomas_oster.visicut.misc.FileUtils;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PlfFile implements Iterable<PlfPart> {
  private final List<PlfPart> parts = new LinkedList<PlfPart>();
  private File file = null;

  public File getFile()
  {
    return file;
  }

  public void setFile(File file)
  {
    this.file = file;
  }

  // Needs to be implemented because of interface Iterable
  // Can not avoid external writes here
  // As it seems it is not used to modify the list references
  // directly (e.g. with iterator().remove())
  // Consider using getPartsCopy() for iterating elements
  public Iterator<PlfPart> iterator()
  {
    return parts.iterator();
  }

  public synchronized int size()
  {
      return parts.size();
  }

  public synchronized boolean isEmpty()
  {
      return parts.isEmpty();
  }

  public synchronized boolean contains(PlfPart o)
  {
      return parts.contains(o);
  }

  public synchronized PlfPart get(int i)
  {
      return parts.get(i);
  }
  
  public synchronized boolean add(PlfPart e)
  {
      return parts.add(e);
  }

  public synchronized void clear()
  {
      parts.clear();
  }

  public synchronized boolean remove(PlfPart o)
  {
      if (o.getSourceFile() != null && o.getSourceFile().getName() != null && !o.getSourceFile().getName().isEmpty())
      {
        // If part from PLF file with temp marker in filename is deleted, try to delete corresponding file from disk
        // PLF file loading restores the file again
        if (o.isFileSourcePLF() && o.getSourceFile().getName().contains(FileUtils.FILE_VISICUT_TEMP_MARKER))
        {
          try
          {
            o.getSourceFile().deleteOnExit();
            o.getSourceFile().delete();
          }
          catch (Exception e)
          {
            // Silent exception
          }
        }
      }

      return parts.remove(o);
  }
  
  // Use copy constructor to create a copy of the original list
  // Just interested in a consistent state of references in the list
  // References might be frequently added or removed from list (QR code loading)
  // Do not need an actual copy of the contained elements
  public synchronized List<PlfPart> getPartsCopy()
  {
      return new LinkedList<>(parts);
  }
}
