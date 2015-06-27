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
package com.frochr123.helper;

import com.frochr123.fabqr.FabQRFunctions;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.visicut.misc.Helper;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * CachedFileDownloader.java: Download files specified by their URL and cache them in memory
 * @author Christian
 */
public class CachedFileDownloader
{
  // Constants
  public static final int CACHE_DOWNLOADER_MAX_ENTRIES = 25;
  public static final int CACHE_DOWNLOADER_DEFAULT_TIMEOUT = 5000;
  public static final int CACHE_DOWNLOADER_MAX_FILESIZE_BYTES = 10000000;
  public static final String CACHE_DOWNLOADER_DEFAULT_FILETYPES = "plf,svg";

  // Variables
  // Store: Key: Requested URL, Value: <Key: Final redirected URL (filename), Value: OutputStream>
  // LinkedHashMap stores items in their insertion order, which is important for removing old entries later on
  private static LinkedHashMap<String, SimpleEntry<String, File>> cacheMap = new LinkedHashMap<String, SimpleEntry<String, File>>();

  // Function to download a file to cache and serve the data
  // commaSeperatedAllowedFileTypes, e.g. : plf,svg
  public synchronized static SimpleEntry<String, SimpleEntry<String, File>> downloadFile(String url, String commaSeperatedAllowedFileTypes) throws IOException
  {
    // On invalid URL return null
    if (url == null || url.isEmpty())
    {
      return null;
    }

    // Prepare check file extensions
    String allowedFileTypesString = commaSeperatedAllowedFileTypes;
    
    // Fallback to default allowed file types
    if (allowedFileTypesString == null || allowedFileTypesString.isEmpty())
    {
      allowedFileTypesString = CACHE_DOWNLOADER_DEFAULT_FILETYPES;
    }
    
    // Get seperate file types from string and normalize to lowercase
    String[] allowedFileTypesArray = allowedFileTypesString.split(",");

    if (allowedFileTypesArray.length > 0)
    {
      // Normalize file extensions to lower case
      for (int i = 0; i < allowedFileTypesArray.length; ++i)
      {
        if (allowedFileTypesArray[i] != null && !allowedFileTypesArray[i].isEmpty())
        {
          allowedFileTypesArray[i] = allowedFileTypesArray[i].toLowerCase();
        }
      }
    }

    File file = null;
    String finalUrl = null;
    String fileExtension = null;
    String fileBaseName = null;
    SimpleEntry<String, File> innerResult = null;
    SimpleEntry<String, SimpleEntry<String, File>> finalResult = null;

    // Check if URL is already stored in cache map
    if (cacheMap.containsKey(url))
    {
      if (cacheMap.get(url) != null)
      {
        innerResult = cacheMap.get(url);
        file = (File)(cacheMap.get(url).getValue());
      }
    }
    // URL is not stored in cache, download and store it in cache
    else
    {
      // Resize cache if needed, LinkedHashMap keeps insertion order, oldest entries are first entries
      // Temporary store keys in list and remove afterwards to avoid read / write issues
      // Get one free space for new download
      LinkedList<String> deleteKeys = new LinkedList<String>();
      for (Entry<String, SimpleEntry<String, File>> cacheEntry : cacheMap.entrySet())
      {
        if ((cacheMap.size() - deleteKeys.size()) >= CACHE_DOWNLOADER_MAX_ENTRIES)
        {
          deleteKeys.add(cacheEntry.getKey());
        }
        else
        {
          break;
        }
      }

      // Remove keys
      if (!deleteKeys.isEmpty())
      {
        for (String key : deleteKeys)
        {
          // Delete old files
          if (cacheMap.get(key) != null && cacheMap.get(key).getValue() != null)
          {
            cacheMap.get(key).getValue().delete();
          }

          // Remove entry in cache map
          cacheMap.remove(key);
        }
      }

      // Download file
      SimpleEntry<String, ByteArrayOutputStream> download = getResultFromURL(url);

      if (download == null || download.getKey() == null || download.getKey().isEmpty() || download.getValue() == null)
      {
        return null;
      }
      
      // Check for file type
      if (allowedFileTypesArray.length > 0)
      {
        finalUrl = download.getKey();
        fileExtension = FilenameUtils.getExtension(finalUrl);

        // Check for valid fileExtension
        if (fileExtension == null || fileExtension.isEmpty())
        {
          return null;
        }

        // Check if fileExtension is contained in allowed file extensions
        // Normalize file extensions to lower case
        boolean foundAllowedFileExtension = false;

        for (int i = 0; i < allowedFileTypesArray.length; ++i)
        {
          if (allowedFileTypesArray[i].equals(fileExtension))
          {
            foundAllowedFileExtension = true;
            break;
          }
        }

        // File extension was not found, abort
        if (!foundAllowedFileExtension)
        {
          return null;
        }
      }
      
      // Write result to file, it is allowed file type
      fileBaseName = FilenameUtils.getBaseName(finalUrl);
      file = FileUtils.getNonexistingWritableFile(fileBaseName + "." + fileExtension);
      file.deleteOnExit();
      FileOutputStream filestream = new FileOutputStream(file);
      download.getValue().writeTo(filestream);

      // Insert into cache and result variable
      innerResult = new SimpleEntry<String, File>(finalUrl, file);
      cacheMap.put(url, innerResult);
    }

    if (innerResult != null)
    {
      finalResult = new SimpleEntry<String, SimpleEntry<String, File>>(url, innerResult);
    }

    return finalResult;
  }
  
  // Get the result of a request for a URL
  // Also returns the final URL for checking correct file types after redirects
  public static SimpleEntry<String, ByteArrayOutputStream> getResultFromURL(String url) throws IOException
  {
    SimpleEntry<String, ByteArrayOutputStream> result = null;
    String finalUrl = null;
    ByteArrayOutputStream outputStream = null;

    if (url == null || url.isEmpty())
    {
      return null;
    }
    
    // Create HTTP client and cusomized config for timeouts
    CloseableHttpClient httpClient = HttpClients.createDefault();
    RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(CACHE_DOWNLOADER_DEFAULT_TIMEOUT).setConnectTimeout(CACHE_DOWNLOADER_DEFAULT_TIMEOUT).setConnectionRequestTimeout(CACHE_DOWNLOADER_DEFAULT_TIMEOUT).build();

    // Create HTTP Get request
    HttpGet httpGet = new HttpGet(url);
    httpGet.setConfig(requestConfig);

    // Create context
    HttpContext context = new BasicHttpContext();

    // Check for temporary FabQR download of own configured private FabQR instance
    // In that case Authorization needs to be added
    if (FabQRFunctions.getFabqrPrivateURL() != null && !FabQRFunctions.getFabqrPrivateURL().isEmpty()
        && url.startsWith(FabQRFunctions.getFabqrPrivateURL()) && url.contains("/" + FabQRFunctions.FABQR_TEMPORARY_MARKER + "/"))
    {
      // Set authentication information
      String encodedCredentials = Helper.getEncodedCredentials(FabQRFunctions.getFabqrPrivateUser(), FabQRFunctions.getFabqrPrivatePassword());
      if (!encodedCredentials.isEmpty())
      {
        httpGet.addHeader("Authorization", "Basic " + encodedCredentials);
      }
    }
    
    // Send request
    CloseableHttpResponse response = httpClient.execute(httpGet, context);
    
    // Get all redirected locations from context, if there are any
    RedirectLocations redirectLocations = (RedirectLocations)(context.getAttribute(HttpClientContext.REDIRECT_LOCATIONS));
    if (redirectLocations != null)
    {
      finalUrl = redirectLocations.getAll().get(redirectLocations.getAll().size() - 1).toString();
    }
    else
    {
      finalUrl = url;
    }
    
    // Check response valid and max file size
    if (response.getEntity() == null || response.getEntity().getContentLength() > CACHE_DOWNLOADER_MAX_FILESIZE_BYTES)
    {
      return null;
    }
    
    // Get data
    outputStream = new ByteArrayOutputStream();
    response.getEntity().writeTo(outputStream);
    
    // Return result
    result = new SimpleEntry<String, ByteArrayOutputStream>(finalUrl, outputStream);
    return result;
  }
}
