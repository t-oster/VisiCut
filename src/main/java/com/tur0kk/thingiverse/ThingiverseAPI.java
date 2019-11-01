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