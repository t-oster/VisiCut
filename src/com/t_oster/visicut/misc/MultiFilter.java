package com.t_oster.visicut.misc;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.swing.filechooser.FileFilter;

/**
 * This class implements a FileFilter which takes multiple
 * FileFilters into one.
 * @author thommy
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
