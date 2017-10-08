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
package com.tur0kk.thingiverse;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.misc.Helper;
import com.tur0kk.thingiverse.model.Thing;
import com.tur0kk.thingiverse.model.ThingCollection;
import com.tur0kk.thingiverse.model.ThingFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Singleton class managing all the communication with the Thingiverse API.
 * @author Patrick Schmidt
 */
public class ThingiverseManager
{
  /**
   * Thingiverse App Client ID. Thingiverse apps are managed on the thingiverse
   * website. The owner of the current app "VisiCut Connect" is Patrick Schmidt
   * (patrick.schmidt1@rwth-aachen.de).
   */
  private static final String clientId = "a91afd847a7c6f2dda2f";
  
  /**
   * Thingiverse App Client Secret. The owner of the app "VisiCut Connect" can
   * see this on the thingiverse website.
   */
  private static final String clientSecret = "00d763afa30cda6248fc203aa997eee2";
  
  /**
   * After a successful login, thingiverse will redirect the user to this url.
   * ATTENTION: Changing this string will have no effect.
   * The app owner has to change the callback url on the thingiverse website
   * instead.
   */
  private static final String clientCallback = "http://hci.rwth-aachen.de/public/VisiCut/show_code.php";
  
  /**
   * The internal webbrowser (JavaFX) will automatically close if it has been
   * redirected to a url with this prefix.
   */
  private static final String redirectUrlPrefix = "http://hci.rwth-aachen.de/public/VisiCut/show_code.php?code=";
  
  /**
   * ThingiverseManager follows the singleton pattern.
   * Use ThingiverseManager.getInstance().
   */
  private static ThingiverseManager instance = null;
  
  /**
   * The client object is available iff a user is currently logged in.
   */
  private ThingiverseClient client = null;
  
  /**
   * Private constructor following the singleton pattern.
   */
  private ThingiverseManager()
  {
   
  }
  
  /**
   * Use this method to get the current thingiverse manager instance.
   * @return ThingiverseManager instance
   */
  public static ThingiverseManager getInstance()
  {
    if (instance == null)
    {
      instance = new ThingiverseManager();
    }
    return instance;
  }
  
  /**
   * The internal webbrowser (JavaFX) will automatically close if it has been
   * redirected to a url with this prefix.
   * @return Url prefix
   */
  public String getRedirectUrlPrefix()
  {
    return this.redirectUrlPrefix;
  }
  
  /**
   * Tries login with access token that has been saved to file.
   * @return True on success.
   */
  public boolean logIn()
  {
    String accessToken = loadAccesToken();
    if (accessToken == null)
    {
      return false;
    }
    
    client = new ThingiverseClient(clientId, clientSecret, clientCallback);
    client.loginWithAccesToken(accessToken);
    return true;
  }
  
  /**
   * Logs out the current user and starts the authentication procedure.
   * Use the returned url to get a browser code and call logIn(browserCode)
   * to complete the login.
   * @return Login URL
   */
  public String initiateAuthentication()
  {
    client = new ThingiverseClient(clientId, clientSecret, clientCallback);
    String loginUrl = client.loginFirstTime();
    return loginUrl;
  }
 
  /**
   * Complete the login procedure using a browser code.
   * @param browserCode 
   */
  public void logIn(String browserCode)
  {
    if (client == null)
    {
      client = new ThingiverseClient(clientId, clientSecret, clientCallback);
    }
    
    try
    {
      if (browserCode == null || browserCode.isEmpty())
      {
        throw new Exception("Invalid browser code");
      }
      
      String accessToken = client.loginWithBrowserCode(browserCode);
      if (accessToken == null || accessToken.isEmpty())
      {
        throw new Exception("Invalid access token");
      }
      
      saveAccessToken(accessToken);
    }
    catch (Exception ex)
    {
      logOut();
      
      System.out.println("Login failed! " + ex.getMessage());
      return;
    }
  }
  
  /**
   * Log the current user out. This will remove the persistent session credentials
   * from the file system.
   */
  public void logOut()
  {
    client = null;
    deleteAccesToken();
  }
  
  /**
   * Check if a user is currently logged in.
   * @return Logged in?
   */
  public boolean isLoggedIn()
  {
    return client != null;
  }
  
  /**
   * Gets user name from thingiverse API.
   * @return "USER NAME" on error.
   */
  public String getUserName()
  {
    try
    {
      String json = client.user("me");
      
      JSONParser parser = new JSONParser();
      JSONObject obj = (JSONObject)parser.parse(json);
      return obj.get("name").toString();
    }
    catch(ParseException ex)
    {
      ex.printStackTrace();
      return "USER NAME";
    }
  }
  
  /**
   * Gets user avatar url from thingiverse API.
   * @return Image url
   */
  public String getUserImage()
  {
    try
    {
      String json = client.user("me");
      
      JSONParser parser = new JSONParser();
      JSONObject obj = (JSONObject)parser.parse(json);
      String url = obj.get("thumbnail").toString();
      
      return url;
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      return "";
    }
  }
  
  /**
   * Gets the list of "my things" from the thingiverse api.
   * @return List of things
   */
  public List<Thing> getMyThings()
  {
    return getMyThings(false, false);
  }
  
  /**
   * Gets a filtered list of "my things" from the thingiverse api.
   * @param filterExtensions Set to true if you want to filter for supported
   * file extensions. Note that this will be relatively slow. The list of file
   * extensions can be set as application preferences.
   * @param filterTags Set to true if you want to filter for certain tags.
   * Note that this will be relatively slow. The list of tags can be set as
   * application preferences.
   * @return List of things
   */
  public List<Thing> getMyThings(boolean filterExtensions, boolean filterTags)
  {
    List<Thing> things = new LinkedList<Thing>();
    
    try
    {
      String json = client.thingsByUser("me");
      
      JSONParser parser = new JSONParser();
      JSONArray array = (JSONArray)parser.parse(json);
      for (Object obj : array)
      {
        JSONObject item = (JSONObject)obj;
        String itemId = item.get("id").toString();
        String itemName = item.get("name").toString();
        String imageUrl = item.get("thumbnail").toString();
        things.add(new Thing(itemId, itemName, imageUrl));
      }
      
      things = filterThings(things, filterExtensions, filterTags);
    }
    catch(ClassCastException ex)
    {
      // Result empty or something wrong with the json response.
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return things;
  }
  
  /**
   * Gets the list of liked things from the thingiverse api.
   * @return List of things
   */
  public List<Thing> getLikedThings()
  {
    return getLikedThings(false, false);
  }
  
  /**
   * Gets a filtered list of liked things from the thingiverse api.
   * @param filterExtensions Set to true if you want to filter for supported
   * file extensions. Note that this will be relatively slow. The list of file
   * extensions can be set as application preferences.
   * @param filterTags Set to true if you want to filter for certain tags.
   * Note that this will be relatively slow. The list of tags can be set as
   * application preferences.
   * @return List of things
   */
  public List<Thing> getLikedThings(boolean filterExtensions, boolean filterTags)
  {
    List<Thing> things = new LinkedList<Thing>();
    
    try
    {
      String json = client.likedThings();
      
      JSONParser parser = new JSONParser();
      JSONArray array = (JSONArray)parser.parse(json);
      for (Object obj : array)
      {
        JSONObject item = (JSONObject)obj;
        String itemId = item.get("id").toString();
        String itemName = item.get("name").toString();
        String imageUrl = item.get("thumbnail").toString();
        things.add(new Thing(itemId, itemName, imageUrl));
      }
      
      things = filterThings(things, filterExtensions, filterTags);
    }
    catch(ClassCastException ex)
    {
      // Result empty or something wrong with the json response.
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return things;
  }
  
  /**
   * Gets the list of "my collections" from the thingiverse api.
   * @return List of collections
   */
  public List<ThingCollection> getMyCollections()
  {
    List<ThingCollection> collections = new LinkedList<ThingCollection>();
    
    try
    {
      String json = client.collectionsByUser("me");
      
      JSONParser parser = new JSONParser();
      JSONArray array = (JSONArray)parser.parse(json);
      for (Object obj : array)
      {
        JSONObject jsonCollection = (JSONObject)obj;
        String collectionId = jsonCollection.get("id").toString();
        String collectionName = jsonCollection.get("name").toString();
        String imageUrl = jsonCollection.get("thumbnail").toString();
        collections.add(new ThingCollection(collectionId, collectionName, imageUrl));
      }
    }
    catch(ClassCastException ex)
    {
      // Result empty or something wrong with the json response.
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return collections;
  }
  
  /**
   * Gets the list of things belonging to a certain collection.
   * @param collection
   * @return List of things
   */
  public List<Thing> getThingsByCollection(ThingCollection collection)
  {
    return getThingsByCollection(collection, false, false);
  }
  
  /**
   * Gets the list of things belonging to a certain collection.
   * @param collection
   * @param filterExtensions Set to true if you want to filter for supported
   * file extensions. Note that this will be relatively slow. The list of file
   * extensions can be set as application preferences.
   * @param filterTags Set to true if you want to filter for certain tags.
   * Note that this will be relatively slow. The list of tags can be set as
   * application preferences.
   * @return List of things
   */
  public List<Thing> getThingsByCollection(ThingCollection collection, boolean filterExtensions, boolean filterTags)
  {
    List<Thing> things = new LinkedList<Thing>();
    
    try
    {
      String json = client.thingsByCollection(collection.getId());

      JSONParser parser = new JSONParser();
      JSONArray array = (JSONArray)parser.parse(json);
      for (Object obj : array)
      {
        JSONObject jsonThing = (JSONObject)obj;
        String thingId = jsonThing.get("id").toString();
        String thingName = jsonThing.get("name").toString();
        String imageUrl = jsonThing.get("thumbnail").toString();
        things.add(new Thing(thingId, thingName, imageUrl));
      }
      
      things = filterThings(things, filterExtensions, filterTags);
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return things;
  }
  
  /**
   * Do a search using the thingiverse api.
   * @param query search string
   * @return List of things matching the search term
   */
  public List<Thing> search(String query)
  {
    return search(query, false, false);
  }
  
    /**
   * Do a search using the thingiverse api.
   * @param query search string
   * @param filterExtensions Set to true if you want to filter for supported
   * file extensions. Note that this will be relatively slow. The list of file
   * extensions can be set as application preferences.
   * @param filterTags Set to true if you want to filter for certain tags.
   * Note that this will be relatively slow. The list of tags can be set as
   * application preferences.
   * @return List of things
   */
  public List<Thing> search(String query, boolean filterExtensions, boolean filterTags)
  {
    List<Thing> things = new LinkedList<Thing>();
    
    try
    {
      String json = client.search(query);
      
      JSONParser parser = new JSONParser();
      JSONArray array = (JSONArray)parser.parse(json);
      for (Object obj : array)
      {
        JSONObject item = (JSONObject)obj;
        String itemId = item.get("id").toString();
        String itemName = item.get("name").toString();
        String imageUrl = item.get("thumbnail").toString();
        Thing thing = new Thing(itemId, itemName, imageUrl);
        
        things.add(thing);
      }
      
      things = filterThings(things, filterExtensions, filterTags);
    }
    catch(ClassCastException ex)
    {
      // Search result empty or something wrong with the json response.
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return things;
  }
  
  /**
   * Get all files belonging to a certain thing. This does not download the
   * actual files but returns ThingFile objects which contain all the information
   * to do the actual download. (see downloadThingFile)
   * @param thing
   * @return List of thing files
   */
  public List<ThingFile> getFiles(Thing thing)
  {
    return getFiles(thing, false);
  }
  
  /**
   * Get all files belonging to a certain thing. This does not download the
   * actual files but returns ThingFile objects which contain all the information
   * to do the actual download. (see downloadThingFile)
   * @param thing
   * @param filterExtensions Set to true if you want to filter for supported
   * file extensions. The list of file extensions can be set as application
   * preferences.
   * @return List of thing files
   */
  public List<ThingFile> getFiles(Thing thing, boolean filterExtensions)
  {
    if (filterExtensions)
    {
      List<String> fileExtensionFilter = this.splitCommaSeparatedString(VisicutModel.getInstance().getPreferences().getSupportedExtensions());
      return getFiles(thing, fileExtensionFilter);
    }
    else
    {
      return getFiles(thing, new LinkedList<String>());
    }
  }
  
  /**
   * Get all files belonging to a certain thing. This does not download the
   * actual files but returns ThingFile objects which contain all the information
   * to do the actual download. (see downloadThingFile)
   * @param thing
   * @param filterExtensions Filter using a specific list of allowed file
   * extensions. A file will be part of the result if it matches one of the
   * given extensions. If the list is empty, all files will be returned.
   * @return List of thing files
   */
  private List<ThingFile> getFiles(Thing thing, List<String> allowedFileExtensions)
  {
    List<ThingFile> files = new LinkedList<ThingFile>();
    
    try
    {
      String json = client.filesByThing(thing.getId());

      JSONParser parser = new JSONParser();
      JSONArray array = (JSONArray)parser.parse(json);
      for (Object obj : array)
      {
        JSONObject file = (JSONObject)obj;
        String fileId = file.get("id").toString();
        String fileName = file.get("name").toString();
        String fileUrl = file.get("download_url").toString();
        String thumbnailUrl = file.get("thumbnail").toString();
        
        if (hasMatchingExtension(fileName, allowedFileExtensions))
        {
          files.add(new ThingFile(fileId, fileName, fileUrl, thumbnailUrl, thing));
        }
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    
    return files;
  }
  
  /**
   * Gets the list of tags that are attached to a thing.
   * @param thing
   * @return List of tags (strings)
   */
  public List<String> getTags(Thing thing)
  {
    List<String> tags = new LinkedList<String>();
    
    try
    {
      String json = client.tagsByThing(thing.getId());

      JSONParser parser = new JSONParser();
      JSONArray array = (JSONArray)parser.parse(json);
      for (Object obj : array)
      {
        JSONObject jsonTag = (JSONObject)obj;
        String tag = jsonTag.get("name").toString();
        tags.add(tag);
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    
    return tags;
  }
  
  /**
   * Takes a list of strings and returns the sublist of things that match all
   * given filter rules. Note that this method is relatively slow. It does
   * a thingiverse api call for each thing in the input list. Requests are done
   * in parallel using a thread pool.
   * @param things Input list of things
   * @param filterExtensions Set to true if you want to filter for supported
   * file extensions. The list of file extensions can be set as application
   * preferences.
   * @param filterTags Set to true if you want to filter for certain tags.
   * The list of tags can be set as application preferences.
   * @return Subset of the input list.
   * @throws InterruptedException
   * @throws ExecutionException 
   */
  private List<Thing> filterThings(List<Thing> things, boolean filterExtensions, boolean filterTags) throws InterruptedException, ExecutionException
  {
    // Return quickly if no filter is set.
    if (!filterExtensions && !filterTags)
    {
      return things;
    }
    
    List<Thing> thingsFiltered = new LinkedList<Thing>();
    
    // Create a filter job that does api calls for each thing and process them
    // in parallel.
    class FilterJob implements Callable<Boolean>
    {
      public Thing thing;
      public boolean filterExtensions;
      public boolean filterTags;
      public List<String> fileExtensionFilter;
      public List<String> tagFilter;
      
      public Boolean call() throws Exception
      {
        // Filter by file extensions
        if (filterExtensions && !this.fileExtensionFilter.isEmpty())
        {
          List<ThingFile> files = getFiles(this.thing, this.fileExtensionFilter);
          if (files.isEmpty())
          {
            // No files with matching extension found
            return false;
          }
        }
        
        // Filter by tags
        if (filterTags && !this.tagFilter.isEmpty())
        {
          List<String> tags = getTags(this.thing);
          if (Collections.disjoint(tags, tagFilter))
          {
            // No matching tags
            return false;
          }
        }
        
        return true;
      }
    }
    
    // Create a job for each thing
    List<Callable<Boolean>> jobs = new LinkedList<Callable<Boolean>>();
    for (Thing thing : things)
    {
      FilterJob job = new FilterJob();
      job.thing = thing;
      job.filterExtensions = filterExtensions;
      job.filterTags = filterTags;
      job.fileExtensionFilter = this.splitCommaSeparatedString(VisicutModel.getInstance().getPreferences().getSupportedExtensions());
      job.tagFilter = this.splitCommaSeparatedString(VisicutModel.getInstance().getPreferences().getLaserCutterTags());
      jobs.add(job);
    }

    // Execute jobs and wait until all of them are finished.
    ExecutorService executor = Executors.newFixedThreadPool(30);
    List<Future<Boolean>> results = executor.invokeAll(jobs);

    // Create filtered result list.
    Iterator<Thing> thingIter = things.iterator();
    Iterator<Future<Boolean>> resultIter = results.iterator();
    
    while (thingIter.hasNext() && resultIter.hasNext())
    {
      Thing thing = thingIter.next();
      Future<Boolean> result = resultIter.next();
      
      if (result.get() == true)
      {
        thingsFiltered.add(thing);
      }
    }

    executor.shutdown();
    return thingsFiltered;
  }
  
  /**
   * Checks if a filename has one of the given file extensions.
   * @param fileName File name as string
   * @param fileExtensions List of file extensions without dot. E.g. "svg"
   * @return True if the file name ends with one of the given file extensions
   * (ignoring case) or the list of extensions is empty.
   */
  private boolean hasMatchingExtension(String fileName, List<String> fileExtensions)
  {
    if (fileExtensions.isEmpty())
    {
      return true;
    }
    
    String fileNameLower = fileName.toLowerCase();
    for (String extension : fileExtensions)
    {
      if (fileNameLower.endsWith(extension.toLowerCase()))
      {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Downloads a file from thingiverse,
   * saves it to disk and returns a File object or null.
   * @param thingFile
   * @return Java.io.File that has been saved to disk.
   */
  public File downloadThingFile(ThingFile thingFile)
  {
    File file = null;
    
    try
    {
      File folder = new File(Helper.getBasePath(),
                             "thingiverse/files/" +
                             thingFile.getThing().getId());
      folder.mkdirs();
      file = new File(folder, thingFile.getName());
      
      // Delete old file from disk.
      // (There may have been an update and we want to download the most recent
      // version each time)
      if (file.exists())
      {
        file.delete();
      }
      
      file.createNewFile();
      client.downloadBinaryFile(thingFile.getUrl(), file, true);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    
    return file;
  }
  
  /**
   * Downloads an image from thingiverse,
   * saves it to disk and returns its location in the file system.
   * @url Web url of the image
   * @return Absolute path to the image on the local file system
   */
  public String downloadImage(String url)
  {
    String returnPath = "";
    File file = null;
    
    try
    {
      File folder = new File(Helper.getBasePath(),
                             "thingiverse/images/");
      
      // Generate a "unique" filename...
      String filename = ((Integer)url.hashCode()).toString()
                      + "."
                      + url.substring(url.length() - 3);
      
      folder.mkdirs();
      file = new File(folder, filename);
      
      // Delete old file from disk.
      // (There may have been an update and we want to download the most recent
      // version each time)
      if (file.exists())
      {
        file.delete();
      }
      
      file.createNewFile();
      client.downloadBinaryFile(url, file, false);
      
      returnPath = file.getAbsolutePath();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    
    return returnPath;
  }
  
  /**
   * Save access token to file.
   */
  private void saveAccessToken(String accessToken)
  {
    try
    {
      File thingiverseFolder = new File(Helper.getBasePath(), "thingiverse");
      thingiverseFolder.mkdirs();

      File sessionFile = new File(thingiverseFolder, "session.properties");
      sessionFile.createNewFile();

      Properties properties = new Properties();
      properties.load(new FileInputStream(sessionFile));
      properties.setProperty("access_token", accessToken);
      properties.store(new FileWriter(sessionFile), "session");
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
  }

  /**
   * Get access token from file.
   * Returns null on failure.
   */
  private String loadAccesToken()
  {
    String result = null;
    
    try
    {
      File thingiverseFolder = new File(Helper.getBasePath(), "thingiverse");
      if (!thingiverseFolder.exists())
      {
        return result;
      }

      File sessionFile = new File(thingiverseFolder, "session.properties");
      if (!sessionFile.exists())
      {
        return result;
      }

      Properties properties = new Properties();
      properties.load(new FileInputStream(sessionFile));
      result = properties.getProperty("access_token");
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
 
    return result;
  }
  
  /**
   * Deletes the access token if it has been saved to file.
   */
  private void deleteAccesToken()
  {
    try
    {
      File thingiverseFolder = new File(Helper.getBasePath(), "thingiverse");
      if (!thingiverseFolder.exists())
      {
        return;
      }

      File sessionFile = new File(thingiverseFolder, "session.properties");
      if (!sessionFile.exists())
      {
        return;
      }

      Properties properties = new Properties();
      properties.load(new FileInputStream(sessionFile));
      properties.remove("access_token");
      properties.store(new FileWriter(sessionFile), "session");
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
  }
  
  /**
   * Splits a string with comma separated values list of strings.
   * @param commaSeparatedString
   * @return 
   */
  private List<String> splitCommaSeparatedString(String commaSeparatedString)
  {
    List<String> separatedList = new ArrayList<String>(Arrays.asList(commaSeparatedString.split("\\s*,\\s*")));
    return separatedList;
  }
}