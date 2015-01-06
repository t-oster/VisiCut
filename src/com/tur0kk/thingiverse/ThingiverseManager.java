package com.tur0kk.thingiverse;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.app.beans.SVGIcon;
import com.t_oster.visicut.misc.Helper;
import com.tur0kk.thingiverse.model.Thing;
import com.tur0kk.thingiverse.model.ThingFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Singleton class managing all the communication with the Thingiverse API.
 * @author Patrick
 */
public class ThingiverseManager
{
  // App name: VisiCutThingiverse
  private static final String clientId = "0d3b8166624f6d05b738";
  private static final String clientSecret = "a0b5368a3d58ddb3b1ade12f4f8f14e7";
  private static final String clientCallback = "http://www.thingiverse.com";
  
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
    
    if (browserCode == null || browserCode.isEmpty())
    {
      logOut();
      
      System.out.println("Login failed!");
      return;
    }
    
    String accessToken = client.loginWithBrowserCode(browserCode);

    if (accessToken == null || accessToken.isEmpty())
    {
      logOut();
      
      System.out.println("Login failed!");
      return;
    }
    
    saveAccessToken(accessToken);
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
  
  public LinkedList<Thing> getMyThings()
  {
    LinkedList<Thing> things = new LinkedList<Thing>();
    
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
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return things;
  }
  
  public LinkedList<Thing> search(String query)
  {
    LinkedList<Thing> things = new LinkedList<Thing>();
    
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
        things.add(new Thing(itemId, itemName, imageUrl));
      }
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return things;
  }
  
  public LinkedList<ThingFile> getSvgFiles(Thing thing)
  {
    LinkedList<ThingFile> files = new LinkedList<ThingFile>();
    
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
        
        if (fileName.toLowerCase().endsWith("svg"))
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
   * Downloads an svg from thingiverse (or disk),
   * saves it to disk and returns a File object or null.
   */
  public File downloadSvgFile(ThingFile thingFile)
  {
    File file = null;
    
    try
    {
      // We save all svgs to disk and only download them only if not yet present.
      // TODO: Replace if newer version available!
      File folder = new File(Helper.getBasePath(),
                             "thingiverse/svg" +
                             thingFile.getThing().getId());
      folder.mkdirs();
      file = new File(folder, thingFile.getName());
      
      if (!file.exists())
      {
        file.createNewFile();

        // Get svg content as string
        String svgString = client.downloadSvg(thingFile.getUrl());
        
        // Write to disk
        PrintWriter out = new PrintWriter(file);
        out.print(svgString);
        out.close();
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    
    return file;
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
