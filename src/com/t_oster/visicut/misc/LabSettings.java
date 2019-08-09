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
package com.t_oster.visicut.misc;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@upstart-it.de>
 */
public class LabSettings
{

  public final String name;
  public final String URL;
  public final String[] domain;
  public final String[] ssid;

  public LabSettings(String name, String URL, String[] domain, String[] ssid)
  {
    this.name = name;
    this.URL = URL;
    this.domain = domain != null ? domain : new String[0];
    this.ssid = ssid != null ? ssid : new String[0];
  }

  public LabSettings(String name, String URL)
  {
    this(name, URL, null, null);
  }

  public boolean acceptsHostname(String hostname) {
    return Arrays.stream(domain).anyMatch(hostname::endsWith);
  }
  
  public boolean acceptsSSID(String ssid) {
    return Arrays.stream(this.ssid).anyMatch(ssid::equals);
  }
  
  public static List<LabSettings> get()
  {
    List<LabSettings> result = new LinkedList<>();
    // Want your lab in this list? Look at https://github.com/t-oster/VisiCut/wiki/How-to-add-default-settings-for-your-lab !
    // result.add(new LabSettings("Country, City: Institution", "https://example.org/foo.zip"));
    // if you have a local domain name in your network or a wifi SSID, you can add them too, so if you are in your lab
    // your settings will be suggested
    // The list is sorted alphabetically.
    result.add(new LabSettings("China, Hong Kong: Renaissance College Hong Kong", "https://github.com/RCHK-DT/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("France, Chemillé en Anjou : FabLab le Boc@l", "https://github.com/bocal-chemille/Visicut/raw/master/config_laser_bocal.vcsettings"));
    result.add(new LabSettings("France, Le Mans: HAUM Hackerspace", "https://github.com/haum/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Germany, Aachen: FabLab RWTH Aachen", "https://github.com/renebohne/zing6030-visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Germany, Berlin: Fab Lab Berlin", "https://github.com/FabLabBerlin/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Germany, Bremen: FabLab Bremen e.V.", "http://www.fablab-bremen.org/FabLab_Bremen.vcsettings"));
    result.add(new LabSettings("Germany, Dresden: Konglomerat e.V.", "https://github.com/konglomerat/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Germany, Dresden: Makerspace Dresden", "https://github.com/Makerspace-Dresden/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Germany, Erlangen: FAU FabLab", "https://github.com/fau-fablab/visicut-settings/archive/master.zip", new String[]{".fau.de", ".uni-erlangen.de"}, null));
    result.add(new LabSettings("Germany, Heidelberg: Heidelberg Makerspace", "https://github.com/heidelberg-makerspace/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Germany, Nuremberg: Fab lab Region Nürnberg e.V.", "https://github.com/fablabnbg/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Germany, Paderborn: FabLab Paderborn e.V.", "https://github.com/fablab-paderborn/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Germany, Veitsbronn: FabLab Landkreis Fürth e.V.", "https://github.com/falafue/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Netherlands, Amersfoort: FabLab", "https://github.com/Fablab-Amersfoort/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("Netherlands, Enschede: TkkrLab", "https://github.com/TkkrLab/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("United Kingdom, Leeds: Hackspace", "https://github.com/leedshackspace/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("United Kingdom, Manchester: Hackspace", "https://github.com/hacmanchester/visicut-settings/archive/master.zip"));
    result.add(new LabSettings("United States, Seattle: Fremont Hangar", "https://github.com/hghile/visicut-settings/archive/master.zip", null, new String[]{"WL-seattle-maker-space"}));
    return result;
  }
}
