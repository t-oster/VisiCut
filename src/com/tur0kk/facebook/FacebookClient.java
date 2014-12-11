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

/**
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
      .scope("publish_actions")
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
    OAuthRequest request = new OAuthRequest(Verb.POST, "https://graph.facebook.com/v2.2/me/photos");
    request.addHeader("Authorization", "Bearer " + accesTokenString);
    
    if(msg != null && image != null){      
      // multipart post structure
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.addTextBody("message", msg);
      
      if(placeId != ""){
        builder.addTextBody("place", placeId);
      }
      
      BufferedImage bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      Graphics2D bGr = bimage.createGraphics();
      bGr.drawImage(image, 0, 0, null);
      bGr.dispose();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(bimage, "png", baos);
      builder.addBinaryBody(msg, baos.toByteArray(), ContentType.MULTIPART_FORM_DATA, "test.png");
      HttpEntity multipart = builder.build();
      
      ByteArrayOutputStream multipartOutStream = new ByteArrayOutputStream((int)multipart.getContentLength());
      multipart.writeTo(multipartOutStream);
      request.addPayload(multipartOutStream.toByteArray());
      
      Header contentType = multipart.getContentType();
      request.addHeader(contentType.getName(), contentType.getValue());
      
      Response response = request.send();
      return response.getBody();
    }else{
      throw new RuntimeException("message and image needed");
    }
  }
}
