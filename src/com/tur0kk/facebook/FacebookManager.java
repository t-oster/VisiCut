/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk.facebook;

import com.t_oster.visicut.misc.Helper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Properties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Singleton class managing all the communication with the Facebook API.
 * @author Sven
 */
public class FacebookManager
{
  // App name: VisiCutFacebook
  private static final String clientId = "521766611285565";
  private static final String clientSecret = "8188c6a387ceaf7aa7919112190e22d8";
  private static final String clientCallback = "http://hci.rwth-aachen.de/visicut";
  
  private static FacebookManager instance = null;
  
  /**
   * The client object is available iff a user is currently logged in.
   */
  private FacebookClient client = null;
  
  public FacebookManager(){
    
  }
  
  public static FacebookManager getInstance()
  {
    if (instance == null)
    {
      instance = new FacebookManager();
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
    
    client = new FacebookClient(clientId, clientSecret, clientCallback);
    client.loginWithAccesToken(accessToken);
    return true;
  }
  
  /**
   * Logs out the current user and starts the authentication procedure.
   * @return Login URL
   */
  public String initiateAuthentication()
  {
    client = new FacebookClient(clientId, clientSecret, clientCallback);
    String loginUrl = client.loginFirstTime();
    return loginUrl;
  }
  
  public void logIn(String browserCode)
  {
    if (client == null)
    {
      client = new FacebookClient(clientId, clientSecret, clientCallback);
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
   * Gets user name from facebook API.
   * Returns "USER NAME" on error.
   */
  public String getUserName()
  {
    try
    {
      String json = client.user();
      
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
   * Save access token to file.
   */
  private void saveAccessToken(String accessToken)
  {
    try
    {
      File facebookFolder = new File(Helper.getBasePath(), "facebook");
      facebookFolder.mkdirs();

      File sessionFile = new File(facebookFolder, "session.properties");
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
      File facebookFolder = new File(Helper.getBasePath(), "facebook");
      if (!facebookFolder.exists())
      {
        return result;
      }

      File sessionFile = new File(facebookFolder, "session.properties");
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
      File facebookFolder = new File(Helper.getBasePath(), "facebook");
      if (!facebookFolder.exists())
      {
        return;
      }

      File sessionFile = new File(facebookFolder, "session.properties");
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
