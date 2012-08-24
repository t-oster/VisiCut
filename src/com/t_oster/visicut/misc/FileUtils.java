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
package com.t_oster.visicut.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class FileUtils
{

  private static int FILE_COPY_BUFFER_SIZE = 1024 * 1024 * 30;

  public static void copyDirectoryToDirectory(File srcDir, File destDir) throws IOException
  {
    if (!srcDir.exists() || !srcDir.isDirectory())
    {
      throw new IOException("Source dir "+srcDir+" is not a directory.");
    }
    if (destDir.exists() && !destDir.isDirectory())
    {
      throw new IOException("Source dir "+destDir+" is not a directory.");
    }
    if (!destDir.exists() && !destDir.mkdirs())
    {
      throw new IOException("Can't create "+destDir);
    }
    for (File f:srcDir.listFiles())
    {
      if (f.isFile())
      {
        copyFileToDirectory(f, destDir);
      }
      else if (f.isDirectory())
      {
        File t = new File(destDir, f.getName());
        copyDirectoryToDirectory(f, t);
      }
    }
    
  }
  
  public static void copyFileToDirectory(File srcFile, File destDir) throws IOException
  {
    if (destDir.exists() && !destDir.isDirectory())
    {
      throw new IOException("Destination '" + destDir + "' exists but is a file");
    }
    if (!destDir.exists() && !destDir.mkdirs())
    {
      throw new IOException("Can't create directory "+destDir);
    }
    copyFile(srcFile, new File(destDir, srcFile.getName()), false);
  }
  
  public static File getUserDirectory()
  {
    return new File(System.getProperty("user.home"));
  }
  
  public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException
  {
    if (destFile.exists() && destFile.isDirectory())
    {
      throw new IOException("Destination '" + destFile + "' exists but is a directory");
    }

    FileInputStream fis = null;
    FileOutputStream fos = null;
    FileChannel input = null;
    FileChannel output = null;
    try
    {
      fis = new FileInputStream(srcFile);
      fos = new FileOutputStream(destFile);
      input = fis.getChannel();
      output = fos.getChannel();
      long size = input.size();
      long pos = 0;
      long count = 0;
      while (pos < size)
      {
        count = size - pos > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : size - pos;
        pos += output.transferFrom(input, pos, count);
      }
    }
    finally
    {
      output.close();
      fos.close();
      input.close();
      fis.close();
    }

    if (srcFile.length() != destFile.length())
    {
      throw new IOException("Failed to copy full contents from '"
        + srcFile + "' to '" + destFile + "'");
    }
    if (preserveFileDate)
    {
      destFile.setLastModified(srcFile.lastModified());
    }
  }
}
