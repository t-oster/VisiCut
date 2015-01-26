package com.tur0kk.thingiverse;

import com.t_oster.visicut.misc.Helper;
import com.tur0kk.thingiverse.model.Thing;
import com.tur0kk.thingiverse.model.ThingCollection;
import com.tur0kk.thingiverse.model.ThingFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
  // App name: VisiCutThingiverse
  private static final String clientId = "0d3b8166624f6d05b738";
  private static final String clientSecret = "a0b5368a3d58ddb3b1ade12f4f8f14e7";
  private static final String clientCallback = "http://www.thingiverse.com";
  private static final String redirectUrlPrefix = "http://hci.rwth-aachen.de/public/VisiCut/show_code.php?code=";
  
  private static final List<String> fileExtensionFilter = Arrays.asList(
    "svg", "plf", "dxf", "eps", "gcode");
  
  private static final List<String> tagFilter = Arrays.asList(
    "lasercutter", "lasercut", "laser cutter", "laser cut");
  
  private static ThingiverseManager instance = null;
  
  /**
   * The client object is available iff a user is currently logged in.
   */
  private ThingiverseClient client = null;
  
  private ThingiverseManager()
  {
   
  }
  
  public static ThingiverseManager getInstance()
  {
    if (instance == null)
    {
      instance = new ThingiverseManager();
    }
    return instance;
  }
  
  public String getRedirectUrlPrefix()
  {
    return this.redirectUrlPrefix;
  }
  
  /**
   * Tries login with access token that has been saved to file.
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
   * @return Login URL
   */
  public String initiateAuthentication()
  {
    client = new ThingiverseClient(clientId, clientSecret, clientCallback);
    String loginUrl = client.loginFirstTime();
    return loginUrl;
  }
 
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
  
  public void logOut()
  {
    client = null;
    deleteAccesToken();
  }
  
  public boolean isLoggedIn()
  {
    return client != null;
  }
  
  /**
   * Gets user name from thingiverse API.
   * Returns "USER NAME" on error.
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
  
  public List<Thing> getMyThings()
  {
    return getMyThings(false, false);
  }
  
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
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return things;
  }
  
  public List<Thing> getLikedThings()
  {
    return getLikedThings(false, false);
  }
  
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
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return things;
  }
  
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
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return collections;
  }
  
  public List<Thing> getThingsByCollection(ThingCollection collection)
  {
    return getThingsByCollection(collection, false, false);
  }
  
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
  
  public List<Thing> search(String query)
  {
    return search(query, false, false);
  }
  
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
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return things;
  }
  
  public List<ThingFile> getFiles(Thing thing)
  {
    return getFiles(thing, false);
  }
  
  public List<ThingFile> getFiles(Thing thing, boolean filterExtensions)
  {
    if (filterExtensions)
    {
      return getFiles(thing, fileExtensionFilter);
    }
    else
    {
      return getFiles(thing, new LinkedList<String>());
    }
  }
  
  /**
   * 
   * @param thing
   * @param allowedFileExtensions Use this to filter the result for specific file
   * extensions like svg or plf. A file is returned if it matches one of the
   * specified extensions. If the filter list is empty, all files are returned.
   * @return 
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
      job.fileExtensionFilter = this.fileExtensionFilter;
      job.tagFilter = this.tagFilter;
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
   * @param fileName
   * @param fileExtensions
   * @return True if the file name ends with on of the given file extensions
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
   * saves it to disk and returns a File object or null.
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
}
