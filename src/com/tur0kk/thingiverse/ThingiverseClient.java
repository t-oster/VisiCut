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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.scribe.exceptions.OAuthException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

/**
 * The original class is taken from https://github.com/frankkienl/FrankkieNL_ThingiverseLib.
 * We extended it by several methods.
 * 
 * @author Patrick Schmidt, frankkie
 */
public class ThingiverseClient {

  private String clientId = "";
  private String clientSecret = "";
  private String clientCallback = "";
  private String accesTokenString = "";
  OAuthService service;

  /**
   * Get instance of ThingClient
   * @param clientId the id of your app, see thingiverse.com
   * @param clientSecret the secret of your app, see thingiverse.com
   * @param clientCallback the callback-url of your app, see thingiverse.com
   */
  @ThingMethod(params = {"clientId", "clientSecret", "clientCallback"})
  public ThingiverseClient(String clientId, String clientSecret, String clientCallback) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.clientCallback = clientCallback;

    //init
    service = new ServiceBuilder().
            provider(ThingiverseAPI.class)
            .apiKey(clientId)
            .apiSecret(clientSecret)
            .callback(clientCallback)
            .build();
  }

   /**
   * Use this when the user has logged in already, and you have the accestoken.
   * The accesToken is used to use further API-calls.
   *
   * @param token
   */
  @ThingMethod(params = {"token"})
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
   * @return accesToken
   */
  @ThingMethod(params = {"code"})
  public String loginWithBrowserCode(String code) throws OAuthException
  {
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
  @ThingMethod(params = {"verb", "url"})
  private String call(Verb verb, String url)
  {
    return call(verb, url, null);
  }

  /**
   * Call api endpoint
   * @param verb http-method to use, like: GET, POST, PUT, DELETE, PATCH
   * @param url the api-url to call
   * @return the output of the api-call, can be a JSON-string
   */
  @ThingMethod(params = {"verb", "url"})
  private String call(Verb verb, String url, String requestBody)
  {
    String urlEnd = url;
    if (!url.startsWith("/"))
    {
      urlEnd = "/" + url;
    }
    OAuthRequest request = new OAuthRequest(verb, "https://api.thingiverse.com" + urlEnd);
    request.addHeader("Authorization", "Bearer " + accesTokenString);

    if (requestBody != null && !requestBody.isEmpty())
    {
      request.addPayload(requestBody);
    }
    
    Response response = request.send();
    return response.getBody();
  }
  
  //USER//
  /**
   * Get information about user, us 'me' to get info about the currently logged in user.
   * @param username the username or 'me'
   * @return information about the user (JSON)
   */
  @ThingMethod(params = {"username"})
  public String user(String username) {
    return call(Verb.GET, "/users/" + username + "/");
  }

  @ThingMethod(params = {"username"})
  public String thingsByUser(String username) {
    return call(Verb.GET, "/users/" + username + "/things");
  }

  @ThingMethod(params = {"username"})
  public String likesByUser(String username) {
    return call(Verb.GET, "/users/" + username + "/likes");
  }

  @ThingMethod(params = {"username"})
  public String copiesByUser(String username) {
    return call(Verb.GET, "/users/" + username + "/copies");
  }

  /**
   * Update the user's profile
   *
   * @param username must be int, get it by calling client.user(me);
   * @param bio Replace bio
   * @param location Replace location
   * @param default_licence One of cc, cc-sa, cc-nd, cc-nc-sa, cc-nc-nd, pd0,
   * gpl, lgpl, bsd.
   * @return The updated user
   */
  @ThingMethod(params = {"username","bio","location","default_licence"})
  public String updateUser(String username, String bio, String location, String default_licence) {
    OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.thingiverse.com/users/" + username + "/");
    request.addHeader("Authorization", "Bearer " + accesTokenString);
    if (bio != null) {
      request.addBodyParameter("bio", bio);
    }
    if (location != null) {
      request.addBodyParameter("location", location);
    }
    if (default_licence != null) {
      request.addBodyParameter("default_licence", default_licence);
    }
    Response response = request.send();
    return response.getBody();
  }

  //THINGS//
  /**
   *
   * @param id
   * @param name
   * @param licence
   * @param category
   * @param description
   * @param instructions
   * @param is_wip boolean as string, 'true', 'false'
   * @param tags comma-separated
   * @return
   */
  @ThingMethod(params = {"id","name","licence","category","description","instructions","is_wip","tags"})
  public String updateThing(String id, String name, String licence, String category, String description, String instructions, String is_wip, String tags) {
    OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.thingiverse.com/things/" + id + "/");
    request.addHeader("Authorization", "Bearer " + accesTokenString);
    if (name != null) {
      request.addBodyParameter("name", name);
    }
    if (licence != null) {
      request.addBodyParameter("licence", licence);
    }
    if (category != null) {
      request.addBodyParameter("category", category);
    }
    if (description != null) {
      request.addBodyParameter("description", description);
    }
    if (instructions != null) {
      request.addBodyParameter("instructions", instructions);
    }
    if (is_wip != null) {
      request.addBodyParameter("is_wip", is_wip);
    }
    if (tags != null) { //eh waitwut? i guess CSV !!
      request.addBodyParameter("tags", name);
    }
    Response response = request.send();
    return response.getBody();
  }

  @ThingMethod(params = {"id"})
  public String thing(String id) {
    return call(Verb.GET, "/things/" + id + "/");
  }

  @ThingMethod(params = {"id"})
  public String imagesBything(String id) {
    return call(Verb.GET, "/things/" + id + "/images/");
  }

  @ThingMethod(params = {"id"})
  public String imageBything(String id, String imageId) {
    return call(Verb.GET, "/things/" + id + "/images/" + imageId);
  }

  @ThingMethod(params = {"id"})
  public String filesByThing(String id) {
    return call(Verb.GET, "/things/" + id + "/files/");
  }
  
  @ThingMethod(params = {"id"})
  public String fileByThing(String id, String fileId) {
    return call(Verb.GET, "/things/" + id + "/files/" + fileId);
  }

  @ThingMethod(params = {"id"})
  public String likesByThing(String id) {
    return call(Verb.GET, "/things/" + id + "/likes/");
  }

  @ThingMethod(params = {"id"})
  public String ancestorsByThing(String id) {
    return call(Verb.GET, "/things/" + id + "/ancestors/");
  }

  @ThingMethod(params = {"id"})
  public String derivaticesByThing(String id) {
    return call(Verb.GET, "/things/" + id + "/derivatices/");
  }

  @ThingMethod(params = {"id"})
  public String tagsByThing(String id) {
    return call(Verb.GET, "/things/" + id + "/tags/");
  }

  @ThingMethod(params = {"id"})
  public String categoryByThing(String id) {
    return call(Verb.GET, "/things/" + id + "/categories/");
  }

  @ThingMethod(params = {"id"})
  public String copiesByThing(String id) {
    return call(Verb.GET, "/things/" + id + "/copies/");
  }

  @ThingMethod(params = {"id"})
  public String likeThing(String id) {
    return call(Verb.POST, "/things/" + id + "/likes");
  }

  @ThingMethod(params = {"id"})
  public String unlikeThing(String id) {
    return call(Verb.DELETE, "/things/" + id + "/likes");
  }

  //COPIES//
  @ThingMethod(params = {"id"})
  public String copies(String id) {
    return call(Verb.GET, "/copies/" + id + "/");
  }

  @ThingMethod(params = {"id"})
  public String imagesByCopy(String id) {
    return call(Verb.GET, "/copies/" + id + "/images");
  }

  @ThingMethod(params = {"id"})
  public String deleteCopy(String id) {
    return call(Verb.DELETE, "/copies/" + id + "/");
  }

  @ThingMethod(params = {"id", "imageFilename"})
  public String newCopy(String id, String imageFilename)
  {
    String body = "{\"filename\": \"" + imageFilename + "\"}";
    return call(Verb.POST, "/things/" + id + "/copies/", body);
  }
  
  //COLLECTIONS//
  /**
   * Update collection
   *
   * @param id
   * @param name
   * @param description
   * @return
   */
  @ThingMethod(params = {"id", "name","description"})
  public String updateCollection(String id, String name, String description) {
    OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.thingiverse.com/collections/" + id + "/");
    request.addHeader("Authorization", "Bearer " + accesTokenString);
    if (name != null) {
      request.addBodyParameter("name", name);
    }
    if (description != null) {
      request.addBodyParameter("description", description);
    }
    Response response = request.send();
    return response.getBody();
  }

  @ThingMethod(params = {"id"})
  public String collection(String id) {
    return call(Verb.GET, "/collections/" + id + "/");
  }
  
  @ThingMethod(params = {"username"})
  public String collectionsByUser(String username) {
    return call(Verb.GET, "/users/" + username + "/collections/");
  }

  @ThingMethod(params = {"id"})
  public String thingsByCollection(String id) {
    return call(Verb.GET, "/collections/" + id + "/things/");
  }

  @ThingMethod(params = {"name","description"})
  public String newCollection(String name, String description) {
    if (description == null) {
      description = "";
    }
    if (name == null || "".equals(name)) {
      throw new RuntimeException("name is not optional, http://www.thingiverse.com/developers/rest-api-reference");
    }
    //return call(Verb.POST, "/collections/");
    OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.thingiverse.com/collections/");
    request.addHeader("Authorization", "Bearer " + accesTokenString);
    request.addBodyParameter("description", description);
    Response response = request.send();
    return response.getBody();
  }

  /**
   * Add thing to collection.
   * http://www.thingiverse.com/developers/rest-api-reference
   *
   * @param collectionId
   * @param thingId
   * @param description Optional. Reason for ading the Thing
   * @return response
   */
  @ThingMethod(params = {"collectionId","thingId","description"})
  public String addThingToCollection(String collectionId, String thingId, String description) {
    if (description == null) {
      description = "";
    }
    OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.thingiverse.com/collections/" + collectionId + "/things/" + thingId);
    request.addHeader("Authorization", "Bearer " + accesTokenString);
    request.addBodyParameter("description", description);
    Response response = request.send();
    return response.getBody();
  }

  @ThingMethod(params = {"collectionId","thingId"})
  public String removeThingFromCollection(String collectionId, String thingId) {
    return call(Verb.DELETE, "/collections/" + collectionId + "/things/" + thingId);
  }

  @ThingMethod(params = {"id"})
  public String removeCollection(String id) {
    return call(Verb.DELETE, "/collections/" + id);
  }
  //OTHER//

  public String newest() {
    return call(Verb.GET, "/newest/");
  }

  public String popular() {
    return call(Verb.GET, "/popular/");
  }

  public String featured() {
    return call(Verb.GET, "/featured/");
  }

  @ThingMethod(params = {"term"})
  public String search(String term) {
    term = term.replaceAll(" ", "+"); //basic url-encode // TODO fix this
    return call(Verb.GET, "/search/" + term + "/");
  }

  //CATEGORIES//
  /**
   * List all categories
   *
   * @return list of all categories
   */
  public String categories() {
    return call(Verb.GET, "/categories/");
  }

  /**
   * Get details about category. Category ids are normalized "slugs". For
   * example, the id for the "Automotive" category's id would be "automotive".
   * The "Replacement Parts" category would have an id of "replacement-parts",
   * etc.
   *
   * @param id
   * @return details
   */
  @ThingMethod(params = {"id"})
  public String category(String id) {
    return call(Verb.GET, "/categories/" + id + "/");
  }

  @ThingMethod(params = {"id"})
  public String latestThingsByCategory(String id) {
    return call(Verb.GET, "/categories/" + id + "/things");
  }

  @ThingMethod(params = {"id"})
  public String latestThingsByTag(String id) {
    return call(Verb.GET, "/tags/" + id + "/things");
  }

  /**
   * Get all tags
   *
   * @return list of tags
   */
  public String tags() {
    return call(Verb.GET, "/tags/");
  }

  /**
   * details about a tag
   *
   * @return details
   */
  @ThingMethod(params = {"id"})
  public String tag(String id) {
    return call(Verb.GET, "/tags/" + id + "/");
  }
  
  public String likedThings() {
    return call(Verb.GET, "/users/me/likes/");
  }
  
  public String downloadTextFile  (String url) {
    OAuthRequest request = new OAuthRequest(Verb.GET, url);
    request.addHeader("Authorization", "Bearer " + accesTokenString);
    Response response = request.send();
    response = followRedirects(response, 2);
    
    return response.getBody();
  }
  /**
   * Download a binary file.
   * @param url
   * @param outFile
   * @param authorizationRequired Set to true to send the OAuth access token
   * with each request.
   * @return Success?
   * @throws IOException 
   */
  public boolean downloadBinaryFile(String url, File outFile, boolean authorizationRequired) throws IOException
  {
    // Perform http request
    OAuthRequest request = new OAuthRequest(Verb.GET, url);
    
    if (authorizationRequired)
    {
      request.addHeader("Authorization", "Bearer " + accesTokenString);
    }
    
    // Quick and dirty solution:
    // Fake user-agent. Otherwise thingiverse may give us 403 Forbidden.
    request.addHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:34.0) Gecko/20100101 Firefox/34.0");
    
    Response response = request.send();
    response = followRedirects(response, 2);

    if (!response.isSuccessful())
    {
      return false;
    }
    
    // Save binary contents to file
    InputStream inputStream = response.getStream();
    OutputStream outputStream = new FileOutputStream(outFile);
    
    byte[] buffer = new byte[4096];
    int n;
    while ((n = inputStream.read(buffer)) != -1)
    {
      outputStream.write(buffer, 0, n);
    }
    outputStream.close();
    
    return true;
  }
  
  private Response followRedirects(Response response, int maxRedirects)
  {
    if (maxRedirects < 1 || response.getCode() != 302)
    {
      return response;
    }
    
    // Follow redirect once
    OAuthRequest redirectRequest = new OAuthRequest(Verb.GET, response.getHeader("Location"));
    Response redirectResponse = redirectRequest.send();
    
    // Recursively follow redirects until success or max tries exceeded
    return followRedirects(redirectResponse, maxRedirects - 1);
  }
}
