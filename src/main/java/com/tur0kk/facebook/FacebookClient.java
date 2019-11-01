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
package com.tur0kk.facebook;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FacebookApi;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

/**Handles native REST communication with facebook api, using scribe library
 * @author Sven
 */
public class FacebookClient
{
  // web app specific properties for identification, set by FacebookManager
  private String clientId = "";
  private String clientSecret = "";
  private String clientCallback = "";
  private String accesTokenString = "";
  OAuthService service;
  
  /**
   * Get instance of Facebook
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
      .scope("publish_actions") // request publish rights
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
    OAuthRequest request = new OAuthRequest(verb, "https://graph.facebook.com/v2.2" + urlEnd);
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
  
  public String userPicture() {
    return call(Verb.GET, "/me/picture?redirect=0&height=100&type=normal&width=100");
  }
  
  /*
   * posts an image to the users news feed
   * @param message to show
   * @param image as form data
   * @return the new image id if successful
   */
  public String publishPicture(String msg, Image image, String placeId) throws IOException {
    OAuthRequest request = new OAuthRequest(Verb.POST, "https://graph.facebook.com/v2.2/me/photos"); // request node
    request.addHeader("Authorization", "Bearer " + accesTokenString); // authentificate
    
    // check input to avoid error responses
    if(msg != null && image != null){      
      // facebook requires multipart post structure
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.addTextBody("message", msg); // description
      
      if(placeId != null && !"".equals(placeId)){
        builder.addTextBody("place", placeId); // add link to FabLab site if property is set in preferences
      }
      
      // convert image to bytearray and append to multipart
      BufferedImage bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      Graphics2D bGr = bimage.createGraphics();
      bGr.drawImage(image, 0, 0, null);
      bGr.dispose();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(bimage, "png", baos);
      builder.addBinaryBody(msg, baos.toByteArray(), ContentType.MULTIPART_FORM_DATA, "test.png");
      
      // generate multipart byte stream and add to payload of post package
      HttpEntity multipart = builder.build();
      ByteArrayOutputStream multipartOutStream = new ByteArrayOutputStream((int)multipart.getContentLength());
      multipart.writeTo(multipartOutStream);
      request.addPayload(multipartOutStream.toByteArray());
      
      // set header of post package
      Header contentType = multipart.getContentType();
      request.addHeader(contentType.getName(), contentType.getValue());
      
      // send and response answer
      Response response = request.send();
      return response.getBody();
    }else{
      throw new RuntimeException("message and image needed");
    }
  }
}
