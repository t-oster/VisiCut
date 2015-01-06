/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk.facebook;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FacebookApi;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

/**
 *
 * @author Sven
 */
public class FacebookClient
{
  private String clientId = "";
  private String clientSecret = "";
  private String clientCallback = "";
  private String accesTokenString = "";
  OAuthService service;
  
  /**
   * Get instance of ThingClient
   * @param clientId the id of your app
   * @param clientSecret the secret of your app
   * @param clientCallback the callback-url of your app
   */
  public FacebookClient(String clientId, String clientSecret, String clientCallback) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.clientCallback = clientCallback;

    //init
    service = new ServiceBuilder().
            provider(FacebookApi.class)
            .apiKey(clientId)
            .apiSecret(clientSecret)
            .callback(clientCallback)
            .build();
  }
  
  /**
   * Use this when the user has logged in already, and you have the accesstoken.
   * The accesToken is used to use further API-calls.
   *
   * @param token
   */
  public void loginWithAccesToken(String token) {
    this.accesTokenString = token;
  }
  
  /**
   * Use this when the user logs in for the first time, and you do not have an
   * access-token. The user needs to login at the given URL. Get the code from the
   * browser.
   *
   * @return url where the user must login.
   */
  public String loginFirstTime() {
    String authUrl = service.getAuthorizationUrl(null);
    return authUrl;
  }
  
  /**
   * Use this when the user has a code from the browser. This browser-code can
   * be exchanged for a accesToken. The accesToken is used to use further
   * API-calls.
   *
   * @param code the code from the browser
   * @return accessToken
   */
  public String loginWithBrowserCode(String code) {
    Verifier v = new Verifier(code);
    Token accessToken = service.getAccessToken(null, v);
    accesTokenString = accessToken.getToken();
    return accesTokenString;
  }
  
  /**
   * Call api endpoint
   * @param verb http-method to use, like: GET, POST, PUT, DELETE, PATCH
   * @param url the api-url to call
   * @return the output of the api-call, can be a JSON-string
   */
  private String call(Verb verb, String url) {
    String urlEnd = url;
    if (!url.startsWith("/")) {
      urlEnd = "/" + url;
    }
    OAuthRequest request = new OAuthRequest(verb, "https://graph.facebook.com" + urlEnd);
    request.addHeader("Authorization", "Bearer " + accesTokenString);
    Response response = request.send();
    return response.getBody();
  }
  
  //USER//
  /**
   * Get information about user, us 'me' to get info about the currently logged in user.
   * @param username the username or 'me'
   * @return information about the user (JSON)
   */
  public String user() {
    return call(Verb.GET, "/me");
  }
  
}
