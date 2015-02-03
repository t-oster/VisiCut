package com.tur0kk.thingiverse;

import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;

/**
 * https://github.com/frankkienl/FrankkieNL_ThingiverseLib
 * @author frankkie
 */
public class ThingiverseAPI extends org.scribe.builder.api.DefaultApi20 {

  private static final String AUTHORIZE_URL = "http://www.thingiverse.com/login/oauth/authorize?client_id=%s";

  @Override
  public Verb getAccessTokenVerb() {
    return Verb.POST;
  }
  
  @Override
  public String getAccessTokenEndpoint() {
    //http or https ??
    return "http://www.thingiverse.com/login/oauth/access_token";
  }

  @Override
  public String getAuthorizationUrl(OAuthConfig oac) {
    return String.format(AUTHORIZE_URL, oac.getApiKey());
  } 
}