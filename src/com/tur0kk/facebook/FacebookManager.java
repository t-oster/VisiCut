/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk.facebook;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.misc.Helper;
import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Singleton class managing all the communication with the Facebook API.
 * @author Sven
 */
public class FacebookManager
{
  // App name: VisiCut
  private static final String clientId = "521766611285565";
  private static final String clientSecret = "8188c6a387ceaf7aa7919112190e22d8";
  private static final String clientCallback = "http://hci.rwth-aachen.de/public/VisiCut/show_code.php";
  private static final String redirectUrlPrefix = "http://hci.rwth-aachen.de/public/VisiCut/show_code.php?code=";
  
  private static FacebookManager instance = null;
  
  /**
   * The client object is available iff a user is currently logged in.
   */
  private FacebookClient client = null;
  
  public FacebookManager(){
    //fablabFacebookPageId = PreferencesManager.getInstance().getPreferences().getFabLabFacebookLocationID();
  }
  
  public static FacebookManager getInstance()
  {
    if (instance == null)
    {
      instance = new FacebookManager();
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
   * Gets user avatar url from facebook API.
   */
  public String getUserImage()
  {
    try
    {
      String json = client.userPicture();
      
      
      JSONParser parser = new JSONParser();
      JSONObject dataObj = (JSONObject)parser.parse(json);
      String data = dataObj.get("data") .toString();
      JSONObject urlObject = (JSONObject)parser.parse(data);
      String url = urlObject.get("url").toString();
      
      return url;
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      return "";
    }
  }
  
  
  /*
   * publishs an project image to the users news feed
   * @param message to display
   * @param image to display
   * @return true iff successful
   */
  public boolean publishProject(String message, Image icon){
    try{
      // add fixed text
      message = "Look what I made with VisiCut:\n" + message;
      
      // get Fablab ID
      String fablabId = this.getFabLabLocationFacebookId();
      
      // post image
      String json = client.publishPicture(message, icon, fablabId);
      
      // evaluate success
      JSONParser parser = new JSONParser();
      JSONObject dataObj = (JSONObject)parser.parse(json);
      String id = (String) dataObj.get("id");
      
      if(id == null){
        MainView.getInstance().getDialog().showErrorMessage("Facebook has not enough permissions");
        return false;
      }
      
      return true;
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      return false;
    }
  }
  
  /*
   * searches for FabLab Aachen place and returns id
   */
  public String getFabLabLocationFacebookId(){
    return VisicutModel.getInstance().getPreferences().getFabLabLocationFacebookId();
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
