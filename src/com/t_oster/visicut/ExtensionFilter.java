package com.t_oster.visicut;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author thommy
 */
public class ExtensionFilter extends FileFilter
  {

    private String extension = ".xml";
    private String description = "XML Document";
    
    public ExtensionFilter(String extension, String description)
    {
      this.extension = extension;
      this.description = description;
    }

    @Override
    public boolean accept(File file)
    {
      return file.isDirectory() || file.getName().toLowerCase().endsWith(extension);
    }

    @Override
    public String getDescription()
    {
      return description;
    }
}
