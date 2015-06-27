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
package com.t_oster.visicut.model;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PlfFile implements Iterable<PlfPart> {
  private List<PlfPart> parts = new LinkedList<PlfPart>();
  private File file = null;

  public File getFile()
  {
    return file;
  }

  public void setFile(File file)
  {
    this.file = file;
  }

  public int size()
  {
    return parts.size();
  }

  public boolean isEmpty()
  {
    return parts.isEmpty();
  }

  public boolean contains(PlfPart o)
  {
    return parts.contains(o);
  }

  public PlfPart get(int i)
  {
    return parts.get(i);
  }

  // Needs to be implemented because of interface Iterable
  // Can not avoid external writes here
  // As it seems it is not used to modify the list references
  // directly (e.g. with iterator().remove())
  public Iterator<PlfPart> iterator()
  {
    return parts.iterator();
  }
  
  public boolean add(PlfPart e)
  {
    synchronized (this)
    {
      return parts.add(e);
    }
  }

  public void clear()
  {
    synchronized (this)
    {
      parts.clear();
    }
  }

  public boolean remove(PlfPart o)
  {
    synchronized (this)
    {
      return parts.remove(o);
    }
  }

  // Use copy constructor to create a copy of the original list
  // Just interested in a consistent state of references in the list
  // Do not need a copy of the contained elements
  public List<PlfPart> getPartsCopy()
  {
    synchronized (this)
    {
      return new LinkedList<PlfPart>(parts);
    }
  }
}
