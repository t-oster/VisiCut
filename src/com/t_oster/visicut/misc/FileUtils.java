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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class FileUtils
{

  private static int FILE_COPY_BUFFER_SIZE = 1024 * 1024 * 30;

  /**
   * Returns a file, which does not exist yet, should be writable
   * and ends with nameSuffix.
   * @param nameSuffix
   * @param deleteOnExit 
   */
  public static File getNonexistingWritableFile(String nameSuffix)
  {
    File b = Helper.getBasePath();
    File f = new File(b, nameSuffix);
    int i=1;
    while(f.exists())
    {
      f = new File(b, (i++) + nameSuffix);
    }
    return f;
  }
  
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
  
  private static void addDirectoryToZip(ZipOutputStream out, File dir, String prefix) throws IOException
  {
    FileInputStream in;
    byte[] buf = new byte[1024];
    int len;
    if (dir.isDirectory())
    {
      for (File f:dir.listFiles())
      {
        if (f.isDirectory())
        {
          addDirectoryToZip(out, f, ("".equals(prefix) ? "" : prefix+"/")+f.getName());
        }
        else if (f.isFile())
        {
          out.putNextEntry(new ZipEntry(("".equals(prefix) ? "" : prefix+"/")+f.getName()));
          in = new FileInputStream(f);
          // Transfer bytes from the file to the ZIP file
          while ((len = in.read(buf)) > 0)
          {
            out.write(buf, 0, len);
          }
          in.close();
          // Complete the entry
          out.closeEntry();    
        }
      }
    }
  }

  public static void zipDirectory(File dir, File file) throws FileNotFoundException, IOException
  {
    // Create the ZIP file
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
    addDirectoryToZip(out, dir, "");
    out.close();
  }
  
  public static void unzipSettingsToDirectory(File file, File dir) throws ZipException, IOException
  {
    ZipFile zip = new ZipFile(file);
    
    // does the zip file contain a settings/... folder?
    // if not, strip the first folder name so that
    // visicut-settings-master/settings/... becomes settings/...
    // this allows directly importing a zip-download from github
    boolean skipFirstFolder=true;
    Enumeration entries = zip.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = (ZipEntry) entries.nextElement();
      String name = entry.getName();
      if ("settings/settings.xml".equals(name)) {
        skipFirstFolder=false;
        break;
      }
    }
    
    entries=zip.entries();
    File inputFile = null;
    while (entries.hasMoreElements())
    {
      ZipEntry entry = (ZipEntry) entries.nextElement();
      String name = entry.getName();
      
      if (skipFirstFolder) {
        // remove first folder name: foo/settings/XY -> settings/XY
        int pos=name.indexOf("/");
        if (pos == -1) {
          continue; // skip the first folder entry
        } else {
          // strip prefix and go on
          name=name.substring(pos+1);
        }
      }
      inputFile = new File(dir, name);
      File parent = inputFile.getParentFile();
      if (!parent.exists())
      {
        parent.mkdirs();
      }
      if (entry.isDirectory())
      {
        inputFile.mkdir();
      }
      else
      {
        byte[] buf = new byte[1024];
        InputStream in = zip.getInputStream(entry);
        FileOutputStream out = new FileOutputStream(inputFile);
        // Transfer bytes from the file to the ZIP file
        int len;
        while ((len = in.read(buf)) > 0)
        {
          out.write(buf, 0, len);
        }
        out.close();
        in.close();
      }
    }
    zip.close();
  }

  public static void deleteRecursively(File f)
  {
    if (f.exists())
    {
      if (f.isDirectory())
      {
        for(File ff:f.listFiles())
        {
          deleteRecursively(ff);
        }
      }
      f.delete();
    }
  }
  
  public static void cleanDirectory(File dir)
  {
    if (dir.exists() && dir.isDirectory())
    {
      for(File f: dir.listFiles())
      {
        deleteRecursively(f);
      }
    }
  }

  /**
   * Reads a file into a String.
   * From http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
   * @param inputFile
   * @return
   * @throws FileNotFoundException
   * @throws IOException 
   */
  public static String readFileToString(File inputFile) throws FileNotFoundException, IOException
  {
    BufferedReader reader = new BufferedReader( new FileReader (inputFile));
    String         line;
    StringBuilder  stringBuilder = new StringBuilder();
    String         ls = System.getProperty("line.separator");

    while( ( line = reader.readLine() ) != null ) {
        stringBuilder.append( line );
        stringBuilder.append( ls );
    }

    return stringBuilder.toString();
  }
  
  /**
   * Download URL to file
   * @param url
   * @param file
   * @throws MalformedURLException
   * @throws IOException 
   */
  public static void downloadUrlToFile(String url, File file) throws MalformedURLException, IOException {
    // thanks to https://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
    ReadableByteChannel rbc = Channels.newChannel(new URL(url).openStream());
    FileOutputStream fos = new FileOutputStream(file);
    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
  }
}
