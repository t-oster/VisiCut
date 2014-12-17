package com.tur0kk.thingiverse;

import java.awt.Desktop;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.thymeleaf.expression.Strings;

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
  
  public void logIn()
  {
    try
    {
      client = new ThingiverseClient(clientId, clientSecret, clientCallback);
      String authUrl = client.loginFirstTime();

      // Start os default browser
      Desktop.getDesktop().browse(URI.create(authUrl));

      String browserCode = javax.swing.JOptionPane.showInputDialog("Log in with your Thingiverse-account, click allow, paste code here:");
      String accessTokenString = client.loginWithBrowserCode(browserCode);
      
      assert(accessTokenString != null);
      assert(!accessTokenString.isEmpty());
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      client = null;
    }
  }
  
  public void logOut()
  {
    client = null;
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
      
      System.out.println(url);
      
      return url;
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      return "";
    }
  }
  
  public Map<String, ImageIcon> getMyThings()
  {
    Map<String, ImageIcon> result = new HashMap<String, ImageIcon>();
    
    try
    {
      String json = client.thingsByUser("me");
      
      JSONParser parser = new JSONParser();
      JSONArray array = (JSONArray)parser.parse(json);
      for (Object obj : array)
      {
        JSONObject item = (JSONObject)obj;
        String itemName = item.get("name").toString();
        String imageUrl = item.get("thumbnail").toString();
        result.put(itemName, new ImageIcon(new URL(imageUrl)));
      }
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return result;
  }
  
  public Map<String, ImageIcon> getFeatured()
  {
    Map<String, ImageIcon> result = new HashMap<String, ImageIcon>();
    
    try
    {
      String json = client.featured();
      
      JSONParser parser = new JSONParser();
      JSONArray array = (JSONArray)parser.parse(json);
      for (Object obj : array)
      {
        JSONObject item = (JSONObject)obj;
        String itemName = item.get("name").toString();
        String imageUrl = item.get("thumbnail").toString();
        result.put(itemName, new ImageIcon(new URL(imageUrl)));
      }
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    
    return result;
  }
}