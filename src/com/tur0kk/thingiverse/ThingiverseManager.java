package com.tur0kk.thingiverse;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * Singleton class managing all the communication with the Thingiverse API.
 * @author patrick
 */
public class ThingiverseManager
{
  // App name: VisiCutThingiverse
  private static final String clientId = "0d3b8166624f6d05b738";
  private static final String clientSecret = "a0b5368a3d58ddb3b1ade12f4f8f14e7";
  private static final String clientCallback = "http://hci.rwth-aachen.de/visicut";
  
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
    client = new ThingiverseClient(clientId, clientSecret, clientCallback);
    String accessTokenString;
    String authUrl = client.loginFirstTime();
    try
    {
      //start the browser
      Desktop.getDesktop().browse(URI.create(authUrl));
    } 
    catch (IOException ex)
    {
      System.err.println("Browser does not work.");
    }

    String browserCode = javax.swing.JOptionPane.showInputDialog("Log in with your Thingiverse-account, click allow, paste code here:");
    accessTokenString = client.loginWithBrowserCode(browserCode);
    
    // Do some API-calls
    // these are only visible in the commandline
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
  
  public void logOut()
  {
   // throw new NotImplementedException();
  }
}