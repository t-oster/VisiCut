/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk.thingiverse;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 *
 * @author Sven
 */

public class ThingiverseManager
{
  public static String clientId = "1e0845ef6c97aed09214";
  public static String clientSecret = "d7b6920da341cdeb063262c09ee402e3";
  public static String clientCallback = "http://frankkie.nl/thingiverse/api.php";
  
  private static ThingiverseManager instance;
  private ThingiverseClient client;
  
  public ThingiverseManager()
  {
    if (instance != null){
      System.err.println("ThingiverseManager should not be instanciated directly");
    }
    
    client = new ThingiverseClient(clientId, clientSecret, clientCallback);
    String accessTokenString;
    String authUrl = client.loginFirstTime();
    try {
      //start the browser
      Desktop.getDesktop().browse(URI.create(authUrl));
    } 
    catch (IOException ex) {
      System.err.println("Browser does not work.");
    }
      String browserCode = javax.swing.JOptionPane.showInputDialog("Log in with your Thingiverse-account, click allow, paste code here:");
      accessTokenString = client.loginWithBrowserCode(browserCode);
    }
    
    //Do some API-calls
    //these are only visible in the commandline
    System.out.println("Featured:");
    String featured = client.featured();
    System.out.println(featured);
    System.out.println("Newest:");
    String newest = client.newest();
    System.out.println(newest);
    System.out.println("Me:");
    String me = client.user("me");
    System.out.println(me);
  }
  
  public static ThingiverseManager getInstance()
  {
    if (instance == null)
    {
      instance = new ThingiverseManager();
    }
    return instance;
  }
}
